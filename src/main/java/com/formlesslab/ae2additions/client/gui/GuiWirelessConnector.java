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
import com.formlesslab.ae2additions.container.ContainerWirelessConnector;
import com.formlesslab.ae2additions.api.WirelessStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.Locale;

public class GuiWirelessConnector extends GuiUpgradeable<ContainerWirelessConnector> {
    public static final int PADDING_X = 8;
    public static final int PADDING_Y = 6;

    private static final ResourceLocation ICON_TEXTURE = new ResourceLocation(Reference.MOD_ID, "textures/guis/nicons.png");
    private static final Blitter INFO = Blitter.texture(ICON_TEXTURE, 64, 64).src(0, 0, 16, 16);
    private static final Blitter HIGHLIGHT = Blitter.texture(ICON_TEXTURE, 64, 64).src(16, 0, 16, 16);
    private static final int REMOTE_X = 24;
    private static final int REMOTE_Y = 76;
    private static final int REMOTE_WIDTH = 129;
    private static final int REMOTE_HEIGHT = 63;

    private final IconButton statusIcon;
    private final IconButton highlightButton;
    private float remoteRotationX = 30.0F;
    private float remoteRotationY = 45.0F;
    private float remoteZoom = 1.0F;
    private float remoteOffsetX;
    private float remoteOffsetY;
    private boolean draggingRemote;
    private int dragButton;
    private int lastDragX;
    private int lastDragY;

