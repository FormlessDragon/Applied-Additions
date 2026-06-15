package com.formlesslab.ae2additions.quantum.client.gui;

import ae2.api.config.CpuSelectionMode;
import ae2.api.config.Settings;
import ae2.api.networking.crafting.CraftingJobStatus;
import ae2.api.networking.crafting.ICraftingCPU;
import ae2.container.guisync.GuiSync;
import ae2.container.implementations.ContainerCraftingCPU;
import ae2.container.implementations.ContainerCraftingStatus.CraftingCpuList;
import ae2.container.implementations.ContainerCraftingStatus.CraftingCpuListEntry;
import ae2.util.EnumCycler;
import net.minecraft.entity.player.InventoryPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.WeakHashMap;

public class QuantumComputerMenu extends ContainerCraftingCPU {
    private static final String ACTION_SELECT_CPU = "selectCpu";
    private static final String ACTION_CYCLE_SELECTION_MODE = "cycleQuantumSelectionMode";
    private static final String ACTION_CANCEL_CRAFTING = "cancelCrafting";

    private static final CraftingCpuList EMPTY_CPU_LIST = new CraftingCpuList(Collections.emptyList());
    private static final Comparator<CraftingCpuListEntry> CPU_COMPARATOR = Comparator
        .comparing((CraftingCpuListEntry e) -> e.name() == null)
        .thenComparing(e -> e.name() != null ? e.name().getFormattedText() : "")
        .thenComparingInt(CraftingCpuListEntry::serial);

    private WeakHashMap<ICraftingCPU, Integer> cpuSerialMap;
    private final QuantumComputerHost host;

    @GuiSync(8)
    public CraftingCpuList cpuList = EMPTY_CPU_LIST;

    @GuiSync(9)
    private int selectedCpuSerial = -1;

    @GuiSync(10)
    public CpuSelectionMode selectionMode = CpuSelectionMode.ANY;

    private int nextCpuSerial = 1;
    private List<? extends ICraftingCPU> lastCpuSet = Collections.emptyList();
    private int lastUpdate;
    private ICraftingCPU selectedCpu;

    public QuantumComputerMenu(InventoryPlayer playerInventory, Object host) {
        super(playerInventory, host);
        this.host = host instanceof QuantumComputerHost ? (QuantumComputerHost) host : null;
        if (this.host != null) {
            this.selectionMode = this.host.getQuantumSelectionMode();
        }
        this.registerClientAction(ACTION_SELECT_CPU, Integer.class, this::selectCpu);
        this.registerClientAction(ACTION_CYCLE_SELECTION_MODE, Integer.class, this::cycleSelectionMode);
    }

    @Override
    protected void setCPU(ICraftingCPU cpu) {
        super.setCPU(cpu);
        this.selectedCpu = cpu;
        this.selectedCpuSerial = cpu == null ? -1 : getOrAssignCpuSerial(cpu);
    }

    @Override
    public void detectAndSendChanges() {
        if (this.isServerSide()) {
            updateCpuListFromHost();
            updateSelectedCpu();
            if (this.host != null) {
                this.selectionMode = this.host.getQuantumSelectionMode();
            }
        }

        super.detectAndSendChanges();
    }

    private void updateCpuListFromHost() {
        if (this.host == null) {
            this.lastUpdate = 20;
            if (!this.lastCpuSet.isEmpty()) {
                this.cpuList = EMPTY_CPU_LIST;
                this.lastCpuSet = Collections.emptyList();
            }
            return;
        }

        List<? extends ICraftingCPU> newCpuSet = this.host.getQuantumCpus();
        if (newCpuSet == null) {
            newCpuSet = Collections.emptyList();
        }

        if (!this.lastCpuSet.equals(newCpuSet) || ++this.lastUpdate >= 20) {
            this.lastCpuSet = new ArrayList<>(newCpuSet);
            this.cpuList = createCpuList();
            this.lastUpdate = 0;
        }
    }

