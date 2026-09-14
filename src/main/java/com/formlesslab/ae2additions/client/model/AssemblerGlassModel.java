package com.formlesslab.ae2additions.client.model;

import com.formlesslab.ae2additions.Tags;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import org.jspecify.annotations.NonNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

public enum AssemblerGlassModel implements IModel, ICustomModelLoader {
    INSTANCE;

    /**
     * Path this loader claims, as it arrives in {@link #accepts}: the blockstate writes
     * "ae2additions:assembler_matrix_glass_connected", vanilla's {@code Variant$Deserializer} prepends "block/" to
     * blockstate variant model references, and {@code ModelLoaderRegistry.getActualLocation} then prepends
     * "models/". {@link #accepts} receives "ae2additions:models/block/assembler_matrix_glass_connected", whose path
     * matches this constant after the "models/" prefix is stripped.
     * <p>
     * The dedicated "_connected" suffix is what keeps this loader from colliding with the vanilla json at
     * "models/block/assembler_matrix_glass.json": that json backs the item model (via the item json's parent
     * reference), and both references arrive here as plain {@link ResourceLocation}s after the same "block/" +
     * "models/" prefixing, so they cannot be told apart by location alone. Only the blockstate points at the
     * connected variant, so only the blockstate gets the connected-glass geometry.
     */
    public static final String MODEL_PATH = "block/assembler_matrix_glass_connected";

    /**
     * Forge hands non-builtin model references to {@link #accepts} with a "models/" prefix preprended by
     * {@code ModelLoaderRegistry.getActualLocation}. A reference written as "ae2additions:assembler_matrix_glass"
     * therefore arrives here as "ae2additions:models/assembler_matrix_glass".
     */
    private static final String MODELS_PREFIX = "models/";

    public static void register() {
        ModelLoaderRegistry.registerLoader(INSTANCE);
    }

    /**
     * Strips only the "models/" prefix that Forge adds in {@code ModelLoaderRegistry.getActualLocation} for plain
     * model references. A "block/" prefix must not be stripped: parent chains such as
     * "ae2additions:block/assembler_matrix_glass" have to keep resolving through the vanilla json loader.
     */
    private static String normalizePath(String path) {
        while (path.startsWith(MODELS_PREFIX)) {
            path = path.substring(MODELS_PREFIX.length());
        }
        return path;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Item models arrive as a {@link ModelResourceLocation} ("ae2additions:assembler_matrix_glass#inventory"), which
     * {@code ModelLoaderRegistry.getActualLocation} returns unchanged, so they reach this method with a path that
     * never carries the "block/" prefix blockstate variants get. Rejecting them explicitly keeps the loader honest
     * even if the item json's parent reference were ever pointed back at the connected model.
     */
    @Override
    public boolean accepts(@NonNull ResourceLocation modelLocation) {
        if (modelLocation instanceof ModelResourceLocation) {
            return false;
        }
        return Tags.MOD_ID.equals(modelLocation.getNamespace())
            && MODEL_PATH.equals(normalizePath(modelLocation.getPath()));
    }

    @Override
    public IModel loadModel(@NonNull ResourceLocation modelLocation) {
        return INSTANCE;
    }

    @Override
    public IBakedModel bake(@NonNull IModelState state, @NonNull VertexFormat format, @NonNull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        return new AssemblerGlassBakedModel(format, bakedTextureGetter);
    }

    @Override
    public Collection<ResourceLocation> getTextures() {
        return Arrays.asList(AssemblerGlassBakedModel.SIDE, AssemblerGlassBakedModel.FACE_A, AssemblerGlassBakedModel.FACE_B, AssemblerGlassBakedModel.FACE_C, AssemblerGlassBakedModel.FULL);
    }

    @Override
    public void onResourceManagerReload(@NonNull IResourceManager resourceManager) {
    }
}
