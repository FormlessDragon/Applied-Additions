package com.formlesslab.ae2additions.init;

import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.upgrades.Upgrades;
import ae2.core.definitions.AEItems;
import ae2.recipes.AERecipeTypes;
import ae2.recipes.handlers.InscriberProcessType;
import ae2.recipes.handlers.InscriberRecipe;
import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.api.AAECraftingUnitType;
import com.formlesslab.ae2additions.block.assembler.*;
import com.formlesslab.ae2additions.block.material.*;
import com.formlesslab.ae2additions.block.quantum.BlockAAECraftingUnit;
import com.formlesslab.ae2additions.block.reaction.BlockReactionChamber;
import com.formlesslab.ae2additions.block.wireless.BlockWirelessConnector;
import com.formlesslab.ae2additions.block.wireless.BlockWirelessHub;
import com.formlesslab.ae2additions.cell.InfinityCellContents;
import com.formlesslab.ae2additions.fluid.QuantumInfusionFluid;
import com.formlesslab.ae2additions.item.ItemInfinityCell;
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
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.oredict.OreDictionary;

import java.util.*;
import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID)
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
    public static final ItemInfinityCell INFINITY_CELL;
    public static final ItemStack INFINITY_COBBLESTONE_CELL;
    public static final ItemStack INFINITY_WATER_CELL;
    public static final BlockReactionChamber REACTION_CHAMBER;
    public static final QuantumInfusionFluid.QuantumInfusionBlock QUANTUM_INFUSION_BLOCK;
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
    private static final Map<String, ItemStack> INFINITY_CELL_VARIANTS;
    private static final Map<AAECraftingUnitType, BlockAAECraftingUnit> QUANTUM_BLOCKS = new EnumMap<>(AAECraftingUnitType.class);

    static {
        QuantumInfusionFluid.init();
        QUANTUM_INFUSED_DUST = new Item();
        CREATIVE_TAB = new CreativeTabs("ae2additions") {
            public ItemStack createIcon() {
                return new ItemStack(ModContent.WIRELESS_TOOL);
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
        INFINITY_CELL = new ItemInfinityCell();
        REACTION_CHAMBER = new BlockReactionChamber();
        QUANTUM_INFUSION_BLOCK = new QuantumInfusionFluid.QuantumInfusionBlock();
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
        INFINITY_CELL_VARIANTS = new LinkedHashMap<>();

        registerItem(WIRELESS_TOOL, "wireless_tool");
        registerItem(WIRELESS_CONNECTOR_UPGRADE, "wireless_connector_upgrade");
        registerItem(QUANTUM_PROCESSOR_PRESS, "quantum_processor_press");
        registerItem(PRINTED_QUANTUM_PROCESSOR, "printed_quantum_processor");
        registerItem(QUANTUM_PROCESSOR, "quantum_processor");
        registerItem(QUANTUM_STORAGE_COMPONENT, "quantum_storage_component");
        registerItem(INFINITY_CELL, "infinity_cell");
        registerItem(QUANTUM_INFUSED_DUST, "quantum_infused_dust");
        registerItem(SHATTERED_SINGULARITY, "shattered_singularity");
        registerItem(QUANTUM_ALLOY, "quantum_alloy");
        registerItem(QUANTUM_ALLOY_PLATE, "quantum_alloy_plate");
        registerBlockNoItem(QUANTUM_INFUSION_BLOCK, "quantum_infusion_block");
        registerBlock(QUANTUM_ALLOY_SLAB, "quantum_alloy_slab");
        setupBlock(QUANTUM_ALLOY_DOUBLE_SLAB, "quantum_alloy_double_slab");
        registerBlock(QUANTUM_ALLOY_STAIRS, "quantum_alloy_stairs");
        registerBlock(QUANTUM_ALLOY_WALL, "quantum_alloy_wall");
        registerBlock(QUANTUM_ALLOY_BLOCK, "quantum_alloy_block");

        BLOCKS.add(QUANTUM_ALLOY_DOUBLE_SLAB);

        registerBlock(REACTION_CHAMBER, "reaction_chamber");
        registerBlock(WIRELESS_CONNECTOR, "wireless_connect");
        registerBlock(WIRELESS_HUB, "wireless_hub");

        for (AAECraftingUnitType type : AAECraftingUnitType.values()) {
            BlockAAECraftingUnit blockAAECraftingUnit = new BlockAAECraftingUnit(type);
            QUANTUM_BLOCKS.put(type, blockAAECraftingUnit);
            registerBlock(blockAAECraftingUnit, type.getRegistryName());
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

        INFINITY_COBBLESTONE_CELL = registerBuiltinInfinityCell("infinity_cobblestone", Collections.singletonList(Objects.requireNonNull(AEItemKey.of(new ItemStack(Blocks.COBBLESTONE)), "Unable to create cobblestone key")));
        INFINITY_WATER_CELL = registerBuiltinInfinityCell("infinity_water", Collections.singletonList(Objects.requireNonNull(AEFluidKey.of(Objects.requireNonNull(FluidRegistry.WATER, "minecraft:water is unavailable")), "Unable to create water key")));
    }

    public static void registerUpgrades() {
        Upgrades.add(AEItems.ENERGY_CARD.item(), Item.getItemFromBlock(ModContent.WIRELESS_CONNECTOR), 4);
        Upgrades.add(AEItems.ENERGY_CARD.item(), Item.getItemFromBlock(ModContent.WIRELESS_HUB), 4);
        Upgrades.add(AEItems.SPEED_CARD.item(), Item.getItemFromBlock(ModContent.REACTION_CHAMBER), 4);
        Upgrades.add(AEItems.PARALLEL_CARD.item(), Item.getItemFromBlock(ModContent.REACTION_CHAMBER), 3);
    }

    public static void registerOreDictionary() {
        OreDictionary.registerOre("dustShatteredSingularity", QUANTUM_INFUSED_DUST);
        OreDictionary.registerOre("ingotQuantumAlloy", QUANTUM_ALLOY);
        OreDictionary.registerOre("plateQuantumAlloy", QUANTUM_ALLOY_PLATE);
        OreDictionary.registerOre("blockQuantumAlloy", QUANTUM_ALLOY_BLOCK);
    }

    public static void registerMachineRecipes() {
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(Ingredient.fromStacks(new ItemStack(SHATTERED_SINGULARITY)), new ItemStack(QUANTUM_INFUSED_DUST), Ingredient.EMPTY, Ingredient.EMPTY, InscriberProcessType.PRESS));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(Ingredient.fromStacks(new ItemStack(SHATTERED_SINGULARITY)), new ItemStack(QUANTUM_PROCESSOR_PRESS), Ingredient.fromStacks(AEItems.ENGINEERING_PROCESSOR_PRESS.stack()), Ingredient.fromStacks(AEItems.LOGIC_PROCESSOR_PRESS.stack()), InscriberProcessType.PRESS));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(Ingredient.fromStacks(new ItemStack(Blocks.IRON_BLOCK)), new ItemStack(QUANTUM_PROCESSOR_PRESS), Ingredient.fromStacks(new ItemStack(QUANTUM_PROCESSOR_PRESS)), Ingredient.EMPTY, InscriberProcessType.INSCRIBE));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(Ingredient.fromStacks(new ItemStack(QUANTUM_ALLOY)), new ItemStack(PRINTED_QUANTUM_PROCESSOR), Ingredient.fromStacks(new ItemStack(QUANTUM_PROCESSOR_PRESS)), Ingredient.EMPTY, InscriberProcessType.INSCRIBE));
        AERecipeTypes.INSCRIBER.register(new InscriberRecipe(Ingredient.fromStacks(new ItemStack(Items.REDSTONE)), new ItemStack(QUANTUM_PROCESSOR), Ingredient.fromStacks(new ItemStack(PRINTED_QUANTUM_PROCESSOR)), Ingredient.fromStacks(AEItems.SILICON_PRINT.stack()), InscriberProcessType.PRESS));
    }

    public static BlockAAECraftingUnit getQuantumBlock(AAECraftingUnitType type) {
        return QUANTUM_BLOCKS.get(type);
    }

    public static void registerTileEntities() {
        for (TileEntityEntry entry : TILE_ENTITIES) {
            GameRegistry.registerTileEntity(entry.tileClass, id(entry.name));
        }
    }

    /**
     * Creates and records an NBT-backed variant of the single infinity-cell item.
     * This method never registers a new Forge item.
     */
    public static synchronized ItemStack registerInfinityCell(String name, Collection<? extends Supplier<? extends AEKey>> keySuppliers, String displayName) {
        String variantId = validateVariantId(name);
        Objects.requireNonNull(keySuppliers, "keySuppliers");

        List<AEKey> keys = new ArrayList<>(keySuppliers.size());
        for (Supplier<? extends AEKey> supplier : keySuppliers) {
            keys.add(Objects.requireNonNull(Objects.requireNonNull(supplier, "keySupplier").get(), "Infinity cell key supplier returned null"));
        }
        ItemStack candidate = InfinityCellContents.createStack(INFINITY_CELL, keys, variantId, displayName);

        ItemStack existing = INFINITY_CELL_VARIANTS.get(variantId);
        if (existing != null) {
            if (ItemStack.areItemStacksEqual(existing, candidate)) {
                return existing.copy();
            }
            throw new IllegalArgumentException("An infinity-cell variant is already registered as " + variantId);
        }
        INFINITY_CELL_VARIANTS.put(variantId, candidate.copy());
        return candidate;
    }

    /**
     * Returns defensive copies for the creative tab and JEI integration.
     */
    public static synchronized List<ItemStack> getInfinityCellVariants() {
        List<ItemStack> result = new ArrayList<>(INFINITY_CELL_VARIANTS.size());
        for (ItemStack stack : INFINITY_CELL_VARIANTS.values()) {
            result.add(stack.copy());
        }
        return Collections.unmodifiableList(result);
    }

    private static ItemStack registerBuiltinInfinityCell(String name, Collection<? extends AEKey> keys) {
        ItemStack stack = InfinityCellContents.createStack(INFINITY_CELL, keys, name, null);
        INFINITY_CELL_VARIANTS.put(name, stack.copy());
        return stack;
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(Tags.MOD_ID, path);
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
        ModelLoader.setCustomStateMapper(QUANTUM_INFUSION_BLOCK, new StateMapperBase() {
            @Override
            protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
                return new ModelResourceLocation(id("quantum_infusion_block"), "normal");
            }
        });
        for (ModelEntry entry : MODELS) {
            ModelLoader.setCustomModelResourceLocation(entry.item, 0, new ModelResourceLocation(id(entry.modelName), "inventory"));
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
        return registerItem(item, id(name), name);
    }

    private static <T extends Item> T registerItem(T item, ResourceLocation registryName, String modelName) {
        T registered = setupItem(item, registryName);
        ITEMS.add(registered);
        MODELS.add(new ModelEntry(registered, modelName));
        return registered;
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileClass, String name) {
        TILE_ENTITIES.add(new TileEntityEntry(tileClass, name));
    }

    private static <T extends Block> T setupBlock(T block, String name) {
        block.setRegistryName(id(name));
        block.setTranslationKey(Tags.MOD_ID + "." + name);
        block.setCreativeTab(CREATIVE_TAB);
        if (!(block instanceof BlockQuantumAlloyBlock) && !(block instanceof BlockQuantumAlloyStairs) && !(block instanceof BlockQuantumAlloyWall) && !(block instanceof BlockQuantumAlloySlab) && !(block instanceof BlockQuantumAlloyDoubleSlab)) {
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
        item.setTranslationKey(Tags.MOD_ID + "." + name);
        return item;
    }

    private static <T extends Item> T setupItem(T item, ResourceLocation registryName) {
        item.setRegistryName(registryName);
        item.setTranslationKey(registryName.getNamespace() + "." + registryName.getPath());
        item.setCreativeTab(CREATIVE_TAB);
        return item;
    }

    private static String validateVariantId(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Infinity-cell variant name cannot be empty");
        }
        return name.trim();
    }

    private record ModelEntry(Item item, String modelName) {
    }

    private record TileEntityEntry(Class<? extends TileEntity> tileClass, String name) {
    }
}
