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

/**
 * AE2 storage view for one NBT-configured infinity-cell stack.
 */
public final class InfinityCellInventory implements StorageCell {
    public static final ICellHandler HANDLER = new Handler();

    private final ItemStack stack;
    private final Set<AEKey> keys;

    private InfinityCellInventory(ItemStack stack) {
        this.stack = stack.copy();
        this.keys = new LinkedHashSet<>(InfinityCellContents.readKeys(stack));
    }

    public static void registerHandler() {
        StorageCells.addCellHandler(HANDLER);
    }

    @Override
    public CellState getStatus() {
        return keys.isEmpty() ? CellState.EMPTY : CellState.NOT_EMPTY;
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
        // The configured key list is immutable for the lifetime of the stack.
    }

    @Override
    public void addListener(MEStorageChangeListener listener, Object verificationToken) {
        // Contents never change, so there are no listeners to notify.
    }

    @Override
    public void removeListener(MEStorageChangeListener listener) {
    }

    private static final class Handler implements ICellHandler {
        @Override
        public boolean isCell(ItemStack stack) {
            return stack != null && !stack.isEmpty() && stack.getItem() instanceof ItemInfinityCell && InfinityCellContents.isConfigured(stack);
        }

        @Override
        @Nullable
        public StorageCell getCellInventory(ItemStack stack, @Nullable ISaveProvider host) {
            return isCell(stack) ? new InfinityCellInventory(stack) : null;
        }
    }
}
