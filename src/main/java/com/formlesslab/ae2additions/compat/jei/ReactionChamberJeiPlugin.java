package com.formlesslab.ae2additions.compat.jei;

import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.client.gui.GuiReactionChamber;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.init.ModRecipes;
import com.formlesslab.ae2additions.recipe.ReactionChamberRecipe;
import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import net.minecraft.item.ItemStack;

@JEIPlugin
public class ReactionChamberJeiPlugin implements IModPlugin {
    public static final String REACTION_CHAMBER_UID = Tags.MOD_ID + ".reaction_chamber";

    @Override
    public void registerCategories(IRecipeCategoryRegistration registry) {
        IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
        registry.addRecipeCategories(new ReactionChamberRecipeCategory(guiHelper));
    }

    @Override
    public void register(IModRegistry registry) {
        registry.handleRecipes(ReactionChamberRecipe.class, ReactionChamberRecipeWrapper::new, REACTION_CHAMBER_UID);
        registry.addRecipes(ModRecipes.getRecipes(), REACTION_CHAMBER_UID);
        registry.addRecipeCatalyst(new ItemStack(ModContent.REACTION_CHAMBER), REACTION_CHAMBER_UID);
        registry.addRecipeClickArea(GuiReactionChamber.class, 95, 43, 24, 16, REACTION_CHAMBER_UID);
    }
}
