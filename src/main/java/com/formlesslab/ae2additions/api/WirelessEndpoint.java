package com.formlesslab.ae2additions.api;

import ae2.api.util.AEColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface WirelessEndpoint {
    int allocatePort();

    void setFrequency(long frequency, int port);

    long getNewFrequency();

    World getEndpointWorld();

    BlockPos getEndpointPos();

    AEColor getEndpointColor();
}
