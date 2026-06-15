package com.formlesslab.ae2additions.quantum.tile;

import ae2.api.implementations.IPowerChannelState;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridMultiblock;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.orientation.BlockOrientation;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.block.crafting.AbstractCraftingUnitBlock;
import ae2.block.crafting.ICraftingUnitType;
import ae2.core.definitions.AEBlocks;
import ae2.crafting.inv.ListCraftingInventory;
import ae2.me.cluster.implementations.CraftingCPUCluster;
import ae2.tile.crafting.ICraftingCPUTileEntity;
import ae2.tile.grid.AENetworkedTile;
import ae2.util.NullConfigManager;
import ae2.util.Platform;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.ModGuiHandler;
import com.formlesslab.ae2additions.quantum.AAECraftingUnitType;
import com.formlesslab.ae2additions.quantum.block.AAECraftingUnitBlock;
import com.formlesslab.ae2additions.quantum.cluster.AdvCraftingCPUCalculator;
import com.formlesslab.ae2additions.quantum.cluster.AdvCraftingCPUCluster;
import com.formlesslab.ae2additions.quantum.client.gui.QuantumComputerHost;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

public class AdvCraftingBlockEntity extends AENetworkedTile
    implements ICraftingCPUTileEntity, IPowerChannelState, IConfigurableObject, QuantumComputerHost {

    private final AdvCraftingCPUCalculator calc = new AdvCraftingCPUCalculator(this);
    private NBTTagCompound previousState;
    private boolean coreBlock;
    private AdvCraftingCPUCluster cluster;
    private ICraftingCPUTileEntity.ClientState clientState = ICraftingCPUTileEntity.ClientState.DEFAULT;

    public AdvCraftingBlockEntity() {
        this.getMainNode()
            .setFlags(GridFlags.MULTIBLOCK, GridFlags.REQUIRE_CHANNEL)
            .addService(IGridMultiblock.class, this::getMultiblockNodes);
    }

    @Override
    public ItemStack getItemFromTile() {
        if (this.world == null) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(this.getCraftingUnitType().getItemFromType());
    }

    @Override
    public void setName(String name) {
        this.setCustomName(name);
        if (this.cluster != null) {
            this.cluster.updateName();
            this.cluster.postCpuChange();
        }
    }

    public AbstractCraftingUnitBlock<?> getUnitBlock() {
        if (this.world == null || this.isInvalid()) {
            return AEBlocks.CRAFTING_UNIT.block();
        }
        Block block = this.world.getBlockState(this.pos).getBlock();
        return block instanceof AbstractCraftingUnitBlock
            ? (AbstractCraftingUnitBlock<?>) block
            : AEBlocks.CRAFTING_UNIT.block();
    }

    @Override
    public ICraftingUnitType getCraftingUnitType() {
        return this.getUnitBlock().type;
    }

    public AAECraftingUnitType getQuantumUnitType() {
        ICraftingUnitType type = this.getCraftingUnitType();
        return type instanceof AAECraftingUnitType ? (AAECraftingUnitType) type : AAECraftingUnitType.QUANTUM_UNIT;
    }

    public int getStorageMultiplier() {
        return this.getQuantumUnitType().getStorageMultiplier();
    }

    public int getAccelerationMultiplier() {
        return this.getQuantumUnitType().getAccelerationMultiplier();
    }

    @Override
    public void onReady() {
        super.onReady();
        this.getMainNode().setVisualRepresentation(this.getItemFromTile());
        if (this.world != null && !this.world.isRemote) {
            this.calc.calculateMultiblock(this.world, this.pos);
            this.recalculateDisplay();
        }
    }

    @Override
    public void updateMultiBlock(BlockPos changedPos) {
        if (this.world != null && !this.world.isRemote) {
            this.calc.updateMultiblockAfterNeighborUpdate(this.world, this.pos, changedPos);
        }
    }

    @Override
    public void updateStatus(CraftingCPUCluster cluster) {
        if (this.cluster != null && this.cluster != cluster) {
            this.cluster.breakCluster();
        }
        this.cluster = cluster instanceof AdvCraftingCPUCluster ? (AdvCraftingCPUCluster) cluster : null;
        this.updateSubType(true);
    }

    public void updateStatus(AdvCraftingCPUCluster cluster) {
        this.updateStatus((CraftingCPUCluster) cluster);
    }

    @Override
    public Set<EnumFacing> getGridConnectableSides(BlockOrientation orientation) {
        if (!this.isFormed()) {
            return EnumSet.noneOf(EnumFacing.class);
        }
        if (this.getQuantumUnitType() == AAECraftingUnitType.QUANTUM_CORE) {
            return EnumSet.of(EnumFacing.UP, EnumFacing.DOWN);
        }
        return EnumSet.allOf(EnumFacing.class);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return this.isFormed() ? AECableType.SMART : AECableType.NONE;
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setBoolean("core", this.isCoreBlock());
        if (this.isCoreBlock() && this.cluster != null) {
            NBTTagCompound clusterData = new NBTTagCompound();
            this.cluster.writeToNBT(clusterData);
            data.setTag("cluster", clusterData);
        }
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.setCoreBlock(data.getBoolean("core"));
        if (this.isCoreBlock()) {
            NBTTagCompound clusterData = data.hasKey("cluster", 10) ? data.getCompoundTag("cluster") : data;
            if (this.cluster != null) {
                this.cluster.readFromNBT(clusterData);
            } else {
                this.setPreviousState(clusterData.copy());
            }
        }
    }

    @Override
    public void disconnect(boolean update) {
        if (this.cluster != null) {
            this.cluster.destroy();
            if (update) {
                this.updateSubType(true);
            }
        }
    }

    @Override
    public AdvCraftingCPUCluster getCluster() {
        return this.cluster;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        if (reason != IGridNodeListener.State.GRID_BOOT) {
            this.updateSubType(false);
            if (this.cluster != null) {
                this.cluster.postCpuChange();
            }
        }
    }

    @Override
    public void breakCluster() {
        if (this.cluster == null) {
            return;
        }

        List<ListCraftingInventory> inventories = this.cluster.getInventories();
        List<BlockPos> places = new ObjectArrayList<>();
        Iterator<AdvCraftingBlockEntity> blockEntities = this.cluster.getQuantumBlockEntities();
        while (blockEntities.hasNext()) {
            AdvCraftingBlockEntity blockEntity = blockEntities.next();
            if (this == blockEntity) {
                places.add(this.pos);
            } else {
                for (EnumFacing side : EnumFacing.values()) {
                    BlockPos dropPos = blockEntity.pos.offset(side);
                    if (this.world.isAirBlock(dropPos)) {
                        places.add(dropPos);
                    }
                }
            }
        }

        if (places.isEmpty()) {
            places.add(this.pos);
        }

        for (ListCraftingInventory inventory : inventories) {
            for (var entry : inventory.list) {
                BlockPos dropPos = places.get(this.world.rand.nextInt(places.size()));
                List<ItemStack> drops = new ObjectArrayList<>();
                entry.getKey().addDrops(entry.getLongValue(), drops, this.world, dropPos);
                Platform.spawnDrops(this.world, dropPos, drops);
            }
            inventory.clear();
        }

        this.cluster.destroy();
    }

    @Override
    public void cancelJobAndDropContents() {
        this.breakCluster();
    }

    @Override
    public void updateSubType(boolean updateFormed) {
        if (this.world == null || this.isInvalid()) {
            return;
        }

        boolean formed = this.isFormed();
        boolean powered = this.getMainNode().isOnline();
        IBlockState current = this.world.getBlockState(this.pos);
        IBlockState changed = setBooleanProperty(current, "powered", powered);
        changed = setBooleanProperty(changed, "formed", formed);

        if (current != changed) {
            this.world.setBlockState(this.pos, changed, 2);
        }
        if (updateFormed) {
            this.onGridConnectableSidesChanged();
        }
        this.recalculateDisplay();
    }

    @Override
    public boolean isActive() {
        if (this.world == null || !this.world.isRemote) {
            return this.getMainNode().isActive();
        }
        return this.isPowered() && this.isFormed();
    }

    @Override
    public boolean isCoreBlock() {
        return this.coreBlock;
    }

    @Override
    public void setCoreBlock(boolean coreBlock) {
        this.coreBlock = coreBlock;
    }

    @Override
    public NBTTagCompound getPreviousState() {
        return this.previousState;
    }

    @Override
    public void setPreviousState(NBTTagCompound previousState) {
        this.previousState = previousState;
    }

    @Override
    public boolean isFormed() {
        if (this.world != null && this.world.isRemote) {
            return this.clientState.formed();
        }
        return this.cluster != null;
    }

    @Override
    public boolean isPowered() {
        if (this.world != null && this.world.isRemote) {
            return this.clientState.powered();
        }
        return this.getMainNode().isActive();
    }

    @Override
    public ICraftingCPUTileEntity.ClientState getRenderState() {
        if (this.world != null && !this.world.isRemote) {
            return this.createRenderState();
        }
        return this.clientState;
    }

    @Override
    public IConfigManager getConfigManager() {
        return this.cluster != null ? this.cluster.getConfigManager() : NullConfigManager.INSTANCE;
    }

    public boolean isMultiblocked() {
        return this.cluster != null && this.cluster.numBlockEntities() > 1;
    }

    public void onQuantumComputerActivated(EntityPlayer player) {
        if (this.world != null && !this.world.isRemote) {
            player.openGui(AppliedAdditions.INSTANCE, ModGuiHandler.QUANTUM_COMPUTER, this.world,
                this.pos.getX(), this.pos.getY(), this.pos.getZ());
        }
    }

    @Override
    public java.util.List<? extends ae2.api.networking.crafting.ICraftingCPU> getQuantumCpus() {
        if (this.cluster == null) {
            return java.util.Collections.emptyList();
        }
        java.util.List<ae2.api.networking.crafting.ICraftingCPU> cpus = new java.util.ArrayList<>(this.cluster.getActiveCPUs());
        cpus.add(this.cluster.getRemainingCapacityCPU());
        return cpus;
    }

    @Override
    public ae2.api.config.CpuSelectionMode getQuantumSelectionMode() {
        return this.cluster == null
            ? ae2.api.config.CpuSelectionMode.ANY
            : this.cluster.getSelectionMode();
    }

    @Override
    public void setQuantumSelectionMode(ae2.api.config.CpuSelectionMode mode) {
        if (this.cluster != null) {
            this.cluster.getConfigManager().putSetting(ae2.api.config.Settings.CPU_SELECTION_MODE, mode);
            this.cluster.postCpuChange();
        } else {
            this.getConfigManager().putSetting(ae2.api.config.Settings.CPU_SELECTION_MODE, mode);
        }
    }

    @Override
    public void onChunkUnloaded() {
        if (this.cluster != null) {
            this.cluster.cancelJobs();
        }
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        if (this.cluster != null) {
            this.cluster.destroy();
        }
        super.setRemoved();
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        ICraftingCPUTileEntity.ClientState state = this.getRenderState();
        data.writeBoolean(state.formed());
        data.writeBoolean(state.powered());
        data.writeByte(encodeConnections(state.connections()));
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        ICraftingCPUTileEntity.ClientState state = new ICraftingCPUTileEntity.ClientState(
            data.readBoolean(),
            data.readBoolean(),
            decodeConnections(data.readUnsignedByte())
        );
        if (!state.equals(this.clientState)) {
            this.clientState = state;
            return true;
        }
        return changed;
    }

    protected void recalculateDisplay() {
        if (this.world == null || this.world.isRemote) {
            return;
        }
        ICraftingCPUTileEntity.ClientState state = this.createRenderState();
        if (!state.equals(this.clientState)) {
            this.clientState = state;
            this.markForUpdate();
        }
    }

    private ICraftingCPUTileEntity.ClientState createRenderState() {
        return new ICraftingCPUTileEntity.ClientState(this.isFormed(), this.isPowered(), this.getConnections());
    }

    private EnumSet<EnumFacing> getConnections() {
        EnumSet<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);
        if (this.world == null) {
            return connections;
        }
        AAECraftingUnitType ownType = this.getQuantumUnitType();
        for (EnumFacing side : EnumFacing.values()) {
            Block block = this.world.getBlockState(this.pos.offset(side)).getBlock();
            if (block instanceof AAECraftingUnitBlock quantumBlock
                && quantumBlock.type instanceof AAECraftingUnitType neighborType
                && ownType.isBoundaryOnly() == neighborType.isBoundaryOnly()) {
                connections.add(side);
            }
        }
        return connections;
    }

    private Iterator<IGridNode> getMultiblockNodes() {
        if (this.cluster == null) {
            return java.util.Collections.emptyIterator();
        }
        List<IGridNode> nodes = new ObjectArrayList<>();
        Iterator<AdvCraftingBlockEntity> blockEntities = this.cluster.getQuantumBlockEntities();
        while (blockEntities.hasNext()) {
            IGridNode node = blockEntities.next().getGridNode();
            if (node != null) {
                nodes.add(node);
            }
        }
        return nodes.iterator();
    }

    private static int encodeConnections(EnumSet<EnumFacing> connections) {
        int encoded = 0;
        for (EnumFacing side : connections) {
            encoded |= 1 << side.getIndex();
        }
        return encoded;
    }

    private static EnumSet<EnumFacing> decodeConnections(int encoded) {
        EnumSet<EnumFacing> connections = EnumSet.noneOf(EnumFacing.class);
        for (EnumFacing side : EnumFacing.values()) {
            if ((encoded & (1 << side.getIndex())) != 0) {
                connections.add(side);
            }
        }
        return connections;
    }

    private static IBlockState setBooleanProperty(IBlockState state, String name, boolean value) {
        for (IProperty<?> property : state.getPropertyKeys()) {
            if (property instanceof PropertyBool && property.getName().equals(name)) {
                @SuppressWarnings("unchecked")
                IProperty<Boolean> boolProperty = (IProperty<Boolean>) property;
                return state.withProperty(boolProperty, value);
            }
        }
        return state;
    }
}
