package com.formlesslab.ae2additions.network;

import ae2.api.upgrades.IUpgradeInventory;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.init.ModGuiHandler;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.util.WirelessTerminalAccess;
import com.formlesslab.ae2additions.network.base.ModServerboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

public final class COpenImportExportCard extends ModServerboundPacket {
    private int terminalSlot;
    private int cardSlot;
    private boolean export;

    public COpenImportExportCard() {
    }

    public COpenImportExportCard(int terminalSlot, int cardSlot, boolean export) {
        this.terminalSlot = terminalSlot;
        this.cardSlot = cardSlot;
        this.export = export;
    }

    @Override
    protected void read(ByteBuf buf) {
        terminalSlot = buf.readByte();
        cardSlot = buf.readByte();
        export = buf.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeByte(terminalSlot);
        buf.writeByte(cardSlot);
        buf.writeBoolean(export);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        ItemStack terminal = WirelessTerminalAccess.getTerminal(player, terminalSlot);
        if (!WirelessTerminalAccess.isWirelessTerminal(terminal)) {
            return;
        }
        IUpgradeInventory upgrades = WirelessTerminalAccess.getUpgrades(terminal);
        if (upgrades == null) {
            return;
        }
        if (cardSlot < 0 || cardSlot >= upgrades.size()) {
            return;
        }
        ItemStack card = upgrades.getStackInSlot(cardSlot);
        if (card.getItem() != (export ? ModContent.EXPORT_CARD : ModContent.IMPORT_CARD)) {
            return;
        }
        player.openGui(AppliedAdditions.INSTANCE, ModGuiHandler.IMPORT_EXPORT_CARD, player.world, terminalSlot, cardSlot, export ? 1 : 0);
    }
}
