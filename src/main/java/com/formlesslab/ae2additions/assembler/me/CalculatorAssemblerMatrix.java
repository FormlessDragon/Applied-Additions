package com.formlesslab.ae2additions.assembler.me;

import ae2.me.cluster.IAEMultiBlock;
import ae2.me.cluster.MBCalculator;
import com.formlesslab.ae2additions.assembler.AssemblerMatrixConfig;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixBase;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixCrafter;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixFrame;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixFunction;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixPattern;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixWall;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CalculatorAssemblerMatrix extends MBCalculator<TileAssemblerMatrixBase, ClusterAssemblerMatrix> {
    private static final int MIN_SIZE = 3;

    public CalculatorAssemblerMatrix(TileAssemblerMatrixBase tile) {
        super(tile);
    }

    @Override
    public boolean checkMultiblockScale(BlockPos min, BlockPos max) {
        int xSize = max.getX() - min.getX() + 1;
        int ySize = max.getY() - min.getY() + 1;
        int zSize = max.getZ() - min.getZ() + 1;
        int maxSize = AssemblerMatrixConfig.assemblerMatrixMaxSize;
        return xSize >= MIN_SIZE && ySize >= MIN_SIZE && zSize >= MIN_SIZE
            && xSize <= maxSize && ySize <= maxSize && zSize <= maxSize;
    }

    @Override
    public ClusterAssemblerMatrix createCluster(World world, BlockPos min, BlockPos max) {
        return new ClusterAssemblerMatrix(min, max);
    }

    @Override
    public boolean verifyInternalStructure(World world, BlockPos min, BlockPos max) {
        boolean anyPattern = false;
        boolean anyCrafter = false;

        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            TileEntity te = world.getTileEntity(pos);
            if (!(te instanceof IAEMultiBlock) || !((IAEMultiBlock<?>) te).isValid()) {
                return false;
            }

            if (te instanceof TileAssemblerMatrixPattern) {
                anyPattern = true;
            }
            if (te instanceof TileAssemblerMatrixCrafter) {
                anyCrafter = true;
            }

            if (isInternal(pos, min, max)) {
                if (!(te instanceof TileAssemblerMatrixFunction)) {
                    return false;
                }
            } else if (isEdge(pos, min, max)) {
                if (!(te instanceof TileAssemblerMatrixFrame)) {
                    return false;
                }
            } else if (!(te instanceof TileAssemblerMatrixWall)) {
                return false;
            }
        }

        return anyCrafter && anyPattern;
    }

    @Override
    public void updateBlockEntities(ClusterAssemblerMatrix cluster, World world, BlockPos min, BlockPos max) {
        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileAssemblerMatrixBase matrixTile) {
                matrixTile.updateStatus(cluster);
                cluster.addTileEntity(matrixTile);
            }
        }
        cluster.done();
    }

    @Override
    public boolean isValidBlockEntity(TileEntity te) {
        return te instanceof TileAssemblerMatrixBase;
    }

    private static boolean isInternal(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() > min.getX() && pos.getX() < max.getX()
            && pos.getY() > min.getY() && pos.getY() < max.getY()
            && pos.getZ() > min.getZ() && pos.getZ() < max.getZ();
    }

    private static boolean isEdge(BlockPos pos, BlockPos min, BlockPos max) {
        int boundaryAxes = 0;
        if (pos.getX() == min.getX() || pos.getX() == max.getX()) {
            boundaryAxes++;
        }
        if (pos.getY() == min.getY() || pos.getY() == max.getY()) {
            boundaryAxes++;
        }
        if (pos.getZ() == min.getZ() || pos.getZ() == max.getZ()) {
            boundaryAxes++;
        }
        return boundaryAxes >= 2;
    }
}
