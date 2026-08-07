package com.formlesslab.ae2additions.tile;

import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;

public abstract class TileAssemblerMatrixFunction extends TileAssemblerMatrixBase {
    protected TileAssemblerMatrixFunction() {
        this(1);
    }

    protected TileAssemblerMatrixFunction(double idlePowerUsage) {
        super();
        this.getMainNode().setIdlePowerUsage(idlePowerUsage);
    }

    public abstract void add(ClusterAssemblerMatrix cluster);
}
