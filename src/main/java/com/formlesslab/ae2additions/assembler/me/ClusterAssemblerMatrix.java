package com.formlesslab.ae2additions.assembler.me;

import ae2.api.config.Setting;
import ae2.api.crafting.IPatternDetails;
import ae2.api.networking.IGridNode;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.KeyCounter;
import ae2.api.util.IConfigManager;
import ae2.me.cluster.IAECluster;
import ae2.me.cluster.MBCalculator;
import ae2.me.helpers.MachineSource;
import ae2.util.NullConfigManager;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixBase;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixCrafter;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixFunction;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixPattern;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClusterAssemblerMatrix implements IAECluster {
    private final BlockPos boundsMin;
    private final BlockPos boundsMax;
    private boolean isDestroyed;
    private ITextComponent myName;
    private final List<TileAssemblerMatrixBase> tiles = new ArrayList<>();
    private MachineSource machineSrc;
    private IConfigManager manager = NullConfigManager.INSTANCE;
    private final List<TileAssemblerMatrixPattern> patterns = new ArrayList<>();
    private final Set<TileAssemblerMatrixCrafter> availableCrafters = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<TileAssemblerMatrixCrafter> busyCrafters = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<TileAssemblerMatrixCrafter, Integer> crafterStatusCache = new IdentityHashMap<>();
    private int speedCore;

    public ClusterAssemblerMatrix(BlockPos boundsMin, BlockPos boundsMax) {
        this.boundsMin = boundsMin.toImmutable();
        this.boundsMax = boundsMax.toImmutable();
    }

    public void addCrafter(TileAssemblerMatrixCrafter crafter) {
        if (crafter.usedThread() < TileAssemblerMatrixCrafter.MAX_THREAD) {
            this.availableCrafters.add(crafter);
        } else {
            this.busyCrafters.add(crafter);
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

    public int getBusyCrafterAmount() {
        int count = this.busyCrafters.size() * TileAssemblerMatrixCrafter.MAX_THREAD;
        for (TileAssemblerMatrixCrafter crafter : this.availableCrafters) {
            count += crafter.usedThread();
        }
        return count;
    }

    public int getAvailableThreadAmount() {
        int count = 0;
        for (TileAssemblerMatrixCrafter crafter : this.availableCrafters) {
            count += TileAssemblerMatrixCrafter.MAX_THREAD - crafter.usedThread();
        }
        return count;
    }

    public void updateCrafter(TileAssemblerMatrixCrafter crafter) {
        int used = crafter.usedThread();
        Integer previous = this.crafterStatusCache.get(crafter);
        if (previous != null && previous == used) {
            return;
        }
        this.crafterStatusCache.put(crafter, used);
        this.availableCrafters.remove(crafter);
        this.busyCrafters.remove(crafter);
        this.addCrafter(crafter);
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

    @Nullable
    private TileAssemblerMatrixCrafter getAvailableCrafter() {
        for (TileAssemblerMatrixCrafter crafter : this.availableCrafters) {
            return crafter;
        }
        return null;
    }

    @Override
    public void destroy() {
        if (this.isDestroyed) {
            return;
        }
        this.isDestroyed = true;
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
        return this.availableCrafters.isEmpty();
    }

    public void cancelJobs() {
        for (TileAssemblerMatrixCrafter crafter : this.availableCrafters) {
            crafter.stop();
            this.updateCrafter(crafter);
        }
        for (TileAssemblerMatrixCrafter crafter : new ArrayList<>(this.busyCrafters)) {
            crafter.stop();
            this.updateCrafter(crafter);
        }
    }

    public boolean pushCraftingJob(IPatternDetails patternDetails, KeyCounter[] inputHolder, int craftCount) {
        if (craftCount != 1) {
            return false;
        }
        TileAssemblerMatrixCrafter crafter = this.getAvailableCrafter();
        if (crafter == null) {
            return false;
        }
        return crafter.pushJob(patternDetails, inputHolder);
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
        return this.machineSrc.machine()
            .filter(TileAssemblerMatrixBase.class::isInstance)
            .map(TileAssemblerMatrixBase.class::cast)
            .orElse(null);
    }

    public IActionSource getSrc() {
        return Objects.requireNonNull(this.machineSrc);
    }

    public ITextComponent getName() {
        return this.myName;
    }

    @Nullable
    public IGridNode getNode() {
        TileAssemblerMatrixBase core = getCore();
        return core != null ? core.getActionableNode() : null;
    }
}
