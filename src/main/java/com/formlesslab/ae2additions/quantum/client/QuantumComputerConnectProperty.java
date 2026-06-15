package com.formlesslab.ae2additions.quantum.client;

import net.minecraftforge.common.property.IUnlistedProperty;

public final class QuantumComputerConnectProperty implements IUnlistedProperty<QuantumComputerConnect> {
    public static final QuantumComputerConnectProperty INSTANCE = new QuantumComputerConnectProperty();

    private QuantumComputerConnectProperty() {
    }

    @Override
    public String getName() {
        return "quantum_computer_connect";
    }

    @Override
    public boolean isValid(QuantumComputerConnect value) {
        return value != null;
    }

    @Override
    public Class<QuantumComputerConnect> getType() {
        return QuantumComputerConnect.class;
    }

    @Override
    public String valueToString(QuantumComputerConnect value) {
        return value == null ? "null" : value.toString();
    }
}
