package com.formlesslab.ae2additions.init;

import ae2.core.definitions.AEItems;
import ae2.recipes.AERecipeTypes;
import ae2.recipes.handlers.InscriberProcessType;
import ae2.recipes.handlers.InscriberRecipe;
import com.formlesslab.ae2additions.Reference;
import com.formlesslab.ae2additions.api.AAECraftingUnitType;
import com.formlesslab.ae2additions.block.assembler.*;
import com.formlesslab.ae2additions.block.material.*;
import com.formlesslab.ae2additions.block.reaction.BlockReactionChamber;
import com.formlesslab.ae2additions.block.wireless.BlockWirelessConnector;
import com.formlesslab.ae2additions.block.wireless.BlockWirelessHub;
import com.formlesslab.ae2additions.fluid.ModFluids;
import com.formlesslab.ae2additions.item.ItemWirelessConnectorUpgrade;
import com.formlesslab.ae2additions.item.ItemWirelessTool;
import com.formlesslab.ae2additions.tile.*;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid = Reference.MOD_ID)
public final class ModContent {
    public static final Item QUANTUM_INFUSED_DUST;
    public static final CreativeTabs CREATIVE_TAB;
    public static final BlockWirelessConnector WIRELESS_CONNECTOR;
    public static final BlockWirelessHub WIRELESS_HUB;
    public static final ItemWirelessTool WIRELESS_TOOL;
    public static final ItemWirelessConnectorUpgrade WIRELESS_CONNECTOR_UPGRADE;
    public static final Item SHATTERED_SINGULARITY;
    public static final Item QUANTUM_PROCESSOR_PRESS;
    public static final Item PRINTED_QUANTUM_PROCESSOR;
    public static final Item QUANTUM_ALLOY;
    public static final Item QUANTUM_ALLOY_PLATE;
    public static final Item QUANTUM_PROCESSOR;
    public static final Item QUANTUM_STORAGE_COMPONENT;
    public static final BlockReactionChamber REACTION_CHAMBER;
    public static final ModFluids.QuantumInfusionBlock QUANTUM_INFUSION_BLOCK;
    public static final BlockQuantumAlloyBlock QUANTUM_ALLOY_BLOCK;
    public static final BlockQuantumAlloyWall QUANTUM_ALLOY_WALL;
    public static final BlockQuantumAlloySlab QUANTUM_ALLOY_SLAB;
    public static final BlockQuantumAlloyStairs QUANTUM_ALLOY_STAIRS;
    public static final BlockQuantumAlloyDoubleSlab QUANTUM_ALLOY_DOUBLE_SLAB;
    public static final BlockAssemblerMatrixFrame ASSEMBLER_MATRIX_FRAME;
    public static final BlockAssemblerMatrixWall ASSEMBLER_MATRIX_WALL;
    public static final BlockAssemblerMatrixGlass ASSEMBLER_MATRIX_GLASS;
    public static final BlockAssemblerMatrixPattern ASSEMBLER_MATRIX_PATTERN;
    public static final BlockAssemblerMatrixCrafter ASSEMBLER_MATRIX_CRAFTER;
    public static final BlockAssemblerMatrixSpeed ASSEMBLER_MATRIX_SPEED;

    private static final List<Block> BLOCKS;
    private static final List<Item> ITEMS;
    private static final List<ModelEntry> MODELS;
    private static final List<TileEntityEntry> TILE_ENTITIES;


