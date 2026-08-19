package com.formlesslab.ae2additions.recipe;

import ae2.util.inv.AppEngInternalInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;

import java.util.ArrayList;
import java.util.List;

public class ReactionChamberRecipe {
    private final List<SizedIngredient> itemInputs;
    private final Fluid inputFluid;
    private final int inputFluidAmount;
    private final int energy;
    private final ItemStack itemOutput;
    private final FluidStack fluidOutput;

    public ReactionChamberRecipe(List<SizedIngredient> itemInputs, Fluid inputFluid, int inputFluidAmount, int energy, ItemStack itemOutput, FluidStack fluidOutput) {
        this.itemInputs = itemInputs;
        this.inputFluid = inputFluid;
        this.inputFluidAmount = inputFluidAmount;
        this.energy = energy;
        this.itemOutput = itemOutput;
        this.fluidOutput = fluidOutput;
    }

    private static List<ItemStack> copyInputs(AppEngInternalInventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(inventory.size());
        for (int slot = 0; slot < inventory.size(); slot++) {
            stacks.add(inventory.getStackInSlot(slot).copy());
        }
        return stacks;
    }

    public boolean matches(AppEngInternalInventory inventory, FluidTank tank) {
        if (!hasFluid(tank, 1)) {
            return false;
        }
        return this.canConsumeItems(copyInputs(inventory), 1);
    }

    public boolean canOutput(ItemStack currentOutput, FluidTank outputTank, int outputTankCapacity) {
        return this.canOutput(currentOutput, outputTank, outputTankCapacity, 1);
    }

    public boolean canOutput(ItemStack currentOutput, FluidTank outputTank, int outputTankCapacity, int runs) {
        if (runs <= 0) {
            return false;
        }
        if (this.isItemOutput()) {
            ItemStack output = this.getItemOutput();
            output.setCount(output.getCount() * runs);
            if (currentOutput.isEmpty()) {
                return output.getCount() <= output.getMaxStackSize();
            }
            if (!ItemStack.areItemsEqual(currentOutput, output) || !ItemStack.areItemStackTagsEqual(currentOutput, output)) {
                return false;
            }
            return currentOutput.getCount() + output.getCount() <= currentOutput.getMaxStackSize();
        }

        FluidStack produced = this.getFluidOutput();
        produced.amount *= runs;
        FluidStack output = outputTank.getFluid();
        if (output == null) {
            return produced.amount <= outputTankCapacity;
        }
        return output.isFluidEqual(produced) && output.amount + produced.amount <= outputTankCapacity;
    }

    public void consume(AppEngInternalInventory inventory, FluidTank tank, int runs) {
        for (SizedIngredient input : this.itemInputs) {
            int remaining = input.amount() * runs;
            for (int slot = 0; slot < inventory.size(); slot++) {
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty() || !input.ingredient().apply(stack)) {
                    continue;
                }
                int consumed = Math.min(remaining, stack.getCount());
                inventory.extractItem(slot, consumed, false);
                remaining -= consumed;
                if (remaining <= 0) {
                    break;
                }
            }
        }
        tank.drain(new FluidStack(this.inputFluid, this.inputFluidAmount * runs), true);
    }

    private boolean hasFluid(FluidTank tank, int runs) {
        FluidStack stack = tank.getFluid();
        return stack != null && stack.getFluid() == this.inputFluid && stack.amount >= this.inputFluidAmount * runs;
    }

    public boolean containsIngredient(ItemStack stack) {
        for (SizedIngredient input : this.itemInputs) {
            if (input.ingredient().apply(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isItemOutput() {
        return !this.itemOutput.isEmpty();
    }

    public List<SizedIngredient> getItemInputs() {
        return this.itemInputs;
    }

    public FluidStack getInputFluid() {
        return new FluidStack(this.inputFluid, this.inputFluidAmount);
    }

    public int getInputFluidAmount() {
        return this.inputFluidAmount;
    }

    public ItemStack getItemOutput() {
        return this.itemOutput.copy();
    }

    public FluidStack getFluidOutput() {
        return this.fluidOutput == null ? null : this.fluidOutput.copy();
    }

    public boolean hasItemOutput(ItemStack output) {
        return this.isItemOutput() && output != null && !output.isEmpty()
                && ItemStack.areItemsEqual(this.itemOutput, output)
                && ItemStack.areItemStackTagsEqual(this.itemOutput, output);
    }

    public boolean hasFluidOutput(FluidStack output) {
        return this.fluidOutput != null && output != null && this.fluidOutput.isFluidEqual(output);
    }

    public int getEnergy() {
        return this.energy;
    }

    public int getMaxRuns(AppEngInternalInventory inventory, FluidTank tank, int limit) {
        int runs = Math.max(0, limit);
        FluidStack fluidStack = tank.getFluid();
        if (fluidStack == null || fluidStack.getFluid() != this.inputFluid) {
            return 0;
        }
        runs = Math.min(runs, fluidStack.amount / this.inputFluidAmount);
        for (int candidate = runs; candidate > 0; candidate--) {
            if (this.canConsumeItems(copyInputs(inventory), candidate)) {
                return candidate;
            }
        }
        return 0;
    }

    private boolean canConsumeItems(List<ItemStack> stacks, int runs) {
        for (SizedIngredient input : this.itemInputs) {
            int remaining = input.amount() * runs;
            for (ItemStack stack : stacks) {
                if (stack.isEmpty() || !input.ingredient().apply(stack)) {
                    continue;
                }
                int consumed = Math.min(remaining, stack.getCount());
                stack.shrink(consumed);
                remaining -= consumed;
                if (remaining <= 0) {
                    break;
                }
            }
            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    public record SizedIngredient(Ingredient ingredient, int amount) {
    }
}
