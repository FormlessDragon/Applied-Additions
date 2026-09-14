package com.formlesslab.ae2additions.me.service;

import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridServiceProvider;
import com.formlesslab.ae2additions.me.cluster.ClusterAdvCraftingCPU;
import com.formlesslab.ae2additions.tile.TileAdvCraftingBlock;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.nbt.NBTTagCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Retires quantum computer CPUs whose job has finished.
 * <p>
 * AE2's crafting service only queries {@link ae2.api.networking.crafting.ICraftingCpuProvider} and never mutates the
 * CPUs it gets back, so recycling them needs its own tick. Registering this after AE2's own services makes it run
 * after {@code CraftingService} in the same tick, so a CPU is only inspected once its crafting logic has run.
 */
public final class QuantumCpuMaintenanceService implements IGridServiceProvider {
    private final Set<IGridNode> quantumNodes = new ReferenceOpenHashSet<>();
    private final Set<ClusterAdvCraftingCPU> tickedClusters = new ReferenceOpenHashSet<>();

    @Override
    public void addNode(IGridNode gridNode, @Nullable NBTTagCompound savedData) {
        if (gridNode.getOwner() instanceof TileAdvCraftingBlock) {
            this.quantumNodes.add(gridNode);
        }
    }

    @Override
    public void removeNode(IGridNode gridNode) {
        this.quantumNodes.remove(gridNode);
    }

    @Override
    public void onServerEndTick() {
        if (this.quantumNodes.isEmpty()) {
            return;
        }

        // Every block of a multiblock is its own node but they all share one cluster.
        this.tickedClusters.clear();
        for (IGridNode node : this.quantumNodes) {
            if (!(node.getOwner() instanceof TileAdvCraftingBlock tile)) {
                continue;
            }

            ClusterAdvCraftingCPU cluster = tile.getCluster();
            if (cluster != null && !cluster.isDestroyed() && this.tickedClusters.add(cluster)) {
                cluster.pruneFinishedCpus();
            }
        }
        this.tickedClusters.clear();
    }
}
