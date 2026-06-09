package com.formlesslab.ae2additions.assembler.model;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public final class AssemblerGlassConnect {
    private final boolean[][][] connects = new boolean[3][3][3];
    private int face;

    private AssemblerGlassConnect() {
    }

    public static AssemblerGlassConnect from(BlockPos pos, NeighbourCheck check) {
        AssemblerGlassConnect connect = new AssemblerGlassConnect();
        connect.init(pos);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (check.isGlass(x, y, z)) {
                        connect.set(x, y, z);
                    }
                }
            }
        }
        return connect;
    }

    public int getFace(EnumFacing face) {
        if (blocked(face)) {
            return -1;
        }
        return this.face;
    }

    public int getIndex(EnumFacing face, int corner) {
        if (blocked(face)) {
            return -1;
        }
        switch (face) {
            case WEST:
            case EAST:
                return getIndexX(face, corner);
            case DOWN:
            case UP:
                return getIndexY(face, corner);
            case NORTH:
            case SOUTH:
                return getIndexZ(face, corner);
            default:
                return -1;
        }
    }

    private void init(BlockPos pos) {
        this.face = Math.abs((pos.getX() ^ pos.getY() ^ pos.getZ()) % 3);
    }

    private void set(int x, int y, int z) {
        this.connects[x + 1][y + 1][z + 1] = true;
    }

    private boolean blocked(EnumFacing face) {
        Vec3i normal = face.getDirectionVec();
        return this.connects[normal.getX() + 1][normal.getY() + 1][normal.getZ() + 1];
    }

    private int getIndexX(EnumFacing face, int corner) {
        int x = face.getXOffset();
        switch (corner) {
            case 0:
                return getIndex(this.connects[1][1][1 + x], this.connects[1][2][1],
                    this.connects[1][2][1 + x]);
            case 1:
                return getIndex(this.connects[1][1][1 - x], this.connects[1][2][1],
                    this.connects[1][2][1 - x]);
            case 2:
                return getIndex(this.connects[1][1][1 + x], this.connects[1][0][1],
                    this.connects[1][0][1 + x]);
            case 4:
                return getIndex(this.connects[1][1][1 - x], this.connects[1][0][1],
                    this.connects[1][0][1 - x]);
            default:
                return -1;
        }
    }

    private int getIndexY(EnumFacing face, int corner) {
        int y = face.getYOffset();
        switch (corner) {
            case 0:
                return getIndex(this.connects[1][1][2], this.connects[1 - y][1][1],
                    this.connects[1 - y][1][2]);
            case 1:
                return getIndex(this.connects[1][1][0], this.connects[1 - y][1][1],
                    this.connects[1 - y][1][0]);
            case 2:
                return getIndex(this.connects[1][1][2], this.connects[1 + y][1][1],
                    this.connects[1 + y][1][2]);
            case 4:
                return getIndex(this.connects[1][1][0], this.connects[1 + y][1][1],
                    this.connects[1 + y][1][0]);
            default:
                return -1;
        }
    }

    private int getIndexZ(EnumFacing face, int corner) {
        int z = face.getZOffset();
        switch (corner) {
            case 0:
                return getIndex(this.connects[1 - z][1][1], this.connects[1][2][1],
                    this.connects[1 - z][2][1]);
            case 1:
                return getIndex(this.connects[1 + z][1][1], this.connects[1][2][1],
                    this.connects[1 + z][2][1]);
            case 2:
                return getIndex(this.connects[1 - z][1][1], this.connects[1][0][1],
                    this.connects[1 - z][0][1]);
            case 4:
                return getIndex(this.connects[1 + z][1][1], this.connects[1][0][1],
                    this.connects[1 + z][0][1]);
            default:
                return -1;
        }
    }

    private int getIndex(boolean a, boolean b, boolean c) {
        if (!a && !b) {
            return 0;
        }
        if (a && b && !c) {
            return 1;
        }
        if (!a && b) {
            return 2;
        }
        if (a && !b) {
            return 3;
        }
        return -1;
    }

    @Override
    public String toString() {
        return "AssemblerGlassConnect(face=" + this.face + ")";
    }

    @FunctionalInterface
    public interface NeighbourCheck {
        boolean isGlass(int x, int y, int z);
    }
}
