package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.block.Block;
import net.minecraft.block.BlockWall;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderSnowball;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@SideOnly(Side.CLIENT)
final class LppClient {
    private LppClient() {}

    static void preInit() {
        LootPlusPlus.LOG.info("[Loot++] creative tab '{}'", LppCreativeTab.TAB.getTabLabel());
        try {
            Field f = FMLClientHandler.class.getDeclaredField("resourcePackList");
            f.setAccessible(true);
            @SuppressWarnings("unchecked")
            List<IResourcePack> list = (List<IResourcePack>) f.get(FMLClientHandler.instance());
            list.add(new LppResourcePack(new File(Minecraft.getMinecraft().gameDir, "addons/lucky_block")));
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Could not install the asset resource pack: {}", t.toString());
        }
        MinecraftForge.EVENT_BUS.register(new LppClient.Hooks());
        RenderingRegistry.registerEntityRenderingHandler(AddedItems.Thrown.class, m -> new ThrownRender(m));
    }

    static final class ThrownRender extends RenderSnowball<AddedItems.Thrown> {
        ThrownRender(RenderManager m) {
            super(m, net.minecraft.init.Items.SNOWBALL, Minecraft.getMinecraft().getRenderItem());
        }

        @Override
        public ItemStack getStackToRender(AddedItems.Thrown e) {
            AddedItems.ThrowableItem it = e.getItem();
            return it != null ? new ItemStack(it) : new ItemStack(net.minecraft.init.Items.SNOWBALL);
        }
    }

    public static final class Hooks {
        @SubscribeEvent
        public void onModels(ModelRegistryEvent ev) {
            try {
                register();
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Model registration failed: {}", t.toString());
            }
        }
    }

    private static ModelResourceLocation inv(String path) {
        return new ModelResourceLocation(new ResourceLocation(LootPlusPlus.MODID, path), "inventory");
    }

    static void register() {
        for (Block b : LppRegistry.blocks.values()) {
            if (b instanceof BlockWall) ModelLoader.setCustomStateMapper(b, new StateMap.Builder().ignore(BlockWall.VARIANT).build());
        }
        for (Item item : LppRegistry.items.values()) {
            String path = item.getRegistryName().getPath();
            if (item instanceof AddedItems.Bow && LppRegistry.bows.contains(path) || item instanceof AddedItems.Bow) {
                final AddedItems.Bow bow = (AddedItems.Bow) item;
                final ModelResourceLocation base = inv(path);
                final ModelResourceLocation[] pull = {inv(path + "_pulling_0"), inv(path + "_pulling_1"), inv(path + "_pulling_2")};
                ModelBakery.registerItemVariants(item, base, pull[0], pull[1], pull[2]);
                ModelLoader.setCustomMeshDefinition(item, new ItemMeshDefinition() {
                    @Override
                    public ModelResourceLocation getModelLocation(ItemStack stack) {
                        EntityPlayer p = Minecraft.getMinecraft().player;
                        if (p != null && p.isHandActive() && p.getActiveItemStack().getItem() == bow) {
                            int used = stack.getMaxItemUseDuration() - p.getItemInUseCount();
                            int draw = bow.getDrawTime(stack);
                            if (used >= draw) return pull[2];
                            if (used > draw * 2 / 3) return pull[1];
                            if (used > 0) return pull[0];
                        }
                        return base;
                    }
                });
            } else if (item instanceof net.minecraft.item.ItemBlock && LppResourcePack.INSTANCE != null
                    && !LppResourcePack.INSTANCE.has("assets/" + LootPlusPlus.MODID + "/models/item/" + path + ".json")
                    && LppResourcePack.INSTANCE.has("assets/" + LootPlusPlus.MODID + "/blockstates/" + path + ".json")) {
                // 1.8 fell back to the block's "normal" variant when an item block had no item model.
                ModelLoader.setCustomModelResourceLocation(item, 0,
                        new ModelResourceLocation(new ResourceLocation(LootPlusPlus.MODID, path), "normal"));
            } else {
                ModelLoader.setCustomModelResourceLocation(item, 0, inv(path));
            }
        }
    }
}
