package com.formlesslab.ae2additions.container;

import ae2.container.guisync.GuiSync;
import ae2.container.guisync.PacketWritable;
import ae2.container.implementations.UpgradeableContainer;
import ae2.me.helpers.IGridConnectedTile;
import com.formlesslab.ae2additions.tile.TileWirelessConnector;
import com.formlesslab.ae2additions.tile.TileWirelessHub;
import com.formlesslab.ae2additions.api.WirelessStatus;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;

import java.util.Arrays;

public class ContainerWirelessHub extends UpgradeableContainer<TileWirelessHub> {
    private static final String ACTION_DISCONNECT_PORT = "disconnectPort";

    @GuiSync(7)
    public double powerUse = 0;
    @GuiSync(8)
    public int usedChannels = 0;
    @GuiSync(9)
    public int maxChannels = 0;
    @GuiSync(10)
    public PortState ports = PortState.empty();

    public ContainerWirelessHub(InventoryPlayer ip, TileWirelessHub host) {
        super(ip, host);
        this.registerClientAction(ACTION_DISCONNECT_PORT, Integer.class, this::disconnectPort);
    }

    @Override
    protected int getPlayerInventoryTop() {
        return 185;
    }

    @Override
    public void broadcastChanges() {
        if (this.isServerSide()) {
            TileWirelessHub host = this.getHost();
            this.powerUse = host.getPowerUse();
            this.usedChannels = host.getUsedChannels();
            this.maxChannels = host.getMaxChannels();
            this.ports = PortState.fromHost(host);
        }
        super.broadcastChanges();
    }

    public void disconnectPort(int port) {
        if (this.isClientSide()) {
            this.sendClientAction(ACTION_DISCONNECT_PORT, port);
        } else {
            this.getHost().killPort(port);
        }
    }

    public static class PortState implements PacketWritable {
        private final WirelessStatus[] statuses = new WirelessStatus[TileWirelessHub.MAX_PORTS];
        private final boolean[] hasRemote = new boolean[TileWirelessHub.MAX_PORTS];
        private final int[] remoteX = new int[TileWirelessHub.MAX_PORTS];
        private final int[] remoteY = new int[TileWirelessHub.MAX_PORTS];
        private final int[] remoteZ = new int[TileWirelessHub.MAX_PORTS];
        private final int[] remoteChannels = new int[TileWirelessHub.MAX_PORTS];

        private PortState() {
            Arrays.fill(this.statuses, WirelessStatus.UNCONNECTED);
        }

        public static PortState empty() {
            return new PortState();
        }

        public static PortState fromHost(TileWirelessHub host) {
            PortState state = new PortState();
            for (int i = 0; i < TileWirelessHub.MAX_PORTS; i++) {
                state.statuses[i] = host.getWirelessStatus(i);
                BlockPos remote = host.getOtherSide(i);
                state.hasRemote[i] = remote != null;
                if (remote != null) {
                    state.remoteX[i] = remote.getX();
                    state.remoteY[i] = remote.getY();
                    state.remoteZ[i] = remote.getZ();
                    state.remoteChannels[i] = getRemoteChannels(host, remote);
                }
            }
            return state;
        }

        @Override
        public void writeToPacket(ByteBuf data) {
            for (int i = 0; i < TileWirelessHub.MAX_PORTS; i++) {
                data.writeByte(this.statuses[i].ordinal());
                data.writeBoolean(this.hasRemote[i]);
                data.writeInt(this.remoteX[i]);
                data.writeInt(this.remoteY[i]);
                data.writeInt(this.remoteZ[i]);
                data.writeInt(this.remoteChannels[i]);
            }
        }

        public WirelessStatus getStatus(int port) {
            return isValidPort(port) ? this.statuses[port] : WirelessStatus.UNCONNECTED;
        }

        public boolean hasRemote(int port) {
            return isValidPort(port) && this.hasRemote[port];
        }

        public int getRemoteX(int port) {
            return isValidPort(port) ? this.remoteX[port] : 0;
        }

        public int getRemoteY(int port) {
            return isValidPort(port) ? this.remoteY[port] : 0;
        }

        public int getRemoteZ(int port) {
            return isValidPort(port) ? this.remoteZ[port] : 0;
        }

        public int getRemoteChannels(int port) {
            return isValidPort(port) ? this.remoteChannels[port] : 0;
        }

        private static int getRemoteChannels(TileWirelessHub host, BlockPos remote) {
            TileEntity tile = host.getWorld() != null && host.getWorld().isBlockLoaded(remote)
                ? host.getWorld().getTileEntity(remote)
                : null;
            if (tile instanceof TileWirelessConnector connector) {
                return connector.getUsedChannels();
            }
            if (tile instanceof TileWirelessHub hub) {
                return hub.getUsedChannels();
            }
            if (tile instanceof IGridConnectedTile gridHost && gridHost.getMainNode().getNode() != null) {
                return gridHost.getMainNode().getNode().getUsedChannels();
            }
            return 0;
        }

        private boolean isValidPort(int port) {
            return port >= 0 && port < TileWirelessHub.MAX_PORTS;
        }
    }
}
