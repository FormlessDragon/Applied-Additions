package com.formlesslab.ae2additions.client.render;

import ae2.core.registries.CraftingUnitClientRegistry;
import com.formlesslab.ae2additions.Tags;
import com.formlesslab.ae2additions.api.AAECraftingUnitType;
import com.formlesslab.ae2additions.client.model.AAECraftingUnitModelProvider;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.IRegistry;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = Tags.MOD_ID, value = Side.CLIENT)
public final class QuantumComputerModelOverride {
    public static final QuantumComputerModelOverride INSTANCE = new QuantumComputerModelOverride();

    private QuantumComputerModelOverride() {
    }

    private static void putFormedVariant(IRegistry<ModelResourceLocation, IBakedModel> registry, String path, boolean powered, IBakedModel model) {
        String variant = "formed=true,powered=" + powered;
        putModel(registry, "quantum_crafting/" + path + "_formed", variant, model);
        putModel(registry, "block/quantum_crafting/" + path + "_formed", variant, model);
        putModel(registry, path, variant, model);
    }

    private static void putModel(IRegistry<ModelResourceLocation, IBakedModel> registry, String path, String variant, IBakedModel model) {
        registry.putObject(new ModelResourceLocation(new ResourceLocation(Tags.MOD_ID, path), variant), model);
    }

    @SubscribeEvent
    public void onModelRegistry(ModelRegistryEvent event) {
        CraftingUnitClientRegistry.getInstance().registerModelProvider(AAECraftingUnitType.MODEL_PROVIDER_ID, AAECraftingUnitModelProvider.FORMED_MODEL_PROVIDER);
    }

    @SubscribeEvent
    public void onTextureStitch(TextureStitchEvent.Pre event) {
        for (ResourceLocation texture : AAECraftingUnitModelProvider.getTextures()) {
            event.getMap().registerSprite(texture);
        }
    }

    @SubscribeEvent
    public void onModelBake(ModelBakeEvent event) {
        IRegistry<ModelResourceLocation, IBakedModel> registry = event.getModelRegistry();

        for (AAECraftingUnitType type : AAECraftingUnitType.values()) {
            IBakedModel model = new AAECraftingUnitModelProvider(type).bake(DefaultVertexFormats.BLOCK, ModelLoader.defaultTextureGetter());
            putFormedVariant(registry, type.getRegistryName(), false, model);
            putFormedVariant(registry, type.getRegistryName(), true, model);
        }
    }
}
