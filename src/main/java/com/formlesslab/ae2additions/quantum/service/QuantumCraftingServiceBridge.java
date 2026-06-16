package com.formlesslab.ae2additions.quantum.service;

import ae2.api.config.Actionable;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.crafting.ICraftingCPU;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingRequester;
import ae2.api.networking.crafting.ICraftingSubmitResult;
import ae2.api.networking.crafting.UnsuitableCpus;
import ae2.api.networking.energy.IEnergyService;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.me.service.CraftingService;
import com.formlesslab.ae2additions.quantum.cluster.AdvCraftingCPU;
import com.formlesslab.ae2additions.quantum.cluster.AdvCraftingCPUCluster;
import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class QuantumCraftingServiceBridge {
    private static final Comparator<AdvCraftingCPUCluster> FAST_FIRST = Comparator
        .comparingInt(AdvCraftingCPUCluster::getCoProcessors)
        .reversed()
        .thenComparingLong(AdvCraftingCPUCluster::getAvailableStorage);

    private QuantumCraftingServiceBridge() {
    }

    public static Set<AdvCraftingCPUCluster> collectClusters(IGrid grid) {
        Set<AdvCraftingCPUCluster> clusters = new HashSet<>();
        if (grid == null) {
            return clusters;
        }
        for (AdvCraftingBlockEntity tile : grid.getMachines(AdvCraftingBlockEntity.class)) {
            AdvCraftingCPUCluster cluster = tile.getCluster();
            if (cluster != null && !cluster.isDestroyed()) {
                clusters.add(cluster);
            }
        }
        return clusters;
    }

    public static boolean ownsQuantumCpuNode(IGridNode node) {
        return node != null && node.getOwner() instanceof AdvCraftingBlockEntity;
    }

    public static long tick(Collection<AdvCraftingCPUCluster> clusters, IEnergyService energy, CraftingService service) {
        long latestChange = 0;
        for (AdvCraftingCPUCluster cluster : clusters) {
            latestChange = Math.max(latestChange, cluster.tickActiveCpus(energy, service));
        }
        return latestChange;
    }

    public static void collectWaitingFor(Collection<AdvCraftingCPUCluster> clusters, Set<AEKey> currentlyCrafting) {
        for (AdvCraftingCPUCluster cluster : clusters) {
            cluster.collectWaitingFor(currentlyCrafting);
        }
    }

    public static long insertIntoCpus(
        Collection<AdvCraftingCPUCluster> clusters,
        AEKey what,
        long amount,
        Actionable type,
        long alreadyInserted
    ) {
        long inserted = alreadyInserted;
        for (AdvCraftingCPUCluster cluster : clusters) {
            if (inserted >= amount) {
                break;
            }
            inserted += cluster.insertIntoActiveCpus(what, amount - inserted, type);
        }
        return inserted;
    }

    public static ICraftingSubmitResult submitJob(
        Collection<AdvCraftingCPUCluster> clusters,
        IGrid grid,
        ICraftingPlan plan,
        ICraftingRequester requester,
        ICraftingCPU target,
        IActionSource src,
        AtomicReference<UnsuitableCpus> unsuitable
    ) {
        if (target instanceof AdvCraftingCPUCluster quantumCluster) {
            return quantumCluster.submitJob(grid, plan, src, requester);
        }
        AdvCraftingCPUCluster cluster = findSuitableCpu(clusters, plan, src, unsuitable);
        return cluster == null ? null : cluster.submitJob(grid, plan, src, requester);
    }

    public static AdvCraftingCPUCluster findSuitableCpu(
        Collection<AdvCraftingCPUCluster> clusters,
        ICraftingPlan plan,
        IActionSource src,
        AtomicReference<UnsuitableCpus> unsuitable
    ) {
        java.util.List<AdvCraftingCPUCluster> candidates = new java.util.ArrayList<>();
        int offline = 0;
        int tooSmall = 0;
        int excluded = 0;

        for (AdvCraftingCPUCluster cluster : clusters) {
            if (!cluster.isActive()) {
                offline++;
            } else if (cluster.getAvailableStorage() < plan.bytes()) {
                tooSmall++;
            } else if (!cluster.canBeAutoSelectedFor(src)) {
                excluded++;
            } else {
                candidates.add(cluster);
            }
        }

        if (candidates.isEmpty()) {
            if ((offline | tooSmall | excluded) != 0 && unsuitable != null) {
                unsuitable.set(new UnsuitableCpus(offline, 0, tooSmall, excluded));
            }
            return null;
        }

        candidates.sort((a, b) -> {
            boolean firstPreferred = a.isPreferredFor(src);
            boolean secondPreferred = b.isPreferredFor(src);
            if (firstPreferred != secondPreferred) {
                return Boolean.compare(secondPreferred, firstPreferred);
            }
            return FAST_FIRST.compare(a, b);
        });
        return candidates.getFirst();
    }

    public static ImmutableSet<ICraftingCPU> appendCpus(
        Collection<AdvCraftingCPUCluster> clusters,
        ImmutableSet<ICraftingCPU> vanilla
    ) {
        ImmutableSet.Builder<ICraftingCPU> builder = ImmutableSet.builder();
        builder.addAll(vanilla);
        for (AdvCraftingCPUCluster cluster : clusters) {
            for (AdvCraftingCPU cpu : cluster.getActiveCPUs()) {
                builder.add(cpu);
            }
            builder.add(cluster.getRemainingCapacityCPU());
        }
        return builder.build();
    }

    public static long getRequestedAmount(Collection<AdvCraftingCPUCluster> clusters, AEKey what, long vanillaAmount) {
        long requested = vanillaAmount;
        for (AdvCraftingCPUCluster cluster : clusters) {
            requested += cluster.getRequestedAmount(what);
        }
        return requested;
    }

    public static boolean hasCpu(Collection<AdvCraftingCPUCluster> clusters, ICraftingCPU cpu) {
        for (AdvCraftingCPUCluster cluster : clusters) {
            for (AdvCraftingCPU activeCpu : cluster.getActiveCPUs()) {
                if (activeCpu == cpu) {
                    return true;
                }
            }
            if (cluster.getRemainingCapacityCPU() == cpu) {
                return true;
            }
        }
        return false;
    }
}
