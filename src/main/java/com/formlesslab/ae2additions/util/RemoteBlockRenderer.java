package com.formlesslab.ae2additions.util;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.pipeline.LightUtil;
import org.lwjgl.opengl.GL11;

import java.util.List;

public final class RemoteBlockRenderer {
    private RemoteBlockRenderer() {
    }

    public static boolean renderSingle(BlockPos pos, int x, int y, float size) {
        World world = Minecraft.getMinecraft().world;
        PreviewBlock previewBlock = getPreviewBlock(world, pos);
        if (previewBlock == null) {
            return false;
        }

        beginBlockRendering();
        GlStateManager.translate(x + size / 2.0F, y + size / 2.0F, 100.0F);
        GlStateManager.scale(size, -size, size);
        renderState(world, pos, previewBlock);
        endBlockRendering();
        return true;
    }

    public static boolean renderScene(BlockPos pos, int centerX, int centerY, float scale, float rotationX, float rotationY, float offsetX, float offsetY, int clipX, int clipY, int clipWidth, int clipHeight) {
        World world = Minecraft.getMinecraft().world;
        if (world == null || pos == null) {
            return false;
        }

        boolean rendered = false;
        beginScissor(clipX, clipY, clipWidth, clipHeight);
        beginBlockRendering();
        GlStateManager.translate(centerX + offsetX, centerY + offsetY, 100.0F);
        GlStateManager.scale(scale, -scale, scale);
        GlStateManager.rotate(rotationX, 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(rotationY, 0.0F, 1.0F, 0.0F);
        for (int axis = 0; axis < 3; axis++) {
            for (int offset = -1; offset <= 1; offset++) {
                BlockPos renderPos = offset(pos, axis, offset);
                PreviewBlock previewBlock = getPreviewBlock(world, renderPos);
                if (previewBlock == null) {
                    continue;
                }

                GlStateManager.pushMatrix();
                GlStateManager.translate(renderPos.getX() - pos.getX(), renderPos.getY() - pos.getY(), renderPos.getZ() - pos.getZ());
                renderState(world, renderPos, previewBlock);
                GlStateManager.popMatrix();
                rendered = true;
            }
        }
        endBlockRendering();
        endScissor();
        return rendered;
    }

    private static void beginBlockRendering() {
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(770, 771);
        GlStateManager.disableCull();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        if (Minecraft.isAmbientOcclusionEnabled()) {
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
        } else {
            GlStateManager.shadeModel(GL11.GL_FLAT);
        }
    }

    private static void renderState(World world, BlockPos pos, PreviewBlock previewBlock) {
        GlStateManager.pushMatrix();
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        renderModel(world, pos, previewBlock);
        GlStateManager.popMatrix();
    }

    private static void endBlockRendering() {
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.disableDepth();
        GlStateManager.shadeModel(GL11.GL_FLAT);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void renderModel(World world, BlockPos pos, PreviewBlock previewBlock) {
        BlockRendererDispatcher dispatcher = Minecraft.getMinecraft().getBlockRendererDispatcher();
        IBakedModel model = dispatcher.getModelForState(previewBlock.modelState);
        long random = MathHelper.getPositionRandom(pos);
        boolean useBiomeTint = shouldRenderWithBiomeTint(previewBlock.renderState, model, random);

        try {
            for (BlockRenderLayer layer : BlockRenderLayer.values()) {
                if (!previewBlock.modelState.getBlock().canRenderInLayer(previewBlock.modelState, layer)) {
                    continue;
                }

                ForgeHooksClient.setRenderLayer(layer);
                if (layer == BlockRenderLayer.TRANSLUCENT) {
                    GlStateManager.depthMask(false);
                }
                if (useBiomeTint) {
                    renderQuads(world, pos, previewBlock.renderState, model.getQuads(previewBlock.renderState, null, random));
                    for (EnumFacing side : EnumFacing.values()) {
                        renderQuads(world, pos, previewBlock.renderState, model.getQuads(previewBlock.renderState, side, random));
                    }
                } else {
                    dispatcher.getBlockModelRenderer().renderModelBrightnessColor(previewBlock.renderState, model, 1.0F, 1.0F, 1.0F, 1.0F);
                }
                if (layer == BlockRenderLayer.TRANSLUCENT) {
                    GlStateManager.depthMask(true);
                }
            }
        } finally {
            GlStateManager.depthMask(true);
            ForgeHooksClient.setRenderLayer(null);
        }
    }

    private static boolean shouldRenderWithBiomeTint(IBlockState state, IBakedModel model, long random) {
        ResourceLocation registryName = state.getBlock().getRegistryName();
        if (registryName == null || !"minecraft".equals(registryName.getNamespace())) {
            return false;
        }
        if (hasTintedQuads(model.getQuads(state, null, random))) {
            return true;
        }
        for (EnumFacing side : EnumFacing.values()) {
            if (hasTintedQuads(model.getQuads(state, side, random))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTintedQuads(List<BakedQuad> quads) {
        for (BakedQuad quad : quads) {
            if (quad.hasTintIndex()) {
                return true;
            }
        }
        return false;
    }

    private static void renderQuads(World world, BlockPos pos, IBlockState state, List<BakedQuad> quads) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (BakedQuad quad : quads) {
            int color = quad.hasTintIndex()
                    ? Minecraft.getMinecraft().getBlockColors().colorMultiplier(state, world, pos, quad.getTintIndex())
                    : 0xFFFFFF;
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.ITEM);
            LightUtil.renderQuadColor(buffer, quad, 0xFF000000 | color);
            tessellator.draw();
        }
    }

    private static PreviewBlock getPreviewBlock(World world, BlockPos pos) {
        if (world == null || pos == null || !world.isBlockLoaded(pos)) {
            return null;
        }

        IBlockState state = world.getBlockState(pos);
        if (state.getBlock().isAir(state, world, pos)) {
            return null;
        }
        IBlockState modelState = state.getActualState(world, pos);
        IBlockState renderState = modelState.getBlock().getExtendedState(modelState, world, pos);
        return new PreviewBlock(modelState, renderState);
    }

    private static void beginScissor(int x, int y, int width, int height) {
        Minecraft minecraft = Minecraft.getMinecraft();
        ScaledResolution scaled = new ScaledResolution(minecraft);
        int scale = scaled.getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * scale,
            minecraft.displayHeight - (y + height) * scale,
            width * scale,
            height * scale);
    }

    private static void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private static BlockPos offset(BlockPos pos, int axis, int offset) {
        return switch (axis) {
            case 0 -> pos.add(offset, 0, 0);
            case 1 -> pos.add(0, offset, 0);
            default -> pos.add(0, 0, offset);
        };
    }

    private record PreviewBlock(IBlockState modelState, IBlockState renderState) {
    }
}
