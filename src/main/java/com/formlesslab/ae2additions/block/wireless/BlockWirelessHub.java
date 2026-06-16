package com.formlesslab.ae2additions.block.wireless;

import com.formlesslab.ae2additions.ModGuiHandler;
import com.formlesslab.ae2additions.tile.TileWirelessHub;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockWirelessHub extends BlockWirelessBase<TileWirelessHub> {
    public BlockWirelessHub() {
        super(TileWirelessHub.class);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
        TileWirelessHub tile = this.getTileEntity(world, pos);
        if (tile != null) {
            tile.reactiveAll();
        }
    }

    @Override
    protected int getGuiId() {
        return ModGuiHandler.WIRELESS_HUB;
    }
}
