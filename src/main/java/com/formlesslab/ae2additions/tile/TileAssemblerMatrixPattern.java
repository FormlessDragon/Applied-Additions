package com.formlesslab.ae2additions.tile;

import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.crafting.IAssemblerPattern;
import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.implementations.blockentities.PatternContainerGroup;
import ae2.api.inventories.InternalInventory;
import ae2.api.networking.IGrid;
import ae2.api.networking.crafting.ICraftingProvider;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.KeyCounter;
import ae2.helpers.patternprovider.PatternContainer;
import ae2.util.inv.AppEngInternalInventory;
import ae2.util.inv.InternalInventoryHost;
import ae2.util.inv.filter.IAEItemFilter;
import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class TileAssemblerMatrixPattern extends TileAssemblerMatrixFunction implements InternalInventoryHost, ICraftingProvider, PatternContainer {

    public static final int INV_SIZE = 36;

    private final AppEngInternalInventory patternInventory;
    private final List<IAssemblerPattern> patterns = new ObjectArrayList<>();
    private final Set<AEItemKey> patternKeys = new ObjectOpenHashSet<>();

    public TileAssemblerMatrixPattern() {
        this.patternInventory = new AppEngInternalInventory(this, INV_SIZE, 1);
        this.patternInventory.setFilter(new PatternFilter(this::getWorld));
        this.getMainNode().addService(ICraftingProvider.class, this);
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.patternInventory.writeToNBT(data, "pattern");
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.patternInventory.readFromNBT(data, "pattern");
        this.updatePatterns();
    }

    @Override
    public boolean isVisibleInTerminal() {
        return this.manager.getSetting(Settings.PATTERN_ACCESS_TERMINAL) == YesNo.YES;
    }

    public AppEngInternalInventory getPatternInventory() {
        return this.patternInventory;
    }

    public AppEngInternalInventory getExposedInventory() {
        return this.patternInventory;
    }

    public long getLocateID() {
        BlockPosBits bits = new BlockPosBits(this.pos.getX(), this.pos.getY(), this.pos.getZ());
        return bits.asLong();
    }

    public void updatePatterns() {
        this.patterns.clear();
        this.patternKeys.clear();
        World world = this.getWorld();
        if (world != null) {
            for (ItemStack stack : this.patternInventory) {
                IPatternDetails details = PatternDetailsHelper.decodePattern(stack, world);
                if (details instanceof IAssemblerPattern pattern) {
                    this.patterns.add(pattern);
                    this.patternKeys.add(pattern.getDefinition());
                }
            }
        }
        ICraftingProvider.requestUpdate(this.getMainNode());
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        for (ItemStack pattern : this.patternInventory) {
            if (!pattern.isEmpty()) {
                drops.add(pattern.copy());
            }
        }
    }

    public void drainPatternsTo(List<ItemStack> drops) {
        InternalInventory inventory = this.patternInventory;
        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack pattern = inventory.getStackInSlot(slot);
            if (!pattern.isEmpty()) {
                drops.add(pattern.copy());
                inventory.setItemDirect(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.patternInventory.clear();
    }

    @Override
    public void add(ClusterAssemblerMatrix cluster) {
        cluster.addPattern(this);
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        this.saveChanges();
        this.updatePatterns();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        this.saveChangedInventory(inv);
    }

    @Override
    public void onReady() {
        super.onReady();
        this.updatePatterns();
    }

    /**
     * Patterns are only visible to the crafting service while the matrix is formed and powered. Otherwise they drop
     * out of the network's pattern index instead of failing later inside {@link #pushPattern}.
     */
    @Override
    public List<IAssemblerPattern> getAvailablePatterns() {
        if (!isFormed() || !this.getMainNode().isOnline()) {
            return List.of();
        }
        return this.patterns;
    }

    @Override
    public int getPatternPriority() {
        return this.cluster == null ? 0 : this.cluster.getPatternPriority();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int craftCount) {
        if (!isFormed() || !this.getMainNode().isActive() || !(patternDetails instanceof IAssemblerPattern) || !this.patterns.contains(patternDetails) || this.cluster == null) {
            return false;
        }
        return this.cluster.pushCraftingJob(patternDetails, inputHolder, craftCount);
    }

    /**
     * Merge push is decided purely by pattern ownership so AE2S keeps using the merged path for this provider. When
     * the matrix is currently unable to work, {@link #getMaxPatternPushMultiplier} reports 0 and AE2S skips this
     * provider for the pass instead of extracting inputs and failing in {@link #pushPattern}.
     */
    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        return patternDetails instanceof IAssemblerPattern && this.patterns.contains(patternDetails);
    }

    @Override
    public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
        if (maxMultiplier <= 0 || !isFormed() || !this.getMainNode().isActive() || this.cluster == null || !this.canMergePatternPush(patternDetails)) {
            return 0;
        }
        return this.cluster.getMaxPatternPushMultiplier(maxMultiplier);
    }

    /**
     * Only the shared parallel capacity limits this provider; when it is exhausted AE2S must not send patterns.
     */
    @Override
    public boolean isBusy() {
        return this.cluster == null || this.cluster.isBusy();
    }

    @Override
    public @Nullable IGrid getGrid() {
        return this.getMainNode().getGrid();
    }

    @Override
    public InternalInventory getTerminalPatternInventory() {
        return this.patternInventory;
    }

    @Override
    public boolean isAssemblerPatternContainer() {
        return true;
    }

    @Override
    public boolean containsPattern(AEItemKey pattern) {
        return this.patternKeys.contains(pattern);
    }

    @Override
    public long getTerminalSortOrder() {
        return this.getLocateID();
    }

    @Override
    public PatternContainerGroup getTerminalGroup() {
        ItemStack iconStack = this.getItemFromTile();
        if (iconStack.isEmpty()) {
            iconStack = new ItemStack(Items.PAPER);
        }
        AEItemKey icon = AEItemKey.of(iconStack);
        ITextComponent name = this.hasCustomName() ? new TextComponentString(this.getCustomName()) : Objects.requireNonNull(icon).getDisplayName();
        return new PatternContainerGroup(icon, name, List.of(new TextComponentTranslation("gui.ae2additions.assembler_matrix.pattern")));
    }

    private record BlockPosBits(int x, int y, int z) {
        long asLong() {
            return ((long) (this.x & 0x3FFFFFF) << 38) | ((long) (this.z & 0x3FFFFFF) << 12) | (this.y & 0xFFF);
        }
    }

    private record PatternFilter(Supplier<World> world) implements IAEItemFilter {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            World level = this.world.get();
            return level != null && PatternDetailsHelper.decodePattern(stack, level) instanceof IAssemblerPattern;
        }
    }
}
