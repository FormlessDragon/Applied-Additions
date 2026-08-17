package com.formlesslab.ae2additions.util;

import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.IntFunction;

public final class TooltipHelper {
    private TooltipHelper() {
    }

    public static void addTranslatedLines(List<String> tooltip, String keyPrefix, IntFunction<Object[]> arguments) {
        for (int line = 1; ; line++) {
            String key = keyPrefix + "." + String.format("%02d", line);
            String text = new TextComponentTranslation(key, arguments.apply(line)).getUnformattedText();
            if ((key + "§r").equals(text) || line > 10) {
                return;
            }
            tooltip.add(TextFormatting.GRAY + text);
        }
    }

    public static String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
