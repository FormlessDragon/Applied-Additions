package com.formlesslab.ae2additions.mixin;

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
import ae2.crafting.CraftingLink;
import ae2.me.service.CraftingService;
import com.formlesslab.ae2additions.me.cluster.AdvCraftingCPU;
import com.formlesslab.ae2additions.me.cluster.AdvCraftingCPUCluster;
import com.formlesslab.ae2additions.me.service.QuantumCraftingServiceBridge;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.nbt.NBTTagCompound;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingService.class, remap = false)
public abstract class MixinCraftingService {
    @Unique
    private final Set<AdvCraftingCPUCluster> ae2additions$quantumCpuClusters = new HashSet<>();

    @Final
    @Shadow
    private IGrid grid;

    @Final
    @Shadow
    private IEnergyService energyGrid;

    @Shadow
    private boolean updateList;

    @Shadow
    private long lastProcessedCraftingLogicChangeTick;

    @Shadow
    public abstract void addLink(CraftingLink link);

    @Inject(method = "onServerEndTick", at = @At("HEAD"))
    private void ae2additions$tickQuantumCpus(CallbackInfo ci) {
        long latestChange = QuantumCraftingServiceBridge.tick(
            this.ae2additions$quantumCpuClusters,
            this.energyGrid,
            (CraftingService) (Object) this);
        if (latestChange > 0) {
            this.lastProcessedCraftingLogicChangeTick = -1;
        }
    }

    @Inject(method = "addNode", at = @At("TAIL"))
    private void ae2additions$addQuantumNode(IGridNode gridNode, NBTTagCompound savedData, CallbackInfo ci) {
        if (QuantumCraftingServiceBridge.ownsQuantumCpuNode(gridNode)) {
            this.updateList = true;
        }
    }

    @Inject(method = "removeNode", at = @At("TAIL"))
    private void ae2additions$removeQuantumNode(IGridNode gridNode, CallbackInfo ci) {
        if (QuantumCraftingServiceBridge.ownsQuantumCpuNode(gridNode)) {
            this.updateList = true;
        }
    }

    @Inject(method = "updateCPUClusters", at = @At("TAIL"))
    private void ae2additions$updateQuantumClusters(CallbackInfo ci) {
        this.ae2additions$quantumCpuClusters.clear();
        this.ae2additions$quantumCpuClusters.addAll(QuantumCraftingServiceBridge.collectClusters(this.grid));
        for (AdvCraftingCPUCluster cluster : this.ae2additions$quantumCpuClusters) {
            for (AdvCraftingCPU cpu : cluster.getActiveCPUs()) {
                if (cpu.craftingLogic.getLastLink() instanceof CraftingLink link) {
                    this.addLink(link);
                }
            }
        }
    }

    @Inject(method = "insertIntoCpus", at = @At("RETURN"), cancellable = true)
    private void ae2additions$insertIntoQuantumCpus(AEKey what, long amount, Actionable type,
                                                    CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(QuantumCraftingServiceBridge.insertIntoCpus(
            this.ae2additions$quantumCpuClusters,
            what,
            amount,
            type,
            cir.getReturnValue()));
    }

    @Inject(method = "submitJob*", at = @At("HEAD"), cancellable = true)
    private void ae2additions$submitQuantumJob(ICraftingPlan job, ICraftingRequester requestingMachine,
                                               ICraftingCPU target, boolean prioritizePower, IActionSource src,
                                               boolean forceStart,
                                               CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        if (job.simulation() || (!forceStart && !job.missingItems().isEmpty())) {
            return;
        }
        AtomicReference<UnsuitableCpus> unsuitable = new AtomicReference<>();
        ICraftingSubmitResult result = QuantumCraftingServiceBridge.submitJob(
            this.ae2additions$quantumCpuClusters,
            this.grid,
            job,
            requestingMachine,
            target,
            src,
            unsuitable);
        if (result != null) {
            cir.setReturnValue(result);
        }
    }

    @Inject(method = "getCpus", at = @At("RETURN"), cancellable = true)
    private void ae2additions$getQuantumCpus(CallbackInfoReturnable<ImmutableSet<ICraftingCPU>> cir) {
        cir.setReturnValue(QuantumCraftingServiceBridge.appendCpus(
            this.ae2additions$quantumCpuClusters,
            cir.getReturnValue()));
    }

    @Inject(method = "getRequestedAmount", at = @At("RETURN"), cancellable = true)
    private void ae2additions$getQuantumRequestedAmount(AEKey what, CallbackInfoReturnable<Long> cir) {
        cir.setReturnValue(QuantumCraftingServiceBridge.getRequestedAmount(
            this.ae2additions$quantumCpuClusters,
            what,
            cir.getReturnValue()));
    }

    @Inject(method = "hasCpu", at = @At("HEAD"), cancellable = true)
    private void ae2additions$hasQuantumCpu(ICraftingCPU cpu, CallbackInfoReturnable<Boolean> cir) {
        if (QuantumCraftingServiceBridge.hasCpu(this.ae2additions$quantumCpuClusters, cpu)) {
            cir.setReturnValue(true);
        }
    }
}
