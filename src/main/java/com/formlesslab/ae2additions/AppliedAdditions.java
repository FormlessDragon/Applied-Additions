package com.formlesslab.ae2additions;

import com.formlesslab.ae2additions.init.ModConfig;
import com.formlesslab.ae2additions.init.ModContent;
import com.formlesslab.ae2additions.init.ModUpgrades;
import com.formlesslab.ae2additions.network.ModNetwork;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(
    modid = Reference.MOD_ID,
    name = Reference.MOD_NAME,
    version = Reference.VERSION,
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:ae2"
)
public class AppliedAdditions {

    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);
    @Mod.Instance(Reference.MOD_ID)
    public static AppliedAdditions INSTANCE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ModConfig.init(event.getSuggestedConfigurationFile());
        ModContent.registerTileEntities();
        ModNetwork.init();
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new ModGuiHandler());
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            com.formlesslab.ae2additions.assembler.client.model.AssemblerGlassModel.register();
            MinecraftForge.EVENT_BUS.register(com.formlesslab.ae2additions.quantum.client.QuantumComputerModelOverride.INSTANCE);
            MinecraftForge.EVENT_BUS.register(com.formlesslab.ae2additions.client.render.WirelessHighlightHandler.INSTANCE);
        }
        LOGGER.info("{} initialized", Reference.MOD_NAME);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ModUpgrades.init();
    }
}
