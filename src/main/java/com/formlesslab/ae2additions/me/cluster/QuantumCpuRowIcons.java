package com.formlesslab.ae2additions.me.cluster;

import ae2.client.gui.Icon;
import com.formlesslab.ae2additions.Tags;
import net.minecraft.util.ResourceLocation;

/**
 * Crafting status row backgrounds that mark a row as part of a quantum computer group.
 * <p>
 * These have to exist on both sides: the server picks the icon and serializes its id, the client resolves that id
 * again through {@link Icon#byId(ResourceLocation)}.
 */
public final class QuantumCpuRowIcons {
    /**
     * AE2 rejects crafting CPU row backgrounds that are not exactly this size.
     */
    private static final int ROW_WIDTH = 67;
    private static final int ROW_HEIGHT = 22;

    private static final Icon SINGLE = register("quantum_cpu_row_single");
    private static final Icon FIRST = register("quantum_cpu_row_first");
    private static final Icon MIDDLE = register("quantum_cpu_row_middle");
    private static final Icon LAST = register("quantum_cpu_row_last");
    private static final Icon SINGLE_FOCUSED = register("quantum_cpu_row_single_focused");
    private static final Icon FIRST_FOCUSED = register("quantum_cpu_row_first_focused");
    private static final Icon MIDDLE_FOCUSED = register("quantum_cpu_row_middle_focused");
    private static final Icon LAST_FOCUSED = register("quantum_cpu_row_last_focused");

    private QuantumCpuRowIcons() {
    }

    /**
     * Loads this class during mod init. The client never picks an icon itself, so without this the icons would stay
     * unregistered there and every id the server sent would fail to resolve.
     */
    public static void bootstrap() {
    }

    public static Icon forRow(boolean firstInGroup, boolean lastInGroup, boolean focused) {
        if (firstInGroup && lastInGroup) {
            return focused ? SINGLE_FOCUSED : SINGLE;
        }
        if (firstInGroup) {
            return focused ? FIRST_FOCUSED : FIRST;
        }
        if (lastInGroup) {
            return focused ? LAST_FOCUSED : LAST;
        }
        return focused ? MIDDLE_FOCUSED : MIDDLE;
    }

    private static Icon register(String name) {
        return Icon.register(new ResourceLocation(Tags.MOD_ID, name), ROW_WIDTH, ROW_HEIGHT);
    }
}
