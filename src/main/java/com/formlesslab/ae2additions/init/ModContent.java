package com.formlesslab.ae2additions.init;

import com.formlesslab.ae2additions.Reference;
import com.formlesslab.ae2additions.assembler.block.BlockAssemblerMatrixCrafter;
import com.formlesslab.ae2additions.assembler.block.BlockAssemblerMatrixFrame;
import com.formlesslab.ae2additions.assembler.block.BlockAssemblerMatrixGlass;
import com.formlesslab.ae2additions.assembler.block.BlockAssemblerMatrixPattern;
import com.formlesslab.ae2additions.assembler.block.BlockAssemblerMatrixSpeed;
import com.formlesslab.ae2additions.assembler.block.BlockAssemblerMatrixWall;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixCrafter;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixFrame;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixGlass;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixPattern;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixSpeed;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixWall;
import com.formlesslab.ae2additions.block.BlockWirelessConnector;
import com.formlesslab.ae2additions.block.BlockWirelessHub;
import com.formlesslab.ae2additions.item.ItemWirelessConnectorUpgrade;
import com.formlesslab.ae2additions.item.ItemWirelessTool;
import com.formlesslab.ae2additions.quantum.AAECraftingUnitType;
import com.formlesslab.ae2additions.quantum.QuantumContent;
import com.formlesslab.ae2additions.tile.TileWirelessConnector;
import com.formlesslab.ae2additions.tile.TileWirelessHub;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ModContent {
    public static final CreativeTabs CREATIVE_TAB = new CreativeTabs(Reference.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(WIRELESS_CONNECTOR);
        }
    };

    public static final BlockWirelessConnector WIRELESS_CONNECTOR = new BlockWirelessConnector();
    public static final BlockWirelessHub WIRELESS_HUB = new BlockWirelessHub();
    public static final ItemWirelessTool WIRELESS_TOOL = new ItemWirelessTool();
    public static final ItemWirelessConnectorUpgrade WIRELESS_CONNECTOR_UPGRADE = new ItemWirelessConnectorUpgrade();
    public static final BlockAssemblerMatrixFrame ASSEMBLER_MATRIX_FRAME = new BlockAssemblerMatrixFrame();
    public static final BlockAssemblerMatrixWall ASSEMBLER_MATRIX_WALL = new BlockAssemblerMatrixWall();
    public static final BlockAssemblerMatrixGlass ASSEMBLER_MATRIX_GLASS = new BlockAssemblerMatrixGlass();
    public static final BlockAssemblerMatrixPattern ASSEMBLER_MATRIX_PATTERN = new BlockAssemblerMatrixPattern();
    public static final BlockAssemblerMatrixCrafter ASSEMBLER_MATRIX_CRAFTER = new BlockAssemblerMatrixCrafter();
    public static final BlockAssemblerMatrixSpeed ASSEMBLER_MATRIX_SPEED = new BlockAssemblerMatrixSpeed();

    private static final List<Block> BLOCKS = new ArrayList<>();
    private static final List<Item> ITEMS = new ArrayList<>();
    private static final List<ModelEntry> MODELS = new ArrayList<>();
    private static final List<TileEntityEntry> TILE_ENTITIES = new ArrayList<>();

    static {
        registerBlock(WIRELESS_CONNECTOR, "wireless_connect");
        registerBlock(WIRELESS_HUB, "wireless_hub");
        registerItem(WIRELESS_TOOL, "wireless_tool");
        registerItem(WIRELESS_CONNECTOR_UPGRADE, "wireless_connector_upgrade");
        for (AAECraftingUnitType type : AAECraftingUnitType.values()) {
            registerBlock(QuantumContent.getBlock(type), type.getRegistryName());
        }
        registerBlock(ASSEMBLER_MATRIX_FRAME, "assembler_matrix_frame");
        registerBlock(ASSEMBLER_MATRIX_WALL, "assembler_matrix_wall");
        registerBlock(ASSEMBLER_MATRIX_GLASS, "assembler_matrix_glass");
        registerBlock(ASSEMBLER_MATRIX_PATTERN, "assembler_matrix_pattern");
        registerBlock(ASSEMBLER_MATRIX_CRAFTER, "assembler_matrix_crafter");
        registerBlock(ASSEMBLER_MATRIX_SPEED, "assembler_matrix_speed");
        registerTileEntity(TileWirelessConnector.class, "wireless_connect");
        registerTileEntity(TileWirelessHub.class, "wireless_hub");
        registerTileEntity(com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity.class, "quantum_crafting_unit");
        registerTileEntity(TileAssemblerMatrixFrame.class, "assembler_matrix_frame");
        registerTileEntity(TileAssemblerMatrixWall.class, "assembler_matrix_wall");
        registerTileEntity(TileAssemblerMatrixGlass.class, "assembler_matrix_glass");
        registerTileEntity(TileAssemblerMatrixPattern.class, "assembler_matrix_pattern");
        registerTileEntity(TileAssemblerMatrixCrafter.class, "assembler_matrix_crafter");
        registerTileEntity(TileAssemblerMatrixSpeed.class, "assembler_matrix_speed");
    }

    private ModContent() {
    }

    public static void registerTileEntities() {
        for (TileEntityEntry entry : TILE_ENTITIES) {
            GameRegistry.registerTileEntity(entry.tileClass, id(entry.name));
        }
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(Reference.MOD_ID, path);
    }

    @SubscribeEvent
    public static void registerBlocks(RegistryEvent.Register<Block> event) {
        event.getRegistry().registerAll(BLOCKS.toArray(new Block[0]));
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(ITEMS.toArray(new Item[0]));
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void registerModels(ModelRegistryEvent event) {
        for (ModelEntry entry : MODELS) {
            registerModel(entry.item, entry.name);
        }
    }

    public static <T extends Block> T registerBlock(T block, String name) {
        T registered = setupBlock(block, name);
        BLOCKS.add(registered);
        registerBlockItem(registered, name);
        return registered;
    }

    public static Item registerBlockItem(Block block, String name) {
        Item item = setupBlockItem(block, name);
        ITEMS.add(item);
        MODELS.add(new ModelEntry(item, name));
        return item;
    }

    public static <T extends Item> T registerItem(T item, String name) {
        T registered = setupItem(item, name);
        ITEMS.add(registered);
        MODELS.add(new ModelEntry(registered, name));
        return registered;
    }

    public static void registerTileEntity(Class<? extends net.minecraft.tileentity.TileEntity> tileClass, String name) {
        TILE_ENTITIES.add(new TileEntityEntry(tileClass, name));
    }

    private static <T extends Block> T setupBlock(T block, String name) {
        block.setRegistryName(id(name));
        block.setTranslationKey(Reference.MOD_ID + "." + name);
        block.setCreativeTab(CREATIVE_TAB);
        block.setHardness(2.2F);
        block.setResistance(10.0F);
        return block;
    }

    private static Item setupBlockItem(Block block, String name) {
        ItemBlock item = new ItemBlock(block);
        item.setRegistryName(id(name));
        item.setTranslationKey(Reference.MOD_ID + "." + name);
        return item;
    }

    private static <T extends Item> T setupItem(T item, String name) {
        item.setRegistryName(id(name));
        item.setTranslationKey(Reference.MOD_ID + "." + name);
        item.setCreativeTab(CREATIVE_TAB);
        return item;
    }

    @SideOnly(Side.CLIENT)
    private static void registerModel(Item item, String name) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
            new ModelResourceLocation(id(name), "inventory"));
    }

    private static final class ModelEntry {
        private final Item item;
        private final String name;

        private ModelEntry(Item item, String name) {
            this.item = item;
            this.name = name;
        }
    }

    private static final class TileEntityEntry {
        private final Class<? extends net.minecraft.tileentity.TileEntity> tileClass;
        private final String name;

        private TileEntityEntry(Class<? extends net.minecraft.tileentity.TileEntity> tileClass, String name) {
            this.tileClass = tileClass;
            this.name = name;
        }
    }
}
