package com.formlesslab.ae2additions.client.gui;

import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.GuiStyleManager;
import ae2.client.gui.widgets.ServerSettingToggleButton;
import ae2.client.gui.widgets.TabButton;
import ae2.client.gui.widgets.UpgradesPanel;
import ae2.api.upgrades.Upgrades;
import ae2.api.config.FuzzyMode;
import ae2.api.config.Settings;
import ae2.container.SlotSemantics;
import ae2.core.definitions.AEItems;
import ae2.core.localization.GuiText;
import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.container.CardPlayerSlot;
import com.formlesslab.ae2additions.container.ContainerImportExportCard;
import com.formlesslab.ae2additions.init.ModNetworks;
import com.formlesslab.ae2additions.network.CCloseImportExportCard;
import com.formlesslab.ae2additions.network.CCardConfig;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.ITextComponent;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class GuiImportExportCard extends AEBaseGui<ContainerImportExportCard> {
    private static final int MASS_SELECT_X = 23 + 16 * 10;
    private static final int HOTBAR_OFFSET_Y = 16 * 3 + 10;
    private static final Blitter CHECKMARK = Blitter.texture(new ResourceLocation(Tags.MOD_ID, "textures/guis/checkmark.png"), 16, 16);
    private static final Blitter XMARK = Blitter.texture(new ResourceLocation(Tags.MOD_ID, "textures/guis/xmark.png"), 16, 16);
    private static final Blitter MASS_SELECT = Blitter.texture(new ResourceLocation(Tags.MOD_ID, "textures/guis/mass_select.png"), 16, 16);

    private final int[] selectedInventorySlots;
    private final TabButton backButton;
    private final ServerSettingToggleButton<FuzzyMode> fuzzyMode;
    private boolean selectingPlayerSlot;
    private boolean draggingPlayerStack;
    private int clickedPlayerSlot = -1;

    public GuiImportExportCard(ContainerImportExportCard container, InventoryPlayer playerInventory, GuiStyle style) {
        super(container, playerInventory, style);
        this.selectedInventorySlots = container.getSelectedSlots();
        this.backButton = new TabButton(Icon.BACK, new TextComponentTranslation(container.export ? "item.ae2additions.export_card.name" : "item.ae2additions.import_card.name"), this::closeScreen);
        this.widgets.add("back", this.backButton);
        this.widgets.add("upgrades", UpgradesPanel.create(this.widgets, container.getSlots(SlotSemantics.UPGRADE), this::getCompatibleUpgrades));
        this.fuzzyMode = this.addToLeftToolbar(new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL));
    }

    private List<ITextComponent> getCompatibleUpgrades() {
        var lines = Upgrades.getTooltipLinesForInventory(this.container.getUpgrades());
        var result = new ArrayList<ITextComponent>(lines.size() + 1);
        result.add(GuiText.CompatibleUpgrades.text());
        result.addAll(lines);
        return result;
    }

    public static GuiImportExportCard create(ContainerImportExportCard container, InventoryPlayer inventory) {
        String type = container.export ? "export" : "import";
        String path = "/screens/" + type + "_card_" + container.getCapacityCardCount() + ".json";
        return new GuiImportExportCard(container, inventory, GuiStyleManager.loadStyleDoc(path));
    }

    private void closeScreen() {
        ModNetworks.sendToServer(new CCloseImportExportCard());
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            closeScreen();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.fuzzyMode.set(this.container.fuzzyMode);
        this.fuzzyMode.setVisibility(!this.container.getCard().isEmpty() && this.hasFuzzyCard());
    }

    private boolean hasFuzzyCard() {
        return this.container.getUpgrades()
                .isInstalled(AEItems.FUZZY_CARD.item());
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        try {
            drawCardOverlays();
        } finally {
            GlStateManager.enableDepth();
        }
    }

    private void drawCardOverlays() {
        for (Slot slot : this.container.inventorySlots) {
            int x = slot.xPos;
            int y = slot.yPos;
            if (slot instanceof CardPlayerSlot) {
                int index = slot.getSlotIndex();
                if (index < 0 || index >= selectedInventorySlots.length) {
                    continue;
                }
                int selected = selectedInventorySlots[index];
                if (selected > 0) {
                    if (this.container.export) {
                        drawFilterNumber(x, y, selected);
                    } else {
                        CHECKMARK.copy().dest(x, y, 16, 16).zOffset(4).blit();
                    }
                } else if (selected == 0) {
                    XMARK.copy().dest(x, y, 16, 16).zOffset(4).blit();
                }
            }
        }

        if (this.container.export) {
            int filter = 1;
            for (Slot slot : this.container.getSlots(ContainerImportExportCard.CARD_CONFIG)) {
                drawFilterNumber(slot.xPos, slot.yPos, filter++);
            }
        }

        for (int group = 0; group < 2; group++) {
            MASS_SELECT.copy().dest(MASS_SELECT_X, getMassSelectY(group), 16, 16).zOffset(4).blit();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int group = findMassSelectGroup(mouseX, mouseY);
        if (group >= 0) {
            drawTooltipLines(mouseX, mouseY, List.of(
                    I18n.format("gui.ae2additions.card.mass_select." + (group == 0 ? "inventory" : "hotbar")),
                    I18n.format("gui.ae2additions.card.mass_select." + (this.container.export ? "export" : "import")),
                    I18n.format("gui.ae2additions.card.mass_select.clear")));
        }
    }

    private void drawFilterNumber(int x, int y, int value) {
        String text = Integer.toString(value);
        GlStateManager.pushMatrix();
        GlStateManager.translate(x + 16, y + 1, 5);
        GlStateManager.scale(0.5F, 0.5F, 1.0F);
        this.fontRenderer.drawString(text, -this.fontRenderer.getStringWidth(text), 0, 0xFF55FF55);
        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        this.draggingPlayerStack = false;
        Slot slot = findCardPlayerSlot(mouseX, mouseY);
        if (slot != null && (mouseButton == 0 || mouseButton == 1)
                && this.playerInventory.getItemStack().isEmpty()) {
            if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)) {
                super.mouseClicked(mouseX, mouseY, mouseButton);
                return;
            }
            this.selectingPlayerSlot = true;
            this.clickedPlayerSlot = slot.getSlotIndex();
            return;
        }

        if (handleMassSelect(mouseX, mouseY, mouseButton)) {
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.draggingPlayerStack) {
            resetPlayerSelection();
            super.mouseReleased(mouseX, mouseY, state);
            return;
        }
        if (this.selectingPlayerSlot) {
            if (state == 0 || state == 1) {
                Slot slot = findCardPlayerSlot(mouseX, mouseY);
                if (slot != null && slot.getSlotIndex() == this.clickedPlayerSlot) {
                    updateSelectedSlot(this.clickedPlayerSlot, state == 1);
                }
            }
            resetPlayerSelection();
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    private void resetPlayerSelection() {
        this.draggingPlayerStack = false;
        this.selectingPlayerSlot = false;
        this.clickedPlayerSlot = -1;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.selectingPlayerSlot && !this.draggingPlayerStack && (clickedMouseButton == 0 || clickedMouseButton == 1) && this.playerInventory.getItemStack().isEmpty()) {
            Slot origin = findCardPlayerSlotByIndex(this.clickedPlayerSlot);
            if (origin != null && origin.getHasStack()) {
                handleMouseClick(origin, origin.slotNumber, clickedMouseButton, ClickType.PICKUP);
                this.draggingPlayerStack = true;
                super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
                return;
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    private Slot findCardPlayerSlot(int mouseX, int mouseY) {
        for (Slot slot : this.container.inventorySlots) {
            if (!(slot instanceof CardPlayerSlot)) {
                continue;
            }
            if (this.isCardPointInRegion(slot.xPos, slot.yPos, 16, 16, mouseX, mouseY)) {
                return slot;
            }
        }
        return null;
    }

    private Slot findCardPlayerSlotByIndex(int index) {
        for (Slot slot : this.container.inventorySlots) {
            if (slot instanceof CardPlayerSlot && slot.getSlotIndex() == index) {
                return slot;
            }
        }
        return null;
    }

    private boolean isCardPointInRegion(int x, int y, int width, int height, int mouseX, int mouseY) {
        return mouseX >= this.guiLeft + x && mouseX < this.guiLeft + x + width && mouseY >= this.guiTop + y && mouseY < this.guiTop + y + height;
    }

    private void updateSelectedSlot(int slot, boolean rightClick) {
        int value;
        if (rightClick) {
            value = 0;
        } else if (this.container.export) {
            value = selectedInventorySlots[slot] >= this.container.getFilterSize() ? 0 : selectedInventorySlots[slot] + 1;
        } else {
            value = selectedInventorySlots[slot] == 0 ? 1 : 0;
        }
        selectedInventorySlots[slot] = value;
        ModNetworks.sendToServer(CCardConfig.assignSlot(this.container.terminalSlot, this.container.cardSlot, slot, value - 1));
    }

    private int findMassSelectGroup(int mouseX, int mouseY) {
        for (int group = 0; group < 2; group++) {
            if (isCardPointInRegion(MASS_SELECT_X, getMassSelectY(group), 16, 16, mouseX, mouseY)) {
                return group;
            }
        }
        return -1;
    }

    private int getMassSelectY(int group) {
        return 76 + this.container.getCapacityCardCount() * 18 + group * HOTBAR_OFFSET_Y;
    }

    private boolean handleMassSelect(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0 && mouseButton != 1) {
            return false;
        }
        int group = findMassSelectGroup(mouseX, mouseY);
        if (group < 0) {
            return false;
        }
        int start = group == 1 ? 0 : 9;
        int end = group == 1 ? 9 : 36;
        for (int i = start; i < end; i++) {
            updateSelectedSlot(i, mouseButton == 1);
        }
        return true;
    }
}
