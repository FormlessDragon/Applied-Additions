package com.formlesslab.ae2additions.quantum.client.gui;

import ae2.api.config.CpuSelectionMode;
import ae2.api.networking.crafting.ICraftingCPU;

import java.util.Collections;
import java.util.List;

/**
 * Narrow bridge for the quantum computer GUI while the real tile/container code
 * is migrated by the backend workers.
 */
public interface QuantumComputerHost {
    default List<? extends ICraftingCPU> getQuantumCpus() {
        return Collections.emptyList();
    }

    default CpuSelectionMode getQuantumSelectionMode() {
        return CpuSelectionMode.ANY;
    }

    default void setQuantumSelectionMode(CpuSelectionMode mode) {
    }
}
