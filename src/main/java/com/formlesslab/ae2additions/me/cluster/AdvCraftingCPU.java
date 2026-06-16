package com.formlesslab.ae2additions.me.cluster;

import ae2.api.config.CpuSelectionMode;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.crafting.CraftingJobStatus;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.GenericStack;
import ae2.crafting.execution.ElapsedTimeTracker;
import ae2.crafting.inv.ListCraftingInventory;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import com.formlesslab.ae2additions.me.logic.AdvCraftingCPULogic;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;

public class AdvCraftingCPU extends CraftingCPUCluster {
    final UUID uniqueId;
    final long bytes;
    private final AdvCraftingCPUCluster parent;
    public final AdvCraftingCPULogic craftingLogic = new AdvCraftingCPULogic(this);
    private boolean markedForDeletion;

    public AdvCraftingCPU(AdvCraftingCPUCluster parent, UUID uniqueId, long bytes) {
        super(parent.getBoundsMin(), parent.getBoundsMax());
        this.parent = parent;
        this.uniqueId = uniqueId;
        this.bytes = bytes;
    }

    AdvCraftingCPU(AdvCraftingCPUCluster parent, long bytes) {
        this(parent, null, bytes);
    }

    @Override
    public BlockPos getBoundsMin() {
        return this.parent.getBoundsMin();
    }

    @Override
    public BlockPos getBoundsMax() {
        return this.parent.getBoundsMax();
    }

    @Override
    public boolean isBusy() {
        return this.craftingLogic.hasJob();
    }

    @Override
    public CraftingJobStatus getJobStatus() {
        GenericStack finalOutput = this.craftingLogic.getFinalJobOutput();
        if (finalOutput == null) {
            return null;
        }
        ElapsedTimeTracker tracker = this.craftingLogic.getElapsedTimeTracker();
        long started = tracker.getStartedWorkUnits();
        long progress = Math.max(0, started - tracker.getRemainingWorkUnits());
        return new CraftingJobStatus(finalOutput, started, progress, tracker.getElapsedTime());
    }

    @Override
    public void cancelJob() {
        if (this.uniqueId != null) {
            this.parent.cancelJob(this.uniqueId);
        }
    }

    @Override
    public long getAvailableStorage() {
        return this.bytes;
    }

    @Override
    public int getCoProcessors() {
        return this.parent.getCoProcessors();
    }

    @Override
    public ITextComponent getName() {
        return this.parent.getName();
    }

    @Override
    public CpuSelectionMode getSelectionMode() {
        return this.parent.getSelectionMode();
    }

    @Override
    public void markDirty() {
        this.parent.markDirty();
    }

    @Override
    public boolean isActive() {
        return this.parent.isActive();
    }

    @Override
    public World getLevel() {
        return this.parent.getLevel();
    }

    @Override
    public IGrid getGrid() {
        return this.parent.getGrid();
    }

    @Override
    public IGridNode getNode() {
        return this.parent.getNode();
    }

    @Override
    public IActionSource getSrc() {
        return this.parent.getSrc();
    }

    @Override
    public void updateOutput(GenericStack stack) {
        this.parent.updateOutput(stack);
    }

    public ListCraftingInventory getInventory() {
        return this.craftingLogic.getInventory();
    }

    public void writeToNBT(NBTTagCompound data) {
        this.craftingLogic.writeToNBT(data);
    }

    public void readFromNBT(NBTTagCompound data) {
        this.craftingLogic.readFromNBT(data);
    }

    public AdvCraftingCPUCluster getParent() {
        return this.parent;
    }

    public boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    public void markForDeletion() {
        this.markedForDeletion = true;
    }
}
