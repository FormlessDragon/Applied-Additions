package com.formlesslab.ae2additions.client.gui;

import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.client.gui.Icon;
import ae2.client.gui.implementations.GuiUpgradeable;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.widgets.IconButton;
import ae2.client.gui.widgets.ProgressBar;
import ae2.client.gui.widgets.ServerSettingToggleButton;
import ae2.core.localization.ButtonToolTips;
import com.formlesslab.ae2additions.container.ContainerReactionChamber;
import com.formlesslab.ae2additions.init.ModNetworks;
import com.formlesslab.ae2additions.network.CReactionChamberOutputSides;
import com.formlesslab.ae2additions.tile.TileReactionChamber;
import com.formlesslab.ae2additions.util.FluidStackRenderer;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fluids.FluidStack;
import org.jspecify.annotations.NonNull;

import java.awt.Rectangle;
import java.util.List;

public class GuiReactionChamber extends GuiUpgradeable<ContainerReactionChamber> {
    private static final Rectangle INPUT_TANK = new Rectangle(9, 21, 16, 58);
    private static final Rectangle OUTPUT_TANK = new Rectangle(151, 21, 16, 58);
    private final ProgressBar progressBar;
    private final ServerSettingToggleButton<YesNo> autoExportButton;
    private final IconButton outputSidesButton;

    public GuiReactionChamber(ContainerReactionChamber container, InventoryPlayer playerInventory, GuiStyle style) {
        super(container, playerInventory, new TextComponentTranslation("gui.ae2additions.reaction_chamber"), style);
        this.xSize = 176;
        this.ySize = 184;
        this.progressBar = new ProgressBar(this.container, style.getImage("progressBar"), ProgressBar.Direction.VERTICAL);
        this.widgets.add("progressBar", this.progressBar);
        this.autoExportButton = this.addToLeftToolbar(new ServerSettingToggleButton<>(Settings.AUTO_EXPORT, YesNo.NO));
        this.outputSidesButton = this.addToLeftToolbar(new IconButton(this::openOutputSides) {
            {
                this.setMessage(ButtonToolTips.OutputSideConfig.text());
            }

            @Override
            protected Icon getIcon() {
                return Icon.OUTPUT_SIDE_CONFIG;
            }

            @Override
            public @NonNull List<ITextComponent> getTooltipMessage() {
                return List.of(ButtonToolTips.OutputSideConfig.text(), ButtonToolTips.OutputSideConfigHint.text());
            }
        });
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        int maxProgress = this.container.getMaxProgress();
        int progress = maxProgress > 0 ? this.container.getCurrentProgress() * 100 / maxProgress : 0;
        this.progressBar.setFullMsg(new TextComponentString(progress + "%"));
        this.autoExportButton.set(this.container.getAutoExport());
        this.outputSidesButton.setVisibility(this.container.getAutoExport() == YesNo.YES);
    }

    @Override
    public void drawBG(int offsetX, int offsetY, int mouseX, int mouseY, float partialTicks) {
        super.drawBG(offsetX, offsetY, mouseX, mouseY, partialTicks);
        FluidStackRenderer.drawFluid(this.mc, offsetX + INPUT_TANK.x, offsetY + INPUT_TANK.y, INPUT_TANK.width, INPUT_TANK.height, TileReactionChamber.TANK_CAPACITY, this.container.getHost().getInputTank().getFluid());
        FluidStackRenderer.drawFluid(this.mc, offsetX + OUTPUT_TANK.x, offsetY + OUTPUT_TANK.y, OUTPUT_TANK.width, OUTPUT_TANK.height, TileReactionChamber.TANK_CAPACITY, this.container.getHost().getOutputTank().getFluid());
    }

    @Override
    protected void renderHoveredToolTip(int mouseX, int mouseY) {
        if (this.drawTankTooltip(mouseX, mouseY, INPUT_TANK, this.container.getHost().getInputTank().getFluid(), this.container.inputFluidAmount)) {
            return;
        }
        if (this.drawTankTooltip(mouseX, mouseY, OUTPUT_TANK, this.container.getHost().getOutputTank().getFluid(), this.container.outputFluidAmount)) {
            return;
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private void openOutputSides() {
        ModNetworks.sendToServer(new CReactionChamberOutputSides());
    }

    private boolean drawTankTooltip(int mouseX, int mouseY, Rectangle tank, FluidStack fluid, int amount) {
        int relX = mouseX - this.guiLeft;
        int relY = mouseY - this.guiTop;
        if (!tank.contains(relX, relY) || fluid == null || amount <= 0) {
            return false;
        }

        List<String> tooltip = new ObjectArrayList<>();
        tooltip.add(fluid.getFluid().getLocalizedName(new FluidStack(fluid.getFluid(), 1)));
        tooltip.add(TextFormatting.GRAY + "" + amount + " / " + TileReactionChamber.TANK_CAPACITY + " mB");
        this.drawTooltipLines(mouseX, mouseY, tooltip);
        return true;
    }
}
