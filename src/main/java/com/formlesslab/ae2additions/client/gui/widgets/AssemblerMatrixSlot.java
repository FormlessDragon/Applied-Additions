package com.formlesslab.ae2additions.client.gui.widgets;

import ae2.api.inventories.InternalInventory;
import ae2.container.slot.AppEngSlot;
import ae2.crafting.pattern.EncodedPatternItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class AssemblerMatrixSlot extends AppEngSlot {
    private final long patternId;
    private final int offset;

    public AssemblerMatrixSlot(InternalInventory machineInv, int machineInvSlot, int offset, long patternId, int x, int y) {
        super(machineInv, machineInvSlot, x, y);
        this.patternId = patternId;
        this.offset = offset;
        this.setNotDraggable();
    }

    public int getActualSlot() {
        return this.getSlotIndex() + this.offset;
    }

    public long getPatternId() {
        return this.patternId;
    }

    @Override
    public ItemStack getDisplayStack() {
        ItemStack stack = super.getDisplayStack();
        if (!stack.isEmpty() && stack.getItem() instanceof EncodedPatternItem<?>) {
            World world = getDisplayWorld();
            ItemStack output = ((EncodedPatternItem<?>) stack.getItem()).getOutput(stack, world);
            if (!output.isEmpty()) {
                return output;
            }
        }
        return stack;
    }

    @Override
    public boolean isItemValid(ItemStack stack) {
        return super.isItemValid(stack);
    }

    @Override
    public int getSlotStackLimit() {
        return super.getSlotStackLimit();
    }

    private static World getDisplayWorld() {
        return net.minecraft.client.Minecraft.getMinecraft().world;
    }
}
