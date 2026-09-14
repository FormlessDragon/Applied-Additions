package com.formlesslab.ae2additions;

import ae2.api.networking.GridServices;
import com.formlesslab.ae2additions.cell.InfinityCellInventory;
import com.formlesslab.ae2additions.client.model.AssemblerGlassModel;
import com.formlesslab.ae2additions.client.notification.CraftingJobNotificationHandler;
import com.formlesslab.ae2additions.client.render.QuantumComputerModelOverride;
import com.formlesslab.ae2additions.client.render.WirelessHighlightHandler;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.init.ModGuiHandler;
import com.formlesslab.ae2additions.init.ModNetworks;
import com.formlesslab.ae2additions.me.cluster.QuantumCpuRowIcons;
import com.formlesslab.ae2additions.me.service.QuantumCpuMaintenanceService;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Tags.MOD_ID, name = Tags.MOD_NAME, version = Tags.VERSION, acceptedMinecraftVersions = "[1.12.2]", dependencies = "required-after:ae2")
public class AppliedAdditions {

    public static final Logger LOGGER = LogManager.getLogger(Tags.MOD_NAME);
    @Mod.Instance(Tags.MOD_ID)
    public static AppliedAdditions INSTANCE;

    static {
        FluidRegistry.enableUniversalBucket();
    }

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModContent.registerTileEntities();
        InfinityCellInventory.registerHandler();
        ModNetworks.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        GridServices.register(QuantumCpuMaintenanceService.class, QuantumCpuMaintenanceService.class);
        QuantumCpuRowIcons.bootstrap();
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            AssemblerGlassModel.register();
            MinecraftForge.EVENT_BUS.register(QuantumComputerModelOverride.INSTANCE);
            MinecraftForge.EVENT_BUS.register(WirelessHighlightHandler.INSTANCE);
            MinecraftForge.EVENT_BUS.register(CraftingJobNotificationHandler.INSTANCE);
        }
        LOGGER.info("{} initialized", Tags.MOD_NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModContent.registerUpgrades();
        ModContent.registerOreDictionary();
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        ModContent.registerMachineRecipes();
    }
}
