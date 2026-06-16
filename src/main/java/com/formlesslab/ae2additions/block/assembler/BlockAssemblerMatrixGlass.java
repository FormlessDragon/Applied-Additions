package com.formlesslab.ae2additions.block.assembler;

import com.formlesslab.ae2additions.client.model.AssemblerGlassConnect;
import com.formlesslab.ae2additions.client.model.AssemblerGlassConnectProperty;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixGlass;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public class BlockAssemblerMatrixGlass extends BlockAssemblerMatrixBase<TileAssemblerMatrixGlass> {
    public BlockAssemblerMatrixGlass() {
        super(TileAssemblerMatrixGlass.class, Material.GLASS);
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new ExtendedBlockState(this, new IProperty<?>[] {FORMED, POWERED},
            new IUnlistedProperty<?>[] {FORWARD, UP, AssemblerGlassConnectProperty.INSTANCE});
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        state = super.getExtendedState(state, world, pos);
        if (!(state instanceof IExtendedBlockState)) {
            return state;
        }

        AssemblerGlassConnect connect = AssemblerGlassConnect.from(pos,
            (x, y, z) -> world.getBlockState(pos.add(x, y, z)).getBlock() instanceof BlockAssemblerMatrixGlass);
        return ((IExtendedBlockState) state).withProperty(AssemblerGlassConnectProperty.INSTANCE, connect);
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean shouldSideBeRendered(IBlockState blockState, IBlockAccess blockAccess, BlockPos pos, EnumFacing side) {
        return !(blockAccess.getBlockState(pos.offset(side)).getBlock() instanceof BlockAssemblerMatrixGlass)
            && super.shouldSideBeRendered(blockState, blockAccess, pos, side);
    }
}
