package com.formlesslab.ae2additions.block.assembler;

import com.formlesslab.ae2additions.init.Configurations;
import com.formlesslab.ae2additions.tile.TileAssemblerMatrixSpeed;
import com.formlesslab.ae2additions.util.TooltipHelper;

public class BlockAssemblerMatrixSpeed extends BlockAssemblerMatrixBase<TileAssemblerMatrixSpeed> {
    public BlockAssemblerMatrixSpeed() {
        super(TileAssemblerMatrixSpeed.class);
    }

    @Override
    protected Object[] getTooltipArguments(int line) {
        return line == 3 ? new Object[]{TooltipHelper.formatNumber(Configurations.ASSEMBLER_MATRIX.speedIdlePowerUsage)} : super.getTooltipArguments(line);
    }
}
