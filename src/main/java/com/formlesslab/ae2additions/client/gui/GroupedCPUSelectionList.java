package com.formlesslab.ae2additions.client.gui;

import ae2.api.client.AEKeyRendering;
import ae2.api.config.CpuSelectionMode;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.AmountFormat;
import ae2.client.Point;
import ae2.client.gui.ICompositeWidget;
import ae2.client.gui.Icon;
import ae2.client.gui.Tooltip;
import ae2.client.gui.me.crafting.CraftingTimeDisplay;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.style.Color;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.PaletteColor;
import ae2.client.gui.widgets.Scrollbar;
import ae2.container.implementations.ContainerCraftingStatus;
import ae2.core.definitions.AEParts;
import ae2.core.localization.ButtonToolTips;
import ae2.core.localization.GuiText;
import com.formlesslab.ae2additions.client.util.CraftingStatusCpuMetadata;
import com.formlesslab.ae2additions.client.util.CraftingStatusCpuMetadataProvider;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import org.jetbrains.annotations.Nullable;

import java.awt.Rectangle;
import java.util.List;
import java.util.Map;
import java.util.function.IntSupplier;

public class GroupedCPUSelectionList implements ICompositeWidget {
    private static final int HEADER_HEIGHT = 19;
    private static final int FOOTER_HEIGHT = 7;
    private static final int MODE_BUTTON_X = 55;
    private static final int MODE_BUTTON_Y = 9;
    private static final int MODE_BUTTON_SIZE = 10;
    private static final int MODE_BUTTON_CONTENT_OFFSET = 1;
    private static final int QUANTUM_GROUP_COLOR = 0xFFB65CFF;

    private final Blitter background;
    private final Blitter buttonBg;
    private final Blitter buttonBgSelected;
    private final ContainerCraftingStatus container;
    private final Color textColor;
    private final int selectedColor;
    private final Scrollbar scrollbar;
    private final IntSupplier visibleRowsSupplier;
    private Rectangle bounds = new Rectangle(0, 0, 0, 0);

    public GroupedCPUSelectionList(
        ContainerCraftingStatus container,
        Scrollbar scrollbar,
        GuiStyle style,
        IntSupplier visibleRowsSupplier
    ) {
        this.container = container;
        this.scrollbar = scrollbar;
        this.visibleRowsSupplier = visibleRowsSupplier;
        this.background = style.getImage("cpuList");
        this.buttonBg = style.getImage("cpuListButton");
        this.buttonBgSelected = style.getImage("cpuListButtonSelected");
        this.textColor = style.getColor(PaletteColor.DEFAULT_TEXT_COLOR);
        this.selectedColor = style.getColor(PaletteColor.SELECTION_COLOR).toARGB();
        this.scrollbar.setCaptureMouseWheel(false);
    }

    @Override
    public void setPosition(Point position) {
        this.bounds = new Rectangle(position.x(), position.y(), this.bounds.width, this.bounds.height);
    }

    @Override
    public void setSize(int width, int height) {
        this.bounds = new Rectangle(this.bounds.x, this.bounds.y, width, height);
    }

    @Override
    public Rectangle getBounds() {
        return this.bounds;
    }

    @Override
    public boolean onMouseWheel(Point mousePos, double delta) {
        this.scrollbar.onMouseWheel(mousePos, delta);
        return true;
    }

    @Override
    public boolean onMouseUp(Point mousePos, int button) {
        ContainerCraftingStatus.CraftingCpuListEntry modeButtonCpu = hitTestModeButton(mousePos);
        if (modeButtonCpu != null) {
            this.container.cycleCpuMode(modeButtonCpu.serial(), button == 1);
            return true;
        }

        ContainerCraftingStatus.CraftingCpuListEntry cpu = hitTestCpu(mousePos);
        if (cpu != null) {
            this.container.selectCpu(cpu.serial());
            return true;
        }

        return false;
    }

