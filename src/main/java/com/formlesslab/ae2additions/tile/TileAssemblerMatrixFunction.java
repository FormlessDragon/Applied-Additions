package com.formlesslab.ae2additions.tile;

import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;

public abstract class TileAssemblerMatrixFunction extends TileAssemblerMatrixBase {
    public TileAssemblerMatrixFunction() {
        super();
        this.getMainNode().setIdlePowerUsage(1);
    }

    public abstract void add(ClusterAssemblerMatrix cluster);
}
