package com.formlesslab.ae2additions.tile;

import ae2.api.config.CpuSelectionMode;
import ae2.api.config.Settings;
import ae2.api.implementations.IPowerChannelState;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridMultiblock;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.networking.crafting.ICraftingCPU;
import ae2.api.orientation.BlockOrientation;
import ae2.api.util.AECableType;
import ae2.api.util.IConfigManager;
import ae2.api.util.IConfigurableObject;
import ae2.block.crafting.ICraftingUnitType;
import ae2.crafting.inv.ListCraftingInventory;
import ae2.me.cluster.IAEMultiBlock;
import ae2.tile.crafting.ICraftingCPUTileEntity;
import ae2.tile.grid.AENetworkedTile;
import ae2.util.NullConfigManager;
import ae2.util.Platform;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.ModGuiHandler;
import com.formlesslab.ae2additions.api.AAECraftingUnitType;
import com.formlesslab.ae2additions.api.QuantumComputerHost;
import com.formlesslab.ae2additions.block.quantum.BlockAAEAbstractCraftingUnit;
import com.formlesslab.ae2additions.block.quantum.BlockAAECraftingUnit;
import com.formlesslab.ae2additions.init.QuantumContent;
import com.formlesslab.ae2additions.me.cluster.AdvCraftingCPUCalculator;
import com.formlesslab.ae2additions.me.cluster.AdvCraftingCPUCluster;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class TileAdvCraftingBlock extends AENetworkedTile
    implements IAEMultiBlock<AdvCraftingCPUCluster>, IPowerChannelState, IConfigurableObject, QuantumComputerHost {

    private final AdvCraftingCPUCalculator calc = new AdvCraftingCPUCalculator(this);
    private NBTTagCompound previousState;
    private boolean coreBlock;
    private AdvCraftingCPUCluster cluster;
    private ICraftingCPUTileEntity.ClientState clientState = ICraftingCPUTileEntity.ClientState.DEFAULT;

    public TileAdvCraftingBlock() {
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

    public void setName(String name) {
        this.setCustomName(name);
        if (this.cluster != null) {
            this.cluster.updateName();
            this.cluster.postCpuChange();
        }
    }

    public BlockAAEAbstractCraftingUnit<?> getUnitBlock() {
        if (this.world == null || this.isInvalid()) {
            return QuantumContent.getBlock(AAECraftingUnitType.QUANTUM_UNIT);
        }
        Block block = this.world.getBlockState(this.pos).getBlock();
        return block instanceof BlockAAEAbstractCraftingUnit<?>
            ? (BlockAAEAbstractCraftingUnit<?>) block
            : QuantumContent.getBlock(AAECraftingUnitType.QUANTUM_UNIT);
    }

    public ICraftingUnitType getCraftingUnitType() {
        return this.getUnitBlock().type;
    }

    public AAECraftingUnitType getQuantumUnitType() {
        ICraftingUnitType type = this.getCraftingUnitType();
        return type instanceof AAECraftingUnitType ? (AAECraftingUnitType) type : AAECraftingUnitType.QUANTUM_UNIT;
    }

    public long getStorageBytes() {
        return this.getQuantumUnitType().getStorageBytes();
    }

    public int getStorageMultiplier() {
        return this.getQuantumUnitType().getStorageMultiplier();
    }

    public int getAcceleratorThreads() {
        return this.getQuantumUnitType().getAcceleratorThreads();
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

    public void updateMultiBlock(BlockPos changedPos) {
        if (this.world != null && !this.world.isRemote) {
            this.calc.updateMultiblockAfterNeighborUpdate(this.world, this.pos, changedPos);
        }
    }

    public void updateStatus(AdvCraftingCPUCluster cluster) {
        if (this.cluster != null && this.cluster != cluster) {
            this.cluster.breakCluster();
        }
        this.cluster = cluster;
        this.updateSubType(true);
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
        return this.isFormed() ? AECableType.COVERED : AECableType.NONE;
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

    public void breakCluster() {
        if (this.cluster == null) {
            return;
        }

        List<ListCraftingInventory> inventories = this.cluster.getInventories();
        List<BlockPos> places = new ObjectArrayList<>();
        Iterator<TileAdvCraftingBlock> blockEntities = this.cluster.getQuantumBlockEntities();
        while (blockEntities.hasNext()) {
            TileAdvCraftingBlock blockEntity = blockEntities.next();
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

    public boolean isCoreBlock() {
        return this.coreBlock;
    }

    public void setCoreBlock(boolean coreBlock) {
        this.coreBlock = coreBlock;
    }

    public NBTTagCompound getPreviousState() {
        return this.previousState;
    }

    public void setPreviousState(NBTTagCompound previousState) {
        this.previousState = previousState;
    }

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

    public void onQuantumComputerActivated(EntityPlayer player) {
        if (this.world != null && !this.world.isRemote) {
            player.openGui(AppliedAdditions.INSTANCE, ModGuiHandler.QUANTUM_COMPUTER, this.world,
                this.pos.getX(), this.pos.getY(), this.pos.getZ());
        }
    }

    @Override
    public List<? extends ICraftingCPU> getQuantumCpus() {
        if (this.cluster == null) {
            return Collections.emptyList();
        }
        List<ICraftingCPU> cpus = new ArrayList<>(this.cluster.getActiveCPUs());
        cpus.add(this.cluster.getRemainingCapacityCPU());
        return cpus;
    }

    @Override
    public CpuSelectionMode getQuantumSelectionMode() {
        return this.cluster == null
            ? CpuSelectionMode.ANY
            : this.cluster.getSelectionMode();
    }

    @Override
    public void setQuantumSelectionMode(CpuSelectionMode mode) {
        if (this.cluster != null) {
            this.cluster.getConfigManager().putSetting(Settings.CPU_SELECTION_MODE, mode);
            this.cluster.postCpuChange();
        } else {
            this.getConfigManager().putSetting(Settings.CPU_SELECTION_MODE, mode);
        }
    }

    @Override
    public ICraftingCPU getLastSelectedQuantumCpu() {
        return this.cluster == null ? null : this.cluster.getLastSelectedCpu();
    }

    @Override
    public void setLastSelectedQuantumCpu(ICraftingCPU cpu) {
        if (this.cluster != null) {
            this.cluster.setLastSelectedCpu(cpu);
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
            if (block instanceof BlockAAECraftingUnit quantumBlock
                && quantumBlock.type instanceof AAECraftingUnitType neighborType
                && ownType.isBoundaryOnly() == neighborType.isBoundaryOnly()) {
                connections.add(side);
            }
        }
        return connections;
    }

    private Iterator<IGridNode> getMultiblockNodes() {
        if (this.cluster == null) {
            return Collections.emptyIterator();
        }
        List<IGridNode> nodes = new ObjectArrayList<>();
        Iterator<TileAdvCraftingBlock> blockEntities = this.cluster.getQuantumBlockEntities();
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
