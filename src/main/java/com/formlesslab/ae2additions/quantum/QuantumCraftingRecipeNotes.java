package com.formlesslab.ae2additions.quantum;

/**
 * Notes for the recipe worker only. The backend does not implement Quantum
 * Crafter, Reaction Chamber, or Quantum Armor behavior.
 */
public final class QuantumCraftingRecipeNotes {
    public static final String[] REQUIRED_ADVANCED_AE_MATERIAL_CHAIN = {
        "quantum_unit uses ae2:crafting_unit, ae2:singularity, and shattered_singularity",
        "quantum_accelerator uses quantum_unit, ae2:singularity, and shattered_singularity",
        "quantum_storage_128 uses quantum_unit, quantum_storage_component, and shattered_singularity",
        "quantum_storage_256 uses two quantum_storage_128, quantum_unit, and shattered_singularity",
        "quantum_core uses quantum_accelerator, quantum_storage_256, quantum_unit, ae2:singularity, and shattered_singularity",
        "data_entangler uses quantum_storage_256, quantum_core, quantum_unit, and shattered_singularity",
        "quantum_multi_threader and quantum_structure should follow the AdvancedAE material chain"
    };

    private QuantumCraftingRecipeNotes() {
    }
}
