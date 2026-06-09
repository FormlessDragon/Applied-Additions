package com.formlesslab.ae2additions.init;

import net.minecraftforge.common.config.Configuration;

import java.io.File;

public final class ModConfig {
    public static double wirelessConnectorMaxRange = 1000.0;
    public static double wirelessConnectorPowerMultiplier = 1.0;
    public static int quantumComputerMaxSize = 7;
    public static int quantumComputerAcceleratorThreads = 8;
    public static int quantumComputerMaxMultiThreaders = 1;
    public static int quantumComputerMaxDataEntanglers = 1;
    public static int quantumComputerMultiThreaderMultiplication = 4;
    public static int quantumComputerDataEntanglerMultiplication = 4;
    public static boolean enableQuantumComputerEffects = true;
    public static int assemblerMatrixMaxSize = 6;

    private ModConfig() {
    }

    public static void init(File file) {
        Configuration config = new Configuration(file);
        try {
            config.load();
            wirelessConnectorMaxRange = config.getFloat(
                "wireless_connector_max_range",
                "device",
                1000.0F,
                10.0F,
                10000.0F,
                "Maximum wireless connector range in blocks.");
            wirelessConnectorPowerMultiplier = config.getFloat(
                "wireless_connector_power_multiplier",
                "device",
                1.0F,
                0.0F,
                100.0F,
                "Power multiplier for wireless connector idle drain.");
            quantumComputerMaxSize = config.getInt(
                "quantumComputerMaxSize",
                "quantum_computer",
                7,
                5,
                12,
                "Maximum dimensions of the Quantum Computer multiblock.");
            quantumComputerAcceleratorThreads = config.getInt(
                "quantumComputerAcceleratorThreads",
                "quantum_computer",
                8,
                4,
                16,
                "Threads provided by each Quantum Computer Accelerator.");
            quantumComputerMaxMultiThreaders = config.getInt(
                "quantumComputerMaxMultiThreaders",
                "quantum_computer",
                1,
                1,
                2,
                "Maximum Multi Threaders per Quantum Computer multiblock.");
            quantumComputerMaxDataEntanglers = config.getInt(
                "quantumComputerMaxDataEntanglers",
                "quantum_computer",
                1,
                1,
                2,
                "Maximum Data Entanglers per Quantum Computer multiblock.");
            quantumComputerMultiThreaderMultiplication = config.getInt(
                "quantumComputerMultiThreaderMultiplication",
                "quantum_computer",
                4,
                2,
                8,
                "Multiplication factor for Quantum Computer Multi Threaders.");
            quantumComputerDataEntanglerMultiplication = config.getInt(
                "quantumComputerDataEntanglerMultiplication",
                "quantum_computer",
                4,
                2,
                8,
                "Multiplication factor for Quantum Computer Data Entanglers.");
            enableQuantumComputerEffects = config.getBoolean(
                "enableQuantumComputerEffects",
                "quantum_computer",
                true,
                "Enable visual effects for Quantum Computer machines.");
            assemblerMatrixMaxSize = config.getInt(
                "assemblerMatrixMaxSize",
                "assembler_matrix",
                6,
                3,
                16,
                "Maximum dimensions of the Assembler Matrix multiblock.");
        } finally {
            if (config.hasChanged()) {
                config.save();
            }
        }
    }
}