    static {
        ModFluids.init();
        QUANTUM_INFUSED_DUST = new Item();
        CREATIVE_TAB = new CreativeTabs("ae2additions") {
            public ItemStack createIcon() {
                return new ItemStack(ModContent.WIRELESS_CONNECTOR);
            }
        };
        WIRELESS_CONNECTOR = new BlockWirelessConnector();
        WIRELESS_HUB = new BlockWirelessHub();
        WIRELESS_TOOL = new ItemWirelessTool();
        WIRELESS_CONNECTOR_UPGRADE = new ItemWirelessConnectorUpgrade();
        SHATTERED_SINGULARITY = new Item();
        QUANTUM_PROCESSOR_PRESS = new Item();
        PRINTED_QUANTUM_PROCESSOR = new Item();
        QUANTUM_ALLOY = new Item();
        QUANTUM_ALLOY_PLATE = new Item();
        QUANTUM_PROCESSOR = new Item();
        QUANTUM_STORAGE_COMPONENT = new Item();
        REACTION_CHAMBER = new BlockReactionChamber();
        QUANTUM_INFUSION_BLOCK = new ModFluids.QuantumInfusionBlock();
        QUANTUM_ALLOY_BLOCK = new BlockQuantumAlloyBlock();
        QUANTUM_ALLOY_WALL = new BlockQuantumAlloyWall(QUANTUM_ALLOY_BLOCK);
        QUANTUM_ALLOY_SLAB = new BlockQuantumAlloySlab();
        QUANTUM_ALLOY_STAIRS = new BlockQuantumAlloyStairs(QUANTUM_ALLOY_BLOCK.getDefaultState());
        QUANTUM_ALLOY_DOUBLE_SLAB = new BlockQuantumAlloyDoubleSlab(QUANTUM_ALLOY_SLAB);
        ASSEMBLER_MATRIX_FRAME = new BlockAssemblerMatrixFrame();
        ASSEMBLER_MATRIX_WALL = new BlockAssemblerMatrixWall();
        ASSEMBLER_MATRIX_GLASS = new BlockAssemblerMatrixGlass();
        ASSEMBLER_MATRIX_PATTERN = new BlockAssemblerMatrixPattern();
        ASSEMBLER_MATRIX_CRAFTER = new BlockAssemblerMatrixCrafter();
        ASSEMBLER_MATRIX_SPEED = new BlockAssemblerMatrixSpeed();
        BLOCKS = new ArrayList<>();
        ITEMS = new ArrayList<>();
        MODELS = new ArrayList<>();
        TILE_ENTITIES = new ArrayList<>();
        registerBlock(WIRELESS_CONNECTOR, "wireless_connect");
        registerBlock(WIRELESS_HUB, "wireless_hub");
        registerItem(WIRELESS_TOOL, "wireless_tool");
        registerItem(WIRELESS_CONNECTOR_UPGRADE, "wireless_connector_upgrade");
        registerItem(QUANTUM_INFUSED_DUST, "quantum_infused_dust");
        registerItem(SHATTERED_SINGULARITY, "shattered_singularity");
        registerItem(QUANTUM_PROCESSOR_PRESS, "quantum_processor_press");
        registerItem(PRINTED_QUANTUM_PROCESSOR, "printed_quantum_processor");
        registerItem(QUANTUM_ALLOY, "quantum_alloy");
        registerItem(QUANTUM_ALLOY_PLATE, "quantum_alloy_plate");
        registerItem(QUANTUM_PROCESSOR, "quantum_processor");
        registerItem(QUANTUM_STORAGE_COMPONENT, "quantum_storage_component");
        registerBlock(REACTION_CHAMBER, "reaction_chamber");
        registerBlockNoItem(QUANTUM_INFUSION_BLOCK, "quantum_infusion_block");
        registerBlock(QUANTUM_ALLOY_BLOCK, "quantum_alloy_block");
        registerBlock(QUANTUM_ALLOY_STAIRS, "quantum_alloy_stairs");
        registerBlock(QUANTUM_ALLOY_WALL, "quantum_alloy_wall");
        registerBlock(QUANTUM_ALLOY_SLAB, "quantum_alloy_slab");
        setupBlock(QUANTUM_ALLOY_DOUBLE_SLAB, "quantum_alloy_double_slab");
        BLOCKS.add(QUANTUM_ALLOY_DOUBLE_SLAB);

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
        registerTileEntity(TileReactionChamber.class, "reaction_chamber");
        registerTileEntity(TileAdvCraftingBlock.class, "quantum_crafting_unit");
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
        registerFluidModel(QUANTUM_INFUSION_BLOCK, "quantum_infusion_block");
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

    public static <T extends Block> T registerBlockNoItem(T block, String name) {
        T registered = setupBlock(block, name);
        BLOCKS.add(registered);
        return registered;
    }

    public static <T extends Item> T registerItem(T item, String name) {
        T registered = setupItem(item, name);
        ITEMS.add(registered);
        MODELS.add(new ModelEntry(registered, name));
        return registered;
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileClass, String name) {
        TILE_ENTITIES.add(new TileEntityEntry(tileClass, name));
    }

    private static <T extends Block> T setupBlock(T block, String name) {
        block.setRegistryName(id(name));
        block.setTranslationKey(Reference.MOD_ID + "." + name);
        block.setCreativeTab(CREATIVE_TAB);
        if (!(block instanceof BlockQuantumAlloyBlock)
                && !(block instanceof BlockQuantumAlloyStairs)
                && !(block instanceof BlockQuantumAlloyWall)
                && !(block instanceof BlockQuantumAlloySlab)
                && !(block instanceof BlockQuantumAlloyDoubleSlab)) {
            block.setHardness(2.2F);
            block.setResistance(10.0F);
        }
        return block;
    }

    private static Item setupBlockItem(Block block, String name) {
        ItemBlock item;
        if (block == QUANTUM_ALLOY_SLAB) {
            item = new ItemSlab(block, QUANTUM_ALLOY_SLAB, QUANTUM_ALLOY_DOUBLE_SLAB);
        } else {
            item = new ItemBlock(block);
        }
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

    @SideOnly(Side.CLIENT)
    private static void registerFluidModel(Block block, String name) {
        ModelResourceLocation location = new ModelResourceLocation(id(name), "normal");
        ModelLoader.setCustomStateMapper(block, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return location;
            }
        });
    }

