package com.formlesslab.ae2additions.assembler.tile;

import ae2.api.config.Setting;
import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.implementations.IPowerChannelState;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridMultiblock;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.orientation.BlockOrientation;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.me.cluster.IAEMultiBlock;
import ae2.tile.grid.AENetworkedTile;
import ae2.util.ConfigManager;
import ae2.util.inv.CombinedInternalInventory;
import com.formlesslab.ae2additions.assembler.block.BlockAssemblerMatrixBase;
import com.formlesslab.ae2additions.assembler.me.CalculatorAssemblerMatrix;
import com.formlesslab.ae2additions.assembler.me.ClusterAssemblerMatrix;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class TileAssemblerMatrixBase extends AENetworkedTile
    implements IAEMultiBlock<ClusterAssemblerMatrix>, IPowerChannelState, IConfigurableObject {

    protected final CalculatorAssemblerMatrix calc = new CalculatorAssemblerMatrix(this);
    protected final ConfigManager manager;
    protected boolean isCore;
    protected NBTTagCompound previousState;
    protected ClusterAssemblerMatrix cluster;
    private boolean applyingClusterConfig;

    public TileAssemblerMatrixBase() {
        this.getMainNode()
            .setFlags(GridFlags.MULTIBLOCK, GridFlags.REQUIRE_CHANNEL)
            .addService(IGridMultiblock.class, this::getMultiblockNodes)
            .setIdlePowerUsage(0.5);
        this.manager = new ConfigManager((configManager, setting) -> this.onConfigChanged(setting));
        this.manager.registerSetting(Settings.PATTERN_ACCESS_TERMINAL, YesNo.YES);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.manager;
    }

    public NBTTagCompound getPreviousState() {
        return this.previousState;
    }

    public void setPreviousState(NBTTagCompound previousState) {
        this.previousState = previousState;
    }

    public boolean isCore() {
        return this.isCore;
    }

    public void setCore(boolean core) {
        this.isCore = core;
    }

    @Override
    public ItemStack getItemFromTile() {
        BlockAssemblerMatrixBase<?> block = this.getMatrixBlock();
        return block == null ? ItemStack.EMPTY : new ItemStack(block.getPresentItem());
    }

    @Override
    public void setCustomName(String customName) {
        super.setCustomName(customName);
        if (this.cluster != null) {
            this.cluster.updateName();
        }
    }

    @Override
    public void onReady() {
        super.onReady();
        this.getMainNode().setVisualRepresentation(this.getItemFromTile());
        if (this.world != null && !this.world.isRemote) {
            this.calc.calculateMultiblock(this.world, this.pos);
        }
    }

    public void updateMultiBlock(BlockPos changedPos) {
        if (this.world != null && !this.world.isRemote) {
            this.calc.updateMultiblockAfterNeighborUpdate(this.world, this.pos, changedPos);
        }
    }

    public void breakCluster() {
        if (this.cluster != null) {
            this.cluster.destroy();
        }
    }

    public boolean isFormed() {
        if (isClientSide()) {
            IBlockState state = this.world.getBlockState(this.pos);
            return state.getBlock() instanceof BlockAssemblerMatrixBase && state.getValue(BlockAssemblerMatrixBase.FORMED);
        }
        return this.cluster != null;
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setBoolean("core", this.isCore);
        this.manager.writeToNBT(data);
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.setCore(data.getBoolean("core"));
        this.manager.readFromNBT(data);
        if (this.isCore) {
            this.setPreviousState(data.copy());
        }
    }

    @Override
    public void disconnect(boolean update) {
        if (this.cluster != null) {
            this.cluster.destroy();
            if (update) {
                this.updateSubType(true);
            }
        }
    }

    @Override
    public ClusterAssemblerMatrix getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return !this.isInvalid();
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            this.updateSubType(false);
        }
    }

    @Override
    public boolean isActive() {
        if (!isClientSide()) {
            return this.getMainNode().isActive();
        }
        return this.isPowered() && this.isFormed();
    }

    @Nullable
    public IItemHandler getPatternInv(EnumFacing ignored) {
        if (this.cluster == null) {
            return null;
        }
        List<InternalInventory> inv = new ArrayList<>();
        for (TileAssemblerMatrixPattern pc : this.cluster.getPatterns()) {
            inv.add(pc.getExposedInventory());
        }
        return new CombinedInternalInventory(inv.toArray(new InternalInventory[0])).toItemHandler();
    }

    @Override
    public boolean hasCapability(Capability<?> capability, EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && this.getPatternInv(facing) != null
            || super.hasCapability(capability, facing);
    }

    @Override
    public <T> T getCapability(Capability<T> capability, EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            IItemHandler handler = this.getPatternInv(facing);
            if (handler != null) {
                return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(handler);
            }
        }
        return super.getCapability(capability, facing);
    }

    public void updateStatus(@Nullable ClusterAssemblerMatrix c) {
        if (this.cluster != null && this.cluster != c) {
            this.cluster.breakCluster();
        }
        this.cluster = c;
        this.updateSubType(true);
    }

    public void updateSubType(boolean updateFormed) {
        if (this.world == null || this.isInvalid()) {
            return;
        }

        boolean formed = this.isFormed();
        boolean power = formed && this.getMainNode().isOnline();
        IBlockState current = this.world.getBlockState(this.pos);

        if (current.getBlock() instanceof BlockAssemblerMatrixBase) {
            IBlockState newState = current
                .withProperty(BlockAssemblerMatrixBase.POWERED, power)
                .withProperty(BlockAssemblerMatrixBase.FORMED, formed);
            if (!current.equals(newState)) {
                this.world.setBlockState(this.pos, newState, 2);
            }
        }

        if (updateFormed) {
            onGridConnectableSidesChanged();
        }
        this.markForUpdate();
    }

    @Override
    public boolean isPowered() {
        if (isClientSide()) {
            IBlockState state = this.world.getBlockState(this.pos);
            return state.getBlock() instanceof BlockAssemblerMatrixBase && state.getValue(BlockAssemblerMatrixBase.POWERED);
        }
        return this.getMainNode().isActive();
    }

    @Override
    public Set<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        if (isFormed()) {
            return EnumSet.allOf(EnumFacing.class);
        }
        return EnumSet.noneOf(EnumFacing.class);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return isFormed() ? AECableType.COVERED : AECableType.NONE;
    }

    public BlockAssemblerMatrixBase<?> getMatrixBlock() {
        if (this.world == null || this.isInvalid()) {
            return null;
        }
        Block block = this.world.getBlockState(this.pos).getBlock();
        return block instanceof BlockAssemblerMatrixBase ? (BlockAssemblerMatrixBase<?>) block : null;
    }

    public boolean isClientSide() {
        return this.world != null && this.world.isRemote;
    }

    public <T extends Enum<T>> void applyConfigFromCluster(Setting<T> setting, T newValue) {
        this.applyingClusterConfig = true;
        try {
            this.manager.putSetting(setting, newValue);
        } finally {
            this.applyingClusterConfig = false;
        }
        this.saveChanges();
    }

    private <T extends Enum<T>> void onConfigChanged(Setting<T> setting) {
        this.saveChanges();
        if (!this.applyingClusterConfig && this.cluster != null) {
            this.cluster.broadcastConfig(setting, this.manager.getSetting(setting), this);
        }
    }

    private Iterator<IGridNode> getMultiblockNodes() {
        if (this.getCluster() == null) {
            return java.util.Collections.emptyIterator();
        }
        List<IGridNode> nodes = new ArrayList<>();
        Iterator<TileAssemblerMatrixBase> it = this.getCluster().getBlockEntities();
        while (it.hasNext()) {
            IGridNode node = it.next().getGridNode();
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes.iterator();
    }
}
