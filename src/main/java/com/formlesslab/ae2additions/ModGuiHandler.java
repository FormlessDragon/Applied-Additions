package com.formlesslab.ae2additions;

import ae2.client.gui.style.GuiStyleManager;
import com.formlesslab.ae2additions.assembler.client.gui.GuiAssemblerMatrix;
import com.formlesslab.ae2additions.assembler.tile.TileAssemblerMatrixBase;
import com.formlesslab.ae2additions.client.gui.GuiWirelessConnector;
import com.formlesslab.ae2additions.client.gui.GuiWirelessHub;
import com.formlesslab.ae2additions.container.ContainerAssemblerMatrix;
import com.formlesslab.ae2additions.container.ContainerWirelessConnector;
import com.formlesslab.ae2additions.container.ContainerWirelessHub;
import com.formlesslab.ae2additions.quantum.client.gui.QuantumComputerMenu;
import com.formlesslab.ae2additions.quantum.client.gui.QuantumComputerScreen;
import com.formlesslab.ae2additions.quantum.tile.AdvCraftingBlockEntity;
import com.formlesslab.ae2additions.tile.TileWirelessConnector;
import com.formlesslab.ae2additions.tile.TileWirelessHub;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.LinkedHashMap;
import java.util.Map;

public class ModGuiHandler implements IGuiHandler {
    public static final int WIRELESS_CONNECTOR = 0;
    public static final int WIRELESS_HUB = 1;
    public static final int QUANTUM_COMPUTER = 2;
    public static final int ASSEMBLER_MATRIX = 3;

    private static final Map<Integer, GuiRegistration<?>> REGISTRATIONS = new LinkedHashMap<>();
    private static boolean clientRegistrationsInitialized;

    static {
        registerGui(
            WIRELESS_CONNECTOR,
            TileWirelessConnector.class,
            (player, world, pos, connector) -> new ContainerWirelessConnector(player.inventory, connector));
        registerGui(
            WIRELESS_HUB,
            TileWirelessHub.class,
            (player, world, pos, hub) -> new ContainerWirelessHub(player.inventory, hub));
        registerGui(
            QUANTUM_COMPUTER,
            AdvCraftingBlockEntity.class,
            (player, world, pos, quantum) -> new QuantumComputerMenu(player.inventory, quantum));
        registerGui(
            ASSEMBLER_MATRIX,
            TileAssemblerMatrixBase.class,
            (player, world, pos, matrix) -> new ContainerAssemblerMatrix(player.inventory, matrix));
    }

    public static <T extends TileEntity> void registerGui(int id, Class<T> tileClass,
                                                          ServerGuiFactory<T> serverFactory) {
        if (REGISTRATIONS.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate GUI id " + id);
        }
        REGISTRATIONS.put(id, new GuiRegistration<>(tileClass, serverFactory));
    }

    public static <T extends TileEntity> void registerGui(int id, Class<T> tileClass,
                                                          ServerGuiFactory<T> serverFactory,
                                                          ClientGuiFactory<T> clientFactory) {
        registerGui(id, tileClass, serverFactory);
        registerClientGui(id, tileClass, clientFactory);
    }

    public static <T extends TileEntity> void registerClientGui(int id, Class<T> tileClass,
                                                                ClientGuiFactory<T> clientFactory) {
        GuiRegistration<?> registration = REGISTRATIONS.get(id);
        if (registration == null) {
            throw new IllegalArgumentException("Missing server GUI registration for id " + id);
        }
        registration.setClientFactory(tileClass, clientFactory);
    }

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        GuiRegistration<?> registration = REGISTRATIONS.get(id);
        return registration == null ? null : registration.createServer(player, world, pos, tile);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        ensureClientRegistrations();
        BlockPos pos = new BlockPos(x, y, z);
        TileEntity tile = world.getTileEntity(pos);
        GuiRegistration<?> registration = REGISTRATIONS.get(id);
        return registration == null ? null : registration.createClient(player, world, pos, tile);
    }

    @SideOnly(Side.CLIENT)
    private static void ensureClientRegistrations() {
        if (clientRegistrationsInitialized) {
            return;
        }
        clientRegistrationsInitialized = true;
        registerClientGui(
            WIRELESS_CONNECTOR,
            TileWirelessConnector.class,
            (player, world, pos, connector) -> new GuiWirelessConnector(
                new ContainerWirelessConnector(player.inventory, connector),
                player.inventory,
                GuiStyleManager.loadStyleDoc("/screens/wireless_connector.json")));
        registerClientGui(
            WIRELESS_HUB,
            TileWirelessHub.class,
            (player, world, pos, hub) -> new GuiWirelessHub(
                new ContainerWirelessHub(player.inventory, hub),
                player.inventory,
                GuiStyleManager.loadStyleDoc("/screens/wireless_hub.json")));
        registerClientGui(
            QUANTUM_COMPUTER,
            AdvCraftingBlockEntity.class,
            (player, world, pos, quantum) -> new QuantumComputerScreen(
                new QuantumComputerMenu(player.inventory, quantum),
                player.inventory,
                quantum.getDisplayName(),
                GuiStyleManager.loadStyleDoc("/screens/quantum_computer.json")));
        registerClientGui(
            ASSEMBLER_MATRIX,
            TileAssemblerMatrixBase.class,
            (player, world, pos, matrix) -> new GuiAssemblerMatrix<>(
                new ContainerAssemblerMatrix(player.inventory, matrix),
                player.inventory,
                GuiAssemblerMatrix.loadStyle()));
    }

    @FunctionalInterface
    public interface ServerGuiFactory<T extends TileEntity> {
        Object create(EntityPlayer player, World world, BlockPos pos, T tile);
    }

    @FunctionalInterface
    public interface ClientGuiFactory<T extends TileEntity> {
        Object create(EntityPlayer player, World world, BlockPos pos, T tile);
    }

    private static final class GuiRegistration<T extends TileEntity> {
        private final Class<T> tileClass;
        private final ServerGuiFactory<T> serverFactory;
        private ClientGuiFactory<T> clientFactory;

        private GuiRegistration(Class<T> tileClass, ServerGuiFactory<T> serverFactory) {
            this.tileClass = tileClass;
            this.serverFactory = serverFactory;
        }

        private <U extends TileEntity> void setClientFactory(Class<U> expectedTileClass,
                                                             ClientGuiFactory<U> clientFactory) {
            if (this.tileClass != expectedTileClass) {
                throw new IllegalArgumentException("Client GUI tile class mismatch for " + expectedTileClass);
            }
            this.clientFactory = (ClientGuiFactory<T>) clientFactory;
        }

        private Object createServer(EntityPlayer player, World world, BlockPos pos, TileEntity tile) {
            if (!this.tileClass.isInstance(tile)) {
                return null;
            }
            return this.serverFactory.create(player, world, pos, this.tileClass.cast(tile));
        }

        @SideOnly(Side.CLIENT)
        private Object createClient(EntityPlayer player, World world, BlockPos pos, TileEntity tile) {
            if (this.clientFactory == null || !this.tileClass.isInstance(tile)) {
                return null;
            }
            return this.clientFactory.create(player, world, pos, this.tileClass.cast(tile));
        }
    }
}
