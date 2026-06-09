package com.formlesslab.ae2additions.assembler.tile;

import com.formlesslab.ae2additions.assembler.me.ClusterAssemblerMatrix;

public class TileAssemblerMatrixSpeed extends TileAssemblerMatrixFunction {
    @Override
    public void add(ClusterAssemblerMatrix cluster) {
        cluster.addSpeedCore();
    }
}
