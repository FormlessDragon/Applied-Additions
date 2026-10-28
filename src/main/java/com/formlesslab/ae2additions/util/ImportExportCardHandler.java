package com.formlesslab.ae2additions.util;

import baubles.api.BaublesApi;
import baubles.api.cap.IBaublesItemHandler;
import ae2.api.config.Actionable;
import ae2.api.config.FuzzyMode;
import ae2.api.config.Settings;
import ae2.api.networking.IGrid;
import ae2.api.networking.crafting.CalculationStrategy;
import ae2.api.networking.crafting.ICraftingPlan;
import ae2.api.networking.crafting.ICraftingService;
import ae2.api.networking.security.IActionSource;
import ae2.api.networking.energy.IEnergySource;
import ae2.api.storage.StorageHelper;
import ae2.api.config.PowerMultiplier;
import ae2.api.stacks.AEFluidKey;
import ae2.api.stacks.AEItemKey;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.StorageCell;
import ae2.api.storage.cells.ICellWorkbenchItem;
import ae2.api.upgrades.IUpgradeInventory;
import ae2.api.upgrades.UpgradeInventories;
import ae2.api.util.IConfigManager;
import ae2.core.definitions.AEItems;
import ae2.me.helpers.PlayerSource;
import ae2.util.ConfigInventory;
import com.formlesslab.ae2additions.init.ModContent;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.Loader;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import it.unimi.dsi.fastutil.objects.Object2LongMap;

public final class ImportExportCardHandler {
    private static final Map<UUID, Future<ICraftingPlan>> CRAFTING_JOBS = new ConcurrentHashMap<>();

    private ImportExportCardHandler() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {

        if (event.phase != TickEvent.Phase.END || event.player.world.isRemote || !(event.player instanceof EntityPlayerMP player) || event.player.ticksExisted % 10 != 0) {
            return;
        }

        for (int i = 0; i < player.inventory.getSizeInventory(); i++) {
            tickTerminal(player, player.inventory.getStackInSlot(i));
        }

        tickBaubles(player);
        player.inventory.markDirty();
        player.openContainer.detectAndSendChanges();
    }

    private static void tickBaubles(EntityPlayerMP player) {
        if (!Loader.isModLoaded("baubles")) {
            return;
        }
        IBaublesItemHandler baubles = BaublesApi.getBaublesHandler(player);
        if (baubles == null) {
            return;
        }
        for (int i = 0; i < baubles.getSlots(); i++) {
            tickTerminal(player, baubles.getStackInSlot(i));
        }
    }

    private static void tickTerminal(EntityPlayerMP player, ItemStack terminal) {
        if (!WirelessTerminalAccess.isWirelessTerminal(terminal)) {
            return;
        }

        IGrid grid = WirelessTerminalAccess.getLinkedGrid(terminal, player);
        IUpgradeInventory terminalUpgrades = WirelessTerminalAccess.getUpgrades(terminal);

        if (grid == null || grid.getStorageService() == null || terminalUpgrades == null) {
            return;
        }
        for (int slot = 0; slot < terminalUpgrades.size(); slot++) {
            ItemStack card = terminalUpgrades.getStackInSlot(slot);
            if (card.getItem() == ModContent.IMPORT_CARD) {
                tickCard(player, terminal, card, grid, true);
            } else if (card.getItem() == ModContent.EXPORT_CARD) {
                tickCard(player, terminal, card, grid, false);
            }
        }
    }

    private static void tickCard(EntityPlayerMP player, ItemStack terminal, ItemStack card, IGrid grid, boolean importing) {
        int upgradeSlots = ImportExportCardConfig.getUpgradeSlotCount(!importing);
        IUpgradeInventory upgrades = UpgradeInventories.forItem(card, upgradeSlots, null);
        int filterSlots = ImportExportCardConfig.getFilterSize(upgrades);
        ConfigInventory filters = ImportExportCardConfig.readFilters(card, filterSlots);
        int[] selected = ImportExportCardConfig.getSelectedSlots(card);
        FuzzyMode fuzzyMode = FuzzyMode.IGNORE_ALL;
        boolean fuzzy = upgrades.isInstalled(AEItems.FUZZY_CARD.item());

        if (fuzzy) {
            IConfigManager manager = IConfigManager.builder(card).registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL).build();
            fuzzyMode = manager.getSetting(Settings.FUZZY_MODE);
        }

        boolean invert = upgrades.isInstalled(AEItems.INVERTER_CARD.item());
        IActionSource source = new PlayerSource(player);
        IEnergySource energy = new TerminalEnergySource(terminal.getItem(), player, terminal);

