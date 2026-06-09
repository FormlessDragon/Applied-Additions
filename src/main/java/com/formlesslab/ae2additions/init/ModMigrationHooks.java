package com.formlesslab.ae2additions.init;

public final class ModMigrationHooks {
    private ModMigrationHooks() {
    }

    /*
     * Quantum Computer integration points waiting on backend classes:
     * - Add quantum blocks/items with ModContent.registerBlock/registerItem.
     * - Add quantum tile entities with ModContent.registerTileEntity.
     * - Register ModGuiHandler.QUANTUM_COMPUTER once the container, GUI, and tile classes exist.
     * - Register packets in ModNetwork using QUANTUM_TASK_CANCEL and QUANTUM_CPU_SELECTION ids.
     *
     * Assembler Matrix integration points waiting on backend classes:
     * - Add matrix blocks/items/tile entities through ModContent.
     * - Register ModGuiHandler.ASSEMBLER_MATRIX once the container, GUI, and tile classes exist.
     * - Register packets in ModNetwork using ASSEMBLER_MATRIX_UPDATE and ASSEMBLER_MATRIX_CANCEL ids.
     *
     * CraftingService integration points for the Quantum Computer migration:
     * - Track quantum CPU clusters alongside AE2 crafting CPU clusters.
     * - Mark CraftingService.updateList when quantum CPU nodes are added or removed.
     * - Tick quantum crafting logic during CraftingService.onServerEndTick.
     * - Include quantum waiting stacks in currentlyCrafting/requested amount notifications.
     * - Include quantum CPUs in getCpus/hasCpu and submitJob CPU selection.
     * - Insert returned crafting items into quantum CPUs in insertIntoCpus.
     *
     * The current build already has optional Mixin/AT plumbing in build.gradle and gradle.properties,
     * but it is disabled. Enable only after the quantum backend owns a complete CraftingService mixin
     * and the referenced quantum classes exist.
     */
}
