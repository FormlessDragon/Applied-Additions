package com.formlesslab.ae2additions.util;

import ae2.api.stacks.GenericStack;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.core.definitions.AEItems;
import ae2.util.ConfigInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public final class ImportExportCardConfig {
    public static final String FILTER_CONFIG = "filter_config";
    public static final String SELECTED_SLOTS = "selected_inventory_slots";
    public static final int IMPORT_UPGRADE_SLOTS = 4;
    public static final int EXPORT_UPGRADE_SLOTS = 5;
    public static final int BASE_FILTER_SLOTS = 18;
    public static final int FILTER_SLOTS_PER_CAPACITY = 9;
    public static final int MAX_CAPACITY_CARDS = 3;
    public static final int SLOT_COUNT = 40;

    private ImportExportCardConfig() {
    }

    public static int getUpgradeSlotCount(boolean export) {
        return export ? EXPORT_UPGRADE_SLOTS : IMPORT_UPGRADE_SLOTS;
    }

    public static int getCapacityCardCount(IUpgradeInventory upgrades) {
        return Math.min(MAX_CAPACITY_CARDS, upgrades.getInstalledUpgrades(AEItems.CAPACITY_CARD.item()));
    }

    public static int getFilterSize(IUpgradeInventory upgrades) {
        return BASE_FILTER_SLOTS + getCapacityCardCount(upgrades) * FILTER_SLOTS_PER_CAPACITY;
    }

    public static int[] getSelectedSlots(ItemStack card) {
        int[] result = new int[SLOT_COUNT];
        NBTTagCompound tag = card.getTagCompound();

        if (tag != null && tag.hasKey(SELECTED_SLOTS)) {
            int[] stored = tag.getIntArray(SELECTED_SLOTS);
            System.arraycopy(stored, 0, result, 0, Math.min(stored.length, result.length));
        }
        return result;
    }

    public static void setSelectedSlots(ItemStack card, int[] slots) {
        NBTTagCompound tag = card.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        int[] normalized = new int[SLOT_COUNT];
        System.arraycopy(slots, 0, normalized, 0, Math.min(slots.length, normalized.length));
        tag.setIntArray(SELECTED_SLOTS, normalized);
        card.setTagCompound(tag);
    }

    public static ConfigInventory readFilters(ItemStack card, int size) {
        ConfigInventory inventory = ConfigInventory.configTypes(size).build();
        NBTTagCompound tag = card.getTagCompound();
        if (tag != null && tag.hasKey(FILTER_CONFIG)) {
            inventory.readFromChildTag(tag, FILTER_CONFIG);
        }
        return inventory;
    }

    public static void writeFilters(ItemStack card, ConfigInventory inventory) {
        NBTTagCompound tag = card.getTagCompound();
        if (tag == null) {
            tag = new NBTTagCompound();
        }
        inventory.writeToChildTag(tag, FILTER_CONFIG);
        card.setTagCompound(tag);
    }

    public static void setFilter(ItemStack card, int slot, GenericStack filter, int size) {
        ConfigInventory inventory = readFilters(card, size);
        if (slot < 0 || slot >= inventory.size()) {
            throw new IndexOutOfBoundsException("filter slot " + slot);
        }
        inventory.setStack(slot, filter);
        writeFilters(card, inventory);
    }
}
