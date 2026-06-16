package com.formlesslab.ae2additions.me.cluster;

import ae2.api.config.Actionable;
import ae2.api.config.CpuSelectionMode;
import ae2.api.config.Settings;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.crafting.ICraftingCPU;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingRequester;
import ae2.api.networking.crafting.ICraftingSubmitResult;
import ae2.api.networking.events.GridCraftingCpuChange;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.util.IConfigManager;
import ae2.crafting.execution.CraftingSubmitResult;
import ae2.crafting.inv.ListCraftingInventory;
import ae2.me.cluster.IAECluster;
import ae2.me.cluster.MBCalculator;
import ae2.me.helpers.MachineSource;
import ae2.me.service.CraftingService;
import ae2.tile.crafting.TileCraftingMonitor;
import com.formlesslab.ae2additions.tile.TileAdvCraftingBlock;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class AdvCraftingCPUCluster implements IAECluster {
    private static int nextGuiClusterId = 1;

    private static final String TAG_CPUS = "cpus";
    private static final String TAG_CPU_LIST_COMPAT = "cpuList";
    private static final String TAG_KEY = "key";
    private static final String TAG_BYTES = "bytes";
    private static final String TAG_CPU = "cpu";
    private static final String TAG_CONFIG = "config";

    private final BlockPos boundsMin;
    private final BlockPos boundsMax;
    private final int guiClusterId;

    private final Map<UUID, AdvCraftingCPU> activeCpus = new HashMap<>();
    private final List<TileAdvCraftingBlock> quantumBlockEntities = new ObjectArrayList<>();
    private final List<TileCraftingMonitor> status = new ArrayList<>();
    private AdvCraftingCPU remainingStorageCpu;
    private final IConfigManager configManager;
    private ITextComponent myName = null;
    private boolean destroyed = false;
    private long storage = 0;
    private long storageMultiplier = 0;
    private long remainingStorage = 0;
    private MachineSource machineSrc = null;
    private int accelerator = 0;
    private int acceleratorMultiplier = 0;
    private UUID lastSelectedCpuId;
    private boolean lastSelectedRemainingCapacity;

    public AdvCraftingCPUCluster(BlockPos boundsMin, BlockPos boundsMax) {
        this.boundsMin = boundsMin.toImmutable();
        this.boundsMax = boundsMax.toImmutable();
        this.guiClusterId = nextGuiClusterId++;

        this.configManager = IConfigManager.builder(this::markDirty)
                .registerSetting(Settings.CPU_SELECTION_MODE, CpuSelectionMode.ANY)
                .build();
    }

    public Iterator<TileAdvCraftingBlock> getQuantumBlockEntities() {
        return this.quantumBlockEntities.iterator();
    }

    public int numBlockEntities() {
        return this.quantumBlockEntities.size();
    }

    public void addQuantumBlockEntity(TileAdvCraftingBlock tile) {
        if (this.machineSrc == null || tile.isCoreBlock()) {
            this.machineSrc = new MachineSource(tile);
        }

        tile.setCoreBlock(false);
        tile.saveChanges();
        this.quantumBlockEntities.addFirst(tile);

        if (tile.getStorageBytes() > 0) {
            this.storage += tile.getStorageBytes();
            this.recalculateRemainingStorage();
        }
        if (tile.getStorageMultiplier() > 0) {
            this.storageMultiplier += tile.getStorageMultiplier();
            this.recalculateRemainingStorage();
        }
        if (tile.getAcceleratorThreads() > 0) {
            if (tile.getAcceleratorThreads() <= 16) {
                this.accelerator += tile.getAcceleratorThreads();
            } else {
                throw new IllegalArgumentException("Co-processor threads may not exceed 16 per single unit block.");
            }
        }
        if (tile.getAccelerationMultiplier() > 0) {
            this.acceleratorMultiplier += tile.getAccelerationMultiplier();
            this.recalculateRemainingStorage();
        }
    }

    @Override
    public BlockPos getBoundsMin() {
        return this.boundsMin;
    }

    @Override
    public BlockPos getBoundsMax() {
        return this.boundsMax;
    }

    @Override
    public void updateStatus(boolean updateGrid) {
        for (TileAdvCraftingBlock tile : this.quantumBlockEntities) {
            tile.updateSubType(true);
        }
    }

    @Override
    public void destroy() {
        if (this.destroyed) {
            return;
        }
        this.destroyed = true;
        boolean ownsModification = !MBCalculator.isModificationInProgress();
        if (ownsModification) {
            MBCalculator.setModificationInProgress(this);
        }
        try {
            this.updateGridForChangedCpu(null);
        } finally {
            if (ownsModification) {
                MBCalculator.setModificationInProgress(null);
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return false;
    }

    @Override
    public Iterator<? extends TileEntity> getBlockEntities() {
        return this.quantumBlockEntities.iterator();
    }

    public long insertIntoActiveCpus(AEKey what, long amount, Actionable type) {
        long inserted = 0;
        for (AdvCraftingCPU cpu : this.getActiveCPUs()) {
            if (inserted >= amount) {
                break;
            }
            inserted += cpu.craftingLogic.insert(what, amount - inserted, type);
        }
        return inserted;
    }

    public long getRequestedAmount(AEKey what) {
        long requested = 0;
        for (AdvCraftingCPU cpu : this.getActiveCPUs()) {
            requested += cpu.craftingLogic.getWaitingFor(what);
        }
        return requested;
    }

    public void collectWaitingFor(java.util.Set<AEKey> waitingFor) {
        for (AdvCraftingCPU cpu : this.getActiveCPUs()) {
            cpu.craftingLogic.getAllWaitingFor(waitingFor);
        }
    }

    public long tickActiveCpus(ae2.api.networking.energy.IEnergyService energy, CraftingService craftingService) {
        long latestChange = 0;
        for (AdvCraftingCPU cpu : this.getActiveCPUs()) {
            cpu.craftingLogic.tickCraftingLogic(energy, craftingService);
            latestChange = Math.max(latestChange, cpu.craftingLogic.getLastModifiedOnTick());
        }
        return latestChange;
    }

    public void cancelJobs() {
        for (UUID id : new ArrayList<>(this.activeCpus.keySet())) {
            this.killCpu(id, false);
        }
        this.postCpuChange();
    }

    public void cancelJob(UUID id) {
        this.killCpu(id, true);
    }

    public ICraftingSubmitResult submitJob(
        IGrid grid,
        ICraftingPlan plan,
        IActionSource src,
        ICraftingRequester requester
    ) {
        if (!this.isActive()) {
            return CraftingSubmitResult.CPU_OFFLINE;
        }
        if (this.getAvailableStorage() < plan.bytes()) {
            return CraftingSubmitResult.CPU_TOO_SMALL;
        }

        UUID id = UUID.randomUUID();
        AdvCraftingCPU cpu = new AdvCraftingCPU(this, id, plan.bytes());
        ICraftingSubmitResult result = cpu.craftingLogic.trySubmitJob(grid, plan, src, requester);
        if (result.successful()) {
            this.activeCpus.put(id, cpu);
            this.recalculateRemainingStorage();
            this.updateGridForChangedCpu(this);
        }
        return result;
    }

    public long getAvailableStorage() {
        return this.remainingStorage;
    }

    public int getCoProcessors() {
        int coProcessors = this.accelerator;
        if (this.acceleratorMultiplier > 0) {
            coProcessors *= this.acceleratorMultiplier;
        }
        return coProcessors;
    }

    public ITextComponent getName() {
        return this.myName;
    }

    public void updateOutput(GenericStack finalOutput) {
        GenericStack stack = finalOutput != null && finalOutput.amount() <= 0 ? null : finalOutput;
        for (var monitor : this.status) {
            monitor.setJob(stack);
        }
    }

    public IActionSource getSrc() {
        return Objects.requireNonNull(this.machineSrc);
    }

    public void markDirty() {
        TileAdvCraftingBlock core = this.getCore();
        if (core != null) {
            core.saveChanges();
        }
    }

    protected TileAdvCraftingBlock getCore() {
        if (this.machineSrc == null) {
            return null;
        }
        return this.machineSrc.machine()
            .filter(TileAdvCraftingBlock.class::isInstance)
            .map(TileAdvCraftingBlock.class::cast)
            .orElse(null);
    }

    public World getLevel() {
        TileAdvCraftingBlock core = this.getCore();
        return core == null ? null : core.getWorld();
    }

    public IGrid getGrid() {
        IGridNode node = this.getNode();
        return node == null ? null : node.grid();
    }

    public IGridNode getNode() {
        TileAdvCraftingBlock core = this.getCore();
        return core == null ? null : core.getActionableNode();
    }

    public boolean isActive() {
        IGridNode node = getNode();
        return node != null && node.isActive();
    }

    public void done() {
        TileAdvCraftingBlock core = this.getCore();
        if (core == null) {
            return;
        }
        core.setCoreBlock(true);
        if (core.getPreviousState() != null) {
            this.readFromNBT(core.getPreviousState());
            core.setPreviousState(null);
        }
        this.updateName();
        this.recalculateRemainingStorage();
    }

    public void writeToNBT(NBTTagCompound data) {
        NBTTagList cpuList = new NBTTagList();
        for (Map.Entry<UUID, AdvCraftingCPU> entry : this.activeCpus.entrySet()) {
            NBTTagCompound child = new NBTTagCompound();
            child.setString(TAG_KEY, entry.getKey().toString());
            child.setLong(TAG_BYTES, entry.getValue().bytes);
            NBTTagCompound cpuData = new NBTTagCompound();
            entry.getValue().writeToNBT(cpuData);
            child.setTag(TAG_CPU, cpuData);
            cpuList.appendTag(child);
        }
        data.setTag(TAG_CPUS, cpuList);

        NBTTagCompound config = new NBTTagCompound();
        this.configManager.writeToNBT(config);
        data.setTag(TAG_CONFIG, config);
    }

    public void readFromNBT(NBTTagCompound data) {
        this.activeCpus.clear();
        NBTTagList cpuList = data.getTagList(data.hasKey(TAG_CPUS, 9) ? TAG_CPUS : TAG_CPU_LIST_COMPAT, 10);
        for (int i = 0; i < cpuList.tagCount(); i++) {
            NBTTagCompound child = cpuList.getCompoundTagAt(i);
            UUID id = child.hasKey(TAG_KEY, 8) ? UUID.fromString(child.getString(TAG_KEY)) : UUID.randomUUID();
            long bytes = child.getLong(TAG_BYTES);
            AdvCraftingCPU cpu = new AdvCraftingCPU(this, id, bytes);
            this.activeCpus.put(id, cpu);
            cpu.readFromNBT(child.hasKey(TAG_CPU, 10) ? child.getCompoundTag(TAG_CPU) : child);
        }

        if (data.hasKey(TAG_CONFIG, 10)) {
            this.configManager.readFromNBT(data.getCompoundTag(TAG_CONFIG));
        } else {
            this.configManager.readFromNBT(data);
        }
        this.recalculateRemainingStorage();
    }

    public void updateName() {
        this.myName = null;
        for (TileAdvCraftingBlock tile : this.quantumBlockEntities) {
            if (tile.hasCustomName() && tile.getCustomName() != null) {
                if (this.myName == null) {
                    this.myName = new TextComponentString(tile.getCustomName());
                } else {
                    this.myName.appendText(" ").appendText(tile.getCustomName());
                }
            }
        }
    }

    public void breakCluster() {
        TileAdvCraftingBlock core = this.getCore();
        if (core != null) {
            core.breakCluster();
        }
    }

    public CpuSelectionMode getSelectionMode() {
        return this.configManager.getSetting(Settings.CPU_SELECTION_MODE);
    }

    public int getGuiClusterId() {
        return this.guiClusterId;
    }

    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    public List<ListCraftingInventory> getInventories() {
        List<ListCraftingInventory> inventories = new ArrayList<>();
        for (AdvCraftingCPU cpu : this.activeCpus.values()) {
            inventories.add(cpu.getInventory());
        }
        return inventories;
    }

    public List<AdvCraftingCPU> getActiveCPUs() {
        List<AdvCraftingCPU> cpus = new ArrayList<>();
        List<UUID> remove = new ArrayList<>();
        for (Map.Entry<UUID, AdvCraftingCPU> entry : this.activeCpus.entrySet()) {
            AdvCraftingCPU cpu = entry.getValue();
            if (cpu.craftingLogic.hasJob()) {
                cpus.add(cpu);
            } else {
                cpu.craftingLogic.storeItems();
                if (cpu.isMarkedForDeletion() || cpu.getInventory().list.isEmpty()) {
                    remove.add(entry.getKey());
                } else {
                    cpus.add(cpu);
                }
            }
        }
        for (UUID id : remove) {
            this.activeCpus.remove(id);
            this.clearLastSelectedCpu(id);
        }
        if (!remove.isEmpty()) {
            this.recalculateRemainingStorage();
        }
        return cpus;
    }

    public AdvCraftingCPU getRemainingCapacityCPU() {
        if (this.remainingStorageCpu == null
            || this.remainingStorageCpu.getAvailableStorage() != this.remainingStorage) {
            this.remainingStorageCpu = new AdvCraftingCPU(this, this.remainingStorage);
        }
        return this.remainingStorageCpu;
    }

    public AdvCraftingCPU getLastSelectedCpu() {
        if (this.lastSelectedRemainingCapacity) {
            return this.getRemainingCapacityCPU();
        }
        if (this.lastSelectedCpuId == null) {
            return null;
        }

        AdvCraftingCPU cpu = this.activeCpus.get(this.lastSelectedCpuId);
        if (cpu == null || cpu.isMarkedForDeletion()) {
            this.lastSelectedCpuId = null;
            return null;
        }
        return cpu;
    }

    public void setLastSelectedCpu(ICraftingCPU cpu) {
        if (!(cpu instanceof AdvCraftingCPU quantumCpu) || quantumCpu.getParent() != this) {
            this.lastSelectedCpuId = null;
            this.lastSelectedRemainingCapacity = false;
            return;
        }

        if (quantumCpu.uniqueId == null) {
            this.lastSelectedCpuId = null;
            this.lastSelectedRemainingCapacity = true;
            return;
        }

        this.lastSelectedCpuId = quantumCpu.uniqueId;
        this.lastSelectedRemainingCapacity = false;
    }

    public void deactivate(UUID id) {
        this.activeCpus.remove(id);
        this.clearLastSelectedCpu(id);
        this.recalculateRemainingStorage();
        this.updateGridForChangedCpu(this);
    }

    public void postCpuChange() {
        IGridNode node = this.getNode();
        if (node != null) {
            node.grid().postEvent(new GridCraftingCpuChange(node));
        }
    }

    public void recalculateRemainingStorage() {
        long totalStorage = this.storage;
        if (this.storageMultiplier > 0) {
            totalStorage *= this.storageMultiplier;
        }

        long usedStorage = 0;
        for (AdvCraftingCPU cpu : this.activeCpus.values()) {
            usedStorage += cpu.getAvailableStorage();
        }
        this.remainingStorage = Math.max(0, totalStorage - usedStorage);
        this.remainingStorageCpu = null;
    }

    private void killCpu(UUID id, boolean updateGrid) {
        AdvCraftingCPU cpu = this.activeCpus.remove(id);
        if (cpu == null) {
            return;
        }
        this.clearLastSelectedCpu(id);
        cpu.craftingLogic.cancel();
        cpu.markForDeletion();
        this.recalculateRemainingStorage();
        if (updateGrid) {
            this.updateGridForChangedCpu(this);
        }
    }

    private void updateGridForChangedCpu(AdvCraftingCPUCluster cluster) {
        boolean posted = false;
        for (TileAdvCraftingBlock tile : this.quantumBlockEntities) {
            IGridNode node = tile.getActionableNode();
            if (node != null && !posted) {
                node.grid().postEvent(new GridCraftingCpuChange(node));
                posted = true;
            }
            tile.updateStatus(cluster);
        }
    }

    private void clearLastSelectedCpu(UUID id) {
        if (id.equals(this.lastSelectedCpuId)) {
            this.lastSelectedCpuId = null;
            this.lastSelectedRemainingCapacity = false;
        }
    }

    /**
     * Checks if this CPU cluster can be automatically selected for a crafting request by the given action source.
     */
    public boolean canBeAutoSelectedFor(IActionSource source) {
        return switch (getSelectionMode()) {
            case ANY -> true;
            case PLAYER_ONLY -> source.player().isPresent();
            case MACHINE_ONLY -> source.player().isEmpty();
        };
    }

    /**
     * Checks if this CPU cluster is preferred for crafting requests by the given action source.
     */
    public boolean isPreferredFor(IActionSource source) {
        return switch (getSelectionMode()) {
            case ANY -> false;
            case PLAYER_ONLY -> source.player().isPresent();
            case MACHINE_ONLY -> source.player().isEmpty();
        };
    }
}
