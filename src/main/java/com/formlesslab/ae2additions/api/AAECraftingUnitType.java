package com.formlesslab.ae2additions.api;

import ae2.api.crafting.cpu.CraftingUnitVisualDefinition;
import ae2.api.crafting.cpu.CraftingUnitVisualKind;
import ae2.block.crafting.ICraftingUnitType;
import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.init.Configurations;
import com.formlesslab.ae2additions.init.ModContent;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public enum AAECraftingUnitType implements ICraftingUnitType {
    QUANTUM_UNIT("quantum_unit", 0), QUANTUM_CORE("quantum_core", 0), QUANTUM_STORAGE_128("quantum_storage_128", 128), QUANTUM_STORAGE_256("quantum_storage_256", 256), DATA_ENTANGLER("data_entangler", 0), QUANTUM_ACCELERATOR("quantum_accelerator", 0), QUANTUM_MULTI_THREADER("quantum_multi_threader", 0), QUANTUM_STRUCTURE("quantum_structure", 0);

    private static final ResourceLocation FAMILY_ID = quantumComputerId();
    public static final ResourceLocation MODEL_PROVIDER_ID = FAMILY_ID;

    private final String registryName;
    private final int storageMb;
    private final ResourceLocation id;
    private final CraftingUnitVisualDefinition visualDefinition;

    AAECraftingUnitType(String registryName, int storageMb) {
        this.registryName = registryName;
        this.storageMb = storageMb;
        this.id = new ResourceLocation(Tags.MOD_ID, registryName);
        this.visualDefinition = CraftingUnitVisualDefinition.builder(CraftingUnitVisualKind.CUSTOM, new ResourceLocation(Tags.MOD_ID, "quantum_crafting/" + registryName), new ResourceLocation(Tags.MOD_ID, "quantum_crafting/" + registryName + "_formed")).ringTextures(new ResourceLocation(Tags.MOD_ID, "block/quantum_crafting/quantum_structure_formed_face"), new ResourceLocation(Tags.MOD_ID, "block/quantum_crafting/quantum_structure_formed_sides"), new ResourceLocation(Tags.MOD_ID, "block/quantum_crafting/quantum_structure_formed_sides")).formedModelProviderId(quantumComputerId()).build();
    }

    private static ResourceLocation quantumComputerId() {
        return new ResourceLocation(Tags.MOD_ID, "quantum_computer");
    }

    public String getRegistryName() {
        return this.registryName;
    }

    @Override
    public long getStorageBytes() {
        int configuredStorageMb = this == QUANTUM_CORE ? Configurations.QUANTUM_COMPUTER.coreStorageMb : this.storageMb;
        return 1024L * 1024L * configuredStorageMb;
    }

    public int getStorageMultiplier() {
        return this == DATA_ENTANGLER ? Configurations.QUANTUM_COMPUTER.dataEntanglerMultiplier : 0;
    }

    @Override
    public int getAcceleratorThreads() {
        if (this == QUANTUM_CORE) {
            return Configurations.QUANTUM_COMPUTER.coreParallelism;
        }
        return this == QUANTUM_ACCELERATOR ? Configurations.QUANTUM_COMPUTER.acceleratorParallelism : 0;
    }

    public int getAccelerationMultiplier() {
        return this == QUANTUM_MULTI_THREADER ? Configurations.QUANTUM_COMPUTER.multiThreaderMultiplier : 0;
    }

    public boolean isBoundaryOnly() {
        return this == QUANTUM_STRUCTURE;
    }

    public boolean isInternalOnly() {
        return this != QUANTUM_STRUCTURE;
    }

    @Override
    public ResourceLocation id() {
        return this.id;
    }

    @Override
    public CraftingUnitVisualDefinition getVisualDefinition() {
        return this.visualDefinition;
    }

    @Override
    public ResourceLocation getFamilyId() {
        return FAMILY_ID;
    }

    @Override
    public Item getItemFromType() {
        return Item.getItemFromBlock(ModContent.getQuantumBlock(this));
    }
}
