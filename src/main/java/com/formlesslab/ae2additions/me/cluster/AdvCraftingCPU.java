package com.formlesslab.ae2additions.me.cluster;

import ae2.api.config.CpuSelectionMode;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.crafting.CraftingCpuGroup;
import ae2.api.networking.crafting.CraftingJobOptions;
import ae2.api.networking.crafting.CraftingJobStatus;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingRequester;
import ae2.api.networking.crafting.ICraftingSubmitResult;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.GenericStack;
import ae2.api.util.IConfigManager;
import ae2.client.gui.Icon;
import ae2.crafting.execution.CraftingSubmitResult;
import ae2.crafting.execution.ElapsedTimeTracker;
import ae2.crafting.inv.ListCraftingInventory;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import com.formlesslab.ae2additions.tile.TileAdvCraftingBlock;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AdvCraftingCPU extends CraftingCPUCluster {
    /**
     * List ordinal of the remaining-capacity CPU, which always sorts to the end of its cluster's group.
     */
    static final int CAPACITY_ORDINAL = Integer.MAX_VALUE;

    final UUID uniqueId;
    private final ClusterAdvCraftingCPU parent;
    /**
     * Storage reserved on the parent cluster for this CPU. Fixed at submission, but grows when further plans are
     * merged in; see {@link #expandReservation(long)}.
     */
    long bytes;
    private int listOrdinal;
    private boolean markedForDeletion;

    public AdvCraftingCPU(ClusterAdvCraftingCPU parent, UUID uniqueId, long bytes) {
        super(parent.getBoundsMin(), parent.getBoundsMax());
        this.parent = parent;
        this.uniqueId = uniqueId;
        this.bytes = bytes;
    }

    AdvCraftingCPU(ClusterAdvCraftingCPU parent, long bytes) {
        this(parent, null, bytes);
        this.listOrdinal = CAPACITY_ORDINAL;
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
    public BlockPos getCorePos() {
        TileAdvCraftingBlock core = this.parent.getCore();
        return core == null ? null : core.getPos();
    }

    @Override
    public boolean isBusy() {
        return super.isBusy();
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
    public ICraftingSubmitResult submitJob(IGrid grid, ICraftingPlan plan, IActionSource src, @Nullable ICraftingRequester requester) {
        if (this.uniqueId == null) {
            return this.parent.submitJob(grid, plan, src, requester);
        }
        return this.craftingLogic.trySubmitJob(grid, plan, src, requester);
    }

    /**
     * The crafting service submits jobs through this overload. Without the override it would run on the inherited
     * implementation, which submits straight into this CPU's logic and bypasses the parent cluster - the
     * remaining-capacity CPU would swallow the job instead of the cluster splitting off a new CPU for it.
     */
    @Override
    public ICraftingSubmitResult submitJob(IGrid grid, ICraftingPlan plan, IActionSource src, @Nullable ICraftingRequester requester, CraftingJobOptions options) {
        return this.submitJob(grid, plan, src, requester);
    }

    /**
     * Merging is decided against the cluster's free capacity, not this CPU's original reservation: a second plan
     * with the same output may join a running CPU as long as the cluster can still reserve its bytes.
     */
    @Override
    public boolean canMergeJob(ICraftingPlan plan) {
        if (this.uniqueId == null || plan.simulation()) {
            return false;
        }
        GenericStack currentOutput = this.craftingLogic.getFinalJobOutput();
        if (currentOutput == null || !currentOutput.what().equals(plan.finalOutput().what())) {
            return false;
        }
        return this.parent.getAvailableStorage() >= plan.bytes();
    }

    @Override
    public ICraftingSubmitResult mergeJob(IGrid grid, ICraftingPlan plan, IActionSource src) {
        return this.mergeJob(grid, plan, src, 0);
    }

    /**
     * Tops the CPU's reservation up with the merged plan's bytes (taken from the cluster's free capacity) before
     * handing the merge to the crafting logic, and gives the bytes back if the logic rejects the merge. Without the
     * top-up the logic's own capacity check would measure against the original reservation and the cluster would
     * silently over-commit.
     */
    @Override
    public ICraftingSubmitResult mergeJob(IGrid grid, ICraftingPlan plan, IActionSource src, int priority) {
        if (this.uniqueId == null) {
            return CraftingSubmitResult.CPU_BUSY;
        }
        if (!this.parent.moveFreeCapacity(this, plan.bytes())) {
            return CraftingSubmitResult.CPU_TOO_SMALL;
        }
        ICraftingSubmitResult result = this.craftingLogic.tryMergeJob(grid, plan, src, priority);
        if (!result.successful()) {
            this.parent.moveFreeCapacity(this, -plan.bytes());
        } else {
            // The cluster's free capacity changed, but the crafting logic only posts item changes, not a CPU change.
            // Without this event the crafting service keeps listing the stale remaining-capacity CPU object it
            // registered earlier, so terminals keep showing the pre-merge remaining capacity.
            this.parent.postCpuChange();
        }
        return result;
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
    public IConfigManager getConfigManager() {
        return this.parent.getConfigManager();
    }

    @Override
    public boolean rename(@Nullable String name) {
        return this.parent.rename(name);
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
    public boolean isDestroyed() {
        return this.parent.isDestroyed();
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

    @Override
    public CraftingCpuGroup getCpuListGroup() {
        return new CraftingCpuGroup(this.parent.getCpuListGroupId(), this.listOrdinal);
    }

    @Override
    public Icon getUnfocusedCpuListBackgroundIcon() {
        return QuantumCpuRowIcons.forRow(this.isFirstGroupRow(), this.isLastGroupRow(), false);
    }

    @Override
    public Icon getFocusedCpuListBackgroundIcon() {
        return QuantumCpuRowIcons.forRow(this.isFirstGroupRow(), this.isLastGroupRow(), true);
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

    public ClusterAdvCraftingCPU getParent() {
        return this.parent;
    }

    public boolean isMarkedForDeletion() {
        return this.markedForDeletion;
    }

    public void markForDeletion() {
        this.markedForDeletion = true;
    }

    int getListOrdinal() {
        return this.listOrdinal;
    }

    void setListOrdinal(int listOrdinal) {
        this.listOrdinal = listOrdinal;
    }

    void expandReservation(long delta) {
        this.bytes = Math.max(0, this.bytes + delta);
    }

    /**
     * The remaining-capacity row always closes its cluster's group, and it opens the group too while no job is
     * running.
     */
    private boolean isLastGroupRow() {
        return this.uniqueId == null;
    }

    private boolean isFirstGroupRow() {
        return this.uniqueId == null ? !this.parent.hasActiveCpus() : this.parent.isFirstCpuOrdinal(this.listOrdinal);
    }
}
