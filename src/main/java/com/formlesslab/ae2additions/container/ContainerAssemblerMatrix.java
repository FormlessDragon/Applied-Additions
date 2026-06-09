package com.formlesslab.ae2additions.container;

import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.container.AEBaseContainer;
import ae2.container.guisync.GuiSync;
import com.formlesslab.ae2additions.assembler.client.gui.AssemblerMatrixMenu;
import com.formlesslab.ae2additions.assembler.me.ClusterAssemblerMatrix;
import com.formlesslab.ae2additions.assembler.network.AssemblerMatrixServerActionHost;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixBase;
import net.minecraft.entity.player.InventoryPlayer;

public class ContainerAssemblerMatrix extends AEBaseContainer
    implements AssemblerMatrixMenu, AssemblerMatrixServerActionHost {

    private final TileAssemblerMatrixBase host;

    @GuiSync(7)
    private int runningThreads;

    @GuiSync(8)
    private boolean hidePatternProviders;

    public ContainerAssemblerMatrix(InventoryPlayer inventory, TileAssemblerMatrixBase host) {
        super(inventory, host);
        this.host = host;
        this.addPlayerInventorySlots(8, 116);
    }

    @Override
    public void detectAndSendChanges() {
        if (this.isServerSide()) {
            ClusterAssemblerMatrix cluster = this.host.getCluster();
            this.runningThreads = cluster == null ? 0 : cluster.getBusyCrafterAmount();
            this.hidePatternProviders =
                this.host.getConfigManager().getSetting(Settings.PATTERN_ACCESS_TERMINAL) == YesNo.NO;
        }
        super.detectAndSendChanges();
    }

    @Override
    public int getRunningThreads() {
        return this.runningThreads;
    }

    @Override
    public boolean isPatternProvidersHidden() {
        return this.hidePatternProviders;
    }

    @Override
    public void requestCancel() {
        if (this.isClientSide()) {
            com.formlesslab.ae2additions.network.ModNetwork.sendToServer(
                new com.formlesslab.ae2additions.assembler.network.CAssemblerMatrixCancel());
        } else {
            this.cancelAssemblerMatrixJobs();
        }
    }

    @Override
    public void requestPatternMode(boolean hide) {
        if (this.isClientSide()) {
            com.formlesslab.ae2additions.network.ModNetwork.sendToServer(
                new com.formlesslab.ae2additions.assembler.network.CAssemblerMatrixPatternMode(hide));
        } else {
            this.setAssemblerMatrixPatternMode(hide);
        }
    }

    @Override
    public void cancelAssemblerMatrixJobs() {
        ClusterAssemblerMatrix cluster = this.host.getCluster();
        if (cluster != null) {
            cluster.cancelJobs();
        }
    }

    @Override
    public void setAssemblerMatrixPatternMode(boolean hideProviders) {
        this.hidePatternProviders = hideProviders;
        this.host.getConfigManager().putSetting(Settings.PATTERN_ACCESS_TERMINAL, hideProviders ? YesNo.NO : YesNo.YES);
    }
}
