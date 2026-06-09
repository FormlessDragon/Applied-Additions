package com.formlesslab.ae2additions.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public abstract class ModServerboundPacket implements IMessage {
    @Override
    public final void fromBytes(ByteBuf buf) {
        this.read(buf);
    }

    @Override
    public final void toBytes(ByteBuf buf) {
        this.write(buf);
    }

    protected void read(ByteBuf buf) {
    }

    protected void write(ByteBuf buf) {
    }

    public void handleServer(EntityPlayerMP player) {
    }

    public static final class Handler<T extends ModServerboundPacket> implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().player;
            player.getServerWorld().addScheduledTask(() -> message.handleServer(player));
            return null;
        }
    }
}
