package com.formlesslab.ae2additions.item;

import ae2.api.config.FuzzyMode;
import ae2.api.stacks.AEKey;
import ae2.api.storage.cells.ICellWorkbenchItem;
import ae2.api.storage.cells.IStackTooltipDataProvider;
import ae2.items.AEBaseItem;
import ae2.items.storage.StorageCellTooltipComponent;
import com.formlesslab.ae2additions.Reference;
import com.formlesslab.ae2additions.cell.InfinityCellContents;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Supplier;

/**
 * A cell that exposes an unlimited amount of one or more arbitrary AE keys.
 */
public class ItemInfinityCell extends AEBaseItem implements ICellWorkbenchItem, IStackTooltipDataProvider {
    private final List<Supplier<? extends AEKey>> keySuppliers;
    private final String displayName;

    public ItemInfinityCell(Supplier<? extends AEKey> keySupplier) {
        this(Collections.singletonList(keySupplier), null);
    }

    public ItemInfinityCell(Supplier<? extends AEKey> keySupplier, @Nullable String displayName) {
        this(Collections.singletonList(keySupplier), displayName);
    }

    public ItemInfinityCell(Collection<? extends Supplier<? extends AEKey>> keySuppliers, @Nullable String displayName) {
        if (keySuppliers == null || keySuppliers.isEmpty()) {
            throw new IllegalArgumentException("An infinity cell needs at least one AE key");
        }
        List<Supplier<? extends AEKey>> copy = new ArrayList<>(keySuppliers.size());
        for (Supplier<? extends AEKey> supplier : keySuppliers) {
            copy.add(Objects.requireNonNull(supplier, "keySupplier"));
        }
        this.keySuppliers = Collections.unmodifiableList(copy);
        this.displayName = displayName;
        setMaxStackSize(1);
    }

    public AEKey getKey() {
        return getKeys().getFirst();
    }

    public Supplier<? extends AEKey> getKeySupplier() {
        return keySuppliers.getFirst();
    }

    public List<AEKey> getKeys() {
        LinkedHashSet<AEKey> keys = new LinkedHashSet<>();
        for (Supplier<? extends AEKey> supplier : keySuppliers) {
            keys.add(Objects.requireNonNull(supplier.get(), "Infinity cell key supplier returned null"));
        }
        return new ArrayList<>(keys);
    }

    public List<Supplier<? extends AEKey>> getKeySuppliers() {
        return keySuppliers;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (displayName != null && !displayName.isEmpty()) {
            return displayName;
        }
        if (getKeys().size() > 1) {
            return new TextComponentTranslation("item." + Reference.MOD_ID + ".infinity_cell.multiple.name").getFormattedText();
        }
        return new TextComponentTranslation("item." + Reference.MOD_ID + ".infinity_cell.name", getKey().getDisplayName()).getFormattedText();
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GREEN + new TextComponentTranslation("tooltip." + Reference.MOD_ID + ".infinity_cell").getFormattedText());
    }

    @Override
    public FuzzyMode getFuzzyMode(ItemStack stack) {
        return FuzzyMode.IGNORE_ALL;
    }

    @Override
    public void setFuzzyMode(ItemStack stack, FuzzyMode mode) {
        // Infinity cells always compare exact AE keys, including NBT.
    }

    @Override
    public Optional<StorageCellTooltipComponent> getStackTooltipData(ItemStack stack) {
        return Optional.of(InfinityCellContents.createTooltip(getKeys()));
    }
}
