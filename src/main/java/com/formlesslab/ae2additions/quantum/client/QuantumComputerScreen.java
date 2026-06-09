package com.formlesslab.ae2additions.quantum.client;

import ae2.api.config.CpuSelectionMode;
import ae2.api.config.Settings;
import ae2.client.gui.me.crafting.GuiCraftingCPU;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.widgets.Scrollbar;
import ae2.client.gui.widgets.SettingToggleButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;

public class QuantumComputerScreen extends GuiCraftingCPU<QuantumComputerMenu> {
    private final SettingToggleButton<CpuSelectionMode> selectionMode;

    public QuantumComputerScreen(QuantumComputerMenu menu, InventoryPlayer playerInventory, ITextComponent title, GuiStyle style) {
        super(menu, playerInventory, title, style);

        this.selectionMode = new SettingToggleButton<>(
            Settings.CPU_SELECTION_MODE,
            CpuSelectionMode.ANY,
            (button, backwards) -> menu.cycleSelectionMode(backwards));
        addToLeftToolbar(this.selectionMode);

        Scrollbar scrollbar = this.widgets.addScrollBar("selectCpuScrollbar", Scrollbar.BIG);
        this.widgets.add("selectCpuList", new AdvCpuSelectionList(menu, scrollbar, style, this::getCpuListRows));
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.selectionMode.set(this.container.getSelectionMode());
    }

    @Override
    protected ITextComponent getGuiDisplayName(ITextComponent in) {
        return in;
    }

    private int getCpuListRows() {
        return Math.max(1, getCraftingRows() - 1);
    }
}
