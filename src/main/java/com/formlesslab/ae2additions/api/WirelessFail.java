package com.formlesslab.ae2additions.api;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;

public enum WirelessFail {
    OUT_OF_RANGE("chat.wireless_connect.out_of_range"),
    SELF_REFERENCE("chat.wireless_connect.self_reference"),
    CROSS_DIMENSION("chat.wireless_connect.cross_dimension"),
    MISSING("chat.wireless_connect.missing"),
    OUT_OF_PORT("chat.wireless_connect.out_of_port");

    private final String key;

    WirelessFail(String key) {
        this.key = key;
    }

    public ITextComponent text() {
        return new TextComponentTranslation(this.key);
    }
}
