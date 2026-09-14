package com.formlesslab.ae2additions.compat.crafttweaker;

import ae2.api.stacks.AEKey;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

/**
 * A chainable list of arbitrary AE keys for an infinity cell.
 */
@ZenRegister
@ZenClass("mods.ae2additions.KeyList")
public final class KeyList {
    private final List<Supplier<? extends AEKey>> suppliers = new ObjectArrayList<>();

    @ZenMethod
    public static KeyList create() {
        return new KeyList();
    }

    @ZenMethod
    public static KeyList create(Object[] keys) {
        return create().addAll(keys);
    }

    @ZenMethod
    public static KeyList of() {
        return create();
    }

    @ZenMethod
    public static KeyList of(Object[] keys) {
        return create(keys);
    }

    @ZenMethod
    public KeyList add(AEKeyValue key) {
        return add((Object) key);
    }

    @ZenMethod
    public KeyList add(IItemStack key) {
        return add((Object) key);
    }

    @ZenMethod
    public KeyList add(ILiquidStack key) {
        return add((Object) key);
    }

    @ZenMethod
    public KeyList add(Object key) {
        suppliers.add(CraftTweakerKeyUtil.supplier(key));
        return this;
    }

    @ZenMethod
    public KeyList addAll(Object[] keys) {
        if (keys != null) {
            for (Object key : keys) {
                add(key);
            }
        }
        return this;
    }

    /**
     * Alias for scripts that prefer the plural spelling used by the upstream API.
     */
    @ZenMethod
    public KeyList adds(Object[] keys) {
        return addAll(keys);
    }

    public List<Supplier<? extends AEKey>> getSuppliers() {
        return Collections.unmodifiableList(suppliers);
    }

    public boolean isEmpty() {
        return suppliers.isEmpty();
    }
}
