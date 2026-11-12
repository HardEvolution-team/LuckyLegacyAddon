package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.DummyModContainer;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public final class LootPlusPlus {
    public static final String MODID = "lootplusplus";
    public static final Logger LOG = LogManager.getLogger("Loot++");

    private static int duplicates;
    private static boolean active;

    private LootPlusPlus() {}

    static void duplicate(LppConfig.Entry e, String what) {
        duplicates++;
    }

    public static boolean isActive() {
        return active;
    }

    public static void preInit(FMLPreInitializationEvent e) {
        try {
            File mc = e.getModConfigurationDirectory().getParentFile();
            LppConfig.load(mc);
            if (LppConfig.addons().isEmpty()) return;
            active = true;
            ModContainer self = Loader.instance().activeModContainer();
            ModMetadata meta = new ModMetadata();
            meta.modId = MODID;
            meta.name = "Loot++ (native)";
            ModContainer lpp = new DummyModContainer(meta);
            Loader.instance().setActiveModContainer(lpp);
            try {
                MinecraftForge.EVENT_BUS.register(new LppRegistry());
                EntityRegistry.registerModEntity(new ResourceLocation(MODID, "thrown_item"), AddedItems.Thrown.class, "lpp_thrown_item", 0,
                        self.getMod(), 64, 10, true);
            } finally {
                Loader.instance().setActiveModContainer(self);
            }
            MinecraftForge.EVENT_BUS.register(new LppEvents());
            MinecraftForge.EVENT_BUS.register(new LppExtras());
            if (FMLCommonHandler.instance().getSide() == Side.CLIENT) LppClient.preInit();
        } catch (Throwable t) {
            active = false;
            LOG.warn("[Loot++] Initialisation failed, Loot++ content is disabled: {}", t.toString());
        }
    }

    public static void init(FMLInitializationEvent e) {
        if (!active) return;
        try {
            LppExtras.init();
        } catch (Throwable t) {
            LOG.warn("[Loot++] init failed: {}", t.toString());
        }
    }

    public static void postInit(FMLPostInitializationEvent e) {
        if (!active) return;
        try {
            LppRegistry.resolveCrops();
            LppExtras.postInit();
            if (duplicates > 0) {
                LOG.info("[Loot++] {} identical duplicate definition(s) across add-ons ignored (the first add-on, alphabetically, wins)", duplicates);
            }
        } catch (Throwable t) {
            LOG.warn("[Loot++] postInit failed: {}", t.toString());
        }
    }
}
