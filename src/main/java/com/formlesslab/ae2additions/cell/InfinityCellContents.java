package com.formlesslab.ae2additions.cell;

import ae2.api.stacks.AEKey;
import ae2.api.stacks.GenericStack;
import ae2.core.AEConfig;
import ae2.items.storage.StorageCellTooltipComponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

public final class InfinityCellContents {
    private InfinityCellContents() {
    }

    public static long getMaxAmount(AEKey key) {
        return (long) Integer.MAX_VALUE * key.getAmountPerUnit();
    }

    public static List<AEKey> distinct(List<AEKey> keys) {
        return new ArrayList<>(new LinkedHashSet<>(keys));
    }

    public static StorageCellTooltipComponent createTooltip(List<AEKey> keys) {
        int maxShown = Math.max(0, AEConfig.instance().getTooltipMaxCellContentShown());
        List<GenericStack> content = new ArrayList<>(Math.min(keys.size(), maxShown));
        for (int i = 0; i < keys.size() && i < maxShown; i++) {
            AEKey key = keys.get(i);
            content.add(new GenericStack(key, getMaxAmount(key)));
        }
        return new StorageCellTooltipComponent(Collections.emptyList(), content, keys.size() > maxShown, true);
    }
}
