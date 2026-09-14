package com.formlesslab.ae2additions.compat.jei;

import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.recipe.ReactionChamberRecipe;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.FluidStack;

import java.util.Collections;
import java.util.List;

public class ReactionChamberRecipeWrapper implements IRecipeWrapper {
    private static final ResourceLocation BOLT_TEXTURE = new ResourceLocation(Tags.MOD_ID, "textures/guis/emi.png");

    private final ReactionChamberRecipe recipe;
    private final FluidStack inputFluid;
    private final FluidStack fluidOutput;

    public ReactionChamberRecipeWrapper(ReactionChamberRecipe recipe) {
        this.recipe = recipe;
        this.inputFluid = recipe.getInputFluid();
        this.fluidOutput = recipe.getFluidOutput();
    }

    private static List<ItemStack> expand(Ingredient ingredient, int amount) {
        ItemStack[] stacks = ingredient.getMatchingStacks();
        if (stacks.length == 0) {
            return Collections.emptyList();
        }
        List<ItemStack> result = new ObjectArrayList<>(stacks.length);
        for (ItemStack stack : stacks) {
            ItemStack copy = stack.copy();
            copy.setCount(amount);
            result.add(copy);
        }
        return result;
    }

    @Override
    public void getIngredients(IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM, this.getItemInputs());
        ingredients.setInputs(VanillaTypes.FLUID, Collections.singletonList(this.inputFluid));

        ItemStack itemOutput = this.recipe.getItemOutput();
        if (!itemOutput.isEmpty()) {
            ingredients.setOutput(VanillaTypes.ITEM, itemOutput);
        }
        if (this.fluidOutput != null) {
            ingredients.setOutput(VanillaTypes.FLUID, this.fluidOutput);
        }
    }

    @Override
    public void drawInfo(Minecraft minecraft, int recipeWidth, int recipeHeight, int mouseX, int mouseY) {
        String energy = I18n.format("gui.ae2additions.jei.energy", this.recipe.getEnergy() / 1000);
        int textX = recipeWidth / 2 + 4 - minecraft.fontRenderer.getStringWidth(energy) / 2;
        minecraft.fontRenderer.drawString(energy, textX, 66, 0xFF404040);
        minecraft.getTextureManager().bindTexture(BOLT_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(textX - 16, 64, 0, 0, 10, 12, 32, 32);
    }

    public int getInputFluidAmount() {
        return this.inputFluid.amount;
    }

    public boolean hasFluidOutput() {
        return this.fluidOutput != null;
    }

    public int getFluidOutputAmount() {
        return this.fluidOutput == null ? 0 : this.fluidOutput.amount;
    }

    private List<List<ItemStack>> getItemInputs() {
        List<List<ItemStack>> inputs = new ObjectArrayList<>();
        for (ReactionChamberRecipe.SizedIngredient input : this.recipe.getItemInputs()) {
            inputs.add(expand(input.ingredient(), input.amount()));
        }
        return inputs;
    }
}
