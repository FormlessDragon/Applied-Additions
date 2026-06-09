package com.formlesslab.ae2additions.assembler.tile;

import com.formlesslab.ae2additions.assembler.me.ClusterAssemblerMatrix;

public abstract class TileAssemblerMatrixFunction extends TileAssemblerMatrixBase {
    public TileAssemblerMatrixFunction() {
        super();
        this.getMainNode().setIdlePowerUsage(1);
    }

    public abstract void add(ClusterAssemblerMatrix cluster);
}
