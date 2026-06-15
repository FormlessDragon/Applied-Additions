package com.formlesslab.ae2additions.quantum.client;

import com.formlesslab.ae2additions.Reference;
import com.formlesslab.ae2additions.quantum.AAECraftingUnitType;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;

public class AAECraftingUnitModelProvider {
    protected static final ResourceLocation STRUCTURE_FORMED_FACE = texture("quantum_structure_formed_face");
    protected static final ResourceLocation STRUCTURE_FORMED_SIDES = texture("quantum_structure_formed_sides");
    protected static final ResourceLocation STRUCTURE_ANIMATION_SIDES = texture("quantum_structure_powered_sides");

    protected static final ResourceLocation INTERNAL_FORMED_FACE = texture("quantum_internal_formed_face");
    protected static final ResourceLocation INTERNAL_FORMED_SIDES = texture("quantum_internal_formed_sides");
    protected static final ResourceLocation INTERNAL_ANIMATION_SIDES = texture("quantum_internal_powered_sides");

    protected static final ResourceLocation INTERNAL_ANIMATION_FACE = texture("quantum_internal_powered_animation");
    protected static final ResourceLocation INTERNAL_ANIMATION_FACE_TB = texture("quantum_internal_powered_animation_tb");

    private final AAECraftingUnitType type;

    public AAECraftingUnitModelProvider(AAECraftingUnitType type) {
        this.type = type;
    }

    public IBakedModel bake(VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> textureGetter) {
        if (this.type == AAECraftingUnitType.QUANTUM_STRUCTURE) {
            return QuantumComputerBakedModel.structure(
                format,
                textureGetter.apply(STRUCTURE_FORMED_FACE),
                textureGetter.apply(STRUCTURE_FORMED_SIDES),
                textureGetter.apply(STRUCTURE_ANIMATION_SIDES));
        }

        return QuantumComputerBakedModel.internal(
            format,
            textureGetter.apply(INTERNAL_FORMED_FACE),
            textureGetter.apply(INTERNAL_FORMED_SIDES),
            textureGetter.apply(INTERNAL_ANIMATION_SIDES),
            textureGetter.apply(INTERNAL_ANIMATION_FACE),
            textureGetter.apply(INTERNAL_ANIMATION_FACE_TB),
            textureGetter.apply(INTERNAL_ANIMATION_FACE_TB));
    }

    public static Collection<ResourceLocation> getTextures() {
        return Arrays.asList(
            STRUCTURE_FORMED_FACE,
            STRUCTURE_FORMED_SIDES,
            STRUCTURE_ANIMATION_SIDES,
            INTERNAL_FORMED_FACE,
            INTERNAL_FORMED_SIDES,
            INTERNAL_ANIMATION_SIDES,
            INTERNAL_ANIMATION_FACE,
            INTERNAL_ANIMATION_FACE_TB);
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(Reference.MOD_ID, "block/quantum_crafting/" + name);
    }
}
