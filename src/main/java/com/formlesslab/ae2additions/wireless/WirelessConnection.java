package com.formlesslab.ae2additions.wireless;

import ae2.api.features.Locatables;
import ae2.api.networking.GridHelper;
import ae2.api.networking.IGridConnection;
import ae2.api.networking.IGridNode;
import ae2.api.networking.security.IActionHost;
import com.formlesslab.ae2additions.AppliedAdditions;
import com.formlesslab.ae2additions.api.WirelessNode;
import com.formlesslab.ae2additions.init.ModConfig;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jetbrains.annotations.Nullable;

public class WirelessConnection implements IActionHost {
    public static final FrequencyGenerator FREQUENCIES = new FrequencyGenerator();
    private static final Locatables.Type<WirelessConnection> CONNECTORS = new Locatables.Type<>();

    private WirelessNode host;
    private boolean destroyed;
    private boolean registeredToBus;
    private long thisSide;
    private long otherSide;
    private boolean shutdown = true;
    private double distance;
    @Nullable
    private IGridConnection connection;

    public WirelessConnection(WirelessNode host) {
        this.host = host;
        this.active();
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        if (this.host != null && this.host.getWirelessWorld() == event.getWorld()) {
            this.destroy();
        }
    }

    public void active() {
        if (!this.registeredToBus) {
            MinecraftForge.EVENT_BUS.register(this);
            this.registeredToBus = true;
        }
        this.destroyed = false;
    }

    public void updateStatus() {
        if (this.host == null || this.host.getWirelessWorld() == null || this.host.getWirelessWorld().isRemote) {
            return;
        }

        long frequency = this.host.getFrequency();
        if (this.thisSide != frequency && this.thisSide != -frequency) {
            unregisterCurrent();
            this.otherSide = 0;
            this.thisSide = 0;
            if (frequency != 0) {
                if (this.canUseNode(-frequency)) {
                    this.thisSide = -frequency;
                    this.otherSide = frequency;
                } else if (this.canUseNode(frequency)) {
                    this.thisSide = frequency;
                    this.otherSide = -frequency;
                }
                if (this.thisSide != 0) {
                    CONNECTORS.register(this.host.getWirelessWorld(), this.thisSide, this);
                }
            }
        }

        WirelessConnection remote = this.otherSide == 0 ? null : CONNECTORS.get(this.host.getWirelessWorld(), this.otherSide);
        this.shutdown = true;
        this.distance = 0;

        if (remote != null && remote.host != null) {
            this.distance = Math.sqrt(this.host.getWirelessPos().distanceSq(remote.host.getWirelessPos()));
            if (this.isActive() && remote.isActive()
                && this.host.getWirelessWorld() == remote.host.getWirelessWorld()
                && this.distance <= ModConfig.wirelessConnectorMaxRange) {
                this.shutdown = false;
                ensureConnection(remote);
            }
        }

        if (this.shutdown) {
            destroyConnection();
        }
    }

    public boolean isConnected() {
        return !this.shutdown;
    }

    public double getDistance() {
        return this.distance;
    }

    @Nullable
    public BlockPos getOtherSide() {
        if (this.host == null || this.otherSide == 0) {
            return null;
        }
        WirelessConnection remote = CONNECTORS.get(this.host.getWirelessWorld(), this.otherSide);
        return remote != null && remote.host != null ? remote.host.getWirelessPos() : null;
    }

    public void destroy() {
        if (this.destroyed) {
            return;
        }
        this.destroyed = true;
        this.shutdown = true;
        destroyConnection();
        if (this.host != null) {
            unregisterCurrent();
        }
        if (this.registeredToBus) {
            MinecraftForge.EVENT_BUS.unregister(this);
            this.registeredToBus = false;
        }
        this.host = null;
    }

    private void ensureConnection(WirelessConnection remote) {
        IGridNode localNode = this.host.getWirelessGridNode();
        IGridNode remoteNode = remote.host.getWirelessGridNode();
        if (localNode == null || remoteNode == null) {
            this.shutdown = true;
            return;
        }

        if (this.connection != null && this.connection.a() != null) {
            IGridNode a = this.connection.a();
            IGridNode b = this.connection.b();
            if ((a == localNode || b == localNode) && (a == remoteNode || b == remoteNode)) {
                remote.connection = this.connection;
                return;
            }
        }

        destroyConnection();
        remote.destroyConnection();
        try {
            IGridConnection newConnection = GridHelper.createConnection(localNode, remoteNode);
            this.connection = newConnection;
            remote.connection = newConnection;
        } catch (IllegalStateException e) {
            AppliedAdditions.LOGGER.debug(e.getMessage());
        }
    }

    private boolean canUseNode(long key) {
        if (this.host == null) {
            return false;
        }
        WirelessConnection existing = CONNECTORS.get(this.host.getWirelessWorld(), key);
        if (existing == null || existing.host == null || existing.destroyed) {
            return true;
        }
        World world = existing.host.getWirelessWorld();
        BlockPos pos = existing.host.getWirelessPos();
        TileEntity tile = world != null && world.isBlockLoaded(pos) ? world.getTileEntity(pos) : null;
        return tile != existing.host.getWirelessTile();
    }

    private boolean isActive() {
        return !this.destroyed && this.registeredToBus && this.thisSide != 0;
    }

    private void unregisterCurrent() {
        if (this.host != null && this.thisSide != 0) {
            CONNECTORS.unregister(this.host.getWirelessWorld(), this.thisSide);
        }
    }

    private void destroyConnection() {
        if (this.connection != null) {
            try {
                this.connection.destroy();
            } catch (RuntimeException ignored) {
            }
            this.connection = null;
        }
    }

    @Override
    public IGridNode getActionableNode() {
        return this.host != null ? this.host.getWirelessGridNode() : null;
    }
}
