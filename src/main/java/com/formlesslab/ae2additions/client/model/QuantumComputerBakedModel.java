package com.formlesslab.ae2additions.client.model;

import ae2.client.render.VertexFormats;
import ae2.client.render.crafting.CraftingCubeState;
import com.formlesslab.ae2additions.block.quantum.BlockAAEAbstractCraftingUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.vecmath.Vector3f;

import com.formlesslab.ae2additions.client.util.QuantumComputerConnect;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.renderer.vertex.VertexFormatElement;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.client.model.pipeline.UnpackedBakedQuad;
import net.minecraftforge.common.property.IExtendedBlockState;

public class QuantumComputerBakedModel implements IBakedModel {
    private static final int LU = 0;
    private static final int RU = 1;
    private static final int LD = 2;
    private static final int RD = 4;
    private static final Map<FaceCorner, List<Vector3f>> VERTEX_MAP = createVertexMap();
    private static final EnumMap<EnumFacing, List<Vector3f>> FACE_MAP = createFaceMap();

    private final VertexFormat format;
    private final boolean structure;
    private final TextureAtlasSprite face;
    private final TextureAtlasSprite sides;
    private final TextureAtlasSprite poweredSides;
    private final TextureAtlasSprite faceAnimation;
    private final TextureAtlasSprite faceTopAnimation;
    private final TextureAtlasSprite faceBottomAnimation;
    private final List<BakedQuad> itemQuads;

    private QuantumComputerBakedModel(VertexFormat format, boolean structure, TextureAtlasSprite face,
                                      TextureAtlasSprite sides, TextureAtlasSprite poweredSides,
                                      @Nullable TextureAtlasSprite faceAnimation,
                                      @Nullable TextureAtlasSprite faceTopAnimation,
                                      @Nullable TextureAtlasSprite faceBottomAnimation) {
        this.format = format;
        this.structure = structure;
        this.face = face;
        this.sides = sides;
        this.poweredSides = poweredSides;
        this.faceAnimation = faceAnimation;
        this.faceTopAnimation = faceTopAnimation;
        this.faceBottomAnimation = faceBottomAnimation;
        this.itemQuads = this.createItemQuads();
    }

    public static QuantumComputerBakedModel structure(VertexFormat format, TextureAtlasSprite face,
                                                     TextureAtlasSprite sides,
                                                     TextureAtlasSprite poweredSides) {
        return new QuantumComputerBakedModel(format, true, face, sides, poweredSides, null, null, null);
    }

    public static QuantumComputerBakedModel internal(VertexFormat format, TextureAtlasSprite face,
                                                    TextureAtlasSprite sides,
                                                    TextureAtlasSprite poweredSides,
                                                    TextureAtlasSprite faceAnimation,
                                                    TextureAtlasSprite faceTopAnimation,
                                                    TextureAtlasSprite faceBottomAnimation) {
        return new QuantumComputerBakedModel(format, false, face, sides, poweredSides, faceAnimation,
            faceTopAnimation, faceBottomAnimation);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
        if (side == null) {
            return state == null ? this.itemQuads : Collections.emptyList();
        }

        QuantumComputerConnect connect = getConnect(state);
        boolean powered = state != null && state.getValue(BlockAAEAbstractCraftingUnit.POWERED);
        BlockRenderLayer layer = MinecraftForgeClient.getRenderLayer();
        ArrayList<BakedQuad> quads = new ArrayList<>(this.structure ? 8 : 6);

        if (!this.structure || layer == null || layer == BlockRenderLayer.TRANSLUCENT) {
            this.addFaceQuad(quads, side, connect.getFace(side), powered);
        }

        if (!this.structure || layer == null || layer == BlockRenderLayer.CUTOUT) {
            this.addSides(quads, connect, side, powered, false);
            if (this.structure) {
                this.addSides(quads, connect, side.getOpposite(), powered, true);
            }
        }

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
        return this.face;
    }

    @Override
    public ItemCameraTransforms getItemCameraTransforms() {
        return ItemCameraTransforms.DEFAULT;
    }

    @Override
    public ItemOverrideList getOverrides() {
        return ItemOverrideList.NONE;
    }

    private void addSides(List<BakedQuad> quads, QuantumComputerConnect connect, EnumFacing side, boolean powered,
                          boolean renderOpposite) {
        this.addCornerQuad(quads, side, connect.getIndex(side, LU), LU, powered, renderOpposite);
        this.addCornerQuad(quads, side, connect.getIndex(side, RU), RU, powered, renderOpposite);
        this.addCornerQuad(quads, side, connect.getIndex(side, LD), LD, powered, renderOpposite);
        this.addCornerQuad(quads, side, connect.getIndex(side, RD), RD, powered, renderOpposite);
    }

    private void addFaceQuad(List<BakedQuad> quads, EnumFacing side, int index, boolean powered) {
        if (index < 0) {
            return;
        }

        List<Vector3f> vertices = offset(FACE_MAP.get(side), side.getDirectionVec(), -0.002F);
        this.addQuad(quads, side, this.face, vertices, 0.0F, 0.0F, 1.0F, 1.0F, false, false);

        TextureAtlasSprite animation = this.getFaceAnimation(side);
        if (powered && animation != null) {
            this.addQuad(quads, side, animation, vertices, 0.0F, 0.0F, 1.0F, 1.0F, true, false);
        }
    }

