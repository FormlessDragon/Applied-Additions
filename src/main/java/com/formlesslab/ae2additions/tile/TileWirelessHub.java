package com.formlesslab.ae2additions.tile;

import ae2.api.implementations.blockentities.IColorableBlockEntity;
import ae2.api.networking.GridFlags;
import ae2.api.networking.IGridNode;
import ae2.api.networking.IGridNodeListener;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.util.AECableType;
import ae2.api.util.AEColor;
import ae2.core.definitions.AEItems;
import ae2.tile.ServerTickingTile;
import ae2.tile.grid.AENetworkedTile;
import com.formlesslab.ae2additions.init.ModConfig;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.wireless.WirelessConnection;
import com.formlesslab.ae2additions.api.WirelessEndpoint;
import com.formlesslab.ae2additions.api.WirelessNode;
import com.formlesslab.ae2additions.api.WirelessStatus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import io.netty.buffer.ByteBuf;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class TileWirelessHub extends AENetworkedTile
    implements ServerTickingTile, IUpgradeableObject, IColorableBlockEntity, WirelessEndpoint {

    public static final int MAX_PORTS = 8;
    private final boolean[] updateStatus = new boolean[MAX_PORTS];
    private final long[] frequencies = new long[MAX_PORTS];
    private final WirelessConnection[] connections = new WirelessConnection[MAX_PORTS];
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
        Item.getItemFromBlock(ModContent.WIRELESS_HUB), 4, this::onUpgradesChanged);
    private double powerUse = 1.0;
    private AEColor color = AEColor.TRANSPARENT;
    private boolean clientConnected;

    public TileWirelessHub() {
        this.getMainNode().setFlags(GridFlags.DENSE_CAPACITY);
        this.getMainNode().setIdlePowerUsage(this.powerUse);
        this.getMainNode().setGridColor(this.color);
        Arrays.fill(this.updateStatus, true);
        for (int i = 0; i < MAX_PORTS; i++) {
            this.connections[i] = new WirelessConnection(new PortNode(this, i));
        }
    }

    @Override
    public void serverTick() {
        boolean changed = false;
        for (int i = 0; i < MAX_PORTS; i++) {
            if (this.updateStatus[i]) {
                this.updateStatus[i] = false;
                this.connections[i].updateStatus();
                this.reactive(i);
                changed = true;
            }
        }
        if (changed) {
            this.updatePowerUsage();
            this.markForUpdate();
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        Arrays.fill(this.updateStatus, true);
    }

    @Override
    public void onReady() {
        super.onReady();
        Arrays.fill(this.updateStatus, true);
    }

    @Override
    public void onChunkUnloaded() {
        this.disconnectAll();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        this.disconnectAll();
        super.setRemoved();
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.upgrades.readFromNBT(data, "upgrades");
        if (data.hasKey("freq")) {
            this.frequencies[0] = data.getLong("freq");
            WirelessConnection.FREQUENCIES.markUsed(this.frequencies[0]);
        } else {
            for (int i = 0; i < MAX_PORTS; i++) {
                this.frequencies[i] = data.getLong("freq" + i);
                WirelessConnection.FREQUENCIES.markUsed(this.frequencies[i]);
            }
        }
        if (data.hasKey("color", 8)) {
            try {
                this.color = AEColor.valueOf(data.getString("color"));
            } catch (IllegalArgumentException ignored) {
                this.color = AEColor.TRANSPARENT;
            }
        }
        this.getMainNode().setGridColor(this.color);
        Arrays.fill(this.updateStatus, true);
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        this.upgrades.writeToNBT(data, "upgrades");
        for (int i = 0; i < MAX_PORTS; i++) {
            data.setLong("freq" + i, this.frequencies[i]);
            WirelessConnection.FREQUENCIES.markUsed(this.frequencies[i]);
        }
        data.setString("color", this.color.name());
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(ModContent.WIRELESS_HUB);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.DENSE_SMART;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops) {
        super.addAdditionalDrops(drops);
        for (ItemStack upgrade : this.upgrades) {
            if (!upgrade.isEmpty()) {
                drops.add(upgrade);
            }
        }
    }

    @Override
    public void clearContent() {
        super.clearContent();
        this.upgrades.clear();
    }

    @Override
    public AEColor getColor() {
        return this.color;
    }

    @Override
    public boolean recolourBlock(EnumFacing side, AEColor colour, EntityPlayer who) {
        if (colour == this.color) {
            return false;
        }
        this.color = colour;
        this.getMainNode().setGridColor(this.color);
        this.saveChanges();
        this.markForUpdate();
        return true;
    }

    @Override
    public int allocatePort() {
        for (int i = 0; i < MAX_PORTS; i++) {
            if (!this.connections[i].isConnected()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void setFrequency(long frequency, int port) {
        if (!isValidPort(port)) {
            return;
        }
        this.frequencies[port] = frequency;
        this.updateStatus[port] = true;
        this.saveChanges();
    }

    @Override
    public long getNewFrequency() {
        return WirelessConnection.FREQUENCIES.next();
    }

    public void killPort(int port) {
        if (!isValidPort(port)) {
            return;
        }
        this.frequencies[port] = 0;
        this.connections[port].destroy();
        this.connections[port] = new WirelessConnection(new PortNode(this, port));
        this.updateStatus[port] = true;
        this.updatePowerUsage();
        this.saveChanges();
        this.markForUpdate();
    }

    public void reactive(int port) {
        if (isValidPort(port)) {
            this.connections[port].active();
        }
    }

    public void reactiveAll() {
        for (int i = 0; i < MAX_PORTS; i++) {
            this.reactive(i);
        }
    }

    public void disconnectAll() {
        for (WirelessConnection connection : this.connections) {
            connection.destroy();
        }
    }

    public void breakOnRemove() {
        this.disconnectAll();
    }

    public boolean isConnected() {
        for (WirelessConnection connection : this.connections) {
            if (connection.isConnected()) {
                return true;
            }
        }
        return false;
    }

    public boolean isConnectedForRendering() {
        return this.getWorld() != null && this.getWorld().isRemote ? this.clientConnected : this.isConnected();
    }

    public boolean isPortConnected(int port) {
        return isValidPort(port) && this.connections[port].isConnected();
    }

    public long getFrequency(int port) {
        return isValidPort(port) ? this.frequencies[port] : 0;
    }

    public WirelessStatus getWirelessStatus(int port) {
        if (!isValidPort(port) || this.frequencies[port] == 0) {
            return WirelessStatus.UNCONNECTED;
        }
        IGridNode node = this.getMainNode().getNode();
        if (node != null && !node.isPowered()) {
            return WirelessStatus.NO_POWER;
        }
        return this.connections[port].isConnected() ? WirelessStatus.WORKING : WirelessStatus.REMOTE_ERROR;
    }

    @Nullable
    public BlockPos getOtherSide(int port) {
        return isValidPort(port) ? this.connections[port].getOtherSide() : null;
    }

    public double getPowerUse() {
        return this.powerUse;
    }

    public int getUsedChannels() {
        IGridNode node = this.getMainNode().getNode();
        return node != null ? node.getUsedChannels() : 0;
    }

    public int getMaxChannels() {
        IGridNode node = this.getMainNode().getNode();
        return node != null ? node.getMaxChannels() : 0;
    }

    @Override
    public World getEndpointWorld() {
        return this.getWorld();
    }

    @Override
    public BlockPos getEndpointPos() {
        return this.getPos();
    }

    @Override
    public AEColor getEndpointColor() {
        return this.color;
    }

    @Override
    protected void writeToStream(ByteBuf data) {
        super.writeToStream(data);
        data.writeByte(this.color.ordinal());
        data.writeBoolean(this.isConnected());
    }

    @Override
    protected boolean readFromStream(ByteBuf data) {
        boolean changed = super.readFromStream(data);
        AEColor oldColor = this.color;
        boolean oldConnected = this.clientConnected;

        int colorOrdinal = data.readUnsignedByte();
        this.color = colorOrdinal >= 0 && colorOrdinal < AEColor.values().length
            ? AEColor.values()[colorOrdinal]
            : AEColor.TRANSPARENT;
        this.clientConnected = data.readBoolean();
        this.getMainNode().setGridColor(this.color);

        return changed || oldColor != this.color || oldConnected != this.clientConnected;
    }

    private void onUpgradesChanged() {
        this.updatePowerUsage();
        this.saveChanges();
    }

    private void updatePowerUsage() {
        double discount = 1.0 - 0.1 * this.upgrades.getInstalledUpgrades(AEItems.ENERGY_CARD.item());
        boolean anyRunning = false;
        this.powerUse = 0;
        for (WirelessConnection connection : this.connections) {
            if (connection.isConnected()) {
                double distance = Math.max(connection.getDistance(), Math.E);
                this.powerUse += Math.max(1.0, distance * Math.log(distance) * discount)
                    * ModConfig.wirelessConnectorPowerMultiplier;
                anyRunning = true;
            }
        }
        if (!anyRunning) {
            this.powerUse = ModConfig.wirelessConnectorPowerMultiplier;
        }
        this.getMainNode().setIdlePowerUsage(this.powerUse);
    }

    private boolean isValidPort(int port) {
        return port >= 0 && port < MAX_PORTS;
    }

    private static final class PortNode implements WirelessNode {
        private final TileWirelessHub hub;
        private final int port;

        private PortNode(TileWirelessHub hub, int port) {
            this.hub = hub;
            this.port = port;
        }

        @Override
        public long getFrequency() {
            return this.hub.frequencies[this.port];
        }

        @Override
        public World getWirelessWorld() {
            return this.hub.getWorld();
        }

        @Override
        public BlockPos getWirelessPos() {
            return this.hub.getPos();
        }

        @Override
        public IGridNode getWirelessGridNode() {
            return this.hub.getMainNode().getNode();
        }

        @Override
        public TileEntity getWirelessTile() {
            return this.hub;
        }
    }
}
