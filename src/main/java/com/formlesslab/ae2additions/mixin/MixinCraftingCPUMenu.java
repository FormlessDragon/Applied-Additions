package com.formlesslab.ae2additions.mixin;

import ae2.api.config.CpuSelectionMode;
import ae2.api.networking.IGrid;
import ae2.api.networking.crafting.ICraftingCPU;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.container.AEBaseContainer;
import ae2.container.implementations.ContainerCraftingCPU;
import ae2.container.me.common.IncrementalUpdateHelper;
import ae2.container.me.crafting.CraftingStatus;
import ae2.core.localization.PlayerMessages;
import ae2.core.network.clientbound.CraftingStatusPacket;
import ae2.core.network.clientbound.CraftingSupplierLocationsPacket;
import ae2.crafting.execution.CraftingSupplierLocation;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import com.formlesslab.ae2additions.quantum.cluster.AdvCraftingCPU;
import com.formlesslab.ae2additions.quantum.logic.AdvCraftingCPULogic;
import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ContainerCraftingCPU.class, remap = false)
public abstract class MixinCraftingCPUMenu extends AEBaseContainer {

    @Shadow
    @Final
    private IncrementalUpdateHelper incrementalUpdateHelper;

    @Shadow
    @Final
    private Consumer<AEKey> cpuChangeListener;

    @Shadow
    @Final
    @Nullable
    private IGrid grid;

    @Shadow
    @Nullable
    private CraftingCPUCluster cpu;

    @Shadow
    private boolean cachedSuspend;

    @Shadow
    public CpuSelectionMode schedulingMode;

    @Shadow
    public boolean cantStoreItems;

    @Shadow
    protected abstract void setCPU(@Nullable ICraftingCPU cpu);

    @Unique
    @Nullable
    private AdvCraftingCPU ae2additions$quantumCpu;

    protected MixinCraftingCPUMenu(InventoryPlayer playerInventory, @Nullable Object host) {
        super(playerInventory, host);
    }

    @Inject(method = "<init>(Lnet/minecraft/entity/player/InventoryPlayer;Ljava/lang/Object;)V", at = @At("TAIL"))
    private void ae2additions$selectInitialQuantumCpu(InventoryPlayer playerInventory, Object host, CallbackInfo ci) {
        if (!(host instanceof AdvCraftingBlockEntity quantumTile) || quantumTile.getCluster() == null) {
            return;
        }

        List<AdvCraftingCPU> activeCpus = quantumTile.getCluster().getActiveCPUs();
        if (!activeCpus.isEmpty()) {
            this.setCPU(activeCpus.getFirst());
        } else {
            this.setCPU(quantumTile.getCluster().getRemainingCapacityCPU());
        }
    }

    @Inject(method = "setCPU", at = @At("HEAD"), cancellable = true)
    private void ae2additions$setQuantumCpu(@Nullable ICraftingCPU selectedCpu, CallbackInfo ci) {
        if (selectedCpu == this.ae2additions$quantumCpu) {
            ci.cancel();
            return;
        }

        if (this.ae2additions$quantumCpu != null) {
            this.ae2additions$quantumCpu.craftingLogic.removeListener(this.cpuChangeListener);
        }

        if (selectedCpu instanceof AdvCraftingCPU quantumCpu) {
            if (this.cpu != null) {
                this.cpu.craftingLogic.removeListener(this.cpuChangeListener);
                this.cpu = null;
            }

            this.incrementalUpdateHelper.reset();
            this.cachedSuspend = false;
            this.ae2additions$quantumCpu = quantumCpu;

            KeyCounter allItems = new KeyCounter();
            quantumCpu.craftingLogic.getAllItems(allItems);
            for (it.unimi.dsi.fastutil.objects.Object2LongMap.Entry<AEKey> entry : allItems) {
                this.incrementalUpdateHelper.addChange(entry.getKey());
            }

            quantumCpu.craftingLogic.addListener(this.cpuChangeListener);
            ci.cancel();
        } else {
            this.ae2additions$quantumCpu = null;
        }
    }

    @Inject(method = "cancelCrafting", at = @At("TAIL"))
    private void ae2additions$cancelQuantumCrafting(CallbackInfo ci) {
        if (this.isServerSide() && this.ae2additions$quantumCpu != null) {
            this.ae2additions$quantumCpu.cancelJob();
        }
    }

    @Inject(method = "toggleScheduling", at = @At("TAIL"))
    private void ae2additions$toggleQuantumScheduling(CallbackInfo ci) {
        if (this.isServerSide() && this.ae2additions$quantumCpu != null) {
            AdvCraftingCPULogic logic = this.ae2additions$quantumCpu.craftingLogic;
            logic.setJobSuspended(!logic.isJobSuspended());
        }
    }

    @Inject(method = "onContainerClosed", at = @At("TAIL"))
    private void ae2additions$removeQuantumListener(EntityPlayer player, CallbackInfo ci) {
        if (this.ae2additions$quantumCpu != null) {
            this.ae2additions$quantumCpu.craftingLogic.removeListener(this.cpuChangeListener);
        }
    }

    @Inject(method = "detectAndSendChanges", at = @At("HEAD"))
    private void ae2additions$sendQuantumCraftingStatus(CallbackInfo ci) {
        if (!this.isServerSide() || this.ae2additions$quantumCpu == null) {
            return;
        }

        AdvCraftingCPULogic logic = this.ae2additions$quantumCpu.craftingLogic;
        this.schedulingMode = this.ae2additions$quantumCpu.getSelectionMode();
        this.cantStoreItems = logic.isCantStoreItems();

        if (this.incrementalUpdateHelper.hasChanges() || this.cachedSuspend != logic.isJobSuspended()) {
            CraftingStatus status = CraftingStatus.create(this.incrementalUpdateHelper, logic);
            this.incrementalUpdateHelper.commitChanges();
            this.cachedSuspend = status.suspended();

            this.sendPacketToClient(new CraftingStatusPacket(this.windowId, status));
        }
    }

    @Inject(method = "traceSupplierForSerial", at = @At("HEAD"), cancellable = true)
    private void ae2additions$traceQuantumSupplier(long serial, CallbackInfo ci) {
        if (this.ae2additions$quantumCpu == null) {
            return;
        }

        ci.cancel();
        if (!(this.getPlayer() instanceof EntityPlayerMP player) || this.grid == null) {
            return;
        }

        AEKey key = this.incrementalUpdateHelper.getBySerial(serial);
        if (key == null) {
            return;
        }

        AdvCraftingCPULogic logic = this.ae2additions$quantumCpu.craftingLogic;
        long activeAmount = logic.getWaitingFor(key);
        long pendingAmount = logic.getPendingOutputs(key);
        if (activeAmount <= 0 && pendingAmount <= 0) {
            return;
        }

        List<CraftingSupplierLocation> locations = logic.findSupplierLocations(this.grid, key);
        if (locations.isEmpty()) {
            player.sendStatusMessage(PlayerMessages.CraftingSupplierNotFound.text(), true);
            return;
        }

        this.sendPacketToClient(new CraftingSupplierLocationsPacket(locations));
    }
}