    public static void registerOreDictionary() {
        OreDictionary.registerOre("dustShatteredSingularity", QUANTUM_INFUSED_DUST);
        OreDictionary.registerOre("ingotQuantumAlloy", QUANTUM_ALLOY);
        OreDictionary.registerOre("plateQuantumAlloy", QUANTUM_ALLOY_PLATE);
        OreDictionary.registerOre("blockQuantumAlloy", QUANTUM_ALLOY_BLOCK);
    }

    public static void registerMachineRecipes() {
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
                Ingredient.fromStacks(new ItemStack(SHATTERED_SINGULARITY)),
                new ItemStack(QUANTUM_INFUSED_DUST),
                Ingredient.EMPTY,
                Ingredient.EMPTY,
                InscriberProcessType.PRESS));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
                Ingredient.fromStacks(new ItemStack(SHATTERED_SINGULARITY)),
                new ItemStack(QUANTUM_PROCESSOR_PRESS),
                Ingredient.fromStacks(AEItems.ENGINEERING_PROCESSOR_PRESS.stack()),
                Ingredient.fromStacks(AEItems.LOGIC_PROCESSOR_PRESS.stack()),
                InscriberProcessType.PRESS));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
                Ingredient.fromStacks(new ItemStack(Blocks.IRON_BLOCK)),
                new ItemStack(QUANTUM_PROCESSOR_PRESS),
                Ingredient.fromStacks(new ItemStack(QUANTUM_PROCESSOR_PRESS)),
                Ingredient.EMPTY,
                InscriberProcessType.INSCRIBE));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
                Ingredient.fromStacks(new ItemStack(QUANTUM_ALLOY)),
                new ItemStack(PRINTED_QUANTUM_PROCESSOR),
                Ingredient.fromStacks(new ItemStack(QUANTUM_PROCESSOR_PRESS)),
                Ingredient.EMPTY,
                InscriberProcessType.INSCRIBE));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(
                Ingredient.fromStacks(new ItemStack(Items.REDSTONE)),
                new ItemStack(QUANTUM_PROCESSOR),
                Ingredient.fromStacks(new ItemStack(PRINTED_QUANTUM_PROCESSOR)),
                Ingredient.fromStacks(AEItems.SILICON_PRINT.stack()),
                InscriberProcessType.PRESS));
    }

    private record ModelEntry(Item item, String name) {
    }

    private record TileEntityEntry(Class<? extends TileEntity> tileClass, String name) {
    }
}
