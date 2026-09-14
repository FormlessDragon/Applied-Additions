package com.formlesslab.ae2additions.recipe;

import com.formlesslab.ae2additions.init.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.IRecipeFactory;
import net.minecraftforge.common.crafting.JsonContext;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

@SuppressWarnings("unused")
public class ReactionChamberRecipeFactory implements IRecipeFactory {
    public static ReactionChamberRecipe fromJson(JsonObject json, JsonContext ctx) {
        JsonArray inputItems = JsonUtils.getJsonArray(json, "input_items");
        List<ReactionChamberRecipe.SizedIngredient> items = new ObjectArrayList<>(inputItems.size());
        for (JsonElement element : inputItems) {
            JsonObject input = JsonUtils.getJsonObject(element, "input item");
            int amount = JsonUtils.getInt(input, "amount", 1);
            items.add(new ReactionChamberRecipe.SizedIngredient(readIngredient(input.get("ingredient"), ctx), amount));
        }

        JsonObject inputFluid = JsonUtils.getJsonObject(json, "input_fluid");
        FluidStack fluid = new FluidStack(readFluid(JsonUtils.getString(inputFluid, "ingredient")), JsonUtils.getInt(inputFluid, "amount"));
        int energy = JsonUtils.getInt(json, "input_energy");

        ItemStack itemOutput = ItemStack.EMPTY;
        FluidStack fluidOutput = null;
        if (json.has("itemOutput")) {
            itemOutput = readItemStack(JsonUtils.getJsonObject(json, "itemOutput"), ctx);
        }
        if (json.has("fluidOutput")) {
            JsonObject output = JsonUtils.getJsonObject(json, "fluidOutput");
            fluidOutput = new FluidStack(readFluid(JsonUtils.getString(output, "id")), JsonUtils.getInt(output, "amount"));
        }

        return create(items, fluid, energy, itemOutput, fluidOutput);
    }

    public static ReactionChamberRecipe create(List<ReactionChamberRecipe.SizedIngredient> items,
                                               FluidStack inputFluid, int energy,
                                               ItemStack itemOutput, FluidStack fluidOutput) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Reaction chamber recipe must define at least one item input");
        }
        if (inputFluid == null || inputFluid.getFluid() == null || inputFluid.amount <= 0) {
            throw new IllegalArgumentException("Reaction chamber recipe must define a positive input fluid amount");
        }
        if (energy <= 0) {
            throw new IllegalArgumentException("Reaction chamber recipe must define positive input energy");
        }
        if ((itemOutput == null || itemOutput.isEmpty()) && (fluidOutput == null || fluidOutput.amount <= 0)) {
            throw new IllegalArgumentException("Reaction chamber recipe must define itemOutput or fluidOutput");
        }
        return new ReactionChamberRecipe(items, inputFluid.getFluid(), inputFluid.amount, energy,
            itemOutput == null ? ItemStack.EMPTY : itemOutput.copy(),
            fluidOutput == null ? null : fluidOutput.copy());
    }

    private static Ingredient readIngredient(JsonElement element, JsonContext ctx) {
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            JsonObject object = new JsonObject();
            object.addProperty("item", value);
            return CraftingHelper.getIngredient(object, ctx);
        }
        return CraftingHelper.getIngredient(element, ctx);
    }

    private static ItemStack readItemStack(JsonObject json, JsonContext ctx) {
        JsonObject copy = json.deepCopy();
        if (copy.has("id")) {
            copy.addProperty("item", JsonUtils.getString(copy, "id"));
            copy.remove("id");
        }
        return CraftingHelper.getItemStack(copy, ctx);
    }

    private static Fluid readFluid(String id) {
        ResourceLocation location = new ResourceLocation(id);
        Fluid fluid = FluidRegistry.getFluid(location.toString());
        if (fluid == null) {
            fluid = FluidRegistry.getFluid(location.getPath());
        }
        if (fluid == null) {
            throw new JsonParseException("Unknown fluid: " + id);
        }
        return fluid;
    }

    @Override
    public IRecipe parse(JsonContext context, JsonObject json) {
        ModRecipes.REACTION_CHAMBER_RECIPES.add(ReactionChamberRecipeFactory.fromJson(json, context));
        return new NonCraftingRecipe();
    }

}
