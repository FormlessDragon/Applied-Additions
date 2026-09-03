package com.formlesslab.ae2additions.fluid;

import com.formlesslab.ae2additions.Tags;
import net.minecraft.block.material.Material;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.BlockFluidClassic;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;

public final class QuantumInfusionFluid {
    public static final int QUANTUM_INFUSION_COLOR = 0xFF7362D3;
    private static final ResourceLocation STILL = new ResourceLocation("ae2additions", "block/quantum_infusion_still");
    private static final ResourceLocation FLOW = new ResourceLocation("ae2additions", "block/quantum_infusion_flow");

    public static final Fluid QUANTUM_INFUSION = new Fluid("quantum_infusion", STILL, FLOW).setColor(QUANTUM_INFUSION_COLOR).setDensity(300).setViscosity(1000);

    public static void init() {
        FluidRegistry.registerFluid(QUANTUM_INFUSION);
        FluidRegistry.addBucketForFluid(QUANTUM_INFUSION);
    }

    public static class QuantumInfusionBlock extends BlockFluidClassic {
        public QuantumInfusionBlock() {
            super(QUANTUM_INFUSION, Material.WATER);
            this.setQuantaPerBlock(4);
            this.setLightOpacity(2);
        }

        @Override
        public String getTranslationKey() {
            return "tile." + Tags.MOD_ID + ".quantum_infusion_block";
        }
    }
}
