package com.formlesslab.ae2additions.assembler.client.model;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraft.client.resources.IResourceManager;

public enum AssemblerGlassModel implements IModel, ICustomModelLoader {
    INSTANCE;

    public static final ResourceLocation MODEL_ID = new ResourceLocation("ae2additions", "block/assembler_matrix_glass");

    public static void register() {
        ModelLoaderRegistry.registerLoader(INSTANCE);
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return MODEL_ID.equals(modelLocation);
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) {
        return INSTANCE;
    }

    @Override
    public net.minecraft.client.renderer.block.model.IBakedModel bake(
        net.minecraftforge.common.model.IModelState state,
        net.minecraft.client.renderer.vertex.VertexFormat format,
        java.util.function.Function<ResourceLocation, net.minecraft.client.renderer.texture.TextureAtlasSprite> bakedTextureGetter) {
        return new AssemblerGlassBakedModel(format, bakedTextureGetter);
    }

    @Override
    public java.util.Collection<ResourceLocation> getTextures() {
        return java.util.Arrays.asList(
            AssemblerGlassBakedModel.SIDE,
            AssemblerGlassBakedModel.FACE_A,
            AssemblerGlassBakedModel.FACE_B,
            AssemblerGlassBakedModel.FACE_C,
            AssemblerGlassBakedModel.FULL);
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
    }
}
