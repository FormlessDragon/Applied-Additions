package com.formlesslab.ae2additions.compat.crafttweaker;

import ae2.api.stacks.AEKey;
import com.formlesslab.ae2additions.init.ModContent;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * CraftTweaker registration API for script-defined infinity cells.
 */
@ZenRegister
@ZenClass("mods.ae2additions.InfinityCell")
public final class InfinityCell {
    private InfinityCell() {
    }

    /**
     * Registers one or more keys. A single key is represented by a one-element list internally.
     */
    @ZenMethod
    public static IItemStack register(String name, Object keys) {
        return registerKeys(name, null, CraftTweakerKeyUtil.suppliers(keys));
    }

    /**
     * Explicit array overload keeps ZenScript array calls unambiguous.
     */
    @ZenMethod
    public static IItemStack register(String name, Object[] keys) {
        return register(name, (Object) keys);
    }

    @ZenMethod
    public static IItemStack register(String name, IItemStack key) {
        return register(name, (Object) key);
    }

    @ZenMethod
    public static IItemStack register(String name, ILiquidStack key) {
        return register(name, (Object) key);
    }

    @ZenMethod
    public static IItemStack register(String name, AEKeyValue key) {
        return register(name, (Object) key);
    }

    /**
     * Allows a Java.loadClass() value whose field is an AE2 AEKey.
     */
    @ZenMethod
    public static IItemStack register(String name, AEKey key) {
        return register(name, (Object) key);
    }

    @ZenMethod
    public static IItemStack register(String name, KeyList keys) {
        return registerKeys(name, null, keys == null ? null : keys.getSuppliers());
    }

    @ZenMethod
    public static IItemStack register(String name, String displayName, KeyList keys) {
        return registerKeys(name, displayName, keys == null ? null : keys.getSuppliers());
    }

    @ZenMethod
    public static IItemStack register(String name, String displayName, Object keys) {
        return registerKeys(name, displayName, CraftTweakerKeyUtil.suppliers(keys));
    }

    /**
     * Convenience form for a mixed item/fluid list.
     */
    @ZenMethod
    public static IItemStack register(String name, IItemStack[] items, ILiquidStack[] fluids) {
        List<Object> keys = new ArrayList<>();
        if (items != null) {
            Collections.addAll(keys, items);
        }
        if (fluids != null) {
            Collections.addAll(keys, fluids);
        }
        return register(name, keys.toArray());
    }

    @ZenMethod
    public static IItemStack register(String name, IItemStack[] items) {
        return register(name, items, null);
    }

    @ZenMethod
    public static IItemStack registerFluids(String name, ILiquidStack[] fluids) {
        return register(name, new IItemStack[0], fluids);
    }

    private static IItemStack registerKeys(String name, String displayName, List<Supplier<? extends AEKey>> suppliers) {
        if (suppliers == null || suppliers.isEmpty()) {
            throw new IllegalArgumentException("An infinity cell needs at least one key");
        }
        ItemStack stack = new ItemStack(ModContent.registerInfinityCell(name, suppliers, displayName));
        return CraftTweakerMC.getIItemStack(stack);
    }
}
