package com.formlesslab.ae2additions.network;

import com.formlesslab.ae2additions.api.AssemblerMatrixServerActionHost;
import net.minecraft.entity.player.EntityPlayerMP;

public class CAssemblerMatrixCancel extends ModServerboundPacket {
    @Override
    public void handleServer(EntityPlayerMP player) {
        if (player.openContainer instanceof AssemblerMatrixServerActionHost) {
            ((AssemblerMatrixServerActionHost) player.openContainer).cancelAssemblerMatrixJobs();
        }
    }
}
