package com.formlesslab.ae2additions.block.quantum;

import ae2.block.AEBaseTileBlock;
import ae2.block.crafting.ICraftingUnitType;
import ae2.client.render.crafting.CraftingCubeState;
import com.formlesslab.ae2additions.tile.TileAdvCraftingBlock;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.EnumSet;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public abstract class BlockAAEAbstractCraftingUnit<T extends TileAdvCraftingBlock> extends AEBaseTileBlock<T> {
    public static final PropertyBool FORMED = PropertyBool.create("formed");
    public static final PropertyBool POWERED = PropertyBool.create("powered");
    public static final IUnlistedProperty<CraftingCubeState> STATE = new IUnlistedProperty<>() {
        @Override
        public String getName() {
            return "state";
        }

        @Override
        public boolean isValid(CraftingCubeState value) {
            return true;
        }

        @Override
        public Class<CraftingCubeState> getType() {
            return CraftingCubeState.class;
        }

        @Override
        public String valueToString(CraftingCubeState value) {
            return String.valueOf(value);
        }
    };

    public final ICraftingUnitType type;

    protected BlockAAEAbstractCraftingUnit(ICraftingUnitType type, Class<T> tileEntityClass) {
        super(Material.IRON);
        this.type = type;
        this.setHardness(2.2F);
        this.setResistance(11.0F);
        this.setTileEntity(tileEntityClass);
        this.setDefaultState(this.blockState.getBaseState()
            .withProperty(FORMED, Boolean.FALSE)
            .withProperty(POWERED, Boolean.FALSE));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        ObjectList<IProperty<?>> properties = new ObjectArrayList<>(this.getOrientationStrategy().getProperties());
        properties.add(POWERED);
        properties.add(FORMED);
        return new ExtendedBlockState(this, properties.toArray(new IProperty<?>[0]), this.getUnlistedProperties());
    }

    protected IUnlistedProperty<?>[] getUnlistedProperties() {
        return new IUnlistedProperty<?>[] {FORWARD, UP, STATE};
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        state = super.getActualState(state, world, pos);

        T tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return state;
        }

        var renderState = tile.getRenderState();
        return state.withProperty(FORMED, renderState.formed())
            .withProperty(POWERED, renderState.powered());
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        state = super.getExtendedState(state, world, pos);
        if (!(state instanceof IExtendedBlockState extended)) {
            return state;
        }

        T tile = this.getTileEntity(world, pos);
        if (tile != null) {
            return extended.withProperty(STATE, new CraftingCubeState(tile.getRenderState().connections()));
        }

        EnumSet<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);
        for (EnumFacing facing : EnumFacing.values()) {
            if (this.isConnected(world, pos, facing)) {
                connections.add(facing);
            }
        }
        return extended.withProperty(STATE, new CraftingCubeState(connections));
    }

    private boolean isConnected(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return world.getBlockState(pos.offset(side)).getBlock() instanceof BlockAAEAbstractCraftingUnit<?>;
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        int meta = 0;
        if (state.getValue(POWERED)) {
            meta |= 1;
        }
        if (state.getValue(FORMED)) {
            meta |= 2;
        }
        return meta;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return this.getDefaultState()
            .withProperty(POWERED, (meta & 1) == 1)
            .withProperty(FORMED, (meta & 2) == 2);
    }

    @Override
    protected IBlockState updateBlockStateFromTileEntity(IBlockState currentState, T tileEntity) {
        return currentState.withProperty(POWERED, tileEntity.isPowered())
            .withProperty(FORMED, tileEntity.isFormed());
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return layer == BlockRenderLayer.CUTOUT;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        super.neighborChanged(state, world, pos, blockIn, fromPos);
        T tile = this.getTileEntity(world, pos);
        if (tile != null) {
            tile.updateMultiBlock(fromPos);
        }
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        T tile = this.getTileEntity(world, pos);
        if (tile != null) {
            tile.breakCluster();
        }
        super.breakBlock(world, pos, state);
    }
}
