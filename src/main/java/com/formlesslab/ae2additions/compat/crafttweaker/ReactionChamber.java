package com.formlesslab.ae2additions.compat.crafttweaker;

import com.formlesslab.ae2additions.init.ModRecipes;
import com.formlesslab.ae2additions.recipe.ReactionChamberRecipe;
import com.formlesslab.ae2additions.recipe.ReactionChamberRecipeFactory;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.List;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

@ZenRegister
@ZenClass("mods.ae2additions.ReactionChamber")
public class ReactionChamber {

    @ZenMethod
    public static void addRecipe(IIngredient[] inputItems, ILiquidStack inputFluid, int energy, @Optional IItemStack itemOutput, @Optional ILiquidStack fluidOutput) {
        List<ReactionChamberRecipe.SizedIngredient> ingredients = new ObjectArrayList<>();
        if (inputItems != null) {
            for (IIngredient input : inputItems) {
                if (input == null) {
                    continue;
                }
                int amount = input.getAmount();
                ingredients.add(new ReactionChamberRecipe.SizedIngredient(
                        CraftTweakerMC.getIngredient(input), amount > 0 ? amount : 1));
            }
        }
        ReactionChamberRecipe recipe = ReactionChamberRecipeFactory.create(ingredients,
                CraftTweakerMC.getLiquidStack(inputFluid), energy,
                CraftTweakerMC.getItemStack(itemOutput), CraftTweakerMC.getLiquidStack(fluidOutput));
        ModRecipes.REACTION_CHAMBER_RECIPES.add(recipe);
    }

    @ZenMethod
    public static void removeRecipe(IItemStack output) {
        ModRecipes.REACTION_CHAMBER_RECIPES.removeIf(recipe -> recipe.hasItemOutput(CraftTweakerMC.getItemStack(output)));
    }

    @ZenMethod
    public static void removeRecipe(ILiquidStack output) {
        ModRecipes.REACTION_CHAMBER_RECIPES.removeIf(recipe -> recipe.hasFluidOutput(CraftTweakerMC.getLiquidStack(output)));
    }
}
