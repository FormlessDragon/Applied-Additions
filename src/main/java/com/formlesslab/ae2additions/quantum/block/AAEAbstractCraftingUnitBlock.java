package com.formlesslab.ae2additions.quantum.block;

import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;

/**
 * Compatibility alias for code that expects the AdvancedAE class name. The
 * concrete 1.12 implementation lives in {@link AAECraftingUnitBlock}.
 */
public abstract class AAEAbstractCraftingUnitBlock<T extends AdvCraftingBlockEntity> extends AAECraftingUnitBlock {
    protected AAEAbstractCraftingUnitBlock(com.formlesslab.ae2additions.quantum.AAECraftingUnitType type) {
        super(type);
    }
}
