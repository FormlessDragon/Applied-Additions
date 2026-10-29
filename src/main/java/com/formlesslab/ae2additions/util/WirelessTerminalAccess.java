package com.formlesslab.ae2additions.util;

import ae2.api.networking.IGrid;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.core.gui.locator.GuiHostLocators;
import ae2.core.gui.locator.ItemGuiHostLocator;
import ae2.items.tools.powered.WirelessTerminalItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

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
        var item = stack.getItem();
        if (item instanceof WirelessTerminalItem terminal) {
            return terminal.getUpgrades(stack);
        }
        return null;
    }

    public static IGrid getLinkedGrid(ItemStack stack, EntityPlayerMP player) {
        if (!isWirelessTerminal(stack)) {
            return null;
        }
        if (stack.getItem() instanceof WirelessTerminalItem terminal) {
            return terminal.getLinkedGrid(stack, player.world, null);
        }
        return null;
    }
}
