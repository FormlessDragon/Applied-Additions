package com.formlesslab.ae2additions.assembler.block;

import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixFrame;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockAssemblerMatrixFrame extends BlockAssemblerMatrixBase<TileAssemblerMatrixFrame> {
    public static final PropertyEnum<Shape> SHAPE = PropertyEnum.create("shape", Shape.class);

    public BlockAssemblerMatrixFrame() {
        super(TileAssemblerMatrixFrame.class);
        this.setDefaultState(this.blockState.getBaseState()
            .withProperty(FORMED, false)
            .withProperty(POWERED, false)
            .withProperty(SHAPE, Shape.BLOCK));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockState(FORMED, POWERED, SHAPE);
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY,
                                            float hitZ, int meta, net.minecraft.entity.EntityLivingBase placer,
                                            net.minecraft.util.EnumHand hand) {
        return getShapeType(super.getStateForPlacement(world, pos, facing, hitX, hitY, hitZ, meta, placer, hand), world, pos);
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        return getShapeType(super.getActualState(state, world, pos), world, pos);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta).withProperty(SHAPE, Shape.BLOCK);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return 0;
    }

    private IBlockState getShapeType(IBlockState baseState, IBlockAccess world, BlockPos pos) {
        Shape type = Shape.BLOCK;
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();

        boolean xx = isFrame(world, x - 1, y, z) && isFrame(world, x + 1, y, z);
        boolean yy = isFrame(world, x, y - 1, z) && isFrame(world, x, y + 1, z);
        boolean zz = isFrame(world, x, y, z - 1) && isFrame(world, x, y, z + 1);

        if (xx && !yy && !zz) {
            type = Shape.COLUMN_X;
        } else if (!xx && yy && !zz) {
            type = Shape.COLUMN_Y;
        } else if (!xx && !yy && zz) {
            type = Shape.COLUMN_Z;
        }

        return baseState.withProperty(SHAPE, type);
    }

    private static boolean isFrame(IBlockAccess world, int x, int y, int z) {
        return world.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof BlockAssemblerMatrixFrame;
    }

    public enum Shape implements IStringSerializable {
        BLOCK("block"),
        COLUMN_X("column_x"),
        COLUMN_Y("column_y"),
        COLUMN_Z("column_z");

        private final String name;

        Shape(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return this.name;
        }
    }
}
