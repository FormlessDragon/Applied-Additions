package com.formlesslab.ae2additions.init;

import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.network.CAssemblerMatrixCancel;
import com.formlesslab.ae2additions.network.CAssemblerMatrixPatternMode;
import com.formlesslab.ae2additions.network.CReactionChamberOutputSides;
import com.formlesslab.ae2additions.network.SAssemblerMatrixUpdate;
import com.formlesslab.ae2additions.network.base.ModClientboundPacket;
import com.formlesslab.ae2additions.network.base.ModServerboundPacket;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModNetworks {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Tags.MOD_ID);

    public static final int QUANTUM_TASK_CANCEL = 0;
    public static final int QUANTUM_CPU_SELECTION = 1;
    public static final int ASSEMBLER_MATRIX_UPDATE = 2;
    public static final int ASSEMBLER_MATRIX_CANCEL = 3;
    public static final int ASSEMBLER_MATRIX_PATTERN_MODE = 4;
    public static final int REACTION_CHAMBER_OUTPUT_SIDES = 5;

    private static boolean initialized;

    private ModNetworks() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // TODO quantum worker: register packet class for QUANTUM_TASK_CANCEL.
        // TODO quantum worker: register packet class for QUANTUM_CPU_SELECTION.
        registerClientbound(ASSEMBLER_MATRIX_UPDATE, SAssemblerMatrixUpdate.class);
        registerServerbound(ASSEMBLER_MATRIX_CANCEL, CAssemblerMatrixCancel.class);
        registerServerbound(ASSEMBLER_MATRIX_PATTERN_MODE, CAssemblerMatrixPatternMode.class);
        registerServerbound(REACTION_CHAMBER_OUTPUT_SIDES, CReactionChamberOutputSides.class);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends ModServerboundPacket> void registerServerbound(int id, Class<T> packet) {
        CHANNEL.registerMessage((Class) ModServerboundPacket.Handler.class, packet, id, Side.SERVER);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static <T extends ModClientboundPacket> void registerClientbound(int id, Class<T> packet) {
        CHANNEL.registerMessage((Class) ModClientboundPacket.Handler.class, packet, id, Side.CLIENT);
    }

    public static void sendToServer(ModServerboundPacket packet) {
        CHANNEL.sendToServer(packet);
    }

    public static void sendToClient(EntityPlayerMP player, ModClientboundPacket packet) {
        CHANNEL.sendTo(packet, player);
    }
}
