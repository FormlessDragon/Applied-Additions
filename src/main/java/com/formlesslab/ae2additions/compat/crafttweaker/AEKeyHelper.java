package com.formlesslab.ae2additions.compat.crafttweaker;

import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.liquid.ILiquidStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Objects;

/**
 * Constructors for AE keys used by the infinity-cell registration API.
 */
@ZenRegister
@ZenClass("mods.ae2additions.AEKeyHelper")
public final class AEKeyHelper {
    private AEKeyHelper() {
    }

    @ZenMethod
    public static AEKeyValue item(IItemStack stack) {
        return new AEKeyValue(CraftTweakerKeyUtil.supplier(stack));
    }

    @ZenMethod
    public static AEKeyValue item(String id) {
        return item(id, (NBTTagCompound) null);
    }

    @ZenMethod
    public static AEKeyValue item(String id, String nbt) {
        return item(id, parseTag(nbt));
    }

    @ZenMethod
    public static AEKeyValue item(String id, IData nbt) {
        return item(id, CraftTweakerMC.getNBTCompound(nbt));
    }

    private static AEKeyValue item(String id, NBTTagCompound nbt) {
        Item item = Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(new ResourceLocation(id)), "Unknown item: " + id);
        ItemStack stack = new ItemStack(item);
        if (nbt != null) {
            stack.setTagCompound(nbt.copy());
        }
        return new AEKeyValue(() -> Objects.requireNonNull(AEItemKey.of(stack), "Unable to create an AE item key"));
    }

    @ZenMethod
    public static AEKeyValue fluid(ILiquidStack stack) {
        return new AEKeyValue(CraftTweakerKeyUtil.supplier(stack));
    }

    @ZenMethod
    public static AEKeyValue fluid(String id) {
        return fluid(id, (NBTTagCompound) null);
    }

    @ZenMethod
    public static AEKeyValue fluid(String id, String nbt) {
        return fluid(id, parseTag(nbt));
    }

    @ZenMethod
    public static AEKeyValue fluid(String id, IData nbt) {
        return fluid(id, CraftTweakerMC.getNBTCompound(nbt));
    }

    private static AEKeyValue fluid(String id, NBTTagCompound nbt) {
        Fluid fluid = FluidRegistry.getFluid(id);
        if (fluid == null && id.indexOf(':') >= 0) {
            fluid = FluidRegistry.getFluid(new ResourceLocation(id).getPath());
        }
        Fluid resolved = Objects.requireNonNull(fluid, "Unknown fluid: " + id);
        FluidStack stack = new FluidStack(resolved, Fluid.BUCKET_VOLUME);
        if (nbt != null) {
            stack.tag = nbt.copy();
        }
        return new AEKeyValue(() -> Objects.requireNonNull(AEFluidKey.of(stack), "Unable to create an AE fluid key for " + resolved.getName()));
    }

    /**
     * Wraps an AEKey or an addon-provided Supplier&lt;AEKey&gt; for ZenScript.
     */
    @ZenMethod
    public static AEKeyValue of(Object key) {
        return new AEKeyValue(CraftTweakerKeyUtil.supplier(key));
    }

    private static NBTTagCompound parseTag(String nbt) {
        if (nbt == null || nbt.trim().isEmpty()) {
            return null;
        }
        try {
            return JsonToNBT.getTagFromJson(nbt);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid item/fluid NBT: " + nbt, e);
        }
    }
}
