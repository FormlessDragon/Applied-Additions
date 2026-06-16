package com.formlesslab.ae2additions.quantum.cluster;

import ae2.api.config.Actionable;
import ae2.api.config.CpuSelectionMode;
import ae2.api.config.Settings;
import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.crafting.CraftingJobStatus;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingRequester;
import ae2.api.networking.crafting.ICraftingSubmitResult;
import ae2.api.networking.events.GridCraftingCpuChange;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.crafting.execution.CraftingSubmitResult;
import ae2.crafting.inv.ListCraftingInventory;
import ae2.me.cluster.MBCalculator;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import ae2.me.helpers.MachineSource;
import ae2.me.service.CraftingService;
import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

public class AdvCraftingCPUCluster extends CraftingCPUCluster {
    private static final String TAG_CPUS = "cpus";
    private static final String TAG_CPU_LIST_COMPAT = "cpuList";
    private static final String TAG_KEY = "key";
    private static final String TAG_BYTES = "bytes";
    private static final String TAG_CPU = "cpu";
    private static final String TAG_CONFIG = "config";

    private final Map<UUID, AdvCraftingCPU> activeCpus = new HashMap<>();
    private final List<AdvCraftingBlockEntity> quantumBlockEntities = new ObjectArrayList<>();
    private AdvCraftingCPU remainingStorageCpu;
    private long storageMultiplier;
    private long remainingStorage;
    private int acceleratorMultiplier;

    public AdvCraftingCPUCluster(BlockPos boundsMin, BlockPos boundsMax) {
        super(boundsMin, boundsMax);
    }

    public Iterator<AdvCraftingBlockEntity> getQuantumBlockEntities() {
        return this.quantumBlockEntities.iterator();
    }

    public int numBlockEntities() {
        return this.quantumBlockEntities.size();
    }

    public void addQuantumBlockEntity(AdvCraftingBlockEntity tile) {
        if (this.machineSrc == null || tile.isCoreBlock()) {
            this.machineSrc = new MachineSource(tile);
        }

        tile.setCoreBlock(false);
        tile.saveChanges();
        this.quantumBlockEntities.addFirst(tile);
        super.addTileEntity(tile);

        if (tile.getStorageMultiplier() > 0) {
            this.storageMultiplier += tile.getStorageMultiplier();
        }
        if (tile.getAccelerationMultiplier() > 0) {
            this.acceleratorMultiplier += tile.getAccelerationMultiplier();
        }
        this.recalculateRemainingStorage();
    }

    @Override
    public void addTileEntity(ae2.tile.crafting.ICraftingCPUTileEntity tile) {
        if (tile instanceof AdvCraftingBlockEntity quantumTile) {
            this.addQuantumBlockEntity(quantumTile);
        } else {
            super.addTileEntity(tile);
            this.recalculateRemainingStorage();
        }
    }

    @Override
    public void updateStatus(boolean updateGrid) {
        for (AdvCraftingBlockEntity tile : this.quantumBlockEntities) {
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
    public long insert(AEKey what, long amount, Actionable type, IActionSource src) {
        return this.insertIntoActiveCpus(what, amount, type);
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

    @Override
    public void cancelJob() {
        this.cancelJobs();
    }

    public void cancelJob(UUID id) {
        this.killCpu(id, true);
    }

    @Override
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

    @Override
    public boolean isBusy() {
        return !this.getActiveCPUs().isEmpty();
    }

    @Override
    public CraftingJobStatus getJobStatus() {
        List<AdvCraftingCPU> active = this.getActiveCPUs();
        return active.isEmpty() ? null : active.getFirst().getJobStatus();
    }

    @Override
    public long getAvailableStorage() {
        return this.remainingStorage;
    }

    @Override
    public int getCoProcessors() {
        int coProcessors = this.accelerator;
        if (this.acceleratorMultiplier > 0) {
            coProcessors *= this.acceleratorMultiplier;
        }
        return coProcessors;
    }

    @Override
    public void updateOutput(GenericStack finalOutput) {
        GenericStack stack = finalOutput != null && finalOutput.amount() <= 0 ? null : finalOutput;
        for (var monitor : this.status) {
            monitor.setJob(stack);
        }
    }

    @Override
    public void markDirty() {
        AdvCraftingBlockEntity core = this.getCore();
        if (core != null) {
            core.saveChanges();
        }
    }

    @Override
    protected AdvCraftingBlockEntity getCore() {
        if (this.machineSrc == null) {
            return null;
        }
        return this.machineSrc.machine()
            .filter(AdvCraftingBlockEntity.class::isInstance)
            .map(AdvCraftingBlockEntity.class::cast)
            .orElse(null);
    }

    @Override
    public World getLevel() {
        AdvCraftingBlockEntity core = this.getCore();
        return core == null ? null : core.getWorld();
    }

    @Override
    public IGridNode getNode() {
        AdvCraftingBlockEntity core = this.getCore();
        return core == null ? null : core.getActionableNode();
    }

    @Override
    public void done() {
        AdvCraftingBlockEntity core = this.getCore();
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

    @Override
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

    @Override
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

    @Override
    public void updateName() {
        this.myName = null;
        for (AdvCraftingBlockEntity tile : this.quantumBlockEntities) {
            if (tile.hasCustomName() && tile.getCustomName() != null) {
                if (this.myName == null) {
                    this.myName = new TextComponentString(tile.getCustomName());
                } else {
                    this.myName.appendText(" ").appendText(tile.getCustomName());
                }
            }
        }
    }

    @Override
    public void breakCluster() {
        AdvCraftingBlockEntity core = this.getCore();
        if (core != null) {
            core.breakCluster();
        }
    }

    @Override
    public CpuSelectionMode getSelectionMode() {
        return this.configManager.getSetting(Settings.CPU_SELECTION_MODE);
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

    public void deactivate(UUID id) {
        this.activeCpus.remove(id);
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
        cpu.craftingLogic.cancel();
        cpu.markForDeletion();
        this.recalculateRemainingStorage();
        if (updateGrid) {
            this.updateGridForChangedCpu(this);
        }
    }

    private void updateGridForChangedCpu(AdvCraftingCPUCluster cluster) {
        boolean posted = false;
        for (AdvCraftingBlockEntity tile : this.quantumBlockEntities) {
            IGridNode node = tile.getActionableNode();
            if (node != null && !posted) {
                node.grid().postEvent(new GridCraftingCpuChange(node));
                posted = true;
            }
            tile.updateStatus(cluster);
        }
    }
}
