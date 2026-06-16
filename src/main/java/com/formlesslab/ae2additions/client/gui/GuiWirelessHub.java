package com.formlesslab.ae2additions.client.gui;

import ae2.client.gui.Icon;
import ae2.client.gui.implementations.GuiUpgradeable;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.style.GuiStyle;
import ae2.client.gui.style.PaletteColor;
import ae2.client.gui.widgets.IconButton;
import ae2.util.Platform;
import com.formlesslab.ae2additions.Reference;
import com.formlesslab.ae2additions.client.render.WirelessHighlightHandler;
import com.formlesslab.ae2additions.container.ContainerWirelessHub;
import com.formlesslab.ae2additions.tile.TileWirelessHub;
import com.formlesslab.ae2additions.api.WirelessStatus;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

import java.util.List;
import java.util.stream.Collectors;

public class GuiWirelessHub extends GuiUpgradeable<ContainerWirelessHub> {
    private static final ResourceLocation HUB_TEXTURE = new ResourceLocation("ae2", "textures/guis/wireless_hub.png");
    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/guis/nicons.png");
    private static final Blitter PORT = Blitter.texture(HUB_TEXTURE).src(176, 0, 16, 16);
    private static final Blitter HIGHLIGHT = Blitter.texture(ICON_TEXTURE, 64, 64).src(16, 0, 16, 16);

    private static final int COL1_X = 37;
    private static final int COL2_X = 120;
    private static final int COL_Y = 44;
    private static final int Y_OFFSET = 28;
    private static final int PADDING_X = 8;
    private static final int PADDING_Y = 6;

    private final IconButton[] highlightButtons = new IconButton[TileWirelessHub.MAX_PORTS];
    private final IconButton[] disconnectButtons = new IconButton[TileWirelessHub.MAX_PORTS];

    public GuiWirelessHub(ContainerWirelessHub container, InventoryPlayer playerInventory, GuiStyle style) {
        super(container, playerInventory, new TextComponentTranslation("gui.ae2additions.wireless_hub"), style);
        for (int i = 0; i < TileWirelessHub.MAX_PORTS; i++) {
            final int port = i;
            IconButton highlight = new HubIconButton(() -> showHighlightMessage(port), HIGHLIGHT);
            highlight.setMessage(new TextComponentTranslation("gui.ae2additions.highlight.tooltip"));
            this.widgets.add("highlight" + i, highlight);
            this.highlightButtons[i] = highlight;

            IconButton disconnect = new HubIconButton(() -> container.disconnectPort(port), Icon.CLEAR.getBlitter());
            disconnect.setMessage(new TextComponentTranslation("gui.ae2additions.hub.disconnect.tooltip"));
            this.widgets.add("disconnect" + i, disconnect);
            this.disconnectButtons[i] = disconnect;
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        ContainerWirelessHub.PortState ports = this.container.ports;
        for (int i = 0; i < TileWirelessHub.MAX_PORTS; i++) {
            boolean connected = isConnectedForDisplay(ports.getStatus(i), ports.hasRemote(i));
            this.highlightButtons[i].setVisibility(connected);
            this.disconnectButtons[i].enabled = ports.getStatus(i) != WirelessStatus.UNCONNECTED;
        }
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        int textColor = this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB() & 0xFFFFFF;
        int lineHeight = 12;
        this.fontRenderer.drawString(
            new TextComponentTranslation("gui.ae2additions.power",
                Platform.formatPower(this.container.powerUse, true)).getFormattedText(),
            PADDING_X,
            PADDING_Y + lineHeight,
            textColor);
        this.fontRenderer.drawString(
            new TextComponentTranslation("gui.ae2additions.channels",
                this.container.usedChannels, this.container.maxChannels).getFormattedText(),
            PADDING_X,
            PADDING_Y + lineHeight * 2,
            textColor);

        ContainerWirelessHub.PortState ports = this.container.ports;
        for (int i = 0; i < TileWirelessHub.MAX_PORTS; i++) {
            int x = portX(i);
            int y = portY(i);
            if (isConnectedForDisplay(ports.getStatus(i), ports.hasRemote(i))) {
                renderRemoteBlock(ports, i, x, y);
            } else {
                PORT.copy().dest(x, y).blit();
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);
        int port = hoveredPort(mouseX - this.guiLeft, mouseY - this.guiTop);
        if (port >= 0) {
            drawTooltipLines(mouseX, mouseY, tooltipForPort(port));
        }
    }

    private void renderRemoteBlock(ContainerWirelessHub.PortState ports, int port, int x, int y) {
        if (!GuiRemoteBlockRenderer.renderSingle(remotePos(ports, port), x + 1, y + 1, 14.0F)) {
            PORT.copy().dest(x, y).blit();
        }
    }

    private List<String> tooltipForPort(int port) {
        ContainerWirelessHub.PortState ports = this.container.ports;
        List<ITextComponent> lines = new ObjectArrayList<>();
        lines.add(new TextComponentTranslation("gui.ae2additions.hub.port", port + 1));
        lines.add(new TextComponentTranslation(GuiWirelessConnector.statusKey(ports.getStatus(port))));
        if (ports.hasRemote(port)) {
            lines.add(new TextComponentTranslation("gui.ae2additions.remote_channel",
                ports.getRemoteX(port), ports.getRemoteY(port), ports.getRemoteZ(port),
                ports.getRemoteChannels(port)));
        } else {
            lines.add(new TextComponentTranslation("gui.ae2additions.hub.empty_port.tooltip"));
        }
        return lines.stream().map(ITextComponent::getFormattedText).collect(Collectors.toList());
    }

    private void showHighlightMessage(int port) {
        ContainerWirelessHub.PortState ports = this.container.ports;
        if (Minecraft.getMinecraft().player != null && ports.hasRemote(port)) {
            WirelessHighlightHandler.INSTANCE.highlight(remotePos(ports, port));
            Minecraft.getMinecraft().player.sendStatusMessage(
                new TextComponentTranslation("chat.ae2additions.highlight",
                    ports.getRemoteX(port), ports.getRemoteY(port), ports.getRemoteZ(port)),
                false);
        }
    }

    private static boolean isConnectedForDisplay(WirelessStatus status, boolean hasRemote) {
        return hasRemote && (status == WirelessStatus.WORKING || status == WirelessStatus.NO_POWER);
    }

    private static BlockPos remotePos(ContainerWirelessHub.PortState ports, int port) {
        return new BlockPos(ports.getRemoteX(port), ports.getRemoteY(port), ports.getRemoteZ(port));
    }

    private static int portX(int port) {
        return port < 4 ? COL1_X : COL2_X;
    }

    private static int portY(int port) {
        return COL_Y + port % 4 * Y_OFFSET;
    }

    private static int hoveredPort(int mouseX, int mouseY) {
        for (int i = 0; i < TileWirelessHub.MAX_PORTS; i++) {
            int x = portX(i);
            int y = portY(i);
            if (mouseX >= x && mouseX < x + 16 && mouseY >= y && mouseY < y + 16) {
                return i;
            }
        }
        return -1;
    }

    private static class HubIconButton extends IconButton {
        private final Blitter icon;

        HubIconButton(Runnable onPress, Blitter icon) {
            super(onPress);
            this.icon = icon;
        }

        @Override
        protected Icon getIcon() {
            return null;
        }

        @Override
        public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
            super.drawButton(minecraft, mouseX, mouseY, partialTicks);
            if (this.visible) {
                this.icon.copy().dest(this.x, this.y + (this.hovered ? 2 : 1)).zOffset(4).blit();
            }
        }
    }
}
