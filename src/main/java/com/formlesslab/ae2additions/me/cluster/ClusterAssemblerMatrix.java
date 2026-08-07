package com.formlesslab.ae2additions.me.cluster;

import ae2.api.config.Setting;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.ticking.TickRateModulation;
import ae2.api.stacks.KeyCounter;
import ae2.api.util.IConfigManager;
import ae2.me.cluster.IAECluster;
import ae2.me.cluster.MBCalculator;
import ae2.me.helpers.MachineSource;
import ae2.util.NullConfigManager;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixBase;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixCrafter;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixFunction;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixPattern;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClusterAssemblerMatrix implements IAECluster {
    private static final double MAX_QUEUE_PROGRESS = 100;

    private final BlockPos boundsMin;
    private final BlockPos boundsMax;
    private final List<TileAssemblerMatrixBase> tiles = new ArrayList<>();
    private final List<TileAssemblerMatrixPattern> patterns = new ArrayList<>();
    private final Set<TileAssemblerMatrixCrafter> crafters = Collections.newSetFromMap(new IdentityHashMap<>());
    private boolean isDestroyed;
    private ITextComponent myName;
    private MachineSource machineSrc;
    private IConfigManager manager = NullConfigManager.INSTANCE;
    private int speedCore;
    private double queueProgress;
    private boolean queueReboot = true;
    private long lastQueueTick = Long.MIN_VALUE;

    public ClusterAssemblerMatrix(BlockPos boundsMin, BlockPos boundsMax) {
        this.boundsMin = boundsMin.toImmutable();
        this.boundsMax = boundsMax.toImmutable();
    }

    private static int saturatedAdd(int left, int right) {
        return left > Integer.MAX_VALUE - right ? Integer.MAX_VALUE : left + right;
    }

    private static int saturatedMultiply(int left, int right) {
        return left > Integer.MAX_VALUE / right ? Integer.MAX_VALUE : left * right;
    }

    public void addCrafter(TileAssemblerMatrixCrafter crafter) {
        this.crafters.add(crafter);
        if (crafter.hasQueuedJobs()) {
            this.queueProgress = Math.max(this.queueProgress, crafter.getSavedQueueProgress());
        }
    }

    public void addSpeedCore() {
        if (this.speedCore < 5) {
            this.speedCore++;
        }
    }

    public int getSpeedCore() {
        return this.speedCore;
    }

    public IConfigManager getConfigManager() {
        return this.manager;
    }

    public int getQueuedJobAmount() {
        int count = 0;
        for (TileAssemblerMatrixCrafter crafter : this.crafters) {
            count = saturatedAdd(count, crafter.getQueuedJobCount());
        }
        return count;
    }

    public int getUsedParallelAmount() {
        int count = 0;
        for (TileAssemblerMatrixCrafter crafter : this.crafters) {
            count = saturatedAdd(count, crafter.getUsedParallel());
        }
        return count;
    }

    public int getAvailableQueueAmount() {
        return Math.max(0, getTotalQueueCapacity() - this.getQueuedJobAmount());
    }

    public int getAvailableParallelAmount() {
        return Math.max(0, getTotalParallelCapacity() - this.getUsedParallelAmount());
    }

    public int getMaxPatternPushMultiplier(int maxMultiplier) {
        if (maxMultiplier <= 0 || this.getAvailableQueueAmount() <= 0) {
            return 0;
        }
        return Math.min(maxMultiplier, this.getAvailableParallelAmount());
    }

    public boolean hasQueuedJobs() {
        return this.getQueuedJobAmount() > 0;
    }

    public double getQueueProgress() {
        return this.queueProgress;
    }

    public void addPattern(TileAssemblerMatrixPattern pattern) {
        this.patterns.add(pattern);
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
        for (TileAssemblerMatrixBase tile : this.tiles) {
            tile.updateSubType(updateGrid);
        }
    }

    public List<TileAssemblerMatrixPattern> getPatterns() {
        return Collections.unmodifiableList(this.patterns);
    }

    public void drainPatternsTo(List<ItemStack> drops) {
        for (TileAssemblerMatrixPattern pattern : this.patterns) {
            pattern.drainPatternsTo(drops);
        }
    }

    public void done() {
        TileAssemblerMatrixBase core = this.getCore();
        if (core == null && !this.tiles.isEmpty()) {
            core = this.tiles.getFirst();
            this.machineSrc = new MachineSource(core);
        }
        if (core == null) {
            return;
        }

        core.setCore(true);
        if (core.getPreviousState() != null) {
            core.setPreviousState(null);
        }
        this.manager = core.getConfigManager();
        for (Setting<?> setting : this.manager.getSettings()) {
            this.broadcastExistingSetting(setting, core);
        }
        this.updateName();
        this.syncQueueProgress();
        this.wakeWorkingCrafters();
    }

    @SuppressWarnings("unchecked")
    private <T extends Enum<T>> void broadcastExistingSetting(Setting<?> setting, TileAssemblerMatrixBase core) {
        Setting<T> typed = (Setting<T>) setting;
        this.broadcastConfig(typed, this.manager.getSetting(typed), core);
    }

    public <T extends Enum<T>> void broadcastConfig(Setting<T> setting, T newValue, @Nullable TileAssemblerMatrixBase ignore) {
        for (TileAssemblerMatrixBase tile : this.tiles) {
            if (tile != ignore) {
                tile.applyConfigFromCluster(setting, newValue);
            }
        }
    }

    @Override
    public void destroy() {
        if (this.isDestroyed) {
            return;
        }
        this.isDestroyed = true;
        this.syncQueueProgress();
        boolean ownsModification = !MBCalculator.isModificationInProgress();
        if (ownsModification) {
            MBCalculator.setModificationInProgress(this);
        }
        try {
            for (TileAssemblerMatrixBase tile : this.tiles) {
                tile.updateStatus(null);
            }
        } finally {
            if (ownsModification) {
                MBCalculator.setModificationInProgress(null);
            }
        }
    }

    @Override
    public boolean isDestroyed() {
        return this.isDestroyed;
    }

    @Override
    public Iterator<TileAssemblerMatrixBase> getBlockEntities() {
        return this.tiles.iterator();
    }

    public void updateName() {
        StringBuilder name = new StringBuilder();
        for (TileAssemblerMatrixBase tile : this.tiles) {
            if (tile.hasCustomName()) {
                if (!name.isEmpty()) {
                    name.append(' ');
                }
                name.append(tile.getCustomName());
            }
        }
        this.myName = !name.isEmpty() ? new TextComponentString(name.toString()) : null;
    }

    public void addTileEntity(TileAssemblerMatrixBase tile) {
        if (this.machineSrc == null || tile.isCore()) {
            this.machineSrc = new MachineSource(tile);
        }
        tile.setCore(false);
        tile.saveChanges();
        this.tiles.add(tile);
        if (tile instanceof TileAssemblerMatrixFunction function) {
            function.add(this);
        }
    }

    public boolean isBusy() {
        return this.getAvailableQueueAmount() <= 0 || this.getAvailableParallelAmount() <= 0;
    }

    public void cancelJobs() {
        for (TileAssemblerMatrixCrafter crafter : this.crafters) {
            crafter.cancelQueuedJobs();
        }
        this.queueProgress = 0;
        this.queueReboot = true;
        this.lastQueueTick = Long.MIN_VALUE;
        this.syncQueueProgress();
        this.wakeWorkingCrafters();
    }

    public boolean pushCraftingJob(IPatternDetails patternDetails, KeyCounter[] inputHolder, int craftCount) {
        if (craftCount <= 0 || this.getAvailableQueueAmount() <= 0 || craftCount > this.getAvailableParallelAmount()) {
            return false;
        }

        TileAssemblerMatrixCrafter crafter = null;
        for (TileAssemblerMatrixCrafter candidate : this.crafters) {
            if (candidate.hasQueueSpace() && (crafter == null || candidate.getQueuedJobCount() < crafter.getQueuedJobCount())) {
                crafter = candidate;
            }
        }
        if (crafter == null) {
            return false;
        }

        boolean firstJob = !this.hasQueuedJobs();
        if (!crafter.pushJob(patternDetails, inputHolder, craftCount)) {
            return false;
        }
        if (firstJob) {
            this.queueProgress = 0;
            this.queueReboot = true;
            this.lastQueueTick = Long.MIN_VALUE;
        }
        this.syncQueueProgress();
        this.wakeWorkingCrafters();
        return true;
    }

    public TickRateModulation tickCraftingQueue(TileAssemblerMatrixCrafter tickingCrafter, int ticksSinceLastCall) {
        if (!this.hasQueuedJobs()) {
            return TickRateModulation.SLEEP;
        }

        if (tickingCrafter.getWorld() != null) {
            long worldTick = tickingCrafter.getWorld().getTotalWorldTime();
            if (worldTick == this.lastQueueTick) {
                return TickRateModulation.FASTER;
            }
            this.lastQueueTick = worldTick;
        }

        if (this.queueReboot) {
            ticksSinceLastCall = 1;
            this.queueReboot = false;
        }
        this.queueProgress += tickingCrafter.advanceQueueTimer(this.speedCore, ticksSinceLastCall);
        this.syncQueueProgress();
        if (this.queueProgress < MAX_QUEUE_PROGRESS) {
            return TickRateModulation.FASTER;
        }

        for (TileAssemblerMatrixCrafter crafter : this.crafters) {
            crafter.completeQueuedJobs();
        }
        this.queueProgress = 0;
        this.queueReboot = true;
        this.lastQueueTick = Long.MIN_VALUE;
        this.syncQueueProgress();
        this.wakeWorkingCrafters();
        return TickRateModulation.IDLE;
    }

    public void breakCluster() {
        TileAssemblerMatrixBase tile = this.getCore();
        if (tile != null) {
            tile.breakCluster();
        }
    }

    @Nullable
    private TileAssemblerMatrixBase getCore() {
        if (this.machineSrc == null) {
            return null;
        }
        return this.machineSrc.machine().filter(TileAssemblerMatrixBase.class::isInstance).map(TileAssemblerMatrixBase.class::cast).orElse(null);
    }

    public IActionSource getSrc() {
        return Objects.requireNonNull(this.machineSrc);
    }

    public ITextComponent getName() {
        return this.myName;
    }

    private int getTotalQueueCapacity() {
        return saturatedMultiply(this.crafters.size(), TileAssemblerMatrixCrafter.getMaxQueueSize());
    }

    private int getTotalParallelCapacity() {
        return saturatedMultiply(this.crafters.size(), TileAssemblerMatrixCrafter.getMaxParallel());
    }

    private void syncQueueProgress() {
        for (TileAssemblerMatrixCrafter crafter : this.crafters) {
            crafter.setSavedQueueProgress(this.queueProgress);
        }
    }

    private void wakeWorkingCrafters() {
        for (TileAssemblerMatrixCrafter crafter : this.crafters) {
            if (crafter.hasQueuedJobs() || crafter.hasBufferedOutputs()) {
                crafter.wakeForWork();
            }
        }
    }

    @Nullable
    public IGridNode getNode() {
        TileAssemblerMatrixBase core = getCore();
        return core != null ? core.getActionableNode() : null;
    }
}
