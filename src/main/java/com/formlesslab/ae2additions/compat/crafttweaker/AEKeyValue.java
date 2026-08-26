package com.formlesslab.ae2additions.compat.crafttweaker;

import ae2.api.stacks.AEKey;
import crafttweaker.annotations.ZenRegister;
import stanhebben.zenscript.annotations.ZenClass;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A ZenScript-friendly reference to any AE2 key, including keys from addons.
 */
@ZenRegister
@ZenClass("mods.ae2additions.AEKey")
public final class AEKeyValue {
    private final Supplier<? extends AEKey> supplier;

    public AEKeyValue(Supplier<? extends AEKey> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier");
    }

    public AEKey getKey() {
        return Objects.requireNonNull(supplier.get(), "AE key supplier returned null");
    }

    public Supplier<? extends AEKey> getSupplier() {
        return supplier;
    }
}
