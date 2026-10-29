package com.formlesslab.ae2additions.client;

import ae2.api.implementations.guiobjects.ItemGuiHost;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.IUpgradeableObject;
import ae2.client.gui.AEBaseGui;
import ae2.client.gui.me.common.GuiMEStorage;
import ae2.core.gui.locator.BaublesItemLocator;
import ae2.core.gui.locator.ItemGuiHostLocator;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.init.ModNetworks;
import com.formlesslab.ae2additions.util.WirelessTerminalAccess;
import com.formlesslab.ae2additions.network.COpenImportExportCard;
import com.formlesslab.ae2additions.client.gui.widgets.ImportExportCardButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;

public final class ImportExportGuiHooks {
    private static final int NO_TERMINAL = Integer.MIN_VALUE;
    private static final Map<GuiMEStorage<?>, ImportExportCardButton[]> BUTTONS = new WeakHashMap<>();
    private static Method ADD_TO_LEFT_TOOLBAR;

    static {
        try {
            ADD_TO_LEFT_TOOLBAR = AEBaseGui.class.getDeclaredMethod("addToLeftToolbar", GuiButton.class);
            ADD_TO_LEFT_TOOLBAR.setAccessible(true);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private ImportExportGuiHooks() {
    }

    @SubscribeEvent
    public static void onInit(GuiScreenEvent.InitGuiEvent.Pre event) {
        if (!(event.getGui() instanceof GuiMEStorage<?> screen)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (BUTTONS.containsKey(screen)) {
            updateVisibility(screen, mc.player.inventory);
            return;
        }

        WeakReference<GuiMEStorage<?>> screenReference = new WeakReference<>(screen);
        ImportExportCardButton importButton = new ImportExportCardButton(false,
                () -> openCard(screenReference.get(), false));
        importButton.setMessage(new TextComponentTranslation("item.ae2additions.import_card.name"));
        ImportExportCardButton exportButton = new ImportExportCardButton(true,
                () -> openCard(screenReference.get(), true));
        exportButton.setMessage(new TextComponentTranslation("item.ae2additions.export_card.name"));

        addToolbarButton(screen, event, importButton);
        addToolbarButton(screen, event, exportButton);
        BUTTONS.put(screen, new ImportExportCardButton[]{importButton, exportButton});
        updateVisibility(screen, mc.player.inventory);
    }

    @SubscribeEvent
    public static void onDraw(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!(event.getGui() instanceof GuiMEStorage<?> screen)) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            updateVisibility(screen, mc.player.inventory);
        }
    }

    @SubscribeEvent
    public static void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() == null) {
            BUTTONS.clear();
        } else {
            BUTTONS.keySet().removeIf(screen -> screen != event.getGui());
        }
    }

    private static void addToolbarButton(GuiMEStorage<?> screen, GuiScreenEvent.InitGuiEvent.Pre event, ImportExportCardButton button) {
        boolean addedToToolbar = false;
        if (ADD_TO_LEFT_TOOLBAR != null) {
            try {
                ADD_TO_LEFT_TOOLBAR.invoke(screen, button);
                addedToToolbar = true;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        if (!addedToToolbar) {
            event.getButtonList().add(button);
        }
    }

    private static void updateVisibility(GuiMEStorage<?> screen, InventoryPlayer inventory) {
        ImportExportCardButton[] buttons = BUTTONS.get(screen);
        if (buttons == null) {
            return;
        }
        int[] slots = findCardSlots(screen, inventory);
        buttons[0].setVisibility(slots[1] >= 0 && slots[0] != NO_TERMINAL);
        buttons[1].setVisibility(slots[3] >= 0 && slots[2] != NO_TERMINAL);
    }

    private static void openCard(GuiMEStorage<?> screen, boolean export) {
        if (screen == null) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        int[] slots = findCardSlots(screen, mc.player.inventory);
        int terminalSlot = export ? slots[2] : slots[0];
        int cardSlot = export ? slots[3] : slots[1];
        if (terminalSlot != NO_TERMINAL && cardSlot >= 0) {
            ModNetworks.sendToServer(new COpenImportExportCard(terminalSlot, cardSlot, export));
        }
    }

    private static int[] findCardSlots(GuiMEStorage<?> screen, InventoryPlayer inventory) {
        int[] result = {NO_TERMINAL, -1, NO_TERMINAL, -1};
        Object host = screen.getContainer().getHost();
        if (host instanceof ItemGuiHost<?> itemHost) {
            int terminalSlot = encodeTerminalSlot(itemHost.getLocator());
            if (host instanceof IUpgradeableObject upgradeable && terminalSlot != NO_TERMINAL) {
                recordCardSlots(result, terminalSlot, upgradeable.getUpgrades());
            }
            return result;
        }

        for (int candidate = 0; candidate < inventory.getSizeInventory(); candidate++) {
            ItemStack terminal = inventory.getStackInSlot(candidate);
            if (!WirelessTerminalAccess.isWirelessTerminal(terminal)) {
                continue;
            }
            IUpgradeInventory upgrades = WirelessTerminalAccess.getUpgrades(terminal);
            if (upgrades != null) {
                recordCardSlots(result, candidate, upgrades);
            }
        }
        return result;
    }

    private static int encodeTerminalSlot(ItemGuiHostLocator locator) {
        if (locator == null) {
            return NO_TERMINAL;
        }
        Integer inventorySlot = locator.getPlayerInventorySlot();
        if (inventorySlot != null) {
            return inventorySlot;
        }
        if (locator instanceof BaublesItemLocator baublesLocator) {
            return -1 - baublesLocator.getBaubleSlot();
        }
        return NO_TERMINAL;
    }

    private static void recordCardSlots(int[] result, int terminalSlot, IUpgradeInventory upgrades) {
        for (int slot = 0; slot < upgrades.size(); slot++) {
            ItemStack card = upgrades.getStackInSlot(slot);
            if (card.getItem() == ModContent.IMPORT_CARD && result[1] < 0) {
                result[0] = terminalSlot;
                result[1] = slot;
            }
            if (card.getItem() == ModContent.EXPORT_CARD && result[3] < 0) {
                result[2] = terminalSlot;
                result[3] = slot;
            }
        }
    }
}
