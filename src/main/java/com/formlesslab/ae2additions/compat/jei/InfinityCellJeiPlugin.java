package com.formlesslab.ae2additions.compat.jei;

import com.formlesslab.ae2additions.init.ModContent;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.ISubtypeRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.ingredients.IIngredientRegistry;
import mezz.jei.api.ingredients.VanillaTypes;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.NonNull;

import java.util.List;

/**
 * Exposes every NBT-configured infinity-cell stack as a distinct JEI ingredient.
 */
@JEIPlugin
public final class InfinityCellJeiPlugin implements IModPlugin {

    private IIngredientRegistry ingredientRegistry;

    @Override
    public void registerSubtypes(ISubtypeRegistry subtypeRegistry) {
        // JEI must not merge the different configurations of the shared item.
        subtypeRegistry.useNbtForSubtypes(ModContent.INFINITY_CELL);
    }

    @Override
    public void register(IModRegistry registry) {
        ingredientRegistry = registry.getIngredientRegistry();
    }

    @Override
    public void onRuntimeAvailable(@NonNull IJeiRuntime jeiRuntime) {
        List<ItemStack> variants = ModContent.getInfinityCellVariants();
        if (variants.isEmpty()) {
            return;
        }

        ingredientRegistry.addIngredientsAtRuntime(VanillaTypes.ITEM, variants);
    }
}