    private void updateSelectedCpu() {
        if (this.selectedCpuSerial != -1 && this.cpuList.cpus().stream()
            .noneMatch(cpu -> cpu.serial() == this.selectedCpuSerial)) {
            selectCpu(-1);
        }

        if (this.selectedCpuSerial == -1) {
            for (CraftingCpuListEntry cpu : this.cpuList.cpus()) {
                if (cpu.currentJob() != null) {
                    selectCpu(cpu.serial());
                    break;
                }
            }
        }

        if (this.selectedCpuSerial == -1 && !this.cpuList.cpus().isEmpty()) {
            selectCpu(this.cpuList.cpus().getFirst().serial());
        }
    }

    private CraftingCpuList createCpuList() {
        List<CraftingCpuListEntry> entries = new ArrayList<>(this.lastCpuSet.size());
        for (ICraftingCPU cpu : this.lastCpuSet) {
            int serial = getOrAssignCpuSerial(cpu);
            CraftingJobStatus status = cpu.getJobStatus();
            float progress = 0.0F;
            if (status != null && status.totalItems() > 0) {
                progress = (float) (status.progress() / (double) status.totalItems());
            }
            entries.add(new CraftingCpuListEntry(
                serial,
                cpu.getAvailableStorage(),
                cpu.getCoProcessors(),
                cpu.getName(),
                cpu.getSelectionMode(),
                status != null ? status.crafting() : null,
                progress,
                status != null ? status.elapsedTimeNanos() : 0L));
        }
        entries.sort(CPU_COMPARATOR);
        return new CraftingCpuList(entries);
    }

    private int getOrAssignCpuSerial(ICraftingCPU cpu) {
        if (this.cpuSerialMap == null) {
            this.cpuSerialMap = new WeakHashMap<>();
        }
        return this.cpuSerialMap.computeIfAbsent(cpu, ignored -> this.nextCpuSerial++);
    }

    @Override
    public boolean allowConfiguration() {
        return false;
    }

    @Override
    public void cancelCrafting() {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_CANCEL_CRAFTING);
        } else if (this.selectedCpu != null) {
            this.selectedCpu.cancelJob();
        }
    }

    public void selectCpu(int serial) {
        if (this.isClientSide()) {
            this.selectedCpuSerial = serial;
            this.sendClientAction(ACTION_SELECT_CPU, serial);
            return;
        }

        ICraftingCPU newSelectedCpu = null;
        if (serial != -1) {
            WeakHashMap<ICraftingCPU, Integer> serialMap = this.cpuSerialMap;
            for (ICraftingCPU cpu : this.lastCpuSet) {
                if (serialMap != null && serialMap.getOrDefault(cpu, -1) == serial) {
                    newSelectedCpu = cpu;
                    break;
                }
            }
        }

        if (newSelectedCpu != this.selectedCpu) {
            setCPU(newSelectedCpu);
        }
    }

    public void cycleSelectionMode(boolean backwards) {
        if (this.isClientSide()) {
            this.selectionMode = rotateSelectionMode(this.selectionMode, backwards);
            this.sendClientAction(ACTION_CYCLE_SELECTION_MODE, backwards ? -1 : 1);
            return;
        }

        CpuSelectionMode mode = rotateSelectionMode(this.selectionMode, backwards);
        this.selectionMode = mode;
        if (this.host != null) {
            this.host.setQuantumSelectionMode(mode);
        }
    }

    private void cycleSelectionMode(Integer delta) {
        cycleSelectionMode(delta != null && delta < 0);
    }

    private static CpuSelectionMode rotateSelectionMode(CpuSelectionMode current, boolean backwards) {
        return EnumCycler.rotateEnum(current, backwards, Settings.CPU_SELECTION_MODE.getValues());
    }

    public int getSelectedCpuSerial() {
        return this.selectedCpuSerial;
    }

    public CpuSelectionMode getSelectionMode() {
        return this.selectionMode;
    }

}
