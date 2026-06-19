package com.formlesslab.ae2additions.client.gui;

import ae2.api.crafting.IPatternDetails;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.stacks.GenericStack;
import ae2.client.Point;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.Icon;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.PaletteColor;
import ae2.client.gui.style.WidgetStyle;
import ae2.client.gui.widgets.AETextField;
import ae2.client.gui.widgets.IconButton;
import ae2.client.gui.widgets.Scrollbar;
import ae2.container.AEBaseContainer;
import ae2.container.SlotSemantics;
import ae2.core.localization.GuiText;
import ae2.core.network.InitNetwork;
import ae2.core.network.serverbound.InventoryActionPacket;
import ae2.crafting.pattern.EncodedPatternItem;
import ae2.helpers.InventoryAction;
import ae2.util.inv.AppEngInternalInventory;
import com.formlesslab.ae2additions.api.AssemblerMatrixMenu;
import com.formlesslab.ae2additions.client.gui.widgets.AssemblerMatrixSlot;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class GuiAssemblerMatrix<T extends AEBaseContainer & AssemblerMatrixMenu> extends AEBaseGui<T> {
    private static final int ROW_HEIGHT = 18;
    private static final int GUI_PADDING_X = 8;
    private static final int SLOT_SIZE = 18;
    private static final int VISIBLE_ROWS = 4;

    private final Scrollbar scrollbar;
    private final Map<Long, PatternInfo> infos = new TreeMap<>();
    private final ArrayList<PatternRow> rows = new ArrayList<>();
    private final ArrayList<ItemStack> matchedStacks = new ArrayList<>();
    private final ArrayList<AssemblerMatrixSlot> visibleSlots = new ArrayList<>();
    private final AETextField searchField;
    private final MatrixIconButton patternShowButton;
    private int runningThreads;
    private boolean hidePatternProviders;

    public GuiAssemblerMatrix(T container, InventoryPlayer playerInventory, GuiStyle style) {
        super(container, playerInventory, style);
        this.scrollbar = this.widgets.addScrollBar("scrollbar", Scrollbar.BIG);
        this.searchField = this.widgets.addTextField("search");
        this.searchField.setResponder(_ -> this.refreshList());
        this.searchField.setPlaceholder(GuiText.SearchPlaceholder.text());
        this.searchField.setTooltipMessage(Collections.singletonList(
            new TextComponentTranslation("gui.ae2additions.assembler_matrix.tooltip")));

        MatrixIconButton cancel = new MatrixIconButton(() -> Icon.CLEAR, this.container::requestCancel);
        cancel.setMessage(new TextComponentTranslation("gui.ae2additions.assembler_matrix.cancel"));
        this.addToLeftToolbar(cancel);

        this.patternShowButton = new MatrixIconButton(this::patternModeIcon, this::togglePatternMode);
        this.patternShowButton.setMessage(GuiText.PatternAccessTerminalHint.text());
        this.addToLeftToolbar(this.patternShowButton);
    }

    @Override
    public void initGui() {
        super.initGui();
        this.resetScrollbar();
        this.refreshVisibleSlots();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.runningThreads = this.container.getRunningThreads();
        this.hidePatternProviders = this.container.isPatternProvidersHidden();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.refreshVisibleSlots();
        super.drawScreen(mouseX, mouseY, partialTicks);
        if (isMouseOverThreadText(mouseX - this.guiLeft, mouseY - this.guiTop)) {
            this.drawTooltipLines(mouseX, mouseY, Collections.singletonList(
                I18n.format("gui.ae2additions.assembler_matrix.threads", this.runningThreads)));
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        int textColor = this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB() & 0xFFFFFF;
        Point threadPos = this.resolveWidget("threadText");
        this.fontRenderer.drawString(
            I18n.format("gui.ae2additions.assembler_matrix.threads", this.runningThreads),
                threadPos.x(),
                threadPos.y(),
            textColor);
        if (!this.searchField.getText().isEmpty()) {
            for (AssemblerMatrixSlot slot : this.visibleSlots) {
                int color = containsMatched(slot.getStack()) ? 0x8A00FF00 : 0x6A000000;
                drawRect(slot.xPos, slot.yPos, slot.xPos + 16, slot.yPos + 16, color);
            }
        }
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY, partialTicks);
        int size = this.rows.size();
        if (size < VISIBLE_ROWS) {
            boolean first = true;
            while (size < VISIBLE_ROWS) {
                Blitter emptyRow = this.style.getImage(first ? "emptyFirstRow" : "emptyRow");
                if (first) {
                    emptyRow.dest(offsetX + GUI_PADDING_X, offsetY + SLOT_SIZE * size + 31).blit();
                    first = false;
                } else {
                    emptyRow.dest(offsetX + GUI_PADDING_X, offsetY + SLOT_SIZE * size + 29).blit();
                }
                size++;
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 1 && this.searchField.isMouseOver(mouseX, mouseY)) {
            this.searchField.setText("");
            this.refreshList();
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void handleMouseClick(Slot slot, int slotId, int mouseButton, ClickType clickType) {
        if (slot instanceof AssemblerMatrixSlot machineSlot) {
            InventoryAction action = getAction(mouseButton, clickType);
            if (action != null) {
                InitNetwork.sendToServer(new InventoryActionPacket(
                    this.container.windowId,
                    action,
                    -machineSlot.getActualSlot() - 1,
                    machineSlot.getPatternId()));
            }
            return;
        }
        super.handleMouseClick(slot, slotId, mouseButton, clickType);
    }

    public void receiveUpdate(long patternId, Int2ObjectMap<ItemStack> updateMap) {
        if (updateMap.isEmpty()) {
            this.infos.remove(patternId);
            this.refreshList();
            return;
        }

        PatternInfo info = this.infos.computeIfAbsent(patternId, PatternInfo::new);
        for (Int2ObjectMap.Entry<ItemStack> entry : updateMap.int2ObjectEntrySet()) {
            info.getRowBySlot(entry.getIntKey()).setItemByInvSlot(entry.getIntKey(), entry.getValue());
        }
        this.refreshList();
    }

    private void refreshList() {
        this.rows.clear();
        this.matchedStacks.clear();
        for (PatternInfo info : this.infos.values()) {
            for (PatternRow row : info.internalRows) {
                if (this.filterRow(row)) {
                    this.rows.add(row);
                }
            }
        }
        this.resetScrollbar();
        this.refreshVisibleSlots();
    }

    private void refreshVisibleSlots() {
        if (this.mc == null || this.mc.player == null) {
            return;
        }
        for (AssemblerMatrixSlot slot : this.visibleSlots) {
            this.container.removeClientSideSlot(slot);
        }
        this.visibleSlots.clear();
        int scrollLevel = this.scrollbar.getCurrentScroll();
        for (int rowIndex = 0; rowIndex < VISIBLE_ROWS; rowIndex++) {
            int sourceIndex = scrollLevel + rowIndex;
            if (sourceIndex >= this.rows.size()) {
                continue;
            }
            PatternRow row = this.rows.get(sourceIndex);
            for (int col = 0; col < row.slots; col++) {
                AssemblerMatrixSlot slot = new AssemblerMatrixSlot(
                    row.inventory,
                    col,
                    row.offset,
                    row.id,
                    col * SLOT_SIZE + GUI_PADDING_X,
                    (rowIndex + 1) * SLOT_SIZE + 13);
                this.container.addClientSideSlot(slot, SlotSemantics.STORAGE);
                this.visibleSlots.add(slot);
            }
        }
    }

    private void resetScrollbar() {
        this.scrollbar.setHeight(VISIBLE_ROWS * ROW_HEIGHT - 2);
        this.scrollbar.setRange(0, Math.max(0, this.rows.size() - VISIBLE_ROWS), 2);
    }

    private boolean filterRow(PatternRow row) {
        List<String> searchTokens = tokenize(this.searchField.getText());
        if (searchTokens.isEmpty()) {
            return true;
        }
        boolean anyMatch = false;
        for (ItemStack stack : row.inventory) {
            if (matchesSearch(stack, searchTokens)) {
                anyMatch = true;
            }
        }
        return anyMatch;
    }

    private boolean matchesSearch(ItemStack stack, List<String> searchTokens) {
        if (stack.isEmpty() || !(stack.getItem() instanceof EncodedPatternItem<?>)) {
            return false;
        }
        IPatternDetails details = PatternDetailsHelper.decodePattern(stack, this.mc.world);
        if (details == null) {
            return false;
        }
        for (GenericStack output : details.getOutputs()) {
            if (matchesGenericStack(output, searchTokens)) {
                addMatched(stack);
                return true;
            }
        }
        for (IPatternDetails.IInput input : details.getInputs()) {
            for (GenericStack possible : input.possibleInputs()) {
                if (matchesGenericStack(possible, searchTokens)) {
                    addMatched(stack);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean matchesGenericStack(GenericStack stack, List<String> searchTokens) {
        if (stack == null || stack.what() == null) {
            return false;
        }
        String name = stack.what().getDisplayName().getUnformattedText().toLowerCase(Locale.ROOT);
        for (String token : searchTokens) {
            if (!name.contains(token)) {
                return false;
            }
        }
        return true;
    }

    private void addMatched(ItemStack stack) {
        if (!containsMatched(stack)) {
            this.matchedStacks.add(stack.copy());
        }
    }

    private boolean containsMatched(ItemStack stack) {
        for (ItemStack matched : this.matchedStacks) {
            if (ItemStack.areItemStacksEqual(matched, stack)) {
                return true;
            }
        }
        return false;
    }

    private Icon patternModeIcon() {
        return this.hidePatternProviders ? Icon.PATTERN_ACCESS_HIDE : Icon.PATTERN_ACCESS_SHOW;
    }

    private void togglePatternMode() {
        boolean nextHide = !this.hidePatternProviders;
        this.hidePatternProviders = nextHide;
        this.container.requestPatternMode(nextHide);
    }

    private boolean isMouseOverThreadText(int x, int y) {
        WidgetStyle widget = this.style.getWidget("threadText");
        Point pos = this.resolveWidget("threadText");
        int width = widget.getWidth() > 0 ? widget.getWidth() : 110;
        int height = widget.getHeight() > 0 ? widget.getHeight() : 12;
        return x >= pos.x() && x < pos.x() + width && y >= pos.y() && y < pos.y() + height;
    }

    private Point resolveWidget(String id) {
        return this.style.getWidget(id).resolve(new Rectangle(0, 0, this.xSize, this.ySize));
    }

    private InventoryAction getAction(int mouseButton, ClickType clickType) {
        return switch (clickType) {
            case PICKUP -> mouseButton == 1 ? InventoryAction.SPLIT_OR_PLACE_SINGLE : InventoryAction.PICKUP_OR_SET_DOWN;
            case QUICK_MOVE -> mouseButton == 1 ? InventoryAction.PICKUP_SINGLE : InventoryAction.SHIFT_CLICK;
            case CLONE -> this.mc.player != null && this.mc.player.capabilities.isCreativeMode ? InventoryAction.CREATIVE_DUPLICATE : null;
            default -> null;
        };
    }

    private static List<String> tokenize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        String[] split = value.toLowerCase(Locale.ROOT).trim().split("\\s+");
        ArrayList<String> tokens = new ArrayList<>(split.length);
        for (String token : split) {
            if (!token.isEmpty()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private static class PatternInfo {
        private final List<PatternRow> internalRows = new ArrayList<>();

        PatternInfo(long id) {
            int left = AssemblerMatrixMenu.PATTERN_SLOTS;
            int offset = 0;
            do {
                this.internalRows.add(new PatternRow(id, offset, Math.min(left, 9)));
                left -= 9;
                offset += 9;
            } while (left > 0);
        }

        PatternRow getRowBySlot(int slot) {
            return this.internalRows.get(slot / 9);
        }
    }

    private static class PatternRow {
        private final AppEngInternalInventory inventory;
        private final long id;
        private final int offset;
        private final int slots;

        PatternRow(long patternId, int offset, int slots) {
            this.id = patternId;
            this.offset = offset;
            this.slots = slots;
            this.inventory = new AppEngInternalInventory(slots);
        }

        void setItemByInvSlot(int slot, ItemStack stack) {
            this.inventory.setItemDirect(slot - this.offset, stack);
        }
    }

    private static class MatrixIconButton extends IconButton {
        private final IconSupplier icon;

        MatrixIconButton(IconSupplier icon, Runnable onPress) {
            super(onPress);
            this.icon = icon;
        }

        @Override
        protected Icon getIcon() {
            return this.icon.get();
        }
    }

    private interface IconSupplier {
        Icon get();
    }
}
