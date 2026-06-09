package com.formlesslab.ae2additions.quantum;

/**
 * AdvancedAE quantum-computer defaults, kept independent from the global config
 * until the main mod wires these values into its config file.
 */
public final class QuantumConfig {
    public static final int DEFAULT_MAX_SIZE = 7;
    public static final int DEFAULT_ACCELERATOR_THREADS = 8;
    public static final int DEFAULT_MAX_MULTI_THREADERS = 1;
    public static final int DEFAULT_MAX_DATA_ENTANGLERS = 1;
    public static final int DEFAULT_MULTI_THREADER_MULTIPLIER = 4;
    public static final int DEFAULT_DATA_ENTANGLER_MULTIPLIER = 4;

    private static volatile int maxSize = DEFAULT_MAX_SIZE;
    private static volatile int acceleratorThreads = DEFAULT_ACCELERATOR_THREADS;
    private static volatile int maxMultiThreaders = DEFAULT_MAX_MULTI_THREADERS;
    private static volatile int maxDataEntanglers = DEFAULT_MAX_DATA_ENTANGLERS;
    private static volatile int multiThreaderMultiplier = DEFAULT_MULTI_THREADER_MULTIPLIER;
    private static volatile int dataEntanglerMultiplier = DEFAULT_DATA_ENTANGLER_MULTIPLIER;

    private QuantumConfig() {
    }

    public static int getMaxSize() {
        return maxSize;
    }

    public static int getAcceleratorThreads() {
        return acceleratorThreads;
    }

    public static int getMaxMultiThreaders() {
        return maxMultiThreaders;
    }

    public static int getMaxDataEntanglers() {
        return maxDataEntanglers;
    }

    public static int getMultiThreaderMultiplier() {
        return multiThreaderMultiplier;
    }

    public static int getDataEntanglerMultiplier() {
        return dataEntanglerMultiplier;
    }

    public static void apply(
        int maxSize,
        int acceleratorThreads,
        int maxMultiThreaders,
        int maxDataEntanglers,
        int multiThreaderMultiplier,
        int dataEntanglerMultiplier
    ) {
        QuantumConfig.maxSize = clamp(maxSize, 1, 16);
        QuantumConfig.acceleratorThreads = clamp(acceleratorThreads, 1, 16);
        QuantumConfig.maxMultiThreaders = clamp(maxMultiThreaders, 0, 16);
        QuantumConfig.maxDataEntanglers = clamp(maxDataEntanglers, 0, 16);
        QuantumConfig.multiThreaderMultiplier = Math.max(1, multiThreaderMultiplier);
        QuantumConfig.dataEntanglerMultiplier = Math.max(1, dataEntanglerMultiplier);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
