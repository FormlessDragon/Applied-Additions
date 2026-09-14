package com.formlesslab.ae2additions.block.quantum;

import ae2.block.crafting.ICraftingUnitType;
import com.formlesslab.ae2additions.api.AAECraftingUnitType;
import com.formlesslab.ae2additions.client.model.QuantumComputerConnectProperty;
import com.formlesslab.ae2additions.client.util.QuantumComputerConnect;
import com.formlesslab.ae2additions.tile.TileAdvCraftingBlock;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;

public class BlockAAECraftingUnit extends BlockAAEAbstractCraftingUnit<TileAdvCraftingBlock> {
    public BlockAAECraftingUnit(ICraftingUnitType type) {
        super(type, TileAdvCraftingBlock.class);
        if (type == AAECraftingUnitType.QUANTUM_STRUCTURE || type == AAECraftingUnitType.QUANTUM_CORE) {
            this.setLightOpacity(0);
        }
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return this.type == AAECraftingUnitType.QUANTUM_STRUCTURE ? BlockRenderLayer.CUTOUT : super.getRenderLayer();
    }

    @Override
    public boolean canRenderInLayer(IBlockState state, BlockRenderLayer layer) {
        return this.type == AAECraftingUnitType.QUANTUM_STRUCTURE ? layer == BlockRenderLayer.CUTOUT || layer == BlockRenderLayer.TRANSLUCENT : super.canRenderInLayer(state, layer);
    }

    /**
     * AdvancedAE marks both the structure shell and the quantum core with noOcclusion: both render models that are
     * not solid full cubes, so neighbours must keep their faces.
     */
    private boolean occludes() {
        return this.type != AAECraftingUnitType.QUANTUM_STRUCTURE && this.type != AAECraftingUnitType.QUANTUM_CORE;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return this.occludes() && super.isOpaqueCube(state);
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return this.occludes() && super.isFullCube(state);
    }

    @Override
    public int getLightValue(IBlockState state) {
        return this.type == AAECraftingUnitType.QUANTUM_CORE && state.getValue(FORMED) && state.getValue(POWERED) ? 12 : 0;
    }

    @Override
    protected IUnlistedProperty<?>[] getUnlistedProperties() {
        return new IUnlistedProperty<?>[]{FORWARD, UP, STATE, QuantumComputerConnectProperty.INSTANCE};
    }

    @Override
    public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
        state = super.getExtendedState(state, world, pos);
        if (!(state instanceof IExtendedBlockState extended)) {
            return state;
        }

        QuantumComputerConnect connect = QuantumComputerConnect.from(pos, (x, y, z) -> {
            BlockPos otherPos = pos.add(x, y, z);
            if (world == null) {
                return false;
            }
            return shouldConnectTo(world.getBlockState(otherPos).getBlock());
        });
        return extended.withProperty(QuantumComputerConnectProperty.INSTANCE, connect);
    }

    /**
     * Connection rule for the formed shell model, matching AdvancedAE: the structure frame only connects to other
     * structure blocks. Faces toward internal units keep their glass pane so the units behind stay visible through
     * it; the internal units draw their own faces toward the shell.
     */
    private boolean shouldConnectTo(Block block) {
        return block instanceof BlockAAECraftingUnit && ((((BlockAAECraftingUnit) block).type == AAECraftingUnitType.QUANTUM_STRUCTURE) == (this.type == AAECraftingUnitType.QUANTUM_STRUCTURE));
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileAdvCraftingBlock tile = this.getTileEntity(world, pos);
        if (tile != null && tile.isFormed() && !player.isSneaking()) {
            if (!world.isRemote) {
                tile.onQuantumComputerActivated(player);
            }
            return true;
        }
        return super.onBlockActivated(world, pos, state, player, hand, side, hitX, hitY, hitZ);
    }
}