    @Nullable
    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        ContainerCraftingStatus.CraftingCpuListEntry modeButtonCpu = hitTestModeButton(new Point(mouseX, mouseY));
        if (modeButtonCpu != null) {
            ObjectArrayList<ITextComponent> tooltipLines = new ObjectArrayList<>();
            tooltipLines.add(ButtonToolTips.CpuSelectionMode.text());
            tooltipLines.add(gray(getModeButtonTooltip(modeButtonCpu.mode()).text()));
            return new Tooltip(tooltipLines);
        }

        ContainerCraftingStatus.CraftingCpuListEntry cpu = hitTestCpu(new Point(mouseX, mouseY));
        if (cpu == null) {
            return null;
        }

        ObjectArrayList<ITextComponent> tooltipLines = new ObjectArrayList<>();
        tooltipLines.add(getCpuName(cpu));
        tooltipLines.add(gray(ButtonToolTips.CpuStatusStorage.text(formatStorage(cpu))));

        int coProcessors = cpu.coProcessors();
        if (coProcessors == 1) {
            tooltipLines.add(gray(ButtonToolTips.CpuStatusCoProcessor.text(String.valueOf(coProcessors))));
        } else if (coProcessors > 1) {
            tooltipLines.add(gray(ButtonToolTips.CpuStatusCoProcessors.text(String.valueOf(coProcessors))));
        }

        switch (cpu.mode()) {
            case PLAYER_ONLY -> tooltipLines.add(gray(ButtonToolTips.CpuSelectionModePlayersOnly.text()));
            case MACHINE_ONLY -> tooltipLines.add(gray(ButtonToolTips.CpuSelectionModeAutomationOnly.text()));
            default -> {
            }
        }

        var currentJob = cpu.currentJob();
        if (currentJob != null) {
            String amount = currentJob.what().formatAmount(currentJob.amount(), AmountFormat.FULL);
            tooltipLines.add(gray(ButtonToolTips.CpuStatusCrafting.text(amount)
                .appendText(" ")
                .appendSibling(currentJob.what().getDisplayName())));
            var elapsedTimeTooltip = CraftingTimeDisplay.getElapsedTimeTooltip(cpu.progress(), cpu.elapsedTimeNanos());
            tooltipLines.add(gray(new TextComponentTranslation(
                elapsedTimeTooltip.translationKey(),
                elapsedTimeTooltip.args())));
        }

