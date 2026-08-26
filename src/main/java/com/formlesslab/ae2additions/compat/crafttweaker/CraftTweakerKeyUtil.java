package com.formlesslab.ae2additions.compat.crafttweaker;

import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

final class CraftTweakerKeyUtil {
    private CraftTweakerKeyUtil() {
    }

    static Supplier<? extends AEKey> supplier(Object value) {
        if (value instanceof AEKeyValue keyValue) {
            return keyValue::getKey;
        }
        if (value instanceof Supplier<?> source) {
            return () -> resolve(source.get());
        }
        if (value instanceof AEKey) {
            return () -> (AEKey) value;
        }
        if (value instanceof IItemStack) {
            ItemStack stack = CraftTweakerMC.getItemStack((IItemStack) value);
            if (stack == null || stack.isEmpty()) {
                throw new IllegalArgumentException("Cannot register an empty item as an infinity key");
            }
            ItemStack copy = stack.copy();
            return () -> Objects.requireNonNull(AEItemKey.of(copy), "Unable to create an AE item key");
        }
        if (value instanceof ILiquidStack) {
            FluidStack stack = CraftTweakerMC.getLiquidStack((ILiquidStack) value);
            if (stack == null || stack.getFluid() == null) {
                throw new IllegalArgumentException("Cannot register an empty fluid as an infinity key");
            }
            FluidStack copy = stack.copy();
            return () -> Objects.requireNonNull(AEFluidKey.of(copy), "Unable to create an AE fluid key");
        }
        if (value instanceof ItemStack) {
            ItemStack stack = ((ItemStack) value).copy();
            if (stack.isEmpty()) {
                throw new IllegalArgumentException("Cannot register an empty item as an infinity key");
            }
            return () -> Objects.requireNonNull(AEItemKey.of(stack), "Unable to create an AE item key");
        }
        if (value instanceof FluidStack) {
            FluidStack stack = ((FluidStack) value).copy();
            if (stack.getFluid() == null) {
                throw new IllegalArgumentException("Cannot register an empty fluid as an infinity key");
            }
            return () -> Objects.requireNonNull(AEFluidKey.of(stack), "Unable to create an AE fluid key");
        }
        throw new IllegalArgumentException("Unsupported infinity key type: " + (value == null ? "null" : value.getClass().getName()));
    }

    static AEKey resolve(Object value) {
        return Objects.requireNonNull(supplier(value).get(), "Infinity key resolved to null");
    }

    static List<Supplier<? extends AEKey>> suppliers(Object values) {
        List<Supplier<? extends AEKey>> result = new ArrayList<>();
        switch (values) {
            case null -> {
                return result;
            }
            case KeyList keyList -> {
                result.addAll(keyList.getSuppliers());
                return result;
            }
            case Iterable<?> objects -> {
                for (Object value : objects) {
                    result.add(supplier(value));
                }
                return result;
            }
            default -> {
            }
        }
        Class<?> type = values.getClass();
        if (type.isArray()) {
            int length = Array.getLength(values);
            for (int i = 0; i < length; i++) {
                result.add(supplier(Array.get(values, i)));
            }
            return result;
        }
        result.add(supplier(values));
        return result;
    }
}
