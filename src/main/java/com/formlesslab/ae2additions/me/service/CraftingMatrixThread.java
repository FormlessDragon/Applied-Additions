package com.formlesslab.ae2additions.me.service;

import ae2.api.config.Actionable;
import ae2.api.config.PowerMultiplier;
import ae2.api.crafting.IAssemblerPattern;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.IGrid;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.storage.IStorageService;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.tile.grid.AENetworkedTile;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.FilteredInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import ae2.util.inv.filter.IAEItemFilter;
import com.formlesslab.ae2additions.AppliedAdditions;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.NonNullList;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class CraftingMatrixThread {
    private static final int COOL_TIME = 5 * 20;
    private static final int MAX_CRAFT_PROGRESS = 100;
    private static final Container NULL_CONTAINER = new Container() {
        @Override
        public boolean canInteractWith(net.minecraft.entity.player.EntityPlayer playerIn) {
            return false;
        }
    };

    private final AENetworkedTile host;
    private final Supplier<IActionSource> sourceGetter;
    private final AppEngInternalInventory gridInv;
    private final InternalInventory gridInvExt;
    private final InventoryCrafting craftingInv = new InventoryCrafting(NULL_CONTAINER, 3, 3);
    private final SignalAccepter accepter;

    private ItemStack myPattern = ItemStack.EMPTY;
    private IAssemblerPattern myPlan;
    private double progress;
    private boolean isAwake;
    private boolean forcePlan;
    private boolean reboot = true;
    private int blockCoolDown;
    private ItemStack output = ItemStack.EMPTY;

    public CraftingMatrixThread(AENetworkedTile host, Supplier<IActionSource> sourceGetter, SignalAccepter accepter) {
        if (!(host instanceof InternalInventoryHost)) {
            throw new IllegalArgumentException("Host is not an InternalInventoryHost.");
        }
        this.host = host;
        this.sourceGetter = sourceGetter;
        this.accepter = accepter;
        this.gridInv = new AppEngInternalInventory((InternalInventoryHost) host, 10, 1);
        this.gridInvExt = new FilteredInternalInventory(this.gridInv, new CraftingGridFilter());
    }

    public boolean isAwake() {
        return this.isAwake;
    }

    public boolean acceptJob(IPatternDetails patternDetails, KeyCounter[] table) {
        if (this.myPattern.isEmpty()
            && this.gridInv.isEmpty()
            && patternDetails instanceof IAssemblerPattern pattern) {
            this.forcePlan = true;
            this.myPlan = pattern;
            this.fillGrid(table, pattern);
            this.updateSleepiness();
            this.saveChanges();
            return true;
        }
        return false;
    }

    public void stop() {
        this.myPlan = null;
        this.myPattern = ItemStack.EMPTY;
        this.progress = 0;
        this.ejectHeldItems();
        this.updateSleepiness();
    }

    public NBTTagCompound writeNBT() {
        NBTTagCompound data = new NBTTagCompound();
        ItemStack pattern = this.myPlan != null ? this.myPlan.getDefinition().toStack() : this.myPattern;
        if (!pattern.isEmpty()) {
            data.setTag("myPlan", pattern.writeToNBT(new NBTTagCompound()));
        }
        data.setDouble("progress", this.progress);
        data.setBoolean("forcePlan", this.forcePlan);
        data.setInteger("blockCoolDown", this.blockCoolDown);
        this.gridInv.writeToNBT(data, "inv");
        return data;
    }

    public void readNBT(NBTTagCompound data) {
        this.forcePlan = data.getBoolean("forcePlan");
        this.myPattern = ItemStack.EMPTY;
        this.myPlan = null;
        this.progress = data.getDouble("progress");
        this.blockCoolDown = data.getInteger("blockCoolDown");
        this.gridInv.readFromNBT(data, "inv");
        if (data.hasKey("myPlan", 10)) {
            ItemStack pattern = new ItemStack(data.getCompoundTag("myPlan"));
            if (!pattern.isEmpty()) {
                this.forcePlan = true;
                this.myPattern = pattern;
            }
        }
        this.recalculatePlan();
    }

    public AppEngInternalInventory getInternalInventory() {
        return this.gridInv;
    }

    public InternalInventory getExposedInventoryForSide() {
        return this.gridInvExt;
    }

    public int getCraftingProgress() {
        return (int) this.progress;
    }

    public TickRateModulation tick(int speedCores, int ticksSinceLastCall) {
        if (this.blockCoolDown > 0) {
            this.blockCoolDown -= ticksSinceLastCall;
            return TickRateModulation.SAME;
        }

        if (!this.gridInv.getStackInSlot(9).isEmpty()) {
            this.pushOut(this.gridInv.getStackInSlot(9));
            if (this.gridInv.getStackInSlot(9).isEmpty()) {
                this.saveChanges();
            }
            this.ejectHeldItems();
            this.updateSleepiness();
            this.progress = 0;
            return this.isAwake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
        }

        if (this.myPlan == null) {
            this.ejectHeldItems();
            this.updateSleepiness();
            return TickRateModulation.SLEEP;
        }

        if (this.reboot) {
            ticksSinceLastCall = 1;
        }
        if (!this.isAwake) {
            return TickRateModulation.SLEEP;
        }

        this.reboot = false;
        switch (Math.min(speedCores, 5)) {
            case 1 -> this.progress += this.usePower(ticksSinceLastCall, 26, 1.3);
            case 2 -> this.progress += this.usePower(ticksSinceLastCall, 34, 1.7);
            case 3 -> this.progress += this.usePower(ticksSinceLastCall, 40, 2.0);
            case 4 -> this.progress += this.usePower(ticksSinceLastCall, 50, 2.5);
            case 5 -> this.progress += this.usePower(ticksSinceLastCall, 100, 5.0);
            default -> this.progress += this.usePower(ticksSinceLastCall, 20, 1.0);
        }

        if (this.progress >= MAX_CRAFT_PROGRESS) {
            return this.completeCraft();
        }
        return TickRateModulation.FASTER;
    }

    public void recalculatePlan() {
        this.reboot = true;
        if (this.forcePlan) {
            if (this.host.getWorld() != null && this.myPlan == null) {
                if (!this.myPattern.isEmpty()) {
                    IPatternDetails details = PatternDetailsHelper.decodePattern(this.myPattern, this.host.getWorld());
                    if (details instanceof IAssemblerPattern pattern) {
                        this.myPlan = pattern;
                    }
                }
                this.myPattern = ItemStack.EMPTY;
                if (this.myPlan == null) {
                    AppliedAdditions.LOGGER.warn("Unable to restore assembler matrix pattern after load: {}", this.myPattern);
                    this.forcePlan = false;
                }
            }
            this.updateSleepiness();
            return;
        }

        this.progress = 0;
        this.myPlan = null;
        this.myPattern = ItemStack.EMPTY;
        this.updateSleepiness();
    }

    @Nullable
    public IAssemblerPattern getCurrentPattern() {
        return this.host.getWorld() != null && this.host.getWorld().isRemote ? null : this.myPlan;
    }

    public void updateSleepiness() {
        boolean wasEnabled = this.isAwake;
        this.isAwake = (this.myPlan != null && this.hasMats()) || this.canPush();
        if (wasEnabled != this.isAwake) {
            this.accepter.send(this.isAwake);
        }
    }

    public ItemStack getOutput() {
        return this.output;
    }

    private TickRateModulation completeCraft() {
        for (int i = 0; i < 9; i++) {
            this.craftingInv.setInventorySlotContents(i, this.gridInv.getStackInSlot(i));
        }

        this.progress = 0;
        this.output = this.assemblePattern(this.craftingInv);
        if (!this.output.isEmpty() && this.host.getWorld() != null) {
            NonNullList<ItemStack> craftingRemainders = this.myPlan.getRemainingItems(this.craftingInv);
            this.pushOut(this.output.copy());

            for (int i = 0; i < 9; i++) {
                this.gridInv.setItemDirect(i, craftingRemainders.get(i));
            }

            this.forcePlan = false;
            this.myPlan = null;
            this.ejectHeldItems();
            this.saveChanges();
            this.updateSleepiness();
            return this.isAwake ? TickRateModulation.IDLE : TickRateModulation.SLEEP;
        }

        AppliedAdditions.LOGGER.warn("Assembler matrix failed to craft; returning ingredients to output buffer.");
        this.forcePlan = false;
        this.myPlan = null;
        this.ejectHeldItems();
        this.saveChanges();
        this.updateSleepiness();
        return TickRateModulation.IDLE;
    }

    private ItemStack assemblePattern(InventoryCrafting input) {
        return this.myPlan.assemble(input, this.host.getWorld());
    }

    private int usePower(int ticksPassed, int bonusValue, double acceleratorTax) {
        IGrid grid = this.host.getMainNode().getGrid();
        if (grid == null) {
            return 0;
        }
        double safePower = Math.min(ticksPassed * bonusValue * acceleratorTax, 5000);
        return (int) (grid.getEnergyService().extractAEPower(safePower, Actionable.MODULATE, PowerMultiplier.CONFIG) / acceleratorTax);
    }

    private void ejectHeldItems() {
        if (!this.gridInv.getStackInSlot(9).isEmpty()) {
            return;
        }

        for (int i = 0; i < 9; i++) {
            ItemStack stack = this.gridInv.getStackInSlot(i);
            if (!stack.isEmpty()) {
                this.gridInv.setItemDirect(9, stack);
                this.gridInv.setItemDirect(i, ItemStack.EMPTY);
                this.saveChanges();
                return;
            }
        }
    }

    private void pushOut(ItemStack output) {
        output = this.pushTo(output);
        if (!output.isEmpty()) {
            this.blockCoolDown = COOL_TIME;
        }
        if (output.isEmpty() && this.forcePlan) {
            this.forcePlan = false;
            this.recalculatePlan();
        }
        this.gridInv.setItemDirect(9, output);
    }

    private ItemStack pushTo(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        IGrid grid = this.host.getMainNode().getGrid();
        if (grid == null) {
            return stack;
        }

        IStorageService storage = grid.getStorageService();
        long inserted = storage.getInventory()
            .insert(AEItemKey.of(stack), stack.getCount(), Actionable.MODULATE, this.sourceGetter.get());
        if (inserted <= 0) {
            return stack;
        }

        this.saveChanges();
        if (inserted < stack.getCount()) {
            ItemStack remaining = stack.copy();
            remaining.setCount((int) (stack.getCount() - inserted));
            return remaining;
        }
        return ItemStack.EMPTY;
    }

    private void fillGrid(KeyCounter[] table, IAssemblerPattern adapter) {
        adapter.fillCraftingGrid(table, this.gridInv::setItemDirect);
        for (KeyCounter counter : table) {
            counter.removeZeros();
            if (!counter.isEmpty()) {
                throw new IllegalStateException("Could not fill assembler matrix grid with " + counter.getFirstKey());
            }
        }
    }

    private boolean hasMats() {
        return this.myPlan != null && !this.gridInv.isEmpty();
    }

    private boolean canPush() {
        return !this.gridInv.getStackInSlot(9).isEmpty();
    }

    private void saveChanges() {
        this.host.saveChanges();
    }

    private static class CraftingGridFilter implements IAEItemFilter {
        @Override
        public boolean allowExtract(InternalInventory inv, int slot, int amount) {
            return slot == 9;
        }

        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return false;
        }
    }

    public interface SignalAccepter {
        void send(boolean signal);
    }
}