        return new Tooltip(tooltipLines);
    }

    @Override
    public void updateBeforeRender() {
        int rows = getVisibleRows();
        this.bounds.height = HEADER_HEIGHT + rows * getButtonRowHeight() + FOOTER_HEIGHT;
        this.scrollbar.setHeight(Math.max(1, rows * getButtonRowHeight() - 1));
        int hiddenRows = Math.max(0, this.container.cpuList.cpus().size() - rows);
        this.scrollbar.setRange(0, hiddenRows, Math.max(1, rows / 3));
    }

    @Override
    public void drawBackgroundLayer(Rectangle screenBounds, Point mouse) {
        int x = screenBounds.x + this.bounds.x;
        int y = screenBounds.y + this.bounds.y;
        drawBackground(x, y);

        x += 8;
        y += 19;

        int from = MathHelper.clamp(this.scrollbar.getCurrentScroll(), 0, this.container.cpuList.cpus().size());
        int to = MathHelper.clamp(this.scrollbar.getCurrentScroll() + getVisibleRows(), 0, this.container.cpuList.cpus().size());
        List<ContainerCraftingStatus.CraftingCpuListEntry> visibleCpus = this.container.cpuList.cpus().subList(from, to);
        ContainerCraftingStatus.CraftingCpuListEntry hoveredModeButtonCpu = hitTestModeButton(mouse);
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial = getMetadataBySerial();

        int rowY = y;
        for (ContainerCraftingStatus.CraftingCpuListEntry cpu : visibleCpus) {
            drawCpuRow(x, rowY, cpu, hoveredModeButtonCpu);
            rowY += getButtonRowHeight();
        }

        drawQuantumGroupFrames(x, y, from, visibleCpus, metadataBySerial);
    }

    private void drawCpuRow(
        int x,
        int y,
        ContainerCraftingStatus.CraftingCpuListEntry cpu,
        @Nullable ContainerCraftingStatus.CraftingCpuListEntry hoveredModeButtonCpu
    ) {
        if (cpu.serial() == this.container.getSelectedCpuSerial()) {
            this.buttonBgSelected.dest(x, y).blit();
        } else {
            this.buttonBg.dest(x, y).blit();
        }

        drawScaledString(getCpuName(cpu).getFormattedText(), x + 3, y + 2, this.textColor.toARGB());

        var currentJob = cpu.currentJob();
        if (currentJob != null) {
            Icon.S_CRAFT.getBlitter().dest(x + 2, y + 9).blit();
            drawScaledString(currentJob.what().formatAmount(currentJob.amount(), AmountFormat.SLOT),
                x + 14, y + 13, this.textColor.toARGB());
            drawScaledKey(x + 55, y + 9, currentJob.what());

            int progress = (int) (cpu.progress() * (this.buttonBg.getSrcWidth() - 1));
            if (progress > 0) {
                Gui.drawRect(
                    x,
                    y + this.buttonBg.getSrcHeight() - 2,
                    x + progress,
                    y + this.buttonBg.getSrcHeight() - 1,
                    this.container.getSelectedCpuSerial() == cpu.serial() ? 0xFF7da9d2 : this.selectedColor);
            }
        } else {
            Icon.S_STORAGE.getBlitter().dest(x + 27, y + 9).blit();
            drawScaledString(formatStorage(cpu), x + 39, y + 13, this.textColor.toARGB());

            if (cpu.coProcessors() > 0) {
                Icon.S_PROCESSOR.getBlitter().dest(x + 2, y + 9).blit();
                drawScaledString(String.valueOf(cpu.coProcessors()), x + 14, y + 13, this.textColor.toARGB());
            }
            drawModeButton(
                x + MODE_BUTTON_X,
                y + MODE_BUTTON_Y,
                cpu.mode(),
                hoveredModeButtonCpu != null && hoveredModeButtonCpu.serial() == cpu.serial());
        }
    }

    private void drawQuantumGroupFrames(
        int x,
        int firstRowY,
        int firstVisibleIndex,
        List<ContainerCraftingStatus.CraftingCpuListEntry> visibleCpus,
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial
    ) {
        int rowHeight = getButtonRowHeight();
        int width = this.buttonBg.getSrcWidth();
        for (int visibleIndex = 0; visibleIndex < visibleCpus.size(); visibleIndex++) {
            ContainerCraftingStatus.CraftingCpuListEntry cpu = visibleCpus.get(visibleIndex);
            CraftingStatusCpuMetadata metadata = metadataBySerial.get(cpu.serial());
            if (metadata == null || !metadata.quantum()) {
                continue;
            }

            int absoluteIndex = firstVisibleIndex + visibleIndex;
            boolean startsGroup = !sameQuantumGroup(absoluteIndex - 1, metadata.clusterId(), metadataBySerial);
            boolean endsGroup = !sameQuantumGroup(absoluteIndex + 1, metadata.clusterId(), metadataBySerial);
            int y = firstRowY + visibleIndex * rowHeight;
            int bottom = y + this.buttonBg.getSrcHeight();
            int sideBottom = y + rowHeight;

            Gui.drawRect(x - 1, y, x, sideBottom, QUANTUM_GROUP_COLOR);
            Gui.drawRect(x + width, y, x + width + 1, sideBottom, QUANTUM_GROUP_COLOR);
            if (startsGroup) {
                Gui.drawRect(x - 1, y - 1, x + width + 1, y, QUANTUM_GROUP_COLOR);
            }
            if (endsGroup) {
                Gui.drawRect(x - 1, bottom, x + width + 1, bottom + 1, QUANTUM_GROUP_COLOR);
            }
        }
    }

    private boolean sameQuantumGroup(
        int absoluteIndex,
        int clusterId,
        Map<Integer, CraftingStatusCpuMetadata> metadataBySerial
    ) {
        List<ContainerCraftingStatus.CraftingCpuListEntry> cpus = this.container.cpuList.cpus();
        if (absoluteIndex < 0 || absoluteIndex >= cpus.size()) {
            return false;
        }
        CraftingStatusCpuMetadata other = metadataBySerial.get(cpus.get(absoluteIndex).serial());
        return other != null && other.clusterId() == clusterId;
    }

    @Nullable
    private ContainerCraftingStatus.CraftingCpuListEntry hitTestCpu(Point mousePos) {
        int relX = mousePos.x() - this.bounds.x - 8;
        if (relX < 0 || relX >= this.buttonBg.getSrcWidth()) {
            return null;
        }

        int relY = mousePos.y() - this.bounds.y - 19;
        int buttonHeight = getButtonRowHeight();
        int buttonIdx = this.scrollbar.getCurrentScroll() + relY / buttonHeight;
        if (relY < 0 || relY >= getVisibleRows() * buttonHeight || relY % buttonHeight == this.buttonBg.getSrcHeight()) {
            return null;
        }

        List<ContainerCraftingStatus.CraftingCpuListEntry> cpus = this.container.cpuList.cpus();
        if (buttonIdx < 0 || buttonIdx >= cpus.size()) {
            return null;
        }
        return cpus.get(buttonIdx);
    }

    @Nullable
    private ContainerCraftingStatus.CraftingCpuListEntry hitTestModeButton(Point mousePos) {
        ContainerCraftingStatus.CraftingCpuListEntry cpu = hitTestCpu(mousePos);
        if (cpu == null || cpu.currentJob() != null) {
            return null;
        }

        int relX = mousePos.x() - this.bounds.x - 8;
        int relY = mousePos.y() - this.bounds.y - 19;
        int rowY = relY % getButtonRowHeight();
        if (relX < MODE_BUTTON_X || relX >= MODE_BUTTON_X + MODE_BUTTON_SIZE) {
            return null;
        }
        if (rowY < MODE_BUTTON_Y || rowY >= MODE_BUTTON_Y + MODE_BUTTON_SIZE) {
            return null;
        }
        return cpu;
    }

    private void drawBackground(int x, int y) {
        this.background.copy().src(0, 0, 77, 19).dest(x, y).blit();
        drawScrollbarBackground(x + 77, y);

        int rowY = y + 19;
        int visibleRows = getVisibleRows();
        int rowHeight = getButtonRowHeight();
        int middleSourceY = 19 + rowHeight;
        int lastSourceY = this.background.getSrcHeight() - 7 - rowHeight;

        for (int i = 0; i < visibleRows; i++) {
            int sourceY = i == 0 ? 19 : (i == visibleRows - 1 ? lastSourceY : middleSourceY);
            this.background.copy().src(0, sourceY, 77, rowHeight).dest(x, rowY).blit();
            rowY += rowHeight;
        }

        this.background.copy().src(0, this.background.getSrcHeight() - 7, 77, 7).dest(x, rowY).blit();
        this.background.copy().src(77, this.background.getSrcHeight() - 7, 17, 7).dest(x + 77, rowY).blit();
    }

    private void drawScrollbarBackground(int x, int y) {
        this.background.copy().src(77, 0, 17, 19).dest(x, y).blit();
        int rowY = y + 19;
        int rowHeight = getButtonRowHeight();
        for (int i = 0; i < getVisibleRows(); i++) {
            this.background.copy().src(77, 19 + rowHeight, 17, rowHeight).dest(x, rowY).blit();
            rowY += rowHeight;
        }
        this.background.copy().src(77, this.background.getSrcHeight() - 8, 17, 1).dest(x, rowY - 1).blit();
    }

    private Map<Integer, CraftingStatusCpuMetadata> getMetadataBySerial() {
        if (this.container instanceof CraftingStatusCpuMetadataProvider provider) {
            return provider.ae2additions$getCpuMetadata().bySerial();
        }
        return Map.of();
    }

    private static ITextComponent gray(ITextComponent text) {
        return text.setStyle(new Style().setColor(TextFormatting.GRAY));
    }

    private static void drawScaledString(String text, int x, int y, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(0.666F, 0.666F, 1.0F);
        Minecraft.getMinecraft().fontRenderer.drawString(text, 0, 0, color);
        GlStateManager.popMatrix();
    }

    private static void drawScaledKey(int x, int y, AEKey what) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(0.666F, 0.666F, 1.0F);
        AEKeyRendering.drawInGui(Minecraft.getMinecraft(), 0, 0, what);
        GlStateManager.popMatrix();
    }

    private static void drawScaledToolbarBackground(int x, int y, boolean hovered) {
        Icon backgroundIcon = hovered ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER : Icon.TOOLBAR_BUTTON_BACKGROUND;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(0.5F, 0.5F, 1.0F);
        backgroundIcon.getBlitter().dest(0, 0).zOffset(10).blit();
        GlStateManager.popMatrix();
    }

    private static void drawScaledModeIcon(int x, int y) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0);
        GlStateManager.scale(0.5F, 0.5F, 1.0F);
        Icon.CRAFT_HAMMER.getBlitter().dest(0, 0).zOffset(20).blit();
        GlStateManager.popMatrix();
    }

    private static void drawScaledModeItemStack(int x, int y, ItemStack stack) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 20);
        GlStateManager.scale(0.5F, 0.5F, 1.0F);
        GlStateManager.enableDepth();
        RenderHelper.enableGUIStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
        Minecraft.getMinecraft().getRenderItem().renderItemOverlayIntoGUI(
            Minecraft.getMinecraft().fontRenderer,
            stack,
            0,
            0,
            null);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }

    private void drawModeButton(int x, int y, CpuSelectionMode mode, boolean hovered) {
        drawScaledToolbarBackground(x, y, hovered);
        switch (mode) {
            case ANY -> drawScaledModeIcon(x + MODE_BUTTON_CONTENT_OFFSET, y + MODE_BUTTON_CONTENT_OFFSET);
            case PLAYER_ONLY -> drawScaledModeItemStack(
                x + MODE_BUTTON_CONTENT_OFFSET,
                y + MODE_BUTTON_CONTENT_OFFSET,
                AEParts.TERMINAL.stack());
            case MACHINE_ONLY -> drawScaledModeItemStack(
                x + MODE_BUTTON_CONTENT_OFFSET,
                y + MODE_BUTTON_CONTENT_OFFSET,
                AEParts.EXPORT_BUS.stack());
        }
    }

    private ButtonToolTips getModeButtonTooltip(CpuSelectionMode mode) {
        return switch (mode) {
            case ANY -> ButtonToolTips.CpuSelectionModeAny;
            case PLAYER_ONLY -> ButtonToolTips.CpuSelectionModePlayersOnly;
            case MACHINE_ONLY -> ButtonToolTips.CpuSelectionModeAutomationOnly;
        };
    }

    private String formatStorage(ContainerCraftingStatus.CraftingCpuListEntry cpu) {
        long storage = cpu.storage();
        if (storage >= 1024 * 1024) {
            return (storage / (1024 * 1024)) + "M";
        }
        return (storage / 1024) + "k";
    }

    private ITextComponent getCpuName(ContainerCraftingStatus.CraftingCpuListEntry cpu) {
        return cpu.name() != null ? cpu.name() : GuiText.CpuFallbackName.text(cpu.serial());
    }

    private int getVisibleRows() {
        return Math.max(1, this.visibleRowsSupplier.getAsInt());
    }

    private int getButtonRowHeight() {
        return this.buttonBg.getSrcHeight() + 1;
    }
}
