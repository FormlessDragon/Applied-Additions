package com.formlesslab.ae2additions.client.gui.widgets;

import ae2.client.gui.Icon;
import ae2.client.gui.style.Blitter;
import ae2.client.gui.widgets.IconButton;
import com.formlesslab.ae2additions.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public final class ImportExportCardButton extends IconButton {
    private final Blitter icon;

    public ImportExportCardButton(boolean export, Runnable action) {
        super(action);
        this.icon = Blitter.texture(new ResourceLocation(Tags.MOD_ID,
                "textures/guis/" + (export ? "export_card" : "import_card") + ".png"), 16, 16);
    }

    @Override
    protected Icon getIcon() {
        return null;
    }

    @Override
    public void drawButton(Minecraft minecraft, int mouseX, int mouseY, float partialTicks) {
        if (!this.visible) {
            return;
        }

        this.hovered = mouseX >= this.x && mouseY >= this.y
                && mouseX < this.x + this.width && mouseY < this.y + this.height;
        int yOffset = this.hovered ? 1 : 0;
        Icon background = this.hovered || this.isFocused()
                ? Icon.TOOLBAR_BUTTON_BACKGROUND_HOVER : Icon.TOOLBAR_BUTTON_BACKGROUND;
        background.getBlitter().dest(this.x - 1, this.y + yOffset, 18, 20).zOffset(2).blit();
        this.icon.copy().dest(this.x, this.y + 1 + yOffset, 16, 16).zOffset(3).blit();
    }
}
