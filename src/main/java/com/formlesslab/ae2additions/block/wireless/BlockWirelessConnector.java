package com.formlesslab.ae2additions.block.wireless;

import com.formlesslab.ae2additions.ModGuiHandler;
import com.formlesslab.ae2additions.tile.TileWirelessConnector;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockWirelessConnector extends BlockWirelessBase<TileWirelessConnector> {
    public BlockWirelessConnector() {
        super(TileWirelessConnector.class);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileWirelessConnector tile = this.getTileEntity(world, pos);
        if (tile != null) {
            tile.reactive();
        }
    }

    @Override
    protected int getGuiId() {
        return ModGuiHandler.WIRELESS_CONNECTOR;
    }
}
