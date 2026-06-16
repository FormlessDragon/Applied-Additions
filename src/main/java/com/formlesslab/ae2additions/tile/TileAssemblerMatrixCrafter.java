package com.formlesslab.ae2additions.tile;

import ae2.api.crafting.IPatternDetails;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.IGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.ticking.IGridTickable;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.networking.ticking.TickingRequest;
import ae2.api.stacks.GenericStack;
import ae2.api.stacks.KeyCounter;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.CombinedInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;
import com.formlesslab.ae2additions.me.service.CraftingMatrixThread;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import java.util.List;

public class TileAssemblerMatrixCrafter extends TileAssemblerMatrixFunction
    implements InternalInventoryHost, IGridTickable {

    public static final int MAX_THREAD = 8;

    private final CraftingMatrixThread[] threads = new CraftingMatrixThread[MAX_THREAD];
    private final InternalInventory internalInv;
    private short states;

    public TileAssemblerMatrixCrafter() {
        this.getMainNode().addService(IGridTickable.class, this);
        InternalInventory[] invs = new InternalInventory[MAX_THREAD];
        for (int i = 0; i < MAX_THREAD; i++) {
            final int index = i;
            this.threads[index] = new CraftingMatrixThread(this, this::getSrc, signal -> this.changeState(index, signal));
            invs[index] = this.threads[index].getInternalInventory();
        }
        this.internalInv = new CombinedInternalInventory(invs);
    }

    private IActionSource getSrc() {
        return this.cluster.getSrc();
    }

    private void changeState(int index, boolean state) {
        boolean oldState = this.states > 0;
        if (state) {
            this.states |= (short) (1 << index);
        } else {
            this.states &= (short) ~(1 << index);
        }

        if (state) {
            if (!oldState) {
                this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
            }
        } else if (oldState && this.states <= 0) {
            this.getMainNode().ifPresent((grid, node) -> grid.getTickManager().sleepDevice(node));
        }
    }

    public int usedThread() {
        int count = 0;
        for (CraftingMatrixThread thread : this.threads) {
            if (thread.getCurrentPattern() != null || !thread.getInternalInventory().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    public boolean pushJob(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        for (CraftingMatrixThread thread : this.threads) {
            if (thread.acceptJob(patternDetails, inputHolder)) {
                this.cluster.updateCrafter(this);
                return true;
            }
        }
        return false;
    }

    public void stop() {
        for (CraftingMatrixThread thread : this.threads) {
            thread.stop();
        }
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        for (int i = 0; i < MAX_THREAD; i++) {
            data.setTag("#ct" + i, this.threads[i].writeNBT());
        }
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        for (int i = 0; i < MAX_THREAD; i++) {
            if (data.hasKey("#ct" + i, 10)) {
                this.threads[i].readNBT(data.getCompoundTag("#ct" + i));
            }
        }
    }

    @Override
    public void add(ClusterAssemblerMatrix cluster) {
        cluster.addCrafter(this);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        boolean isAwake = false;
        for (CraftingMatrixThread thread : this.threads) {
            thread.recalculatePlan();
            thread.updateSleepiness();
            isAwake |= thread.isAwake();
        }
        return new TickingRequest(1, 1, !isAwake);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.cluster == null) {
            return TickRateModulation.SLEEP;
        }
        TickRateModulation rate = TickRateModulation.SLEEP;
        for (CraftingMatrixThread thread : this.threads) {
            if (thread.isAwake()) {
                TickRateModulation threadRate = thread.tick(this.cluster.getSpeedCore(), ticksSinceLastCall);
                if (threadRate.ordinal() > rate.ordinal()) {
                    rate = threadRate;
                }
            }
        }
        this.cluster.updateCrafter(this);
        return rate;
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        for (CraftingMatrixThread thread : this.threads) {
            if (inv == thread.getInternalInventory()) {
                thread.recalculatePlan();
                break;
            }
        }
        this.saveChanges();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        this.saveChangedInventory(inv);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        for (ItemStack stack : this.internalInv) {
            if (stack.isEmpty()) {
                continue;
            }
            GenericStack genericStack = GenericStack.unwrapItemStack(stack);
            if (genericStack != null && this.world != null) {
                genericStack.what().addDrops(genericStack.amount(), drops, this.world, this.pos);
            } else {
                drops.add(stack);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.internalInv.clear();
    }
}
