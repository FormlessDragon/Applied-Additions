package com.formlesslab.ae2additions.util;

import ae2.api.networking.IGrid;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.items.tools.powered.WirelessTerminalItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.function.Consumer;

public final class WirelessTerminalAccess {

    public static ItemGuiHostLocator locatorForSlot(int encodedSlot) {
        return encodedSlot >= 0
                ? GuiHostLocators.forInventorySlot(encodedSlot)
                : GuiHostLocators.forBaubleSlot(-encodedSlot - 1);
    }

    public static ItemStack getTerminal(EntityPlayer player, int encodedSlot) {
        return locatorForSlot(encodedSlot).locateItem(player);
    }

    public static boolean isWirelessTerminal(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof WirelessTerminalItem;
    }

    public static IUpgradeInventory getUpgrades(ItemStack stack) {
        if (!isWirelessTerminal(stack)) {
            return null;
        }
        try {
            Object result = stack.getItem().getClass()
                    .getMethod("getUpgrades", ItemStack.class)
                    .invoke(stack.getItem(), stack);
            return result instanceof IUpgradeInventory ? (IUpgradeInventory) result : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static IGrid getLinkedGrid(ItemStack stack, EntityPlayerMP player) {
        if (!isWirelessTerminal(stack)) {
            return null;
        }
        try {
            Object result = stack.getItem().getClass().getMethod("getLinkedGrid", ItemStack.class,
                    World.class, Consumer.class)
                    .invoke(stack.getItem(), stack, player.world, null);
            return result instanceof IGrid ? (IGrid) result : null;
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }
}
