package com.formlesslab.ae2additions.quantum.client;

import java.util.EnumSet;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3i;

public final class QuantumComputerConnect {
    private final boolean[][][] connects = new boolean[3][3][3];
    private int face;

    private QuantumComputerConnect() {
    }

    public static QuantumComputerConnect from(BlockPos pos, NeighbourCheck check) {
        QuantumComputerConnect connect = new QuantumComputerConnect();
        connect.face = Math.abs((pos.getX() ^ pos.getY() ^ pos.getZ()) % 3);
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (check.isConnected(x, y, z)) {
                        connect.set(x, y, z);
                    }
                }
            }
        }
        return connect;
    }

    public static QuantumComputerConnect from(BlockPos pos, EnumSet<EnumFacing> faces) {
        return from(pos, (x, y, z) -> {
            if (Math.abs(x) + Math.abs(y) + Math.abs(z) != 1) {
                return x == 0 && y == 0 && z == 0;
            }
            for (EnumFacing facing : faces) {
                if (facing.getXOffset() == x && facing.getYOffset() == y && facing.getZOffset() == z) {
                    return true;
                }
            }
            return false;
        });
    }

    public int getFace(EnumFacing face) {
        return this.blocked(face) ? -1 : this.face;
    }

    public int getIndex(EnumFacing face, int corner) {
        if (this.blocked(face)) {
            return -1;
        }

        return switch (face) {
            case WEST, EAST -> this.getIndexX(face, corner);
            case DOWN, UP -> this.getIndexY(face, corner);
            case NORTH, SOUTH -> this.getIndexZ(face, corner);
        };
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
        return switch (corner) {
            case 0 -> getIndex(this.connects[1][1][1 + x], this.connects[1][2][1],
                    this.connects[1][2][1 + x]);
            case 1 -> getIndex(this.connects[1][1][1 - x], this.connects[1][2][1],
                    this.connects[1][2][1 - x]);
            case 2 -> getIndex(this.connects[1][1][1 + x], this.connects[1][0][1],
                    this.connects[1][0][1 + x]);
            case 4 -> getIndex(this.connects[1][1][1 - x], this.connects[1][0][1],
                    this.connects[1][0][1 - x]);
            default -> -1;
        };
    }

    private int getIndexY(EnumFacing face, int corner) {
        int y = face.getYOffset();
        return switch (corner) {
            case 0 -> getIndex(this.connects[1][1][2], this.connects[1 - y][1][1],
                    this.connects[1 - y][1][2]);
            case 1 -> getIndex(this.connects[1][1][0], this.connects[1 - y][1][1],
                    this.connects[1 - y][1][0]);
            case 2 -> getIndex(this.connects[1][1][2], this.connects[1 + y][1][1],
                    this.connects[1 + y][1][2]);
            case 4 -> getIndex(this.connects[1][1][0], this.connects[1 + y][1][1],
                    this.connects[1 + y][1][0]);
            default -> -1;
        };
    }

    private int getIndexZ(EnumFacing face, int corner) {
        int z = face.getZOffset();
        return switch (corner) {
            case 0 -> getIndex(this.connects[1 - z][1][1], this.connects[1][2][1],
                    this.connects[1 - z][2][1]);
            case 1 -> getIndex(this.connects[1 + z][1][1], this.connects[1][2][1],
                    this.connects[1 + z][2][1]);
            case 2 -> getIndex(this.connects[1 - z][1][1], this.connects[1][0][1],
                    this.connects[1 - z][0][1]);
            case 4 -> getIndex(this.connects[1 + z][1][1], this.connects[1][0][1],
                    this.connects[1 + z][0][1]);
            default -> -1;
        };
    }

    private static int getIndex(boolean a, boolean b, boolean c) {
        if (!a && !b) {
            return 0;
        }
        if (a && b && !c) {
            return 1;
        }
        if (!a) {
            return 2;
        }
        if (!b) {
            return 3;
        }
        return -1;
    }

    @Override
    public String toString() {
        return "QuantumComputerConnect(face=" + this.face + ")";
    }

    @FunctionalInterface
    public interface NeighbourCheck {
        boolean isConnected(int x, int y, int z);
    }
}