    private void addCornerQuad(List<BakedQuad> quads, EnumFacing side, int index, int corner, boolean powered,
                               boolean renderOpposite) {
        if (index < 0) {
            return;
        }

        List<Vector3f> vertices = VERTEX_MAP.get(new FaceCorner(side, corner));
        if (renderOpposite) {
            vertices = reverseAndOffset(vertices, side.getDirectionVec(), -0.004F);
        }

        float u0 = renderOpposite ? getU1(index) : getU0(index);
        float u1 = renderOpposite ? getU0(index) : getU1(index);
        float v0 = getV0(index);
        float v1 = getV1(index);
        TextureAtlasSprite texture = powered ? this.poweredSides : this.sides;
        switch (corner) {
            case LU:
                this.addQuad(quads, side, texture, vertices, u0, v0, u1, v1, powered, renderOpposite);
                break;
            case RU:
                this.addQuad(quads, side, texture, vertices, u1, v0, u0, v1, powered, renderOpposite);
                break;
            case LD:
                this.addQuad(quads, side, texture, vertices, u0, v1, u1, v0, powered, renderOpposite);
                break;
            case RD:
                this.addQuad(quads, side, texture, vertices, u1, v1, u0, v0, powered, renderOpposite);
                break;
            default:
                break;
        }
    }

    private void addQuad(List<BakedQuad> quads, EnumFacing side, TextureAtlasSprite sprite,
                         List<Vector3f> vertices, float u0, float v0, float u1, float v1,
                         boolean fullBright, boolean renderOpposite) {
        UnpackedBakedQuad.Builder builder = new UnpackedBakedQuad.Builder(this.format);
        if (fullBright) {
            builder = new UnpackedBakedQuad.Builder(VertexFormats.getFormatWithLightMap(this.format));
        }
        builder.setTexture(sprite);
        builder.setQuadOrientation(side);
        builder.setApplyDiffuseLighting(true);
        Vec3i normal = side.getDirectionVec();

        if (renderOpposite) {
            this.putVertex(builder, sprite, normal, vertices.get(0), u0, v0, fullBright);
            this.putVertex(builder, sprite, normal, vertices.get(1), u0, v1, fullBright);
            this.putVertex(builder, sprite, normal, vertices.get(2), u1, v1, fullBright);
            this.putVertex(builder, sprite, normal, vertices.get(3), u1, v0, fullBright);
        } else {
            this.putVertex(builder, sprite, normal, vertices.get(0), u0, v0, fullBright);
            this.putVertex(builder, sprite, normal, vertices.get(1), u0, v1, fullBright);
            this.putVertex(builder, sprite, normal, vertices.get(2), u1, v1, fullBright);
            this.putVertex(builder, sprite, normal, vertices.get(3), u1, v0, fullBright);
        }

        quads.add(builder.build());
    }

    private void putVertex(UnpackedBakedQuad.Builder builder, TextureAtlasSprite sprite, Vec3i normal,
                           Vector3f pos, float u, float v, boolean fullBright) {
        VertexFormat format = builder.getVertexFormat();
        for (int elementIndex = 0; elementIndex < format.getElementCount(); elementIndex++) {
            VertexFormatElement element = format.getElement(elementIndex);
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
                    } else if (fullBright) {
                        float light = (15.0F * 0x20) / 0xFFFF;
                        builder.put(elementIndex, light, light, 0.0F, 1.0F);
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

    @Nullable
    private TextureAtlasSprite getFaceAnimation(EnumFacing side) {
        if (this.structure) {
            return null;
        }
        if (side == EnumFacing.UP) {
            return this.faceTopAnimation;
        }
        if (side == EnumFacing.DOWN) {
            return this.faceBottomAnimation;
        }
        return this.faceAnimation;
    }

    private List<BakedQuad> createItemQuads() {
        ArrayList<BakedQuad> quads = new ArrayList<>(EnumFacing.values().length * 5);
        QuantumComputerConnect connect = QuantumComputerConnect.from(BlockPos.ORIGIN, EnumSet.noneOf(EnumFacing.class));
        for (EnumFacing side : EnumFacing.values()) {
            this.addFaceQuad(quads, side, connect.getFace(side), false);
            this.addSides(quads, connect, side, false, false);
            if (this.structure) {
                this.addSides(quads, connect, side.getOpposite(), false, true);
            }
        }
        return quads;
    }

    private static QuantumComputerConnect getConnect(@Nullable IBlockState state) {
        if (state instanceof IExtendedBlockState) {
            QuantumComputerConnect connect =
                ((IExtendedBlockState) state).getValue(QuantumComputerConnectProperty.INSTANCE);
            if (connect != null) {
                return connect;
            }

            CraftingCubeState cubeState = ((IExtendedBlockState) state).getValue(BlockAAEAbstractCraftingUnit.STATE);
            if (cubeState != null) {
                return QuantumComputerConnect.from(BlockPos.ORIGIN, cubeState.connections());
            }
        }
        return QuantumComputerConnect.from(BlockPos.ORIGIN, EnumSet.noneOf(EnumFacing.class));
    }

    private static List<Vector3f> offset(List<Vector3f> vertices, Vec3i normal, float step) {
        return Arrays.asList(
            offset(vertices.get(0), normal, step),
            offset(vertices.get(1), normal, step),
            offset(vertices.get(2), normal, step),
            offset(vertices.get(3), normal, step)
        );
    }

    private static List<Vector3f> reverseAndOffset(List<Vector3f> vertices, Vec3i normal, float step) {
        return Arrays.asList(
            offset(vertices.get(3), normal, step),
            offset(vertices.get(2), normal, step),
            offset(vertices.get(1), normal, step),
            offset(vertices.get(0), normal, step)
        );
    }

    private static Vector3f offset(Vector3f vertex, Vec3i normal, float step) {
        return new Vector3f(
            vertex.x + normal.getX() * step,
            vertex.y + normal.getY() * step,
            vertex.z + normal.getZ() * step
        );
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

    private static final class FaceCorner {
        private final EnumFacing face;
        private final int corner;

        private FaceCorner(EnumFacing face, int corner) {
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
