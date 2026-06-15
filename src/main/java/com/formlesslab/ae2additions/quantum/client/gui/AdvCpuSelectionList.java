package com.formlesslab.ae2additions.quantum.client.gui;

import ae2.api.config.CpuSelectionMode;
import ae2.api.stacks.AmountFormat;
import ae2.api.stacks.GenericStack;
import ae2.client.Point;
import ae2.client.gui.ICompositeWidget;
import ae2.client.gui.Icon;
import ae2.client.gui.Tooltip;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.style.Color;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.PaletteColor;
import ae2.client.gui.widgets.Scrollbar;
import ae2.container.implementations.ContainerCraftingStatus.CraftingCpuListEntry;
import ae2.core.localization.ButtonToolTips;
import ae2.core.localization.GuiText;
import ae2.core.localization.Tooltips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

public class AdvCpuSelectionList implements ICompositeWidget {
    private static final int DEFAULT_ROWS = 6;

    private final Blitter background;
    private final Blitter buttonBg;
    private final Blitter buttonBgSelected;
    private final QuantumComputerMenu menu;
    private final Color textColor;
    private final int selectedColor;
    private final Scrollbar scrollbar;
    private final IntSupplier visibleRowsSupplier;

    private Rectangle bounds = new Rectangle(0, 0, 0, 0);

    public AdvCpuSelectionList(QuantumComputerMenu menu, Scrollbar scrollbar, GuiStyle style) {
        this(menu, scrollbar, style, () -> DEFAULT_ROWS);
    }

    public AdvCpuSelectionList(QuantumComputerMenu menu, Scrollbar scrollbar, GuiStyle style, IntSupplier visibleRowsSupplier) {
        this.menu = menu;
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
        CraftingCpuListEntry cpu = hitTestCpu(mousePos);
        if (cpu != null) {
            this.menu.selectCpu(cpu.serial());
            return true;
        }

        return false;
    }

    @Override
    public Tooltip getTooltip(int mouseX, int mouseY) {
        CraftingCpuListEntry cpu = hitTestCpu(new Point(mouseX, mouseY));
        if (cpu == null) {
            return null;
        }

        List<ITextComponent> tooltipLines = new ArrayList<>();
        tooltipLines.add(getCpuName(cpu));

        int coProcessors = cpu.coProcessors();
        if (coProcessors == 1) {
            tooltipLines.add(gray(ButtonToolTips.CpuStatusCoProcessor.text(Tooltips.ofUnformattedNumber(coProcessors))));
        } else if (coProcessors > 1) {
            tooltipLines.add(gray(ButtonToolTips.CpuStatusCoProcessors.text(Tooltips.ofUnformattedNumber(coProcessors))));
        }

        tooltipLines.add(gray(ButtonToolTips.CpuStatusStorage.text(Tooltips.of(formatStorage(cpu)))));

        ButtonToolTips modeText = modeTooltip(cpu.mode());
        if (modeText != null && cpu.mode() != CpuSelectionMode.ANY) {
            tooltipLines.add(modeText.text());
        }

        GenericStack currentJob = cpu.currentJob();
        if (currentJob != null) {
            tooltipLines.add(ButtonToolTips.CpuStatusCrafting.text(Tooltips.of(
                    currentJob.what().formatAmount(currentJob.amount(), AmountFormat.FULL)))
                .appendText(" ")
                .appendSibling(currentJob.what().getDisplayName()));
            tooltipLines.add(ButtonToolTips.CpuStatusCraftedIn.text(
                Tooltips.of(Math.round(cpu.progress() * 100.0F) + "%"),
                Tooltips.of(TimeUnit.NANOSECONDS.toSeconds(cpu.elapsedTimeNanos()) + "s")));
        }

        return new Tooltip(tooltipLines);
    }

    @Override
    public void updateBeforeRender() {
        int hiddenRows = Math.max(0, this.menu.cpuList.cpus().size() - getVisibleRows());
        this.scrollbar.setRange(0, hiddenRows, Math.max(1, getVisibleRows() / 3));
    }