        for (int slot = 0; slot < selected.length && slot < player.inventory.getSizeInventory(); slot++) {
            int selectedFilter = selected[slot];
            if (selectedFilter < 1 || selectedFilter > filters.size()) {
                continue;
            }
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (stack == terminal || (importing && stack.isEmpty())) {
                continue;
            }
            GenericStack configured = filters.getStack(selectedFilter - 1);
            if (importing) {
                importFromSlot(player, grid, energy, source, slot, stack, filters, fuzzyMode, fuzzy, invert);
            } else if (configured != null) {
                exportToSlot(player, grid, energy, source, slot, stack, configured.what(), upgrades, fuzzy, fuzzyMode);
            }
        }
    }

    private static boolean passes(AEKey key, ConfigInventory filters, FuzzyMode mode, boolean fuzzy, boolean invert) {
        boolean match = false;
        for (int i = 0; i < filters.size(); i++) {
            GenericStack configured = filters.getStack(i);
            if (configured != null && (fuzzy ? key.fuzzyEquals(configured.what(), mode) : key.equals(configured.what()))) {
                match = true;
                break;
            }
        }
        return invert != match;
    }

    private static void importFromSlot(EntityPlayerMP player, IGrid grid, IEnergySource energy, IActionSource source, int slot, ItemStack stack, ConfigInventory filters, FuzzyMode mode, boolean fuzzy, boolean invert) {
        AEItemKey key = AEItemKey.of(stack);
        if (key != null && passes(key, filters, mode, fuzzy, invert)) {
            long inserted = StorageHelper.poweredInsert(energy, grid.getStorageService().getInventory(), key, stack.getCount(), source, Actionable.MODULATE);
            if (inserted > 0) {
                stack.shrink((int) Math.min(inserted, stack.getCount()));
                if (stack.isEmpty()) {
                    player.inventory.setInventorySlotContents(slot, ItemStack.EMPTY);
                }
            }
        }
        ItemStack current = player.inventory.getStackInSlot(slot);
        if (current.isEmpty()) {
            return;
        }
        IFluidHandler handler = current.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
        if (handler == null) {
            return;
        }
        for (IFluidTankProperties tank : handler.getTankProperties()) {
            FluidStack fluid = tank.getContents();
            if (fluid == null || fluid.amount <= 0) {
                continue;
            }
            AEFluidKey fluidKey = AEFluidKey.of(fluid);
            if (fluidKey == null || !passes(fluidKey, filters, mode, fuzzy, invert)) {
                continue;
            }
            FluidStack drained = handler.drain(fluid.amount, true);
            if (drained == null || drained.amount <= 0) {
                continue;
            }
            long inserted = StorageHelper.poweredInsert(energy, grid.getStorageService().getInventory(), fluidKey, drained.amount, source, Actionable.MODULATE);
            if (inserted < drained.amount) {
                handler.fill(new FluidStack(drained, drained.amount - (int) inserted), true);
            }
        }
    }

    private static void exportToSlot(EntityPlayerMP player, IGrid grid, IEnergySource energy, IActionSource source, int inventorySlot, ItemStack stack, AEKey filter, IUpgradeInventory upgrades, boolean fuzzy, FuzzyMode fuzzyMode) {
        AEKey exportKey = filter;
        if (fuzzy) {
                for (Object2LongMap.Entry<AEKey> entry : grid.getStorageService().getCachedInventory().findFuzzy(filter, fuzzyMode)) {
                exportKey = entry.getKey();
                break;
            }
        }
        if (exportKey instanceof AEFluidKey) {
            exportFluid(player, grid, energy, source, stack, (AEFluidKey) exportKey, upgrades);
            return;
        }
        if (stack.getCount() == 1 && stack.getItem() instanceof ICellWorkbenchItem
                && exportToCell(player, grid, energy, source, stack, exportKey, upgrades)) {
            return;
        }
        if (!(exportKey instanceof AEItemKey itemKey)) {
            return;
        }
        if (stack.isEmpty()) {
            int amount = upgrades.isInstalled(AEItems.SPEED_CARD.item()) ? itemKey.getMaxStackSize() : 1;
            long extracted = extractOrRequestCrafting(player, grid, energy, source, itemKey, amount, upgrades);
            if (extracted > 0) {
                player.inventory.setInventorySlotContents(inventorySlot, itemKey.toStack((int) extracted));
            }
            return;
        }
        if (!itemKey.matches(stack)) {
            return;
        }
        int missing = stack.getMaxStackSize() - stack.getCount();
        if (missing <= 0) {
            return;
        }
        int amount = upgrades.isInstalled(AEItems.SPEED_CARD.item()) ? missing : 1;
        long extracted = extractOrRequestCrafting(player, grid, energy, source, exportKey, amount, upgrades);
        if (extracted > 0) {
            stack.grow((int) extracted);
        }
    }

    private static long extractOrRequestCrafting(EntityPlayerMP player, IGrid grid, IEnergySource energy, IActionSource source, AEKey key, long amount, IUpgradeInventory upgrades) {
        long available = StorageHelper.poweredExtraction(energy, grid.getStorageService().getInventory(), key, amount, source, Actionable.SIMULATE);
        if (available <= 0) {
            requestCrafting(player, grid, source, key, amount, upgrades);
            return 0;
        }
        return StorageHelper.poweredExtraction(energy, grid.getStorageService().getInventory(), key, Math.min(amount, available), source, Actionable.MODULATE);
    }

    private static boolean exportToCell(EntityPlayerMP player, IGrid grid, IEnergySource energy, IActionSource source, ItemStack stack, AEKey key, IUpgradeInventory upgrades) {
        StorageCell cell = StorageCells.getCellInventory(stack, null);
        if (cell == null) {
            return false;
        }
        long limit = (long) key.getAmountPerOperation() * (upgrades.isInstalled(AEItems.SPEED_CARD.item()) ? 64 : 1);
        long requested = cell.insert(key, limit, Actionable.SIMULATE, source);
        if (requested <= 0) {
            return false;
        }
        long available = StorageHelper.poweredExtraction(energy, grid.getStorageService().getInventory(), key, requested, source, Actionable.SIMULATE);
        if (available <= 0) {
            requestCrafting(player, grid, source, key, requested, upgrades);
            return true;
        }
        long accepted = cell.insert(key, available, Actionable.MODULATE, source);
        if (accepted <= 0) {
            return true;
        }
        cell.persist();
        long extracted = StorageHelper.poweredExtraction(energy, grid.getStorageService().getInventory(), key, accepted, source, Actionable.MODULATE);
        if (extracted < accepted) {
            StorageHelper.poweredInsert(energy, grid.getStorageService().getInventory(), key, accepted - extracted, source, Actionable.MODULATE);
        }
        cell.persist();
        player.inventory.markDirty();
        return true;
    }

    private static void exportFluid(EntityPlayerMP player, IGrid grid, IEnergySource energy, IActionSource source, ItemStack stack, AEFluidKey key, IUpgradeInventory upgrades) {
        IFluidHandler handler = stack.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null);
        if (handler == null) {
            return;
        }
        int amount = (upgrades.isInstalled(AEItems.SPEED_CARD.item()) ? 64 : 1) * AEFluidKey.AMOUNT_BUCKET;
        FluidStack requested = key.toStack(amount);
        int accepted = handler.fill(requested, false);
        if (accepted <= 0) {
            return;
        }
        long extracted = extractOrRequestCrafting(player, grid, energy, source, key, accepted, upgrades);
        if (extracted > 0) {
            int filled = handler.fill(key.toStack((int) extracted), true);
            if (filled < extracted) {
                StorageHelper.poweredInsert(energy, grid.getStorageService().getInventory(), key, extracted - filled, source, Actionable.MODULATE);
            }
        }
    }

    private static void requestCrafting(EntityPlayerMP player, IGrid grid, IActionSource source, AEKey key, long amount, IUpgradeInventory upgrades) {
        if (!upgrades.isInstalled(AEItems.CRAFTING_CARD.item())) {
            return;
        }
        ICraftingService crafting = grid.getCraftingService();
        if (crafting == null || !crafting.isCraftable(key) || crafting.isRequesting(key)) {
            return;
        }
        Future<ICraftingPlan> current = CRAFTING_JOBS.get(player.getUniqueID());
        if (current != null) {
            if (!current.isDone()) {
                return;
            }
            try {
                ICraftingPlan plan = current.get();
                if (plan != null) {
                    crafting.submitJob(plan, null, null, false, source);
                }
            } catch (InterruptedException | ExecutionException ignored) {
                Thread.currentThread().interrupt();
            } finally {
                CRAFTING_JOBS.remove(player.getUniqueID());
            }
            return;
        }
        CRAFTING_JOBS.put(player.getUniqueID(), crafting.beginCraftingCalculation(player.world, () -> source, key, amount, CalculationStrategy.CRAFT_LESS));
    }

    private static final class TerminalEnergySource implements IEnergySource {
        private final Object item;
        private final EntityPlayerMP player;
        private final ItemStack terminal;

        private TerminalEnergySource(Object item, EntityPlayerMP player, ItemStack terminal) {
            this.item = item;
            this.player = player;
            this.terminal = terminal;
        }

        @Override
        public double extractAEPower(double amount, Actionable mode, PowerMultiplier multiplier) {
            double requested = multiplier.multiply(amount);
            try {
                boolean hasPower = (boolean) item.getClass().getMethod("hasPower", EntityPlayer.class, double.class, ItemStack.class).invoke(item, player, requested, terminal);
                if (!hasPower) {
                    return 0;
                }
                if (mode == Actionable.SIMULATE) {
                    return requested;
                }
                boolean used = (boolean) item.getClass().getMethod("usePower", EntityPlayer.class, double.class, ItemStack.class).invoke(item, player, requested, terminal);
                return used ? requested : 0;
            } catch (ReflectiveOperationException ignored) {
                return 0;
            }
        }
    }
}
