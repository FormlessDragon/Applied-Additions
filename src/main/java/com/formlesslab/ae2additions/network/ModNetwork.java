package com.formlesslab.ae2additions.network;

import com.formlesslab.ae2additions.Reference;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public final class ModNetwork {
    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(Reference.MOD_ID);

    public static final int QUANTUM_TASK_CANCEL = 0;
    public static final int QUANTUM_CPU_SELECTION = 1;
    public static final int ASSEMBLER_MATRIX_UPDATE = 2;
    public static final int ASSEMBLER_MATRIX_CANCEL = 3;
    public static final int ASSEMBLER_MATRIX_PATTERN_MODE = 4;

    private static boolean initialized;

    private ModNetwork() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        // TODO quantum worker: register packet class for QUANTUM_TASK_CANCEL.
        // TODO quantum worker: register packet class for QUANTUM_CPU_SELECTION.
        registerClientbound(ASSEMBLER_MATRIX_UPDATE,
            com.formlesslab.ae2additions.assembler.network.SAssemblerMatrixUpdate.class);
        registerServerbound(ASSEMBLER_MATRIX_CANCEL,
            com.formlesslab.ae2additions.assembler.network.CAssemblerMatrixCancel.class);
        registerServerbound(ASSEMBLER_MATRIX_PATTERN_MODE,
            com.formlesslab.ae2additions.assembler.network.CAssemblerMatrixPatternMode.class);
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
