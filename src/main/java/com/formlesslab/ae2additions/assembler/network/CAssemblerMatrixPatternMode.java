package com.formlesslab.ae2additions.assembler.network;

import com.formlesslab.ae2additions.network.ModServerboundPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;

public class CAssemblerMatrixPatternMode extends ModServerboundPacket {
    private boolean hideProviders;

    public CAssemblerMatrixPatternMode() {
    }

    public CAssemblerMatrixPatternMode(boolean hideProviders) {
        this.hideProviders = hideProviders;
    }

    @Override
    protected void read(ByteBuf buf) {
        this.hideProviders = buf.readBoolean();
    }

    @Override
    protected void write(ByteBuf buf) {
        buf.writeBoolean(this.hideProviders);
    }

    @Override
    public void handleServer(EntityPlayerMP player) {
        if (player.openContainer instanceof AssemblerMatrixServerActionHost) {
            ((AssemblerMatrixServerActionHost) player.openContainer).setAssemblerMatrixPatternMode(this.hideProviders);
        }
    }
}
