package com.formlesslab.ae2additions.network;

import ae2.core.gui.locator.ItemGuiHostLocator;
import com.formlesslab.ae2additions.container.ContainerImportExportCard;
import com.formlesslab.ae2additions.util.WirelessTerminalAccess;
import com.formlesslab.ae2additions.network.base.ModServerboundPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class CCloseImportExportCard extends ModServerboundPacket {
    @Override
    public void handleServer(EntityPlayerMP player) {
        if (!(player.openContainer instanceof ContainerImportExportCard cardContainer)) {
            return;
        }
        int slot = cardContainer.terminalSlot;
        ItemStack terminal = WirelessTerminalAccess.getTerminal(player, slot);
        if (!WirelessTerminalAccess.isWirelessTerminal(terminal)) {
            return;
        }
        try {
            terminal.getItem().getClass().getMethod("openFromInventory", EntityPlayer.class, ItemGuiHostLocator.class).invoke(terminal.getItem(), player, WirelessTerminalAccess.locatorForSlot(slot));
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
