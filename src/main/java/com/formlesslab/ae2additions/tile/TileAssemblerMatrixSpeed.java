package com.formlesslab.ae2additions.tile;

import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;

public class TileAssemblerMatrixSpeed extends TileAssemblerMatrixFunction {
    @Override
    public void add(ClusterAssemblerMatrix cluster) {
        cluster.addSpeedCore();
    }
}
