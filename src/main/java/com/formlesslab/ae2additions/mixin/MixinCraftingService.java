package com.formlesslab.ae2additions.mixin;

import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.crafting.CraftingLink;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import ae2.me.service.CraftingService;
import com.formlesslab.ae2additions.me.cluster.AdvCraftingCPU;
import com.formlesslab.ae2additions.me.cluster.ClusterAdvCraftingCPU;
import com.formlesslab.ae2additions.tile.TileAdvCraftingBlock;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.Set;

@Mixin(value = CraftingService.class, remap = false)
public abstract class MixinCraftingService {
    @Unique
    private final Set<ClusterAdvCraftingCPU> ae2additions$quantumCpuClusters = new HashSet<>();

    @Final
    @Shadow
    private ReferenceOpenHashSet<CraftingCPUCluster> craftingCPUClusters;

    @Final
    @Shadow
    private IGrid grid;

    @Shadow
    private boolean updateList;

    @Unique
    private static Set<ClusterAdvCraftingCPU> applied_Additions$collectClusters(IGrid grid) {
        Set<ClusterAdvCraftingCPU> clusters = new HashSet<>();
        if (grid == null) {
            return clusters;
        }

        for (TileAdvCraftingBlock tile : grid.getMachines(TileAdvCraftingBlock.class)) {
            ClusterAdvCraftingCPU cluster = tile.getCluster();
            if (cluster != null && !cluster.isDestroyed()) {
                clusters.add(cluster);
            }
        }
        return clusters;
    }

    @Unique
    private static boolean applied_Additions$ownsQuantumCpuNode(IGridNode node) {
        return node != null && node.getOwner() instanceof TileAdvCraftingBlock;
    }

    @Shadow
    public abstract void addLink(CraftingLink link);

    @Inject(method = "addNode", at = @At("TAIL"))
    private void ae2additions$addQuantumNode(IGridNode gridNode, NBTTagCompound savedData, CallbackInfo ci) {
        if (applied_Additions$ownsQuantumCpuNode(gridNode)) {
            this.updateList = true;
        }
    }

    @Inject(method = "removeNode", at = @At("TAIL"))
    private void ae2additions$removeQuantumNode(IGridNode gridNode, CallbackInfo ci) {
        if (applied_Additions$ownsQuantumCpuNode(gridNode)) {
            this.updateList = true;
        }
    }

    @Inject(method = "updateCPUClusters", at = @At("TAIL"))
    private void ae2additions$registerQuantumCpus(CallbackInfo ci) {
        this.ae2additions$quantumCpuClusters.clear();
        this.ae2additions$quantumCpuClusters.addAll(applied_Additions$collectClusters(this.grid));

        for (ClusterAdvCraftingCPU cluster : this.ae2additions$quantumCpuClusters) {
            for (AdvCraftingCPU cpu : cluster.getActiveCPUs()) {
                this.craftingCPUClusters.add(cpu);
                if (cpu.craftingLogic.getLastLink() instanceof CraftingLink link) {
                    this.addLink(link);
                }
            }
            this.craftingCPUClusters.add(cluster.getRemainingCapacityCPU());
        }
    }

    @Inject(method = "onServerEndTick", at = @At("HEAD"))
    private void ae2additions$removeFinishedQuantumCpus(CallbackInfo ci) {
        for (ClusterAdvCraftingCPU cluster : this.ae2additions$quantumCpuClusters) {
            cluster.getActiveCPUs();
        }
    }
}
