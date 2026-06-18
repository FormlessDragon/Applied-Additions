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
import com.formlesslab.ae2additions.api.WirelessEndpoint;
import com.formlesslab.ae2additions.api.WirelessNode;
import com.formlesslab.ae2additions.api.WirelessStatus;
import com.formlesslab.ae2additions.init.ModConfig;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.wireless.WirelessConnection;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TileWirelessConnector extends AENetworkedTile
    implements ServerTickingTile, IUpgradeableObject, IColorableBlockEntity, WirelessNode, WirelessEndpoint {

    private boolean updateStatus = true;
    private long frequency;
    private double powerUse = 1.0;
    private AEColor color = AEColor.TRANSPARENT;
    private boolean clientConnected;
    private final WirelessConnection connection = new WirelessConnection(this);
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
        Item.getItemFromBlock(ModContent.WIRELESS_CONNECTOR), 4, this::onUpgradesChanged);

    public TileWirelessConnector() {
        this.getMainNode().setFlags(GridFlags.DENSE_CAPACITY);
        this.getMainNode().setIdlePowerUsage(this.powerUse);
        this.getMainNode().setGridColor(this.color);
    }

    @Override
    public void serverTick() {
        if (this.updateStatus) {
            this.updateStatus = false;
            this.connection.updateStatus();
            this.updatePowerUsage();
            this.markForUpdate();
            this.reactive();
        }
    }

    @Override
    public void onMainNodeStateChanged(IGridNodeListener.State reason) {
        this.updateStatus = true;
    }

    @Override
    public void onReady() {
        super.onReady();
        this.updateStatus = true;
    }

    @Override
    public void onChunkUnloaded() {
        this.disconnect();
        super.onChunkUnloaded();
    }

    @Override
    public void setRemoved() {
        this.disconnect();
        super.setRemoved();
    }

    @Override
    public void loadTag(NBTTagCompound data) {
        super.loadTag(data);
        this.frequency = data.getLong("freq");
        this.upgrades.readFromNBT(data, "upgrades");
        if (data.hasKey("color", 8)) {
            try {
                this.color = AEColor.valueOf(data.getString("color"));
            } catch (IllegalArgumentException ignored) {
                this.color = AEColor.TRANSPARENT;
            }
        }
        this.getMainNode().setGridColor(this.color);
        WirelessConnection.FREQUENCIES.markUsed(this.frequency);
        this.updateStatus = true;
    }

    @Override
    public void saveAdditional(NBTTagCompound data) {
        super.saveAdditional(data);
        data.setLong("freq", this.frequency);
        this.upgrades.writeToNBT(data, "upgrades");
        data.setString("color", this.color.name());
        WirelessConnection.FREQUENCIES.markUsed(this.frequency);
    }

    @Override
    public ItemStack getItemFromTile() {
        return new ItemStack(ModContent.WIRELESS_CONNECTOR);
    }

    @Override
    public AECableType getCableConnectionType(EnumFacing dir) {
        return AECableType.DENSE_COVERED;
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
        return 0;
    }

    @Override
    public void setFrequency(long frequency, int port) {
        this.frequency = frequency;
        this.updateStatus = true;
        this.saveChanges();
    }

    public void clearFrequency() {
        this.frequency = 0;
        this.disconnect();
        this.connection.active();
        this.updateStatus = true;
        this.updatePowerUsage();
        this.saveChanges();
        this.markForUpdate();
    }

    @Override
    public long getNewFrequency() {
        return WirelessConnection.FREQUENCIES.next();
    }

    public void reactive() {
        this.connection.active();
    }

    public void disconnect() {
        this.connection.destroy();
    }

    public void breakOnRemove() {
        this.disconnect();
    }

    public boolean isConnected() {
        return this.connection.isConnected();
    }

    public boolean isConnectedForRendering() {
        return this.getWorld() != null && this.getWorld().isRemote ? this.clientConnected : this.isConnected();
    }

    public double getPowerUse() {
        return this.powerUse;
    }

    public WirelessStatus getWirelessStatus() {
        if (this.frequency == 0) {
            return WirelessStatus.UNCONNECTED;
        }
        IGridNode node = this.getMainNode().getNode();
        if (node != null && !node.isPowered()) {
            return WirelessStatus.NO_POWER;
        }
        return this.isConnected() ? WirelessStatus.WORKING : WirelessStatus.REMOTE_ERROR;
    }

    @Nullable
    public BlockPos getOtherSide() {
        return this.connection.getOtherSide();
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
    public long getFrequency() {
        return this.frequency;
    }

    @Override
    public World getWirelessWorld() {
        return this.getWorld();
    }

    @Override
    public BlockPos getWirelessPos() {
        return this.getPos();
    }

    @Override
    public IGridNode getWirelessGridNode() {
        return this.getMainNode().getNode();
    }

    @Override
    public TileEntity getWirelessTile() {
        return this;
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
        if (this.connection.isConnected()) {
            double distance = Math.max(this.connection.getDistance(), Math.E);
            this.powerUse = Math.max(1.0, distance * Math.log(distance) * discount)
                * ModConfig.wirelessConnectorPowerMultiplier;
        } else {
            this.powerUse = ModConfig.wirelessConnectorPowerMultiplier;
        }
        this.getMainNode().setIdlePowerUsage(this.powerUse);
    }
}
