package com.formlesslab.ae2additions.cell;

import ae2.api.config.Actionable;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.stacks.KeyCounter;
import ae2.api.storage.MEStorage;
import ae2.api.storage.MEStorageChangeListener;
import ae2.api.storage.StorageCells;
import ae2.api.storage.cells.CellState;
import ae2.api.storage.cells.ICellHandler;
import ae2.api.storage.cells.ISaveProvider;
import ae2.api.storage.cells.StorageCell;
import com.formlesslab.ae2additions.item.ItemInfinityCell;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.LinkedHashSet;
import java.util.Set;

public final class InfinityCellInventory implements StorageCell {
    public static final ICellHandler HANDLER = new Handler();
    private final ItemStack stack;
    private final Set<AEKey> keys;

    private InfinityCellInventory(ItemStack stack, ItemInfinityCell cell) {
        this.stack = stack;
        this.keys = new LinkedHashSet<>(cell.getKeys());
    }

    public static void registerHandler() {
        StorageCells.addCellHandler(HANDLER);
    }

    @Override
    public CellState getStatus() {
        return CellState.NOT_EMPTY;
    }

    @Override
    public double getIdleDrain() {
        return 1.0D;
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        return keys.contains(what) ? amount : 0;
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        MEStorage.checkPreconditions(what, amount, mode, source);
        return keys.contains(what) ? amount : 0;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        for (AEKey key : keys) {
            out.add(key, InfinityCellContents.getMaxAmount(key));
        }
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return keys.contains(what);
    }

    @Override
    public ITextComponent getDescription() {
        return new TextComponentString(stack.getDisplayName());
    }

    @Override
    public void persist() {
    }

    @Override
    public void addListener(MEStorageChangeListener listener, Object verificationToken) {
    }

    @Override
    public void removeListener(MEStorageChangeListener listener) {
    }

    private static final class Handler implements ICellHandler {
        @Override
        public boolean isCell(ItemStack stack) {
            return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemInfinityCell;
        }

        @Override
        @Nullable
        public StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
            if (!isCell(stack)) {
                return null;
            }
            return new InfinityCellInventory(stack, (ItemInfinityCell) stack.getItem());
        }
    }
}
