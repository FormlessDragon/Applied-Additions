package com.formlesslab.ae2additions.container;

import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.config.FuzzyMode;
import ae2.api.config.Settings;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantic;
import ae2.container.guisync.GuiSync;
import ae2.container.SlotSemantics;
import ae2.container.slot.FakeSlot;
import ae2.util.ConfigGuiInventory;
import ae2.util.ConfigInventory;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.util.ImportExportCardConfig;
import com.formlesslab.ae2additions.util.WirelessTerminalAccess;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.List;

public final class ContainerImportExportCard extends AEBaseContainer implements IConfigurableObject {

    public static final SlotSemantic CARD_CONFIG = SlotSemantics.register("AE2ADDITIONS_CARD_CONFIG", false);

    public final int terminalSlot;
    public final int cardSlot;
    public final boolean export;
    private final InventoryPlayer inventory;
    private final ItemStack terminal;
    private final ConfigInventory filters;
    private final int filterSize;
    private final ItemStack card;
    private final IUpgradeInventory upgrades;
    private final IConfigManager configManager;

    @GuiSync(0)
    public FuzzyMode fuzzyMode = FuzzyMode.IGNORE_ALL;

    public ContainerImportExportCard(InventoryPlayer inventory, int terminalSlot, int cardSlot, boolean export) {
        super(inventory, null);
        this.inventory = inventory;
        this.terminalSlot = terminalSlot;
        this.cardSlot = cardSlot;
        this.export = export;
        this.terminal = WirelessTerminalAccess.getTerminal(inventory.player, terminalSlot);
        this.card = findCard(this.terminal, cardSlot, export);
        this.upgrades = UpgradeInventories.forItem(this.card, ImportExportCardConfig.getUpgradeSlotCount(export), null);
        this.configManager = IConfigManager.builder(this.card).registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL).build();
        this.fuzzyMode = this.configManager.getSetting(Settings.FUZZY_MODE);
        this.filterSize = ImportExportCardConfig.getFilterSize(this.upgrades);
        this.filters = createFilterInventory(this.card, this.filterSize);
        this.setupUpgrades(this.upgrades);

        if (!this.terminal.isEmpty() && terminalSlot >= 0) {
            this.lockPlayerInventorySlot(terminalSlot);
        }

        ConfigGuiInventory filterView = this.filters.createGuiWrapper();

        for (int i = 0; i < this.filterSize; i++) {
            int x = 23 + (i % 9) * 18;
            int y = 29 + (i / 9) * 18;
            this.addSlot(new FakeSlot(filterView, i, x, y), CARD_CONFIG);
        }

        for (int i = 0; i < 36; i++) {
            Slot slot = new CardPlayerSlot(inventory, i, 0, 0);
            this.addSlot(slot, i < 9 ? SlotSemantics.PLAYER_HOTBAR : SlotSemantics.PLAYER_INVENTORY);
        }

        int armorY = 70 + getCapacityCardCount() * 18;

        for (int armorIndex = 0; armorIndex < 4 && 39 - armorIndex < inventory.getSizeInventory(); armorIndex++) {
            int i = 39 - armorIndex;
            this.addSlotToContainer(new CardPlayerSlot(inventory, i, 2,
                    armorY + armorIndex * 18));
        }
    }

    private ConfigInventory createFilterInventory(ItemStack card, int size) {
        ConfigInventory loaded = card.isEmpty() ? ConfigInventory.configTypes(size).build() : ImportExportCardConfig.readFilters(card, size);
        ConfigInventory[] holder = new ConfigInventory[1];
        ConfigInventory result = ConfigInventory.configTypes(size).changeListener(() -> {
            if (!this.card.isEmpty() && holder[0] != null) {
                ImportExportCardConfig.writeFilters(this.card, holder[0]);
            }
        }).build();
        holder[0] = result;
        result.readFromList(loaded.toList());
        return result;
    }

    public ItemStack getCard() {
        return this.card;
    }

    public ConfigInventory getFilters() {
        return this.filters;
    }

    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    public int getFilterSize() {
        return this.filterSize;
    }

    public int getCapacityCardCount() {
        return ImportExportCardConfig.getCapacityCardCount(this.upgrades);
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.configManager;
    }

    @Override
    public Object getTarget() {
        return this;
    }

    @Override
    public void broadcastChanges() {
        if (this.isServerSide()) {
            this.fuzzyMode = this.configManager.getSetting(Settings.FUZZY_MODE);
            this.saveCard();
        }
        super.broadcastChanges();
    }

    public int[] getSelectedSlots() {
        return this.card.isEmpty() ? new int[ImportExportCardConfig.SLOT_COUNT] : ImportExportCardConfig.getSelectedSlots(this.card);
    }

    public void setSelectedSlot(int slot, int value) {
        if (this.card.isEmpty() || slot < 0 || slot >= ImportExportCardConfig.SLOT_COUNT) {
            return;
        }
        int[] selected = ImportExportCardConfig.getSelectedSlots(this.card);
        selected[slot] = Math.max(0, Math.min(this.filterSize, value));
        ImportExportCardConfig.setSelectedSlots(this.card, selected);
        this.saveCard();
    }

    private void saveCard() {
        if (!this.isServerSide()) {
            return;
        }
        IUpgradeInventory terminalUpgrades = getValidTerminalUpgrades(this.inventory.player);
        if (terminalUpgrades == null) {
            return;
        }
        ItemStack stored = terminalUpgrades.getStackInSlot(this.cardSlot);
        if (!ItemStack.areItemStacksEqual(stored, this.card)) {
            terminalUpgrades.setItemDirect(this.cardSlot, this.card);
            this.inventory.markDirty();
        }
    }

    public List<Slot> getCardPlayerSlots() {
        int start = this.filterSize + this.upgrades.size();
        return this.inventorySlots.subList(start, Math.min(this.inventorySlots.size(), start + ImportExportCardConfig.SLOT_COUNT));
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return getValidTerminalUpgrades(player) != null;
    }

    private IUpgradeInventory getValidTerminalUpgrades(EntityPlayer player) {
        if (player != this.inventory.player || this.card.isEmpty()
                || !WirelessTerminalAccess.isWirelessTerminal(this.terminal)
                || WirelessTerminalAccess.getTerminal(player, this.terminalSlot) != this.terminal) {
            return null;
        }
        IUpgradeInventory terminalUpgrades = WirelessTerminalAccess.getUpgrades(this.terminal);
        if (terminalUpgrades == null || this.cardSlot < 0 || this.cardSlot >= terminalUpgrades.size()
                || terminalUpgrades.getStackInSlot(this.cardSlot).getItem() != this.card.getItem()) {
            return null;
        }
        return terminalUpgrades;
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        return ItemStack.EMPTY;
    }

    private static ItemStack findCard(ItemStack terminal, int cardSlot, boolean export) {
        if (terminal.isEmpty()) {
            return ItemStack.EMPTY;
        }
        IUpgradeInventory upgrades = WirelessTerminalAccess.getUpgrades(terminal);
        if (upgrades == null || cardSlot < 0 || cardSlot >= upgrades.size()) {
            return ItemStack.EMPTY;
        }
        ItemStack card = upgrades.getStackInSlot(cardSlot);
        return card.getItem() == (export ? ModContent.EXPORT_CARD : ModContent.IMPORT_CARD)
                ? card : ItemStack.EMPTY;
    }

}