    @Override
    public void drawBackgroundLayer(Rectangle screenBounds, Point mouse) {
        int x = screenBounds.x + this.bounds.x;
        int y = screenBounds.y + this.bounds.y;
        drawBackground(x, y);

        x += 8;
        y += 19;

        int from = clamp(this.scrollbar.getCurrentScroll(), 0, this.menu.cpuList.cpus().size());
        int to = clamp(this.scrollbar.getCurrentScroll() + getVisibleRows(), 0, this.menu.cpuList.cpus().size());
        for (CraftingCpuListEntry cpu : this.menu.cpuList.cpus().subList(from, to)) {
            if (cpu.serial() == this.menu.getSelectedCpuSerial()) {
                this.buttonBgSelected.dest(x, y).blit();
            } else {
                this.buttonBg.dest(x, y).blit();
            }

            ITextComponent name = getCpuName(cpu);
            drawScaledString(name.getFormattedText(), x + 3, y + 2, this.textColor.toARGB());

            InfoBar infoBar = new InfoBar();
            GenericStack currentJob = cpu.currentJob();
            if (currentJob != null) {
                infoBar.add(Icon.S_CRAFT, 1.0F, x + 2, y + 9);
                infoBar.add(currentJob.what().formatAmount(currentJob.amount(), AmountFormat.SLOT),
                    this.textColor.toARGB(), 0.666F, x + 14, y + 13);
                infoBar.add(currentJob.what(), 0.666F, x + 55, y + 9);

                int progress = (int) (cpu.progress() * (this.buttonBg.getSrcWidth() - 1));
                net.minecraft.client.gui.Gui.drawRect(
                    x,
                    y + this.buttonBg.getSrcHeight() - 2,
                    x + progress,
                    y + this.buttonBg.getSrcHeight() - 1,
                    this.menu.getSelectedCpuSerial() == cpu.serial() ? 0xFF7da9d2 : this.selectedColor);
            } else {
                infoBar.add(Icon.S_STORAGE, 1.0F, x + 32, y + 9);
                infoBar.add(formatStorage(cpu), this.textColor.toARGB(), 0.666F, x + 44, y + 13);

                if (cpu.coProcessors() > 0) {
                    infoBar.add(Icon.S_PROCESSOR, 1.0F, x + 2, y + 9);
                    infoBar.add(String.valueOf(cpu.coProcessors()), this.textColor.toARGB(), 0.666F, x + 14, y + 13);
                }

                switch (cpu.mode()) {
                    case PLAYER_ONLY -> infoBar.add(Icon.S_TERMINAL, 1.0F, x + 55, y + 9);
                    case MACHINE_ONLY -> infoBar.add(Icon.S_MACHINE, 1.0F, x + 55, y + 9);
                    default -> {
                    }
                }
            }

            infoBar.render(x + 2, y + this.buttonBg.getSrcHeight() - 12);
            y += getButtonRowHeight();
        }
    }

    private CraftingCpuListEntry hitTestCpu(Point mousePos) {
        int relX = mousePos.x() - this.bounds.x - 8;
        if (relX < 0 || relX >= this.buttonBg.getSrcWidth()) {
            return null;
        }

        int relY = mousePos.y() - this.bounds.y - 19;
        int rowHeight = getButtonRowHeight();
        int buttonIdx = this.scrollbar.getCurrentScroll() + relY / rowHeight;
        if (relY < 0 || relY >= getVisibleRows() * rowHeight || relY % rowHeight == this.buttonBg.getSrcHeight()) {
            return null;
        }

        List<CraftingCpuListEntry> cpus = this.menu.cpuList.cpus();
        if (buttonIdx >= 0 && buttonIdx < cpus.size()) {
            return cpus.get(buttonIdx);
        }
        return null;
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

    private static void drawScaledString(String text, int x, int y, int color) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(0.666F, 0.666F, 1.0F);
        Minecraft.getMinecraft().fontRenderer.drawString(text, 0, 0, color);
        GlStateManager.popMatrix();
    }

    private String formatStorage(CraftingCpuListEntry cpu) {
        long storage = cpu.storage();
        int unit = -1;

        while (storage > 1024) {
            storage /= 1024;
            unit++;
        }

        return storage + switch (unit) {
            case 0 -> "k";
            case 1 -> "M";
            case 2 -> "G";
            default -> "T";
        };
    }

    private ITextComponent getCpuName(CraftingCpuListEntry cpu) {
        return cpu.name() != null ? cpu.name() : GuiText.CpuFallbackName.text(cpu.serial());
    }

    private int getVisibleRows() {
        return Math.max(1, this.visibleRowsSupplier.getAsInt());
    }

    private int getButtonRowHeight() {
        return this.buttonBg.getSrcHeight() + 1;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static ITextComponent gray(ITextComponent component) {
        return component.setStyle(new Style().setColor(TextFormatting.GRAY));
    }

    private static ButtonToolTips modeTooltip(CpuSelectionMode mode) {
        return switch (mode) {
            case PLAYER_ONLY -> ButtonToolTips.CpuSelectionModePlayersOnly;
            case MACHINE_ONLY -> ButtonToolTips.CpuSelectionModeAutomationOnly;
            default -> ButtonToolTips.CpuSelectionModeAny;
        };
    }
}
