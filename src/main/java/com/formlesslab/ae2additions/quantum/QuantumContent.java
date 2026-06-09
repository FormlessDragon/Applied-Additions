package com.formlesslab.ae2additions.quantum;

import com.formlesslab.ae2additions.Reference;
import com.formlesslab.ae2additions.quantum.block.AAECraftingUnitBlock;
import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

public final class QuantumContent {
    private static final Map<AAECraftingUnitType, AAECraftingUnitBlock> BLOCKS =
        new EnumMap<>(AAECraftingUnitType.class);

    static {
        for (AAECraftingUnitType type : AAECraftingUnitType.values()) {
            BLOCKS.put(type, new AAECraftingUnitBlock(type));
        }
    }

    private QuantumContent() {
    }

    public static AAECraftingUnitBlock getBlock(AAECraftingUnitType type) {
        return BLOCKS.get(type);
    }

    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        for (AAECraftingUnitType type : AAECraftingUnitType.values()) {
            event.getRegistry().register(setupBlock(getBlock(type), type));
        }
    }

    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (AAECraftingUnitType type : AAECraftingUnitType.values()) {
            event.getRegistry().register(setupBlockItem(getBlock(type), type));
        }
    }

    public static void registerTileEntities() {
        GameRegistry.registerTileEntity(AdvCraftingBlockEntity.class, id("quantum_core"));
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(Reference.MOD_ID, path);
    }

    private static AAECraftingUnitBlock setupBlock(AAECraftingUnitBlock block, AAECraftingUnitType type) {
        block.setRegistryName(id(type.getRegistryName()));
        block.setTranslationKey(Reference.MOD_ID + "." + type.getRegistryName());
        block.setHardness(2.2F);
        block.setResistance(10.0F);
        return block;
    }

    private static Item setupBlockItem(Block block, AAECraftingUnitType type) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(id(type.getRegistryName()));
        item.setTranslationKey(Reference.MOD_ID + "." + type.getRegistryName());
        return item;
    }
}
