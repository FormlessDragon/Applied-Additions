package com.formlesslab.ae2additions.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public abstract class ModClientboundPacket implements IMessage {
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

    @SideOnly(Side.CLIENT)
    public void handleClient(Minecraft minecraft) {
    }

    public static final class Handler<T extends ModClientboundPacket> implements IMessageHandler<T, IMessage> {
        @Override
        public IMessage onMessage(T message, MessageContext ctx) {
            runClient(message);
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void runClient(T message) {
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.addScheduledTask(() -> message.handleClient(minecraft));
        }
    }
}
