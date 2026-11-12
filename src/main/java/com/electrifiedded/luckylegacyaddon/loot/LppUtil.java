package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.translation.I18n;

import javax.annotation.Nullable;
import java.util.Locale;

public final class LppUtil {
    private LppUtil() {}

    public interface LineHandler {
        void handle(LppConfig.Entry e, String[] p) throws Exception;
    }

    public static void each(String key, LineHandler h) {
        for (LppConfig.Entry e : LppConfig.get(key)) {
            try {
                h.handle(e, e.parts());
            } catch (NumberFormatException ex) {
                warn(e, "bad number (" + ex.getMessage() + ")");
            } catch (SkipLine ex) {
                warn(e, ex.getMessage());
            } catch (Throwable t) {
                warn(e, t.getClass().getSimpleName() + ": " + t.getMessage());
            }
        }
    }

    public static final class SkipLine extends RuntimeException {
        public SkipLine(String m) {
            super(m, null, false, false);
        }
    }

    public static SkipLine skip(String m) {
        return new SkipLine(m);
    }

    public static void warn(LppConfig.Entry e, String msg) {
        LootPlusPlus.LOG.warn("[Loot++] {}: {}", e.where(), msg);
    }

    public static void need(LppConfig.Entry e, String[] p, int exact) {
        if (p.length != exact) throw skip("wrong number of parts (" + p.length + ", expected " + exact + ")");
    }

    public static void needMin(LppConfig.Entry e, String[] p, int min) {
        if (p.length < min) throw skip("wrong number of parts (" + p.length + ", expected at least " + min + ")");
    }

    public static float f(String s) {
        return Float.parseFloat(s.trim());
    }

    public static int i(String s) {
        s = s.trim();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return (int) Double.parseDouble(s);
        }
    }

    public static boolean b(String s) {
        return Boolean.parseBoolean(s.trim());
    }

    public static String name(String s) {
        if (s.indexOf('�') >= 0) {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < s.length(); k++) {
                char c = s.charAt(k);
                if (c == '�') {
                    if (k + 1 < s.length() && "0123456789abcdefklmnorABCDEFKLMNOR".indexOf(s.charAt(k + 1)) >= 0) sb.append('§');
                } else {
                    sb.append(c);
                }
            }
            s = sb.toString();
        }
        return s.trim();
    }

    public static String display(String raw) {
        try {
            return I18n.translateToLocal(raw).trim();
        } catch (Throwable t) {
            return raw;
        }
    }

    public static String regName(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }

    @Nullable
    public static Item item(String id) {
        id = id.trim();
        int c = id.indexOf(':');
        if (c > 0 && id.substring(0, c).equalsIgnoreCase(LootPlusPlus.MODID)) {
            Item it = LppRegistry.items.get(regName(id.substring(c + 1)));
            if (it != null) return it;
        } else if (c < 0) {
            Item it = LppRegistry.items.get(regName(id));
            if (it != null && !Item.REGISTRY.containsKey(new ResourceLocation("minecraft", regName(id)))) return it;
        }
        try {
            Item it = Item.getByNameOrId(c < 0 ? id : id.toLowerCase(Locale.ROOT));
            if (it != null) return it;
        } catch (Throwable ignored) {
        }
        return null;
    }

    @Nullable
    public static Block block(String id) {
        id = id.trim();
        int c = id.indexOf(':');
        if (c > 0 && id.substring(0, c).equalsIgnoreCase(LootPlusPlus.MODID)) {
            Block b = LppRegistry.blocks.get(regName(id.substring(c + 1)));
            if (b != null) return b;
        }
        try {
            Block b = Block.getBlockFromName(c < 0 ? id : id.toLowerCase(Locale.ROOT));
            if (b != null && b != Blocks.AIR) return b;
        } catch (Throwable ignored) {
        }
        return LppRegistry.blocks.get(regName(c < 0 ? id : id.substring(c + 1)));
    }

    @Nullable
    public static Potion potion(String id) {
        id = id.trim();
        Potion p = Potion.getPotionFromResourceLocation(id);
        if (p != null) return p;
        try {
            return Potion.getPotionById(Integer.parseInt(id));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @Nullable
    public static NBTTagCompound nbt(String s) throws Exception {
        if (s == null) return null;
        s = s.trim();
        if (s.isEmpty() || s.equals("{}")) return null;
        try {
            return JsonToNBT.getTagFromJson(s);
        } catch (Exception e) {
            // 1.8 JsonToNBT was lenient: garbage bytes inside numbers (e.g. "0.�0" from bad encodings) did not
            // break the whole tag. Strip non-printable/non-ASCII junk between digits and retry.
            String fixed = s.replaceAll("(?<=[0-9.])[^\\x20-\\x7E\\u00A7]+(?=[0-9])", "");
            if (fixed.equals(s)) throw e;
            return JsonToNBT.getTagFromJson(fixed);
        }
    }

    public static ItemStack stack(Item item, int meta, @Nullable NBTTagCompound tag) {
        ItemStack st = new ItemStack(item, 1, Math.max(0, meta));
        if (tag != null) st.setTagCompound(tag);
        return st;
    }

    @Nullable
    public static Material material(String n) {
        switch (n.trim().toLowerCase(Locale.ROOT)) {
            case "air": return Material.AIR;
            case "anvil": return Material.ANVIL;
            case "cactus": return Material.CACTUS;
            case "cake": return Material.CAKE;
            case "carpet": return Material.CARPET;
            case "circuits": return Material.CIRCUITS;
            case "clay": return Material.CLAY;
            case "cloth": return Material.CLOTH;
            case "coral": return Material.CORAL;
            case "craftedsnow": return Material.CRAFTED_SNOW;
            case "dragonegg": return Material.DRAGON_EGG;
            case "fire": return Material.FIRE;
            case "glass": return Material.GLASS;
            case "gourd": return Material.GOURD;
            case "grass": return Material.GRASS;
            case "ground": return Material.GROUND;
            case "ice": return Material.ICE;
            case "iron": case "metal": return Material.IRON;
            case "lava": return Material.LAVA;
            case "leaves": return Material.LEAVES;
            case "packedice": return Material.PACKED_ICE;
            case "piston": return Material.PISTON;
            case "plants": return Material.PLANTS;
            case "portal": return Material.PORTAL;
            case "redstonelight": return Material.REDSTONE_LIGHT;
            case "rock": case "stone": return Material.ROCK;
            case "sand": return Material.SAND;
            case "snow": return Material.SNOW;
            case "sponge": return Material.SPONGE;
            case "tnt": return Material.TNT;
            case "vine": return Material.VINE;
            case "water": return Material.WATER;
            case "web": return Material.WEB;
            case "wood": return Material.WOOD;
            default: return null;
        }
    }

    public static SoundType sounds(String n) {
        switch (n.trim().toLowerCase(Locale.ROOT)) {
            case "anvil": return SoundType.ANVIL;
            case "cactus": case "cake": case "carpet": case "cloth": case "fire": return SoundType.CLOTH;
            case "clay": case "ground": return SoundType.GROUND;
            case "coral": case "grass": case "leaves": case "plants": case "sponge": case "tnt": case "vine": return SoundType.PLANT;
            case "craftedsnow": case "snow": return SoundType.SNOW;
            case "glass": case "ice": case "packedice": return SoundType.GLASS;
            case "gourd": case "wood": return SoundType.WOOD;
            case "iron": return SoundType.METAL;
            case "sand": return SoundType.SAND;
            default: return SoundType.STONE;
        }
    }
}
