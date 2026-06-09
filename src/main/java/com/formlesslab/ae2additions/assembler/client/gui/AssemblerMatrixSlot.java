package com.formlesslab.ae2additions.assembler.client.gui;

import ae2.api.inventories.InternalInventory;
import ae2.container.slot.AppEngSlot;
import ae2.crafting.pattern.EncodedPatternItem;
import net.minecraft.entity.player.EntityPlayer;
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
        return false;
    }

    @Override
    public void putStack(ItemStack stack) {
    }

    @Override
    public int getSlotStackLimit() {
        return 0;
    }

    @Override
    public ItemStack decrStackSize(int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canTakeStack(EntityPlayer player) {
        return false;
    }

    private static World getDisplayWorld() {
        return net.minecraft.client.Minecraft.getMinecraft().world;
    }
}
