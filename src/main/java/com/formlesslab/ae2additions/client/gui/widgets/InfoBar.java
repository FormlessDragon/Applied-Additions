package com.formlesslab.ae2additions.client.gui.widgets;

import ae2.api.client.AEKeyRendering;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.client.gui.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.item.Item;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.List;

public class InfoBar {
    private final List<Widget> widgets = new ArrayList<>();

    public void render(int x, int y) {
        int maxHeight = this.widgets.stream().mapToInt(Widget::getHeight).max().orElse(0);
        for (Widget widget : this.widgets) {
            widget.render(x, Math.round(y + maxHeight / 2.0F - widget.getHeight() / 2.0F));
            x += widget.getWidth();
        }
    }

    void add(Icon icon, float scale, int xPos, int yPos) {
        this.widgets.add(new IconWidget(icon, scale, xPos, yPos));
    }

    void add(String text, int color, float scale, int xPos, int yPos) {
        this.widgets.add(new TextWidget(new TextComponentString(text), color, scale, xPos, yPos));
    }

    void add(ITextComponent text, int color, float scale, int xPos, int yPos) {
        this.widgets.add(new TextWidget(text, color, scale, xPos, yPos));
    }

    void add(AEKey key, float scale, int xPos, int yPos) {
        this.widgets.add(new StackWidget(key, scale, xPos, yPos));
    }

    void add(Item item, float scale, int xPos, int yPos) {
        this.widgets.add(new StackWidget(AEItemKey.of(item), scale, xPos, yPos));
    }

    void addSpace(int width) {
        this.widgets.add(new SpaceWidget(width));
    }

    private interface Widget {
        int getWidth();

        int getHeight();

        void render(int x, int y);
    }

    private record StackWidget(AEKey key, float scale, int xPos, int yPos) implements Widget {
        @Override
        public int getWidth() {
            return Math.round(16 * this.scale);
        }

        @Override
        public int getHeight() {
            return Math.round(16 * this.scale);
        }

        @Override
        public void render(int x, int y) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.xPos, this.yPos, 0.0F);
            GlStateManager.scale(this.scale, this.scale, 1.0F);
            AEKeyRendering.drawInGui(Minecraft.getMinecraft(), 0, 0, this.key);
            GlStateManager.popMatrix();
        }
    }

    private record IconWidget(Icon icon, float scale, int xPos, int yPos) implements Widget {
        @Override
        public int getWidth() {
            return Math.round(16 * this.scale);
        }

        @Override
        public int getHeight() {
            return Math.round(16 * this.scale);
        }

        @Override
        public void render(int x, int y) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.xPos, this.yPos, 0.0F);
            GlStateManager.scale(this.scale, this.scale, 1.0F);
            this.icon.getBlitter().dest(0, 0).blit();
            GlStateManager.popMatrix();
        }
    }

    private static final class TextWidget implements Widget {
        private final ITextComponent text;
        private final int color;
        private final float scale;
        private final int xPos;
        private final int yPos;
        private final int width;
        private final int height;

        private TextWidget(ITextComponent text, int color, float scale, int xPos, int yPos) {
            this.text = text;
            this.color = color;
            this.scale = scale;
            this.xPos = xPos;
            this.yPos = yPos;
            this.width = Math.round(Minecraft.getMinecraft().fontRenderer.getStringWidth(text.getFormattedText()) * scale);
            this.height = Math.round(Minecraft.getMinecraft().fontRenderer.FONT_HEIGHT * scale);
        }

        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return this.height;
        }

        @Override
        public void render(int x, int y) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(this.xPos, this.yPos, 0.0F);
            GlStateManager.scale(this.scale, this.scale, 1.0F);
            Minecraft.getMinecraft().fontRenderer.drawString(this.text.getFormattedText(), 0, 0, this.color);
            GlStateManager.popMatrix();
        }
    }

    private record SpaceWidget(int width) implements Widget {
        @Override
        public int getWidth() {
            return this.width;
        }

        @Override
        public int getHeight() {
            return 0;
        }

        @Override
        public void render(int x, int y) {
        }
    }
}
