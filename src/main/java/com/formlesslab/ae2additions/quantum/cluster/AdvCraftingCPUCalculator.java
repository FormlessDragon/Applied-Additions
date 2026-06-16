package com.formlesslab.ae2additions.quantum.cluster;

import ae2.api.networking.IGrid;
import ae2.api.networking.IGridNode;
import ae2.api.networking.events.GridCraftingCpuChange;
import ae2.me.cluster.MBCalculator;
import com.formlesslab.ae2additions.quantum.AAECraftingUnitType;
import com.formlesslab.ae2additions.quantum.QuantumConfig;
import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AdvCraftingCPUCalculator extends MBCalculator<AdvCraftingBlockEntity, AdvCraftingCPUCluster> {
    public AdvCraftingCPUCalculator(AdvCraftingBlockEntity tile) {
        super(tile);
    }

    @Override
    public boolean checkMultiblockScale(BlockPos min, BlockPos max) {
        int maxDelta = QuantumConfig.getMaxSize() - 1;
        return max.getX() - min.getX() <= maxDelta
            && max.getY() - min.getY() <= maxDelta
            && max.getZ() - min.getZ() <= maxDelta;
    }

    @Override
    public AdvCraftingCPUCluster createCluster(World world, BlockPos min, BlockPos max) {
        return new AdvCraftingCPUCluster(min, max);
    }

    @Override
    public boolean verifyInternalStructure(World world, BlockPos min, BlockPos max) {
        boolean core = false;
        boolean storage = false;
        int entanglers = 0;
        int multiThreaders = 0;

        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            TileEntity tile = world.getTileEntity(pos);
            if (!(tile instanceof AdvCraftingBlockEntity quantumTile)) {
                return false;
            }
            if (!quantumTile.isValid()) {
                return false;
            }

            boolean boundary = isBoundary(pos, min, max);
            AAECraftingUnitType type = quantumTile.getQuantumUnitType();
            switch (type) {
                case QUANTUM_CORE:
                    if (min.equals(max)) {
                        return true;
                    }
                    if (boundary || core) {
                        return false;
                    }
                    core = true;
                    break;
                case QUANTUM_STRUCTURE:
                    if (!boundary) {
                        return false;
                    }
                    break;
                case DATA_ENTANGLER:
                    if (boundary || entanglers >= QuantumConfig.getMaxDataEntanglers()) {
                        return false;
                    }
                    entanglers++;
                    break;
                case QUANTUM_MULTI_THREADER:
                    if (boundary || multiThreaders >= QuantumConfig.getMaxMultiThreaders()) {
                        return false;
                    }
                    multiThreaders++;
                    break;
                default:
                    if (boundary) {
                        return false;
                    }
                    break;
            }

            storage |= quantumTile.getStorageBytes() > 0;
        }

        return core && storage;
    }

    @Override
    public void updateBlockEntities(AdvCraftingCPUCluster cluster, World world, BlockPos min, BlockPos max) {
        if (!(cluster instanceof AdvCraftingCPUCluster quantumCluster)) {
            return;
        }
        for (BlockPos pos : BlockPos.getAllInBox(min, max)) {
            TileEntity tile = world.getTileEntity(pos);
            if (tile instanceof AdvCraftingBlockEntity quantumTile) {
                quantumTile.updateStatus(quantumCluster);
                quantumCluster.addQuantumBlockEntity(quantumTile);
            }
        }

        quantumCluster.done();
        IteratorHelper.postFirstCpuChange(quantumCluster);
    }

    @Override
    public boolean isValidBlockEntity(TileEntity tile) {
        return tile instanceof AdvCraftingBlockEntity;
    }

    private static boolean isBoundary(BlockPos pos, BlockPos min, BlockPos max) {
        return pos.getX() == min.getX()
            || pos.getY() == min.getY()
            || pos.getZ() == min.getZ()
            || pos.getX() == max.getX()
            || pos.getY() == max.getY()
            || pos.getZ() == max.getZ();
    }

    private static final class IteratorHelper {
        private static void postFirstCpuChange(AdvCraftingCPUCluster cluster) {
            var iterator = cluster.getQuantumBlockEntities();
            while (iterator.hasNext()) {
                IGridNode node = iterator.next().getGridNode();
                if (node != null) {
                    IGrid grid = node.grid();
                    grid.postEvent(new GridCraftingCpuChange(node));
                    return;
                }
            }
        }
    }
}
