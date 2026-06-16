package com.formlesslab.ae2additions.container;

import ae2.container.guisync.GuiSync;
import ae2.container.implementations.UpgradeableContainer;
import com.formlesslab.ae2additions.tile.TileWirelessConnector;
import com.formlesslab.ae2additions.api.WirelessStatus;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.math.BlockPos;

public class ContainerWirelessConnector extends UpgradeableContainer<TileWirelessConnector> {
    private static final String ACTION_DISCONNECT = "disconnect";

    @GuiSync(7)
    public WirelessStatus status = WirelessStatus.UNCONNECTED;
    @GuiSync(8)
    public double powerUse = 0;
    @GuiSync(9)
    public int usedChannels = 0;
    @GuiSync(10)
    public int maxChannels = 0;
    @GuiSync(11)
    public boolean hasRemote = false;
    @GuiSync(12)
    public int remoteX = 0;
    @GuiSync(13)
    public int remoteY = 0;
    @GuiSync(14)
    public int remoteZ = 0;

    public ContainerWirelessConnector(InventoryPlayer ip, TileWirelessConnector host) {
        super(ip, host);
        this.registerClientAction(ACTION_DISCONNECT, this::disconnect);
    }

    @Override
    protected int getPlayerInventoryTop() {
        return 120;
    }

    @Override
    public void broadcastChanges() {
        if (this.isServerSide()) {
            TileWirelessConnector host = this.getHost();
            this.status = host.getWirelessStatus();
            this.powerUse = host.getPowerUse();
            this.usedChannels = host.getUsedChannels();
            this.maxChannels = host.getMaxChannels();
            BlockPos remote = host.getOtherSide();
            this.hasRemote = remote != null;
            if (remote != null) {
                this.remoteX = remote.getX();
                this.remoteY = remote.getY();
                this.remoteZ = remote.getZ();
            }
        }
        super.broadcastChanges();
    }

    public void disconnect() {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_DISCONNECT);
        } else {
            this.getHost().clearFrequency();
        }
    }
}
