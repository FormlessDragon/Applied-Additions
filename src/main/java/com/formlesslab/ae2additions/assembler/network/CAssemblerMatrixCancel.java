package com.formlesslab.ae2additions.assembler.network;

import com.formlesslab.ae2additions.network.ModServerboundPacket;
import net.minecraft.entity.player.EntityPlayerMP;

public class CAssemblerMatrixCancel extends ModServerboundPacket {
    @Override
    public void handleServer(EntityPlayerMP player) {
        if (player.openContainer instanceof AssemblerMatrixServerActionHost) {
            ((AssemblerMatrixServerActionHost) player.openContainer).cancelAssemblerMatrixJobs();
        }
    }
}
