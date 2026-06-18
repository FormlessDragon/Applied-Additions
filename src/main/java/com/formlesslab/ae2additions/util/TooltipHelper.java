package com.formlesslab.ae2additions.util;

import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.List;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static void addTranslatedLines(List<String> tooltip, String keyPrefix) {
        for (int line = 1; ; line++) {
            String key = keyPrefix + "." + String.format("%02d", line);
            String text = new TextComponentTranslation(key).getFormattedText();
            if ((key + "§r").equals(text) || line > 10) {
                return;
            }
            tooltip.add(TextFormatting.GRAY + text);
        }
    }
}
