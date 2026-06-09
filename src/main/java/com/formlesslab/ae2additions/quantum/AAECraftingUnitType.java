package com.formlesslab.ae2additions.quantum;

import ae2.block.crafting.ICraftingUnitType;
import net.minecraft.item.Item;

public enum AAECraftingUnitType implements ICraftingUnitType {
    QUANTUM_UNIT("quantum_unit", 0),
    QUANTUM_CORE("quantum_core", 256),
    QUANTUM_STORAGE_128("quantum_storage_128", 128),
    QUANTUM_STORAGE_256("quantum_storage_256", 256),
    DATA_ENTANGLER("data_entangler", 0),
    QUANTUM_ACCELERATOR("quantum_accelerator", 0),
    QUANTUM_MULTI_THREADER("quantum_multi_threader", 0),
    QUANTUM_STRUCTURE("quantum_structure", 0);

    private final String registryName;
    private final int storageMb;

    AAECraftingUnitType(String registryName, int storageMb) {
        this.registryName = registryName;
        this.storageMb = storageMb;
    }

    public String getRegistryName() {
        return this.registryName;
    }

    @Override
    public long getStorageBytes() {
        return 1024L * 1024L * this.storageMb;
    }

    public int getStorageMultiplier() {
        return this == DATA_ENTANGLER ? QuantumConfig.getDataEntanglerMultiplier() : 0;
    }

    @Override
    public int getAcceleratorThreads() {
        return this == QUANTUM_ACCELERATOR || this == QUANTUM_CORE ? QuantumConfig.getAcceleratorThreads() : 0;
    }

    public int getAccelerationMultiplier() {
        return this == QUANTUM_MULTI_THREADER ? QuantumConfig.getMultiThreaderMultiplier() : 0;
    }

    public boolean isBoundaryOnly() {
        return this == QUANTUM_STRUCTURE;
    }

    public boolean isInternalOnly() {
        return this != QUANTUM_STRUCTURE;
    }

    @Override
    public Item getItemFromType() {
        return Item.getItemFromBlock(QuantumContent.getBlock(this));
    }
}
