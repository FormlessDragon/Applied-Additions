package com.formlesslab.ae2additions.item;

import ae2.api.config.FuzzyMode;
import ae2.api.stacks.AEKey;
import ae2.api.storage.cells.ICellWorkbenchItem;
import ae2.api.storage.cells.IStackTooltipDataProvider;
import ae2.items.AEBaseItem;
import ae2.items.storage.StorageCellTooltipComponent;
import com.formlesslab.ae2additions.Reference;
import com.formlesslab.ae2additions.cell.InfinityCellContents;
import com.formlesslab.ae2additions.init.ModContent;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

/**
 * The single registered infinity-cell item; each configured instance is NBT-backed.
 */
public class ItemInfinityCell extends AEBaseItem implements ICellWorkbenchItem, IStackTooltipDataProvider {
    public ItemInfinityCell() {
        setMaxStackSize(1);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String customName = InfinityCellContents.getDisplayName(stack);
        if (customName != null) {
            return customName;
        }

        List<AEKey> keys = InfinityCellContents.readKeys(stack);
        if (keys.size() == 1) {
            return new TextComponentTranslation("item." + Reference.MOD_ID + ".infinity_cell.name", keys.getFirst().getDisplayName()).getFormattedText();
        }
        return new TextComponentTranslation("item." + Reference.MOD_ID + ".infinity_cell.multiple.name").getFormattedText();
    }

    @Override
    protected void addCheckedInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
        tooltip.add(TextFormatting.GREEN + new TextComponentTranslation("tooltip." + Reference.MOD_ID + ".infinity_cell").getFormattedText());
    }

    @Override
    protected void getCheckedSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        items.addAll(ModContent.getInfinityCellVariants());
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
        return Optional.of(InfinityCellContents.createTooltip(InfinityCellContents.readKeys(stack)));
    }

}
