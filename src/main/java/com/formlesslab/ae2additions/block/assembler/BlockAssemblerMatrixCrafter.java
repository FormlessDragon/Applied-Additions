package com.formlesslab.ae2additions.block.assembler;

import com.formlesslab.ae2additions.init.Configurations;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixCrafter;
import com.formlesslab.ae2additions.util.TooltipHelper;

public class BlockAssemblerMatrixCrafter extends BlockAssemblerMatrixBase<TileAssemblerMatrixCrafter> {
    public BlockAssemblerMatrixCrafter() {
        super(TileAssemblerMatrixCrafter.class);
    }

    @Override
    protected Object[] getTooltipArguments(int line) {
        return switch (line) {
            case 2 -> new Object[]{Configurations.ASSEMBLER_MATRIX.crafterParallelism};
            case 3 -> new Object[]{TooltipHelper.formatNumber(Configurations.ASSEMBLER_MATRIX.crafterIdlePowerUsage)};
            default -> super.getTooltipArguments(line);
        };
    }
}
