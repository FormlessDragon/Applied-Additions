package com.formlesslab.ae2additions.client.model;

import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraft.client.resources.IResourceManager;
import net.minecraftforge.common.model.IModelState;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public enum AssemblerGlassModel implements IModel, ICustomModelLoader {
    INSTANCE;

    public static final ResourceLocation MODEL_ID = new ResourceLocation("ae2additions", "block/assembler_matrix_glass");

    public static void register() {
        ModelLoaderRegistry.registerLoader(INSTANCE);
    }

    @Override
    public boolean accepts(@NonNull ResourceLocation modelLocation) {
        return MODEL_ID.equals(modelLocation);
    }

    @Override
    public IModel loadModel(@NonNull ResourceLocation modelLocation) {
        return INSTANCE;
    }

    @Override
    public IBakedModel bake(
        @NonNull IModelState state,
        @NonNull VertexFormat format,
        Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return new AssemblerGlassBakedModel(format, bakedTextureGetter);
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return Arrays.asList(
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
