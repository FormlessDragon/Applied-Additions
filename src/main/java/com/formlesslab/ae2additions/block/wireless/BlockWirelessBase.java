package com.formlesslab.ae2additions.block.wireless;

import ae2.api.implementations.blockentities.IColorableBlockEntity;
import ae2.api.util.AEColor;
import ae2.block.AEBaseTileBlock;
import ae2.api.orientation.IOrientationStrategy;
import ae2.api.orientation.OrientationStrategies;
import ae2.tile.AEBaseTile;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.tile.TileWirelessConnector;
import com.formlesslab.ae2additions.tile.TileWirelessHub;
import com.formlesslab.ae2additions.api.WirelessEndpoint;
import com.formlesslab.ae2additions.wireless.WirelessLinking;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public abstract class BlockWirelessBase<T extends AEBaseTile & WirelessEndpoint> extends AEBaseTileBlock<T> {
    public static final PropertyBool CONNECTED = PropertyBool.create("connected");
    public static final PropertyInteger COLOR = PropertyInteger.create("color", 0, AEColor.TRANSPARENT.ordinal());

    protected BlockWirelessBase(Class<T> tileClass) {
        super(Material.IRON);
        this.setTileEntity(tileClass);
        this.setDefaultState(this.blockState.getBaseState()
            .withProperty(CONNECTED, false)
            .withProperty(COLOR, AEColor.TRANSPARENT.ordinal()));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return createBlockState(CONNECTED, COLOR);
    }

    @Override
    public IOrientationStrategy getOrientationStrategy() {
        return OrientationStrategies.facing();
    }

    @Override
    protected IBlockState updateBlockStateFromTileEntity(IBlockState currentState, T tileEntity) {
        return currentState
            .withProperty(CONNECTED, isEndpointConnected(tileEntity))
            .withProperty(COLOR, getColorIndex(tileEntity.getEndpointColor()));
    }

    @Override
    @SuppressWarnings("deprecation")
    public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos pos) {
        T tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return super.getActualState(state, world, pos);
        }
        return updateBlockStateFromTileEntity(super.getActualState(state, world, pos), tile);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return super.getStateFromMeta(meta)
            .withProperty(CONNECTED, false)
            .withProperty(COLOR, AEColor.TRANSPARENT.ordinal());
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player,
                                    EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        ItemStack held = player.getHeldItem(hand);
        T tile = this.getTileEntity(world, pos);
        if (tile == null) {
            return true;
        }

        if (!held.isEmpty() && held.getItem() == ModContent.WIRELESS_TOOL) {
            EnumActionResult result = WirelessLinking.useTool(player, held, tile);
            return result != EnumActionResult.PASS;
        }

        if (super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ)) {
            return true;
        }

        if (!player.isSneaking()) {
            if (!world.isRemote) {
                player.openGui(AppliedAdditions.INSTANCE, getGuiId(), world, pos.getX(), pos.getY(), pos.getZ());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, EnumDyeColor color) {
        T tile = this.getTileEntity(world, pos);
        return tile instanceof IColorableBlockEntity colorable
                && colorable.recolourBlock(side, AEColor.fromDye(color), null);
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        T tile = this.getTileEntity(world, pos);
        if (tile instanceof TileWirelessConnector connector) {
            connector.breakOnRemove();
        } else if (tile instanceof TileWirelessHub hub) {
            hub.breakOnRemove();
        }
        super.breakBlock(world, pos, state);
    }

    protected abstract int getGuiId();

    private static boolean isEndpointConnected(WirelessEndpoint endpoint) {
        if (endpoint instanceof TileWirelessConnector connector) {
            return connector.isConnectedForRendering();
        }
        if (endpoint instanceof TileWirelessHub hub) {
            return hub.isConnectedForRendering();
        }
        return false;
    }

    private static int getColorIndex(AEColor color) {
        return color == null ? AEColor.TRANSPARENT.ordinal() : color.ordinal();
    }
}
