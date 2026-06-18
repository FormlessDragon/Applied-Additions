package com.formlesslab.ae2additions.tile;

import ae2.api.config.*;
import ae2.api.inventories.ISegmentedInventory;
import ae2.api.inventories.InternalInventory;
import ae2.api.inventories.ItemTransfer;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.energy.IEnergySource;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.orientation.BlockOrientation;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.container.ISubGui;
import ae2.core.definitions.AEItems;
import ae2.core.settings.TickRates;
import ae2.helpers.IOutputSideConfigHost;
import ae2.tile.grid.AENetworkedPoweredTile;
import ae2.util.ConfigManager;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.CombinedInternalInventory;
import ae2.util.inv.FilteredInternalInventory;
import ae2.util.inv.filter.IAEItemFilter;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.ModGuiHandler;
import com.formlesslab.ae2additions.block.reaction.BlockReactionChamber;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.recipe.ReactionChamberRecipe;
import com.formlesslab.ae2additions.recipe.ReactionChamberRecipes;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.FluidTankProperties;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;

public class TileReactionChamber extends AENetworkedPoweredTile
        implements IGridTickable, IUpgradeableObject, IConfigurableObject, ISegmentedInventory, IOutputSideConfigHost {
    public static final int INPUT_SLOTS = 9;
    public static final int MAX_PROCESSING_TIME = 200;
    public static final int TANK_CAPACITY = Fluid.BUCKET_VOLUME * 16;
    public static final ResourceLocation INV_MAIN = ModContent.id("reaction_chamber");

    private final AppEngInternalInventory input = new AppEngInternalInventory(this, INPUT_SLOTS, 64, new InputFilter());
    private final AppEngInternalInventory output = new AppEngInternalInventory(this, 1, 64);
    private final InternalInventory inventory = new CombinedInternalInventory(this.input, this.output);
    private final InternalInventory exposedInventory = new CombinedInternalInventory(
            new FilteredInternalInventory(this.input, new InputFilter()),
            new FilteredInternalInventory(this.output, new OutputFilter()));
    private final FluidTank outputTank = new SyncedTank(TANK_CAPACITY, this::onOutputTankChanged);
    private final IFluidHandler fluidHandler = new FluidHandler();
    private final ConfigManager configManager = new ConfigManager(this::onConfigChanged);
    private final EnumMap<EnumFacing, ItemTransfer> neighbors = new EnumMap<>(EnumFacing.class);
    private final EnumSet<EnumFacing> outputSides = EnumSet.allOf(EnumFacing.class);
    private int processingTime;
    private boolean working;
    private boolean powered;
    @Nullable
    private ReactionChamberRecipe cachedTask;
    private final FluidTank inputTank = new SyncedTank(TANK_CAPACITY, this::onInputTankChanged);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            Item.getItemFromBlock(ModContent.REACTION_CHAMBER), 5, this::onUpgradesChanged);

    public TileReactionChamber() {
        this.setInternalMaxPower(500000);
        this.setPowerSides(this.getGridConnectableSides(this.getOrientation()));
        this.getMainNode().setIdlePowerUsage(0).addService(IGridTickable.class, this);
        this.configManager.registerSetting(Settings.AUTO_EXPORT, YesNo.NO);
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(ModContent.REACTION_CHAMBER);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.COVERED;
    }

    @Override
    protected void onOrientationChanged(BlockOrientation orientation) {
        super.onOrientationChanged(orientation);
        this.setPowerSides(this.getGridConnectableSides(orientation));
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inventory;
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(EnumFacing side) {
        return this.exposedInventory;
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (ISegmentedInventory.UPGRADES.equals(id)) {
            return this.upgrades;
        }
        if (ISegmentedInventory.STORAGE.equals(id) || INV_MAIN.equals(id)) {
            return this.inventory;
        }
        return null;
    }

    @Override
    public void onReady() {
        super.onReady();
        this.updateNeighbors();
    }

    public void updateNeighbors() {
        if (this.world == null) {
            return;
        }
        this.neighbors.clear();
        for (EnumFacing side : EnumFacing.values()) {
            ItemTransfer target = InternalInventory.wrapExternal(this.world, this.pos.offset(side), side.getOpposite());
            if (target != null) {
                this.neighbors.put(side, target);
            }
        }
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.input.writeToNBT(data, "input");
        this.output.writeToNBT(data, "output");
        NBTTagCompound inputTankTag = new NBTTagCompound();
        this.inputTank.writeToNBT(inputTankTag);
        data.setTag("inputTank", inputTankTag);
        NBTTagCompound outputTankTag = new NBTTagCompound();
        this.outputTank.writeToNBT(outputTankTag);
        data.setTag("outputTank", outputTankTag);
        this.upgrades.writeToNBT(data, "upgrades");
        this.configManager.writeToNBT(data);
        data.setInteger("processingTime", this.processingTime);
        data.setInteger("outputSides", this.encodeOutputSides());
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.input.readFromNBT(data, "input");
        this.output.readFromNBT(data, "output");
        this.inputTank.readFromNBT(data.getCompoundTag("inputTank"));
        this.outputTank.readFromNBT(data.getCompoundTag("outputTank"));
        this.upgrades.readFromNBT(data, "upgrades");
        this.configManager.readFromNBT(data);
        this.processingTime = data.getInteger("processingTime");
        this.decodeOutputSides(data.hasKey("outputSides") ? data.getInteger("outputSides") : 0x3F);
        this.cachedTask = null;
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        data.writeBoolean(this.working);
        data.writeBoolean(this.isPowered());
        data.writeShort(this.processingTime);
        NBTTagCompound inputTankTag = new NBTTagCompound();
        this.inputTank.writeToNBT(inputTankTag);
        ByteBufUtils.writeTag(data, inputTankTag);
        NBTTagCompound outputTankTag = new NBTTagCompound();
        this.outputTank.writeToNBT(outputTankTag);
        ByteBufUtils.writeTag(data, outputTankTag);
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        boolean oldWorking = this.working;
        boolean oldPowered = this.powered;
        int oldProcessing = this.processingTime;
        this.working = data.readBoolean();
        this.powered = data.readBoolean();
        this.processingTime = data.readUnsignedShort();
        NBTTagCompound inputTankTag = ByteBufUtils.readTag(data);
        this.inputTank.readFromNBT(inputTankTag == null ? new NBTTagCompound() : inputTankTag);
        NBTTagCompound outputTankTag = ByteBufUtils.readTag(data);
        this.outputTank.readFromNBT(outputTankTag == null ? new NBTTagCompound() : outputTankTag);
        return changed || oldWorking != this.working || oldPowered != this.powered
                || oldProcessing != this.processingTime;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            this.markForUpdate();
        }
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        for (ItemStack upgrade : this.upgrades) {
            if (!upgrade.isEmpty()) {
                drops.add(upgrade.copy());
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.upgrades.clear();
        this.inputTank.setFluid(null);
        this.outputTank.setFluid(null);
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        this.cachedTask = null;
        this.processingTime = 0;
        this.saveChanges();
        this.wake();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(TickRates.Inscriber, !this.hasCraftWork() && !this.hasAutoExportWork());
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.pushOutResult()) {
            return TickRateModulation.URGENT;
        }

        ReactionChamberRecipe task = this.getTask();
        if (task == null || this.getParallelRuns(task) <= 0) {
            this.setWorking(false);
            this.processingTime = 0;
            return this.hasAutoExportWork() ? TickRateModulation.SLOWER : TickRateModulation.SLEEP;
        }

        int speedFactor = switch (this.upgrades.getInstalledUpgrades(AEItems.SPEED_CARD.item())) {
            case 1 -> 3;
            case 2 -> 5;
            case 3 -> 10;
            case 4 -> 50;
            default -> 2;
        };
        int powerConsumption = 10 * speedFactor;
        IEnergySource source = this.selectEnergySource(powerConsumption);
        if (source != null
                && source.extractAEPower(powerConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG)
                > powerConsumption - 0.01) {
            source.extractAEPower(powerConsumption, Actionable.MODULATE, PowerMultiplier.CONFIG);
            this.processingTime = Math.min(MAX_PROCESSING_TIME, this.processingTime + speedFactor);
            this.setWorking(true);
            this.saveChanges();
        }

        if (this.processingTime >= MAX_PROCESSING_TIME) {
            this.finishRecipe(task);
        }

        return TickRateModulation.URGENT;
    }

    @Nullable
    private IEnergySource selectEnergySource(double powerConsumption) {
        double internal = this.extractAEPower(powerConsumption, Actionable.SIMULATE, PowerMultiplier.CONFIG);
        if (internal > powerConsumption - 0.01) {
            return this;
        }
        var grid = this.getMainNode().getGrid();
        if (grid != null) {
            return grid.getEnergyService();
        }
        return internal > 0 ? this : null;
    }

    private void finishRecipe(ReactionChamberRecipe task) {
        int runs = this.getParallelRuns(task);
        if (runs <= 0) {
            this.processingTime = 0;
            return;
        }
        if (task.isItemOutput()) {
            ItemStack output = task.getItemOutput();
            output.setCount(output.getCount() * runs);
            this.output.insertItem(0, output, false);
        } else {
            FluidStack output = task.getFluidOutput();
            output.amount *= runs;
            this.outputTank.fill(output, true);
        }
        task.consume(this.input, this.inputTank, runs);
        this.processingTime = 0;
        this.cachedTask = null;
        this.setWorking(false);
        this.saveChanges();
    }

    private boolean hasCraftWork() {
        ReactionChamberRecipe task = this.getTask();
        if (task != null) {
            return this.getParallelRuns(task) > 0;
        }
        this.processingTime = 0;
        return this.working;
    }

    private boolean hasAutoExportWork() {
        return (!this.output.getStackInSlot(0).isEmpty() || this.outputTank.getFluidAmount() > 0)
                && this.configManager.getSetting(Settings.AUTO_EXPORT) == YesNo.YES;
    }

    @Nullable
    public ReactionChamberRecipe getTask() {
        if (this.cachedTask == null) {
            this.cachedTask = ReactionChamberRecipes.findRecipe(this.input, this.inputTank,
                    this.output.getStackInSlot(0), this.outputTank, TANK_CAPACITY);
        }
        return this.cachedTask;
    }

    private int getParallelLimit() {
        return switch (this.upgrades.getInstalledUpgrades(AEItems.PARALLEL_CARD.item())) {
            case 1 -> 4;
            case 2 -> 16;
            case 3 -> 64;
            default -> 1;
        };
    }

    private int getParallelRuns(ReactionChamberRecipe task) {
        int runs = task.getMaxRuns(this.input, this.inputTank, this.getParallelLimit());
        for (int i = runs; i > 0; i--) {
            if (task.canOutput(this.output.getStackInSlot(0), this.outputTank, TANK_CAPACITY, i)) {
                return i;
            }
        }
        return 0;
    }

    private boolean pushOutResult() {
        if (!this.hasAutoExportWork() || this.world == null) {
            return false;
        }

        boolean changed = false;
        if (!this.output.getStackInSlot(0).isEmpty()) {
            ItemStack stack = this.output.getStackInSlot(0);
            for (EnumFacing side : this.outputSides) {
                ItemTransfer target = this.neighbors.get(side);
                if (target == null) {
                    continue;
                }
                ItemStack before = stack.copy();
                stack = target.addItems(stack);
                this.output.setItemDirect(0, stack);
                if (!ItemStack.areItemStacksEqual(before, stack)) {
                    changed = true;
                    break;
                }
            }
        }

        if (changed) {
            this.cachedTask = null;
            this.saveChanges();
            return true;
        }
        return this.pushOutFluid();
    }

    private boolean pushOutFluid() {
        FluidStack stack = this.outputTank.getFluid();
        if (stack == null || stack.amount <= 0) {
            return false;
        }

        for (EnumFacing side : this.outputSides) {
            TileEntity targetTile = this.world.getTileEntity(this.pos.offset(side));
            if (targetTile == null) {
                continue;
            }
            EnumFacing accessSide = side.getOpposite();
            if (!targetTile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, accessSide)) {
                continue;
            }
            IFluidHandler target = targetTile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, accessSide);
            if (target == null) {
                continue;
            }
            FluidStack offered = this.outputTank.drain(TANK_CAPACITY, false);
            if (offered == null || offered.amount <= 0) {
                return false;
            }
            int accepted = target.fill(offered, true);
            if (accepted > 0) {
                this.outputTank.drain(accepted, true);
                this.cachedTask = null;
                this.saveChanges();
                return true;
            }
        }
        return false;
    }

    public AppEngInternalInventory getInputInventory() {
        return this.input;
    }

    public AppEngInternalInventory getOutputInventory() {
        return this.output;
    }

    public FluidTank getInputTank() {
        return this.inputTank;
    }

    public FluidTank getOutputTank() {
        return this.outputTank;
    }

    public IFluidHandler getFluidHandler() {
        return this.fluidHandler;
    }

    public int getProcessingTime() {
        return this.processingTime;
    }

    public boolean isWorking() {
        return this.working;
    }

    private void setWorking(boolean working) {
        if (this.working != working) {
            this.working = working;
            this.markForUpdate();
            IBlockStateHelper.updateWorkingState(this, working);
        }
    }

    public boolean isPowered() {
        if (this.world != null && !this.world.isRemote) {
            return this.getMainNode().isOnline();
        }
        return this.powered;
    }

    public int getMaxProcessingTime() {
        return MAX_PROCESSING_TIME;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    @Override
    public EnumSet<EnumFacing> getOutputSides() {
        return this.outputSides;
    }

    @Override
    public void setOutputSideEnabled(EnumFacing side, boolean enabled) {
        boolean changed = enabled ? this.outputSides.add(side) : this.outputSides.remove(side);
        if (changed) {
            this.saveChanges();
            this.wake();
        }
    }

    @Override
    public BlockOrientation getBlockOrientation() {
        return this.getOrientation();
    }

    @Override
    public EnumSet<EnumFacing> getAllowedOutputSides() {
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public void returnToMainContainer(EntityPlayer player, ISubGui subGui) {
        player.openGui(AppliedAdditions.INSTANCE, ModGuiHandler.REACTION_CHAMBER, this.world,
                this.pos.getX(), this.pos.getY(), this.pos.getZ());
    }

    @Override
    public ItemStack getMainContainerIcon() {
        return new ItemStack(ModContent.REACTION_CHAMBER);
    }

    private void onInputTankChanged() {
        this.cachedTask = null;
        this.processingTime = 0;
        this.markForUpdate();
        this.saveChanges();
        this.wake();
    }

    private void onOutputTankChanged() {
        this.markForUpdate();
        this.saveChanges();
        this.wake();
    }

    private void onUpgradesChanged() {
        this.cachedTask = null;
        this.saveChanges();
        this.wake();
    }

    private void onConfigChanged(IConfigManager manager, Setting<?> setting) {
        this.saveChanges();
        this.wake();
    }

    private void wake() {
        this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
    }

    @Override
    public boolean hasCapability(Capability<?> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return true;
        }
        return super.hasCapability(capability, facing);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getCapability(Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) this.fluidHandler;
        }
        return super.getCapability(capability, facing);
    }

    private int encodeOutputSides() {
        int mask = 0;
        for (EnumFacing side : this.outputSides) {
            mask |= 1 << side.ordinal();
        }
        return mask;
    }

    private void decodeOutputSides(int mask) {
        this.outputSides.clear();
        for (EnumFacing side : EnumFacing.values()) {
            if ((mask & (1 << side.ordinal())) != 0) {
                this.outputSides.add(side);
            }
        }
    }

    private static final class OutputFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return false;
        }
    }

    private static final class IBlockStateHelper {
        private static void updateWorkingState(TileReactionChamber tile, boolean working) {
            if (tile.world == null || tile.world.isRemote) {
                return;
            }
            var state = tile.world.getBlockState(tile.pos);
            if (state.getBlock() instanceof BlockReactionChamber
                    && state.getValue(BlockReactionChamber.WORKING) != working) {
                tile.world.setBlockState(tile.pos, state.withProperty(BlockReactionChamber.WORKING, working), 3);
            }
        }
    }

    private final class SyncedTank extends FluidTank {
        private final Runnable changeListener;

        private SyncedTank(int capacity, Runnable changeListener) {
            super(capacity);
            this.changeListener = changeListener;
        }

        @Override
        protected void onContentsChanged() {
            this.changeListener.run();
        }
    }

    private final class FluidHandler implements IFluidHandler {
        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[]{
                    new FluidTankProperties(inputTank.getFluid(), TANK_CAPACITY, true, false),
                    new FluidTankProperties(outputTank.getFluid(), TANK_CAPACITY, false, true)
            };
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return inputTank.fill(resource, doFill);
        }

        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return outputTank.drain(resource, doDrain);
        }

        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return outputTank.drain(maxDrain, doDrain);
        }
    }

    private final class InputFilter implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return ReactionChamberRecipes.isValidIngredient(stack);
        }
    }
}
