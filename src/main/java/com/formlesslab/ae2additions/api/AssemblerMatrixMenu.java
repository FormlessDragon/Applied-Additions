package com.formlesslab.ae2additions.api;

/**
 * Client-facing contract for the assembler matrix screen.
 *
 * <p>The backend container should implement this once Worker B wires the
 * server-side assembler matrix. Keeping this interface here lets the GUI
 * migrate independently without editing container or registration packages.</p>
 */
public interface AssemblerMatrixMenu {
    int PATTERN_SLOTS = 36;

    default int getRunningThreads() {
        return 0;
    }

    default boolean isPatternProvidersHidden() {
        return false;
    }

    default void requestCancel() {
    }

    default void requestPatternMode(boolean hide) {
    }
}
