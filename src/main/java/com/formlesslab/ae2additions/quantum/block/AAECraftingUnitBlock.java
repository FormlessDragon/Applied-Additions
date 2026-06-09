package com.formlesslab.ae2additions.quantum.block;

import ae2.block.crafting.AbstractCraftingUnitBlock;
import ae2.block.crafting.ICraftingUnitType;
import ae2.tile.crafting.ICraftingCPUTileEntity;
import com.formlesslab.ae2additions.quantum.AAECraftingUnitType;
import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AAECraftingUnitBlock extends AbstractCraftingUnitBlock<AdvCraftingBlockEntity> {
    public AAECraftingUnitBlock(ICraftingUnitType type) {
        super(type, AdvCraftingBlockEntity.class);
        if (type == AAECraftingUnitType.QUANTUM_STRUCTURE || type == AAECraftingUnitType.QUANTUM_CORE) {
            this.setLightOpacity(0);
        }
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return this.type == AAECraftingUnitType.QUANTUM_STRUCTURE ? BlockRenderLayer.CUTOUT : super.getRenderLayer();
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return this.type == AAECraftingUnitType.QUANTUM_STRUCTURE
            ? layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.TRANSLUCENT
            : super.canRenderInLayer(state, layer);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return this.type != AAECraftingUnitType.QUANTUM_STRUCTURE && super.isOpaqueCube(state);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return this.type != AAECraftingUnitType.QUANTUM_STRUCTURE && super.isFullCube(state);
    }

    @Override
    public boolean onBlockActivated(
        World world,
        BlockPos pos,
        IBlockState state,
        EntityPlayer player,
        EnumHand hand,
        EnumFacing side,
        float hitX,
        float hitY,
        float hitZ
    ) {
        AdvCraftingBlockEntity tile = this.getTileEntity(world, pos);
        if (tile != null && tile.isFormed() && tile.isActive()) {
            if (!world.isRemote) {
                tile.onQuantumComputerActivated(player);
            }
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
}
