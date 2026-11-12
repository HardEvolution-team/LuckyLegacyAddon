package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class LppRegistry {
    static final Map<String, Block> blocks = new LinkedHashMap<>();
    static final Map<String, Item> items = new LinkedHashMap<>();
    static final List<String> bows = new ArrayList<>();
    static final List<Object[]> pendingCrops = new ArrayList<>();
    static final Map<Item, AddedBlocks.Crop> seedToCrop = new HashMap<>();

    private static final Map<String, LppConfig.Entry> claimed = new HashMap<>();
    private static boolean blocksDone, itemsDone;

    static boolean claim(LppConfig.Entry e, String rawName, String kind) {
        String key = LppUtil.regName(rawName);
        if (key.isEmpty() || key.indexOf(':') >= 0 || key.indexOf(' ') >= 0 || !key.matches("[a-z0-9/._-]+")) {
            throw LppUtil.skip("invalid " + kind + " name '" + rawName + "'");
        }
        LppConfig.Entry old = claimed.get(key);
        if (old == null) {
            claimed.put(key, e);
            return true;
        }
        if (old.text.equals(e.text)) {
            LootPlusPlus.duplicate(e, kind + " " + rawName);
        } else {
            LppUtil.warn(e, kind + " '" + rawName + "' is already defined by " + old.where() + " - this definition is ignored");
        }
        return false;
    }

    static void addBlock(String rawName, Block block) {
        addBlock(rawName, block, new BlockItem(block));
    }

    static void addBlock(String rawName, Block block, Item itemBlock) {
        String key = LppUtil.regName(rawName);
        block.setRegistryName(LootPlusPlus.MODID, key);
        blocks.put(key, block);
        if (itemBlock != null) {
            itemBlock.setRegistryName(LootPlusPlus.MODID, key);
            items.put(key, itemBlock);
        }
    }

    static void addItem(String rawName, Item item) {
        String key = LppUtil.regName(rawName);
        item.setRegistryName(LootPlusPlus.MODID, key);
        item.setTranslationKey(key);
        items.put(key, item);
    }

    public static class BlockItem extends ItemBlock {
        public BlockItem(Block b) {
            super(b);
        }

        @Override
        public String getItemStackDisplayName(ItemStack stack) {
            return block instanceof AddedBlocks.Added ? LppUtil.display(((AddedBlocks.Added) block).displayName())
                    : super.getItemStackDisplayName(stack);
        }
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> ev) {
        if (blocksDone) return;
        blocksDone = true;
        try {
            LppBlocks.create();
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Block loading failed: {}", t.toString());
        }
        for (Block b : blocks.values()) {
            try {
                ev.getRegistry().register(b);
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Could not register block {}: {}", b.getRegistryName(), t.toString());
            }
        }
        if (anyFurnace()) {
            GameRegistry.registerTileEntity(AddedBlocks.FurnaceTile.class, new ResourceLocation(LootPlusPlus.MODID, "furnace"));
        }
    }

    private static boolean anyFurnace() {
        for (Block b : blocks.values()) if (b instanceof AddedBlocks.Furnace) return true;
        return false;
    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> ev) {
        if (itemsDone) return;
        itemsDone = true;
        try {
            LppItems.create();
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Item loading failed: {}", t.toString());
        }
        for (Item i : new ArrayList<>(items.values())) {
            try {
                ev.getRegistry().register(i);
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Could not register item {}: {}", i.getRegistryName(), t.toString());
            }
        }
        LootPlusPlus.LOG.info("[Loot++] Registered {} block(s) and {} item(s) in domain '{}'", blocks.size(), items.size(), LootPlusPlus.MODID);
    }

    static void resolveCrops() {
        for (Object[] c : pendingCrops) {
            AddedBlocks.Crop crop = (AddedBlocks.Crop) c[0];
            LppConfig.Entry e = (LppConfig.Entry) c[5];
            Item seed = LppUtil.item((String) c[1]);
            if (seed == null) {
                LppUtil.warn(e, "unknown seed item '" + c[1] + "' - crop cannot be planted");
                continue;
            }
            int meta = (Integer) c[2];
            crop.seed = new ItemStack(seed, 1, meta < 0 ? 0 : meta);
            if (seedToCrop.containsKey(seed)) {
                LppUtil.warn(e, "another crop already uses seed '" + c[1] + "'");
                continue;
            }
            seedToCrop.put(seed, crop);
        }
        pendingCrops.clear();
    }

    @SuppressWarnings("unused")
    private static void unused(TileEntity t) {
    }
}
