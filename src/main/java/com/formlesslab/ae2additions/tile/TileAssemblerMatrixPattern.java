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
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class TileAssemblerMatrixPattern extends TileAssemblerMatrixFunction
    implements InternalInventoryHost, ICraftingProvider, PatternContainer {

    public static final int INV_SIZE = 36;

    private final AppEngInternalInventory patternInventory;
    private final List<IAssemblerPattern> patterns = new ArrayList<>();
    private final Set<AEItemKey> patternKeys = new HashSet<>();

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
                drops.add(pattern);
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

    @Override
    public List<IAssemblerPattern> getAvailablePatterns() {
        return this.patterns;
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder, int craftCount) {
        if (!isFormed()
            || !this.getMainNode().isActive()
            || !(patternDetails instanceof IAssemblerPattern)
            || !this.patterns.contains(patternDetails)
            || this.cluster == null) {
            return false;
        }
        return this.cluster.pushCraftingJob(patternDetails, inputHolder, craftCount);
    }

    @Override
    public boolean canMergePatternPush(IPatternDetails patternDetails) {
        return false;
    }

    @Override
    public int getMaxPatternPushMultiplier(IPatternDetails patternDetails, int maxMultiplier) {
        if (maxMultiplier <= 0
            || !(patternDetails instanceof IAssemblerPattern)
            || !this.patterns.contains(patternDetails)
            || this.cluster == null
            || this.cluster.isBusy()) {
            return 0;
        }
        return 1;
    }

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
        ITextComponent name = this.hasCustomName() ? new TextComponentString(this.getCustomName()) : icon.getDisplayName();
        return new PatternContainerGroup(
            icon,
            name,
            java.util.List.of(new TextComponentTranslation("gui.ae2additions.assembler_matrix.pattern"))
        );
    }

    private record BlockPosBits(int x, int y, int z) {
        long asLong() {
            return ((long) (this.x & 0x3FFFFFF) << 38)
                | ((long) (this.z & 0x3FFFFFF) << 12)
                | (this.y & 0xFFF);
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
