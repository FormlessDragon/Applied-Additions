package com.formlesslab.ae2additions.cell;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.core.AEConfig;
import ae2.items.storage.StorageCellTooltipComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Serialization and display helpers for the single infinity-cell item.
 */
public final class InfinityCellContents {
    public static final String KEYS_TAG = "InfinityKeys";
    public static final String VARIANT_TAG = "InfinityVariant";
    public static final String DISPLAY_NAME_TAG = "InfinityDisplayName";

    private InfinityCellContents() {
    }

    /**
     * Creates an item stack whose contents are completely encoded in its NBT.
     */
    public static ItemStack createStack(Item item, Collection<? extends AEKey> keys, @Nullable String variantId, @Nullable String displayName) {
        Objects.requireNonNull(item, "item");
        List<AEKey> distinctKeys = distinct(keys);
        if (distinctKeys.isEmpty()) {
            throw new IllegalArgumentException("An infinity cell needs at least one AE key");
        }

        NBTTagList serializedKeys = new NBTTagList();
        for (AEKey key : distinctKeys) {
            serializedKeys.appendTag(Objects.requireNonNull(key, "key").toTagGeneric());
        }

        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(KEYS_TAG, serializedKeys);
        if (variantId != null && !variantId.isEmpty()) {
            tag.setString(VARIANT_TAG, variantId);
        }
        if (displayName != null && !displayName.isEmpty()) {
            tag.setString(DISPLAY_NAME_TAG, displayName);
        }

        ItemStack result = new ItemStack(item);
        result.setTagCompound(tag);
        return result;
    }

    /**
     * Reads and de-duplicates the AE keys stored in an infinity-cell stack.
     */
    public static List<AEKey> readKeys(@Nullable ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return Collections.emptyList();
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null || !tag.hasKey(KEYS_TAG, Constants.NBT.TAG_LIST)) {
            return Collections.emptyList();
        }

        NBTTagList serializedKeys = tag.getTagList(KEYS_TAG, Constants.NBT.TAG_COMPOUND);
        LinkedHashSet<AEKey> result = new LinkedHashSet<>();
        for (int index = 0; index < serializedKeys.tagCount(); index++) {
            AEKey key = AEKey.fromTagGeneric(serializedKeys.getCompoundTagAt(index));
            if (key != null) {
                result.add(key);
            }
        }
        return new ArrayList<>(result);
    }

    @Nullable
    public static String getDisplayName(@Nullable ItemStack stack) {
        NBTTagCompound tag = getTag(stack);
        if (tag == null || !tag.hasKey(DISPLAY_NAME_TAG, Constants.NBT.TAG_STRING)) {
            return null;
        }
        String value = tag.getString(DISPLAY_NAME_TAG);
        return value.isEmpty() ? null : value;
    }

    public static boolean isConfigured(@Nullable ItemStack stack) {
        return !readKeys(stack).isEmpty();
    }

    public static long getMaxAmount(AEKey key) {
        return (long) Integer.MAX_VALUE * key.getAmountPerUnit();
    }

    public static List<AEKey> distinct(Collection<? extends AEKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedHashSet<AEKey> result = new LinkedHashSet<>();
        for (AEKey key : keys) {
            result.add(Objects.requireNonNull(key, "key"));
        }
        return new ArrayList<>(result);
    }

    public static StorageCellTooltipComponent createTooltip(List<AEKey> keys) {
        int maxShown = Math.max(0, AEConfig.instance().getTooltipMaxCellContentShown());
        List<GenericStack> content = new ArrayList<>(Math.min(keys.size(), maxShown));
        for (int index = 0; index < keys.size() && index < maxShown; index++) {
            AEKey key = keys.get(index);
            content.add(new GenericStack(key, getMaxAmount(key)));
        }
        return new StorageCellTooltipComponent(Collections.emptyList(), content, keys.size() > maxShown, true);
    }

    @Nullable
    private static NBTTagCompound getTag(@Nullable ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.getTagCompound();
    }
}
