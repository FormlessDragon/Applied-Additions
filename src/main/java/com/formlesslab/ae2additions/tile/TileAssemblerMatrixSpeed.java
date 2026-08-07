package com.formlesslab.ae2additions.tile;

import com.formlesslab.ae2additions.init.Configurations;
import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;

public class TileAssemblerMatrixSpeed extends TileAssemblerMatrixFunction {
    public TileAssemblerMatrixSpeed() {
        super(Configurations.ASSEMBLER_MATRIX.speedIdlePowerUsage);
    }

    @Override
    public void add(ClusterAssemblerMatrix cluster) {
        cluster.addSpeedCore();
    }
}