    public GuiWirelessConnector(ContainerWirelessConnector container, InventoryPlayer playerInventory, GuiStyle style) {
        super(container, playerInventory, new TextComponentTranslation("gui.ae2additions.wireless_connector"), style);

        this.statusIcon = new ConnectorIconButton(null, INFO);
        this.statusIcon.setMessage(statusDescription(container.status));
        this.widgets.add("statusIcon", this.statusIcon);

        this.highlightButton = new ConnectorIconButton(this::highlightRemote, HIGHLIGHT);
        this.highlightButton.setMessage(new TextComponentTranslation("gui.ae2additions.highlight.tooltip"));
        this.widgets.add("highlight", this.highlightButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.statusIcon.setMessage(statusDescription(this.container.status));
        this.highlightButton.setVisibility(this.container.status == WirelessStatus.WORKING && this.container.hasRemote);
    }

    @Override
    public void drawFG(int offsetX, int offsetY, int mouseX, int mouseY) {
        super.drawFG(offsetX, offsetY, mouseX, mouseY);
        int textColor = this.style.getColor(PaletteColor.DEFAULT_TEXT_COLOR).toARGB() & 0xFFFFFF;
        int lineHeight = 12;

        this.fontRenderer.drawString(
            new TextComponentTranslation("gui.ae2additions.status",
                new TextComponentTranslation(statusKey(this.container.status)).getFormattedText()).getFormattedText(),
            PADDING_X,
            PADDING_Y + lineHeight,
            textColor);
        this.fontRenderer.drawString(
            new TextComponentTranslation("gui.ae2additions.power",
                Platform.formatPower(this.container.powerUse, true)).getFormattedText(),
            PADDING_X,
            PADDING_Y + lineHeight * 2,
            textColor);
        this.fontRenderer.drawString(
            new TextComponentTranslation("gui.ae2additions.channels",
                this.container.usedChannels, this.container.maxChannels).getFormattedText(),
            PADDING_X,
            PADDING_Y + lineHeight * 3,
            textColor);

        drawRemotePreview(textColor);
    }

    private void drawRemotePreview(int textColor) {
        drawRect(REMOTE_X, REMOTE_Y, REMOTE_X + REMOTE_WIDTH, REMOTE_Y + REMOTE_HEIGHT, 0x66000000);
        if (!this.container.hasRemote || this.container.status != WirelessStatus.WORKING) {
            String text = new TextComponentTranslation("gui.ae2additions.remote.none").getFormattedText();
            this.fontRenderer.drawString(text,
                REMOTE_X + (REMOTE_WIDTH - this.fontRenderer.getStringWidth(text)) / 2,
                REMOTE_Y + (REMOTE_HEIGHT - this.fontRenderer.FONT_HEIGHT) / 2,
                textColor);
            return;
        }

        BlockPos pos = remotePos();
        GuiRemoteBlockRenderer.renderScene(pos, REMOTE_X + REMOTE_WIDTH / 2, REMOTE_Y + 35,
            17.0F * this.remoteZoom, this.remoteRotationX, this.remoteRotationY,
            this.remoteOffsetX, this.remoteOffsetY,
            this.guiLeft + REMOTE_X, this.guiTop + REMOTE_Y, REMOTE_WIDTH, REMOTE_HEIGHT);

        this.fontRenderer.drawString(
            new TextComponentTranslation("gui.ae2additions.remote",
                pos.getX(), pos.getY(), pos.getZ()).getFormattedText(),
            22,
            62,
            textColor);
    }

    private void highlightRemote() {
        if (Minecraft.getMinecraft().player != null && this.container.hasRemote) {
            BlockPos pos = remotePos();
            WirelessHighlightHandler.INSTANCE.highlight(pos);
            Minecraft.getMinecraft().player.sendStatusMessage(
                new TextComponentTranslation("chat.ae2additions.highlight",
                    pos.getX(), pos.getY(), pos.getZ()),
                false);
        }
    }

    private BlockPos remotePos() {
        return new BlockPos(this.container.remoteX, this.container.remoteY, this.container.remoteZ);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (canInteractWithRemote(mouseX, mouseY) && (mouseButton == 0 || mouseButton == 1)) {
            this.draggingRemote = true;
            this.dragButton = mouseButton;
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;
            return;
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (this.draggingRemote) {
            this.draggingRemote = false;
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (this.draggingRemote && clickedMouseButton == this.dragButton) {
            int dx = mouseX - this.lastDragX;
            int dy = mouseY - this.lastDragY;
            this.lastDragX = mouseX;
            this.lastDragY = mouseY;

            if (this.dragButton == 0) {
                this.remoteRotationY += dx;
                this.remoteRotationX = clamp(this.remoteRotationX + dy, -90.0F, 90.0F);
            } else {
                this.remoteOffsetX += dx;
                this.remoteOffsetY += dy;
            }
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    public void handleMouseInput() throws IOException {
        int delta = Mouse.getEventDWheel();
        if (delta != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            if (canInteractWithRemote(mouseX, mouseY)) {
                this.remoteZoom = clamp(this.remoteZoom + (delta > 0 ? 0.1F : -0.1F), 0.5F, 2.5F);
                return;
            }
        }
        super.handleMouseInput();
    }

    private boolean canInteractWithRemote(int mouseX, int mouseY) {
        return this.container.hasRemote
            && this.container.status == WirelessStatus.WORKING
            && isInRemotePreview(mouseX - this.guiLeft, mouseY - this.guiTop);
    }

    private static boolean isInRemotePreview(int x, int y) {
        return x >= REMOTE_X && x < REMOTE_X + REMOTE_WIDTH
            && y >= REMOTE_Y && y < REMOTE_Y + REMOTE_HEIGHT;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    static ITextComponent remoteText(boolean hasRemote, int x, int y, int z) {
        if (!hasRemote) {
            return new TextComponentTranslation("gui.ae2additions.remote.none");
        }
        return new TextComponentTranslation("gui.ae2additions.remote", x, y, z);
    }

    static String statusKey(WirelessStatus status) {
        return "gui.ae2additions.status." + status.name().toLowerCase(Locale.ROOT);
    }

    private static ITextComponent statusDescription(WirelessStatus status) {
        return new TextComponentTranslation(statusKey(status) + ".desc");
    }

    private static class ConnectorIconButton extends IconButton {
        private final Blitter icon;

        ConnectorIconButton(Runnable onPress, Blitter icon) {
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
