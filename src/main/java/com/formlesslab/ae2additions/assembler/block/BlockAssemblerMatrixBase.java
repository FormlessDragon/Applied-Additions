package com.formlesslab.ae2additions.assembler.block;

import ae2.block.AEBaseTileBlock;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.ModGuiHandler;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixBase;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockAssemblerMatrixBase<M extends TileAssemblerMatrixBase> extends AEBaseTileBlock<M> {
    public static final PropertyBool FORMED = PropertyBool.create("formed");
    public static final PropertyBool POWERED = PropertyBool.create("powered");

    protected BlockAssemblerMatrixBase(Class<M> tileClass) {
        this(tileClass, Material.IRON);
    }

    protected BlockAssemblerMatrixBase(Class<M> tileClass, Material material) {
        super(material);
        this.setTileEntity(tileClass);
        this.setDefaultState(this.blockState.getBaseState()
            .withProperty(FORMED, false)
            .withProperty(POWERED, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockState(FORMED, POWERED);
    }

    @Override
    protected IBlockState updateBlockStateFromTileEntity(IBlockState currentState, M tileEntity) {
        return currentState
            .withProperty(FORMED, tileEntity.isFormed())
            .withProperty(POWERED, tileEntity.isPowered());
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        M tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return super.getActualState(state, world, pos);
        }
        return updateBlockStateFromTileEntity(super.getActualState(state, world, pos), tile);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta)
            .withProperty(FORMED, false)
            .withProperty(POWERED, false);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        M tile = this.getTileEntity(world, pos);
        if (tile != null) {
            tile.updateMultiBlock(fromPos);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        M tile = this.getTileEntity(world, pos);
        if (tile != null) {
            tile.breakCluster();
        }
        super.breakBlock(world, pos, state);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        M tile = this.getTileEntity(world, pos);
        if (tile != null && tile.isFormed() && tile.isActive() && !player.isSneaking()
            && this.openAssemblerMatrixGui(tile, world, pos, player)) {
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }

    public Item getPresentItem() {
        return Item.getItemFromBlock(this);
    }

    protected boolean openAssemblerMatrixGui(M tile, World world, BlockPos pos, EntityPlayer player) {
        if (!world.isRemote) {
            player.openGui(AppliedAdditions.INSTANCE, ModGuiHandler.ASSEMBLER_MATRIX, world,
                pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }
}
