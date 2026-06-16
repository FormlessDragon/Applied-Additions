package com.formlesslab.ae2additions.client.model;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;

import javax.annotation.Nullable;
import javax.vecmath.Vector3f;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class AssemblerGlassBakedModel implements IBakedModel {
    public static final ResourceLocation SIDE = new ResourceLocation("ae2additions", "block/assembler_matrix/glass/sides");
    public static final ResourceLocation FACE_A = new ResourceLocation("ae2additions", "block/assembler_matrix/glass/face_a");
    public static final ResourceLocation FACE_B = new ResourceLocation("ae2additions", "block/assembler_matrix/glass/face_b");
    public static final ResourceLocation FACE_C = new ResourceLocation("ae2additions", "block/assembler_matrix/glass/face_c");
    public static final ResourceLocation FULL = new ResourceLocation("ae2additions", "block/assembler_matrix/glass/full");
    public static final AssemblerGlassConnectProperty CONNECT_STATE = AssemblerGlassConnectProperty.INSTANCE;

    private static final int LU = 0;
    private static final int RU = 1;
    private static final int LD = 2;
    private static final int RD = 4;
    private static final Map<FaceCorner, List<Vector3f>> VERTEX_MAP = createVertexMap();
    private static final EnumMap<EnumFacing, List<Vector3f>> FACE_MAP = createFaceMap();

    private final VertexFormat format;
    private final TextureAtlasSprite glassSide;
    private final TextureAtlasSprite[] glassFaces;
    private final TextureAtlasSprite fullGlass;
    private final List<BakedQuad> itemQuads;

    public AssemblerGlassBakedModel(VertexFormat format, Function<ResourceLocation, TextureAtlasSprite> getter) {
        this.format = format;
        this.glassSide = getter.apply(SIDE);
        this.glassFaces = new TextureAtlasSprite[] {
            getter.apply(FACE_A),
            getter.apply(FACE_B),
            getter.apply(FACE_C)
        };
        this.fullGlass = getter.apply(FULL);
        this.itemQuads = createFullCube();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (side == null) {
            return state == null ? this.itemQuads : Collections.emptyList();
        }
        AssemblerGlassConnect connect = getConnect(state);
        if (connect == null) {
            return createFullFace(side);
        }
        ArrayList<BakedQuad> quads = new ArrayList<>(5);
        addCornerQuad(quads, side, connect.getIndex(side, LU), LU);
        addCornerQuad(quads, side, connect.getIndex(side, RU), RU);
        addCornerQuad(quads, side, connect.getIndex(side, LD), LD);
        addCornerQuad(quads, side, connect.getIndex(side, RD), RD);
        addFaceQuad(quads, side, connect.getFace(side));
        return quads;
    }

    @Override
    public boolean isAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean isBuiltInRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleTexture() {
        return this.glassSide;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    private AssemblerGlassConnect getConnect(IBlockState state) {
        if (state instanceof IExtendedBlockState) {
            return ((IExtendedBlockState) state).getValue(CONNECT_STATE);
        }
        return null;
    }

    private List<BakedQuad> createFullCube() {
        ArrayList<BakedQuad> quads = new ArrayList<>(6);
        for (EnumFacing face : EnumFacing.VALUES) {
            quads.addAll(createFullFace(face));
        }
        return quads;
    }

    private List<BakedQuad> createFullFace(EnumFacing face) {
        ArrayList<BakedQuad> quads = new ArrayList<>(1);
        addQuad(quads, face, this.fullGlass, FACE_MAP.get(face), 0, 0, 1, 1);
        return quads;
    }

    private void addFaceQuad(List<BakedQuad> quads, EnumFacing side, int index) {
        if (index < 0) {
            return;
        }
        addQuad(quads, side, this.glassFaces[index], FACE_MAP.get(side), 0, 0, 1, 1);
    }

    private void addCornerQuad(List<BakedQuad> quads, EnumFacing side, int index, int corner) {
        if (index < 0) {
            return;
        }
        List<Vector3f> vertices = VERTEX_MAP.get(new FaceCorner(side, corner));
        float u0 = getU0(index);
        float u1 = getU1(index);
        float v0 = getV0(index);
        float v1 = getV1(index);
        switch (corner) {
            case LU:
                addQuad(quads, side, this.glassSide, vertices, u0, v0, u1, v1);
                break;
            case RU:
                addQuad(quads, side, this.glassSide, vertices, u1, v0, u0, v1);
                break;
            case LD:
                addQuad(quads, side, this.glassSide, vertices, u0, v1, u1, v0);
                break;
            case RD:
                addQuad(quads, side, this.glassSide, vertices, u1, v1, u0, v0);
                break;
            default:
                break;
        }
    }

    private void addQuad(List<BakedQuad> quads, EnumFacing side, TextureAtlasSprite sprite,
                         List<Vector3f> vertices, float u0, float v0, float u1, float v1) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(this.format);
        builder.setTexture(sprite);
        builder.setQuadOrientation(side);
        builder.setApplyDiffuseLighting(true);
        Vec3i normal = side.getDirectionVec();
        putVertex(builder, sprite, normal, vertices.get(0), u0, v0);
        putVertex(builder, sprite, normal, vertices.get(1), u0, v1);
        putVertex(builder, sprite, normal, vertices.get(2), u1, v1);
        putVertex(builder, sprite, normal, vertices.get(3), u1, v0);
        quads.add(builder.build());
    }

    private void putVertex(UnpackedBakedQuad.Builder builder, TextureAtlasSprite sprite, Vec3i normal,
                           Vector3f pos, float u, float v) {
        for (int elementIndex = 0; elementIndex < this.format.getElementCount(); elementIndex++) {
            VertexFormatElement element = this.format.getElement(elementIndex);
            switch (element.getUsage()) {
                case POSITION:
                    builder.put(elementIndex, pos.x, pos.y, pos.z, 1.0F);
                    break;
                case COLOR:
                    builder.put(elementIndex, 1.0F, 1.0F, 1.0F, 1.0F);
                    break;
                case UV:
                    if (element.getIndex() == 0) {
                        builder.put(elementIndex, interpolate(sprite.getMinU(), sprite.getMaxU(), u),
                            interpolate(sprite.getMinV(), sprite.getMaxV(), v), 0.0F, 1.0F);
                    } else {
                        builder.put(elementIndex, 0.0F, 0.0F, 0.0F, 1.0F);
                    }
                    break;
                case NORMAL:
                    builder.put(elementIndex, normal.getX(), normal.getY(), normal.getZ(), 0.0F);
                    break;
                default:
                    builder.put(elementIndex);
                    break;
            }
        }
    }

    private static float interpolate(float start, float end, float value) {
        return start + (end - start) * value;
    }

    private static float getU0(int index) {
        return index == 1 || index == 3 ? 0.5F : 0.0F;
    }

    private static float getU1(int index) {
        return index == 1 || index == 3 ? 1.0F : 0.5F;
    }

    private static float getV0(int index) {
        return index == 2 || index == 3 ? 0.5F : 0.0F;
    }

    private static float getV1(int index) {
        return index == 2 || index == 3 ? 1.0F : 0.5F;
    }

    private static EnumMap<EnumFacing, List<Vector3f>> createFaceMap() {
        EnumMap<EnumFacing, List<Vector3f>> map = new EnumMap<>(EnumFacing.class);
        map.put(EnumFacing.EAST, list(v(1, 1, 1), v(1, 0, 1), v(1, 0, 0), v(1, 1, 0)));
        map.put(EnumFacing.WEST, list(v(0, 1, 0), v(0, 0, 0), v(0, 0, 1), v(0, 1, 1)));
        map.put(EnumFacing.UP, list(v(1, 1, 1), v(1, 1, 0), v(0, 1, 0), v(0, 1, 1)));
        map.put(EnumFacing.DOWN, list(v(0, 0, 1), v(0, 0, 0), v(1, 0, 0), v(1, 0, 1)));
        map.put(EnumFacing.SOUTH, list(v(0, 1, 1), v(0, 0, 1), v(1, 0, 1), v(1, 1, 1)));
        map.put(EnumFacing.NORTH, list(v(1, 1, 0), v(1, 0, 0), v(0, 0, 0), v(0, 1, 0)));
        return map;
    }

    private static Map<FaceCorner, List<Vector3f>> createVertexMap() {
        Map<FaceCorner, List<Vector3f>> map = new HashMap<>();
        map.put(new FaceCorner(EnumFacing.EAST, LU), list(v(1, 1, 1), v(1, 0.5F, 1), v(1, 0.5F, 0.5F), v(1, 1, 0.5F)));
        map.put(new FaceCorner(EnumFacing.EAST, RU), list(v(1, 1, 0.5F), v(1, 0.5F, 0.5F), v(1, 0.5F, 0), v(1, 1, 0)));
        map.put(new FaceCorner(EnumFacing.EAST, LD), list(v(1, 0.5F, 1), v(1, 0, 1), v(1, 0, 0.5F), v(1, 0.5F, 0.5F)));
        map.put(new FaceCorner(EnumFacing.EAST, RD), list(v(1, 0.5F, 0.5F), v(1, 0, 0.5F), v(1, 0, 0), v(1, 0.5F, 0)));
        map.put(new FaceCorner(EnumFacing.WEST, LU), list(v(0, 1, 0), v(0, 0.5F, 0), v(0, 0.5F, 0.5F), v(0, 1, 0.5F)));
        map.put(new FaceCorner(EnumFacing.WEST, RU), list(v(0, 1, 0.5F), v(0, 0.5F, 0.5F), v(0, 0.5F, 1), v(0, 1, 1)));
        map.put(new FaceCorner(EnumFacing.WEST, LD), list(v(0, 0.5F, 0), v(0, 0, 0), v(0, 0, 0.5F), v(0, 0.5F, 0.5F)));
        map.put(new FaceCorner(EnumFacing.WEST, RD), list(v(0, 0.5F, 0.5F), v(0, 0, 0.5F), v(0, 0, 1), v(0, 0.5F, 1)));
        map.put(new FaceCorner(EnumFacing.SOUTH, LU), list(v(0, 1, 1), v(0, 0.5F, 1), v(0.5F, 0.5F, 1), v(0.5F, 1, 1)));
        map.put(new FaceCorner(EnumFacing.SOUTH, RU), list(v(0.5F, 1, 1), v(0.5F, 0.5F, 1), v(1, 0.5F, 1), v(1, 1, 1)));
        map.put(new FaceCorner(EnumFacing.SOUTH, LD), list(v(0, 0.5F, 1), v(0, 0, 1), v(0.5F, 0, 1), v(0.5F, 0.5F, 1)));
        map.put(new FaceCorner(EnumFacing.SOUTH, RD), list(v(0.5F, 0.5F, 1), v(0.5F, 0, 1), v(1, 0, 1), v(1, 0.5F, 1)));
        map.put(new FaceCorner(EnumFacing.NORTH, LU), list(v(1, 1, 0), v(1, 0.5F, 0), v(0.5F, 0.5F, 0), v(0.5F, 1, 0)));
        map.put(new FaceCorner(EnumFacing.NORTH, RU), list(v(0.5F, 1, 0), v(0.5F, 0.5F, 0), v(0, 0.5F, 0), v(0, 1, 0)));
        map.put(new FaceCorner(EnumFacing.NORTH, LD), list(v(1, 0.5F, 0), v(1, 0, 0), v(0.5F, 0, 0), v(0.5F, 0.5F, 0)));
        map.put(new FaceCorner(EnumFacing.NORTH, RD), list(v(0.5F, 0.5F, 0), v(0.5F, 0, 0), v(0, 0, 0), v(0, 0.5F, 0)));
        map.put(new FaceCorner(EnumFacing.UP, LU), list(v(0, 1, 1), v(0.5F, 1, 1), v(0.5F, 1, 0.5F), v(0, 1, 0.5F)));
        map.put(new FaceCorner(EnumFacing.UP, RU), list(v(0, 1, 0.5F), v(0.5F, 1, 0.5F), v(0.5F, 1, 0), v(0, 1, 0)));
        map.put(new FaceCorner(EnumFacing.UP, LD), list(v(0.5F, 1, 1), v(1, 1, 1), v(1, 1, 0.5F), v(0.5F, 1, 0.5F)));
        map.put(new FaceCorner(EnumFacing.UP, RD), list(v(0.5F, 1, 0.5F), v(1, 1, 0.5F), v(1, 1, 0), v(0.5F, 1, 0)));
        map.put(new FaceCorner(EnumFacing.DOWN, LU), list(v(1, 0, 1), v(0.5F, 0, 1), v(0.5F, 0, 0.5F), v(1, 0, 0.5F)));
        map.put(new FaceCorner(EnumFacing.DOWN, RU), list(v(1, 0, 0.5F), v(0.5F, 0, 0.5F), v(0.5F, 0, 0), v(1, 0, 0)));
        map.put(new FaceCorner(EnumFacing.DOWN, LD), list(v(0.5F, 0, 1), v(0, 0, 1), v(0, 0, 0.5F), v(0.5F, 0, 0.5F)));
        map.put(new FaceCorner(EnumFacing.DOWN, RD), list(v(0.5F, 0, 0.5F), v(0, 0, 0.5F), v(0, 0, 0), v(0.5F, 0, 0)));
        return map;
    }

    private static List<Vector3f> list(Vector3f... vectors) {
        return Arrays.asList(vectors);
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    private static class FaceCorner {
        private final EnumFacing face;
        private final int corner;

        FaceCorner(EnumFacing face, int corner) {
            this.face = face;
            this.corner = corner;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof FaceCorner other)) {
                return false;
            }
            return this.face == other.face && this.corner == other.corner;
        }

        @Override
        public int hashCode() {
            return this.face.hashCode() * 31 + this.corner;
        }
    }
}
