package com.formlesslab.ae2additions.api;

import ae2.api.networking.IGridNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface WirelessNode {
    long getFrequency();

    World getWirelessWorld();

    BlockPos getWirelessPos();

    IGridNode getWirelessGridNode();

    TileEntity getWirelessTile();
}
