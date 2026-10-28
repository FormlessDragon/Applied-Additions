package com.formlesslab.ae2additions.network;

import ae2.api.stacks.GenericStack;
import com.formlesslab.ae2additions.container.ContainerImportExportCard;
import com.formlesslab.ae2additions.network.base.ModServerboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

public final class CCardConfig extends ModServerboundPacket {
    private static final int SET_FILTER = 0;
    private static final int ASSIGN_SLOT = 1;
    private static final int CLEAR_FILTER = 2;
    private int action;
    private int terminalSlot;
    private int cardSlot;
    private int filterSlot;
    private int inventorySlot;
    private GenericStack filter;

    public static CCardConfig setFilter(int terminal, int card, int filterSlot, GenericStack stack) {
        CCardConfig packet = base(SET_FILTER, terminal, card, filterSlot);
        packet.filter = stack;
        return packet;
    }

    public static CCardConfig assignSlot(int terminal, int card, int inventorySlot, int filterSlot) {
        CCardConfig packet = base(ASSIGN_SLOT, terminal, card, filterSlot);
        packet.inventorySlot = inventorySlot;
        return packet;
    }

    public static CCardConfig clearFilter(int terminal, int card, int filterSlot) {
        return base(CLEAR_FILTER, terminal, card, filterSlot);
    }

    private static CCardConfig base(int action, int terminal, int card, int filter) {
        CCardConfig packet = new CCardConfig();
        packet.action = action;
        packet.terminalSlot = terminal;
        packet.cardSlot = card;
        packet.filterSlot = filter;
        return packet;
    }

    @Override
    protected void read(ByteBuf buf) {
        this.action = buf.readByte();
        this.terminalSlot = buf.readByte();
        this.cardSlot = buf.readByte();
        this.filterSlot = buf.readByte();
        this.inventorySlot = buf.readByte();
        this.filter = readStack(buf);
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeByte(action);
        buf.writeByte(terminalSlot);
        buf.writeByte(cardSlot);
        buf.writeByte(filterSlot);
        buf.writeByte(inventorySlot);
        GenericStack.writeBuffer(filter, new PacketBuffer(buf));
    }

    private static GenericStack readStack(ByteBuf buf) {
        return GenericStack.readBuffer(new PacketBuffer(buf));
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (!(player.openContainer instanceof ContainerImportExportCard container)
                || container.terminalSlot != this.terminalSlot
                || container.cardSlot != this.cardSlot
                || !container.canInteractWith(player)) {
            return;
        }
        int max = container.getFilterSize();
        if (action == ASSIGN_SLOT) {
            if (filterSlot < -1 || filterSlot >= max) {
                return;
            }
            container.setSelectedSlot(inventorySlot, filterSlot + 1);
        } else if (filterSlot >= 0 && filterSlot < max) {
            if (action == SET_FILTER) {
                container.getFilters().setStack(filterSlot, filter);
            } else if (action == CLEAR_FILTER) {
                container.getFilters().setStack(filterSlot, null);
            }
        }
        container.detectAndSendChanges();
    }
}
