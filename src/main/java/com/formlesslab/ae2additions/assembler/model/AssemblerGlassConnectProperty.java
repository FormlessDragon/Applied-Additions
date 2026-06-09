package com.formlesslab.ae2additions.assembler.model;

import net.minecraftforge.common.property.IUnlistedProperty;

public final class AssemblerGlassConnectProperty implements IUnlistedProperty<AssemblerGlassConnect> {
    public static final AssemblerGlassConnectProperty INSTANCE = new AssemblerGlassConnectProperty();

    private AssemblerGlassConnectProperty() {
    }

    @Override
    public String getName() {
        return "assembler_matrix_glass_connect";
    }

    @Override
    public boolean isValid(AssemblerGlassConnect value) {
        return value != null;
    }

    @Override
    public Class<AssemblerGlassConnect> getType() {
        return AssemblerGlassConnect.class;
    }

    @Override
    public String valueToString(AssemblerGlassConnect value) {
        return value == null ? "null" : value.toString();
    }
}
