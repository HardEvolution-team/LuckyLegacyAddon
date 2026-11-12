package com.electrifiedded.luckylegacyaddon.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.potion.PotionUtils;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;

@Mod.EventBusSubscriber(modid = com.electrifiedded.luckylegacyaddon.Reference.MOD_ID, value = Side.CLIENT)
public final class LegacyPotionsClient {
    @SubscribeEvent
    public static void models(ModelRegistryEvent e) {
        if (LegacyPotions.customPotion != null)
            ModelLoader.setCustomModelResourceLocation(LegacyPotions.customPotion, 0, new ModelResourceLocation("minecraft:bottle_drinkable", "inventory"));
    }
    @SubscribeEvent
    public static void colors(ColorHandlerEvent.Item e) {
        if (LegacyPotions.customPotion != null)
            e.getItemColors().registerItemColorHandler((s, t) -> t > 0 ? -1 : PotionUtils.getColor(s), LegacyPotions.customPotion);
    }
}
