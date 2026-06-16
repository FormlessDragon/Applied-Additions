package com.formlesslab.ae2additions.container;

import ae2.api.config.Settings;
import ae2.api.config.YesNo;
import ae2.api.crafting.IAssemblerPattern;
import ae2.api.crafting.PatternDetailsHelper;
import ae2.api.inventories.InternalInventory;
import ae2.container.AEBaseContainer;
import ae2.container.guisync.GuiSync;
import ae2.helpers.InventoryAction;
import com.formlesslab.ae2additions.api.AssemblerMatrixMenu;
import com.formlesslab.ae2additions.me.cluster.ClusterAssemblerMatrix;
import com.formlesslab.ae2additions.api.AssemblerMatrixServerActionHost;
import com.formlesslab.ae2additions.network.SAssemblerMatrixUpdate;
import com.formlesslab.ae2additions.network.CAssemblerMatrixCancel;
import com.formlesslab.ae2additions.network.CAssemblerMatrixPatternMode;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixBase;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixPattern;
import com.formlesslab.ae2additions.network.ModNetwork;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

public class ContainerAssemblerMatrix extends AEBaseContainer
    implements AssemblerMatrixMenu, AssemblerMatrixServerActionHost {

    private final TileAssemblerMatrixBase host;
    private final Map<Long, ItemStack[]> patternSnapshots = new LinkedHashMap<>();

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
        if (this.isServerSide()) {
            this.sendPatternUpdates(false);
        }
    }

    @Override
    public void doAction(EntityPlayerMP player, InventoryAction action, int slot, long id) {
        if (slot < 0) {
            this.handlePatternAction(player, action, -slot - 1, id);
            return;
        }
        super.doAction(player, action, slot, id);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int index) {
        if (!this.isClientSide() && index >= 0 && index < this.inventorySlots.size()) {
            Slot sourceSlot = this.inventorySlots.get(index);
            if (sourceSlot != null && this.isPlayerSideSlot(sourceSlot) && this.isAssemblerPattern(sourceSlot.getStack(), player)) {
                return this.moveSinglePatternFromPlayerSlot(sourceSlot, player);
            }
        }
        return super.transferStackInSlot(player, index);
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
                new CAssemblerMatrixCancel());
        } else {
            this.cancelAssemblerMatrixJobs();
        }
    }

    @Override
    public void requestPatternMode(boolean hide) {
        if (this.isClientSide()) {
            com.formlesslab.ae2additions.network.ModNetwork.sendToServer(
                new CAssemblerMatrixPatternMode(hide));
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

    private void handlePatternAction(EntityPlayerMP player, InventoryAction action, int slot, long patternId) {
        TileAssemblerMatrixPattern pattern = this.findPattern(patternId);
        if (pattern == null || slot < 0 || slot >= pattern.getPatternInventory().size()) {
            return;
        }

        InternalInventory slotInventory = pattern.getPatternInventory().getSlotInv(slot);
        ItemStack carried = this.getCarried();

        switch (action) {
            case PICKUP_OR_SET_DOWN -> this.pickupOrSetDown(slotInventory, carried);
            case SPLIT_OR_PLACE_SINGLE, PLACE_SINGLE -> this.splitOrPlaceSingle(slotInventory, carried);
            case SHIFT_CLICK, PICKUP_SINGLE -> this.moveSlotToPlayer(player, slotInventory);
            case MOVE_REGION -> this.movePatternInventoryToPlayer(player, pattern.getPatternInventory());
            case CREATIVE_DUPLICATE -> {
                if (player.capabilities.isCreativeMode && carried.isEmpty()) {
                    this.setCarried(slotInventory.getStackInSlot(0).copy());
                }
            }
            default -> {
            }
        }

        this.sendPatternUpdates(false);
    }

    private void pickupOrSetDown(InternalInventory slotInventory, ItemStack carried) {
        if (!carried.isEmpty()) {
            ItemStack inSlot = slotInventory.getStackInSlot(0);
            if (inSlot.isEmpty()) {
                this.setCarried(slotInventory.addItems(carried));
                return;
            }

            ItemStack oldSlot = inSlot.copy();
            ItemStack oldHand = carried.copy();

            slotInventory.setItemDirect(0, ItemStack.EMPTY);
            this.setCarried(ItemStack.EMPTY);
            this.setCarried(slotInventory.addItems(oldHand));

            if (this.getCarried().isEmpty()) {
                this.setCarried(oldSlot);
            } else {
                this.setCarried(oldHand);
                slotInventory.setItemDirect(0, oldSlot);
            }
        } else {
            this.setCarried(slotInventory.extractItem(0, slotInventory.getSlotLimit(0), false));
        }
    }

    private void splitOrPlaceSingle(InternalInventory slotInventory, ItemStack carried) {
        if (!carried.isEmpty()) {
            ItemStack single = carried.splitStack(1);
            if (!single.isEmpty()) {
                single = slotInventory.addItems(single);
            }
            if (!single.isEmpty()) {
                carried.grow(single.getCount());
            }
        } else {
            ItemStack inSlot = slotInventory.getStackInSlot(0);
            if (!inSlot.isEmpty()) {
                this.setCarried(slotInventory.extractItem(0, (inSlot.getCount() + 1) / 2, false));
            }
        }
    }

    private void moveSlotToPlayer(EntityPlayerMP player, InternalInventory slotInventory) {
        ItemStack stack = slotInventory.getStackInSlot(0).copy();
        if (stack.isEmpty()) {
            return;
        }
        if (player.inventory.addItemStackToInventory(stack)) {
            slotInventory.setItemDirect(0, ItemStack.EMPTY);
        } else {
            slotInventory.setItemDirect(0, stack);
        }
    }

    private void movePatternInventoryToPlayer(EntityPlayerMP player, InternalInventory inventory) {
        for (int slot = 0; slot < inventory.size(); slot++) {
            this.moveSlotToPlayer(player, inventory.getSlotInv(slot));
        }
    }

    private ItemStack moveSinglePatternFromPlayerSlot(Slot sourceSlot, EntityPlayer player) {
        if (!sourceSlot.canTakeStack(player) || sourceSlot.getStack().isEmpty()) {
            return ItemStack.EMPTY;
        }

        ClusterAssemblerMatrix cluster = this.host.getCluster();
        if (cluster == null) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getStack();
        ItemStack originalStack = sourceStack.copy();
        ItemStack inserted = sourceStack.copy();
        inserted.setCount(1);

        for (TileAssemblerMatrixPattern pattern : cluster.getPatterns()) {
            InternalInventory inventory = pattern.getPatternInventory();
            for (int slot = 0; slot < inventory.size(); slot++) {
                if (!inventory.getStackInSlot(slot).isEmpty()) {
                    continue;
                }
                if (!inventory.insertItem(slot, inserted, true).isEmpty()) {
                    continue;
                }

                inventory.insertItem(slot, inserted, false);
                sourceSlot.decrStackSize(1);
                sourceSlot.onSlotChanged();
                this.sendPatternUpdates(false);
                return originalStack;
            }
        }

        return ItemStack.EMPTY;
    }

    private boolean isAssemblerPattern(ItemStack stack, EntityPlayer player) {
        return !stack.isEmpty() && PatternDetailsHelper.decodePattern(stack, player.world) instanceof IAssemblerPattern;
    }

    private void sendPatternUpdates(boolean force) {
        if (!(this.getPlayer() instanceof EntityPlayerMP player)) {
            return;
        }

        ClusterAssemblerMatrix cluster = this.host.getCluster();
        if (cluster == null) {
            this.sendRemovedPatternUpdates(player);
            return;
        }

        for (TileAssemblerMatrixPattern pattern : cluster.getPatterns()) {
            this.sendPatternUpdate(player, pattern, force);
        }

        Iterator<Long> knownIds = this.patternSnapshots.keySet().iterator();
        while (knownIds.hasNext()) {
            long knownId = knownIds.next();
            if (this.findPattern(knownId) == null) {
                ModNetwork.sendToClient(player, new SAssemblerMatrixUpdate(knownId, new Int2ObjectOpenHashMap<>()));
                knownIds.remove();
            }
        }
    }

    private void sendRemovedPatternUpdates(EntityPlayerMP player) {
        for (Long patternId : this.patternSnapshots.keySet()) {
            ModNetwork.sendToClient(player, new SAssemblerMatrixUpdate(patternId, new Int2ObjectOpenHashMap<>()));
        }
        this.patternSnapshots.clear();
    }

    private void sendPatternUpdate(EntityPlayerMP player, TileAssemblerMatrixPattern pattern, boolean force) {
        long patternId = pattern.getLocateID();
        InternalInventory inventory = pattern.getPatternInventory();
        ItemStack[] snapshot = this.patternSnapshots.get(patternId);
        boolean firstUpdate = snapshot == null;
        if (firstUpdate) {
            snapshot = emptySnapshot(inventory.size());
            this.patternSnapshots.put(patternId, snapshot);
        }
        Int2ObjectMap<ItemStack> changed = new Int2ObjectOpenHashMap<>();

        for (int slot = 0; slot < inventory.size(); slot++) {
            ItemStack current = inventory.getStackInSlot(slot);
            if (force || firstUpdate || isDifferent(snapshot[slot], current)) {
                ItemStack copy = current.isEmpty() ? ItemStack.EMPTY : current.copy();
                snapshot[slot] = copy;
                changed.put(slot, copy);
            }
        }

        if (!changed.isEmpty()) {
            ModNetwork.sendToClient(player, new SAssemblerMatrixUpdate(patternId, changed));
        }
    }

    private TileAssemblerMatrixPattern findPattern(long patternId) {
        ClusterAssemblerMatrix cluster = this.host.getCluster();
        if (cluster == null) {
            return null;
        }
        for (TileAssemblerMatrixPattern pattern : cluster.getPatterns()) {
            if (pattern.getLocateID() == patternId) {
                return pattern;
            }
        }
        return null;
    }

    private static ItemStack[] emptySnapshot(int size) {
        ItemStack[] snapshot = new ItemStack[size];
        Arrays.fill(snapshot, ItemStack.EMPTY);
        return snapshot;
    }

    private static boolean isDifferent(ItemStack a, ItemStack b) {
        if (a.isEmpty() && b.isEmpty()) {
            return false;
        }
        return !ItemStack.areItemStacksEqual(a, b);
    }
}
