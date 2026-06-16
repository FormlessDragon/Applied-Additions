package com.formlesslab.ae2additions.mixin;

import ae2.api.config.CpuSelectionMode;
import ae2.api.config.Settings;
import ae2.api.networking.crafting.ICraftingCPU;
import ae2.container.guisync.GuiSync;
import ae2.container.implementations.ContainerCraftingCPU;
import ae2.container.implementations.ContainerCraftingStatus;
import ae2.util.EnumCycler;
import com.formlesslab.ae2additions.client.util.CraftingStatusCpuGrouping;
import com.formlesslab.ae2additions.client.util.CraftingStatusCpuMetadata;
import com.formlesslab.ae2additions.client.util.CraftingStatusCpuMetadataList;
import com.formlesslab.ae2additions.client.util.CraftingStatusCpuMetadataProvider;
import com.formlesslab.ae2additions.quantum.cluster.AdvCraftingCPU;
import com.formlesslab.ae2additions.quantum.cluster.AdvCraftingCPUCluster;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.entity.player.InventoryPlayer;

@Mixin(value = ContainerCraftingStatus.class, remap = false)
public abstract class MixinCraftingStatusMenu extends ContainerCraftingCPU implements CraftingStatusCpuMetadataProvider {
    @Shadow
    public ContainerCraftingStatus.CraftingCpuList cpuList;

    @Shadow
    private ImmutableSet<ICraftingCPU> lastCpuSet;

    @Shadow
    @Final
    private WeakHashMap<ICraftingCPU, Integer> cpuSerialMap;

    @Shadow
    private int selectedCpuSerial;

    @Shadow
    private int lastUpdate;

    @GuiSync(10)
    @Unique
    private CraftingStatusCpuMetadataList ae2additions$cpuMetadata = CraftingStatusCpuMetadataList.EMPTY;

    protected MixinCraftingStatusMenu(InventoryPlayer playerInventory, Object host) {
        super(playerInventory, host);
    }

    @Inject(
        method = "detectAndSendChanges",
        at = @At(
            value = "INVOKE",
            target = "Lae2/container/implementations/ContainerCraftingCPU;detectAndSendChanges()V",
            shift = At.Shift.BEFORE))
    private void ae2additions$groupQuantumComputerCpus(CallbackInfo ci) {
        if (!this.isServerSide()) {
            return;
        }

        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial = ae2additions$createMetadataBySerial();
        this.ae2additions$cpuMetadata = new CraftingStatusCpuMetadataList(List.copyOf(metadataBySerial.values()));
        if (metadataBySerial.isEmpty() || this.cpuList.cpus().isEmpty()) {
            return;
        }

        Map<Integer, ContainerCraftingStatus.CraftingCpuListEntry> entriesBySerial = new HashMap<>();
        ObjectArrayList<Integer> serials = new ObjectArrayList<>(this.cpuList.cpus().size());
        for (ContainerCraftingStatus.CraftingCpuListEntry cpu : this.cpuList.cpus()) {
            entriesBySerial.put(cpu.serial(), cpu);
            serials.add(cpu.serial());
        }

        List<Integer> orderedSerials = CraftingStatusCpuGrouping.orderSerials(serials, metadataBySerial);
        ObjectArrayList<ContainerCraftingStatus.CraftingCpuListEntry> orderedEntries =
            new ObjectArrayList<>(orderedSerials.size());
        for (Integer serial : orderedSerials) {
            ContainerCraftingStatus.CraftingCpuListEntry entry = entriesBySerial.get(serial);
            if (entry != null) {
                orderedEntries.add(entry);
            }
        }

        this.cpuList = new ContainerCraftingStatus.CraftingCpuList(orderedEntries);
    }

