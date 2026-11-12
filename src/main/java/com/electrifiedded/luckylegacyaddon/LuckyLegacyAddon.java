package com.electrifiedded.luckylegacyaddon;

import com.electrifiedded.luckylegacyaddon.loot.LootPlusPlus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION,
        dependencies = "required-after:lucky;required-after:archaiclib")
public class LuckyLegacyAddon {
    public static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Lucky Legacy Addon loaded");
        com.electrifiedded.luckylegacyaddon.compat.LuckyUpdateCheckFix.apply();
        LootPlusPlus.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        com.electrifiedded.luckylegacyaddon.compat.LuckyUpdateCheckFix.apply();
        LootPlusPlus.init(event);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LootPlusPlus.postInit(event);
    }
}
