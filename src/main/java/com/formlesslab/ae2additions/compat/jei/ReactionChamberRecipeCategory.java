package com.formlesslab.ae2additions.compat.jei;

import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.tile.TileReactionChamber;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableAnimated;
import mezz.jei.api.gui.IDrawableStatic;
import mezz.jei.api.gui.IGuiFluidStackGroup;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import org.jspecify.annotations.NonNull;

public class ReactionChamberRecipeCategory implements IRecipeCategory<ReactionChamberRecipeWrapper> {
    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/guis/reaction_chamber.png");
    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawableAnimated progress;

    public ReactionChamberRecipeCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createDrawable(TEXTURE, 5, 15, 168, 75);
        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModContent.REACTION_CHAMBER));
        IDrawableStatic progressDrawable = guiHelper.createDrawable(TEXTURE, 176, 0, 6, 18);
        this.progress = guiHelper.createAnimatedDrawable(progressDrawable, 40, IDrawableAnimated.StartDirection.BOTTOM, false);
    }

    @Override
    public String getUid() {
        return ReactionChamberJeiPlugin.REACTION_CHAMBER_UID;
    }

    @Override
    public String getTitle() {
        return I18n.format("gui.ae2additions.reaction_chamber");
    }

    @Override
    public String getModName() {
        return Tags.MOD_NAME;
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayout layout, @NonNull ReactionChamberRecipeWrapper wrapper, IIngredients ingredients) {
        IGuiItemStackGroup items = layout.getItemStacks();
        int inputCount = ingredients.getInputs(VanillaTypes.ITEM).size();
        for (int index = 0; index < inputCount; index++) {
            int x = 36 + index % 3 * 18;
            int y = 8 + index / 3 * 18;
            items.init(index, true, x, y);
        }
        if (!ingredients.getOutputs(VanillaTypes.ITEM).isEmpty()) {
            items.init(9, false, 112, 27);
        }
        items.set(ingredients);

        IGuiFluidStackGroup fluids = layout.getFluidStacks();
        fluids.init(0, true, 4, 6, 16, 58, TileReactionChamber.TANK_CAPACITY, false, null);
        if (wrapper.hasFluidOutput()) {
            fluids.init(1, false, 146, 6, 16, 58, TileReactionChamber.TANK_CAPACITY, false, null);
        }
        fluids.set(ingredients);
    }

    @Override
    public void drawExtras(@NonNull Minecraft minecraft) {
        this.progress.draw(minecraft, 135, 27);
    }
}