    @Inject(method = "cycleCpuMode(IZ)V", at = @At("HEAD"), cancellable = true)
    private void ae2additions$cycleQuantumCpuMode(int serial, boolean backwards, CallbackInfo ci) {
        if (!this.isServerSide() || serial <= 0) {
            return;
        }

        AdvCraftingCPU quantumCpu = ae2additions$findQuantumCpuBySerial(serial);
        if (quantumCpu == null) {
            return;
        }

        AdvCraftingCPUCluster cluster = quantumCpu.getParent();
        if (cluster == null) {
            return;
        }

        CpuSelectionMode updatedMode = EnumCycler.rotateEnum(
            cluster.getSelectionMode(),
            backwards,
            Settings.CPU_SELECTION_MODE.getValues());
        cluster.getConfigManager().putSetting(Settings.CPU_SELECTION_MODE, updatedMode);

        int clusterId = cluster.getGuiClusterId();
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial = ae2additions$createMetadataBySerial();
        this.ae2additions$cpuMetadata = new CraftingStatusCpuMetadataList(List.copyOf(metadataBySerial.values()));
        if (ae2additions$serialBelongsToQuantumCluster(this.selectedCpuSerial, clusterId, metadataBySerial)) {
            this.schedulingMode = updatedMode;
        }
        ae2additions$updateQuantumCpuModes(clusterId, updatedMode, metadataBySerial);
        this.lastUpdate = 0;
        ci.cancel();
    }

    @Override
    public CraftingStatusCpuMetadataList ae2additions$getCpuMetadata() {
        return this.ae2additions$cpuMetadata;
    }

    @Unique
    private Map<Integer, CraftingStatusCpuMetadata> ae2additions$createMetadataBySerial() {
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial = new HashMap<>();
        for (ICraftingCPU cpu : this.lastCpuSet) {
            if (!(cpu instanceof AdvCraftingCPU quantumCpu)) {
                continue;
            }

            AdvCraftingCPUCluster cluster = quantumCpu.getParent();
            if (cluster == null) {
                continue;
            }

            Integer serial = this.cpuSerialMap.get(cpu);
            if (serial == null) {
                continue;
            }

            metadataBySerial.put(serial, new CraftingStatusCpuMetadata(
                serial,
                cluster.getGuiClusterId(),
                cluster.getRemainingCapacityCPU() == quantumCpu));
        }
        return metadataBySerial;
    }

    @Unique
    private AdvCraftingCPU ae2additions$findQuantumCpuBySerial(int serial) {
        for (ICraftingCPU cpu : this.lastCpuSet) {
            if (this.cpuSerialMap.getOrDefault(cpu, -1) == serial && cpu instanceof AdvCraftingCPU quantumCpu) {
                return quantumCpu;
            }
        }
        return null;
    }

    @Unique
    private boolean ae2additions$serialBelongsToQuantumCluster(
        int serial,
        int clusterId,
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial
    ) {
        CraftingStatusCpuMetadata metadata = metadataBySerial.get(serial);
        return metadata != null && metadata.clusterId() == clusterId;
    }

    @Unique
    private void ae2additions$updateQuantumCpuModes(
        int clusterId,
        CpuSelectionMode updatedMode,
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial
    ) {
        ObjectArrayList<ContainerCraftingStatus.CraftingCpuListEntry> updatedEntries =
            new ObjectArrayList<>(this.cpuList.cpus().size());

        for (ContainerCraftingStatus.CraftingCpuListEntry cpu : this.cpuList.cpus()) {
            CraftingStatusCpuMetadata metadata = metadataBySerial.get(cpu.serial());
            if (metadata != null && metadata.clusterId() == clusterId) {
                updatedEntries.add(new ContainerCraftingStatus.CraftingCpuListEntry(
                    cpu.serial(),
                    cpu.storage(),
                    cpu.coProcessors(),
                    cpu.name(),
                    updatedMode,
                    cpu.currentJob(),
                    cpu.progress(),
                    cpu.elapsedTimeNanos()));
            } else {
                updatedEntries.add(cpu);
            }
        }

        this.cpuList = new ContainerCraftingStatus.CraftingCpuList(updatedEntries);
    }
}
