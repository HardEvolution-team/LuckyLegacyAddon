package com.electrifiedded.luckylegacyaddon.loot;

import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemArmor.ArmorMaterial;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.EnumHelper;

import java.util.HashMap;
import java.util.Map;

final class LppItems {
    private LppItems() {}

    static final Map<String, ToolMaterial> toolMats = new HashMap<>();
    static final Map<String, ArmorMaterial> armorMats = new HashMap<>();

    private static String matKey(Item it, int meta) {
        return it.getRegistryName() + "@" + meta;
    }

    private static <T> T findMat(Map<String, T> map, Item it, int meta) {
        T t = map.get(matKey(it, meta));
        return t != null ? t : map.get(matKey(it, 32767));
    }

    private static int metaOr(String[] p, int idx) {
        if (p.length <= idx) return 32767;
        int m = LppUtil.i(p[idx]);
        return m < 0 ? 32767 : m;
    }

    private static void putDefaults() {
        Item planks = Item.getItemFromBlock(net.minecraft.init.Blocks.PLANKS);
        Item cobble = Item.getItemFromBlock(net.minecraft.init.Blocks.COBBLESTONE);
        toolMats.put(matKey(planks, 32767), ToolMaterial.WOOD);
        toolMats.put(matKey(cobble, 32767), ToolMaterial.STONE);
        toolMats.put(matKey(net.minecraft.init.Items.IRON_INGOT, 32767), ToolMaterial.IRON);
        toolMats.put(matKey(net.minecraft.init.Items.GOLD_INGOT, 32767), ToolMaterial.GOLD);
        toolMats.put(matKey(net.minecraft.init.Items.DIAMOND, 32767), ToolMaterial.DIAMOND);
        ToolMaterial leather = EnumHelper.addToolMaterial("lpp_leather", 0, 59, 2.0F, 0.0F, 15);
        if (leather != null) {
            leather.setRepairItem(new ItemStack(net.minecraft.init.Items.LEATHER));
            toolMats.put(matKey(net.minecraft.init.Items.LEATHER, 32767), leather);
        }
        armorMats.put(matKey(net.minecraft.init.Items.LEATHER, 32767), ArmorMaterial.LEATHER);
        armorMats.put(matKey(net.minecraft.init.Items.IRON_INGOT, 32767), ArmorMaterial.IRON);
        armorMats.put(matKey(net.minecraft.init.Items.GOLD_INGOT, 32767), ArmorMaterial.GOLD);
        armorMats.put(matKey(net.minecraft.init.Items.DIAMOND, 32767), ArmorMaterial.DIAMOND);
        ArmorMaterial wood = EnumHelper.addArmorMaterial("lpp_wood", "lpp_wood", 5, new int[]{1, 2, 3, 1}, 15, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.0F);
        if (wood != null) {
            wood.setRepairItem(new ItemStack(planks));
            armorMats.put(matKey(planks, 32767), wood);
        }
        ArmorMaterial stone = EnumHelper.addArmorMaterial("lpp_stone", "lpp_stone", 5, new int[]{1, 2, 3, 1}, 15, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.0F);
        if (stone != null) {
            stone.setRepairItem(new ItemStack(cobble));
            armorMats.put(matKey(cobble, 32767), stone);
        }
    }

    static void create() {
        try {
            putDefaults();
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Could not create the default materials: {}", t.toString());
        }
        LppUtil.each("item_additions/generic_items", (e, p) -> {
            LppUtil.needMin(e, p, 2);
            if (!LppRegistry.claim(e, p[0], "item")) return;
            LppRegistry.addItem(p[0], new AddedItems.Basic(p.length > 2 && LppUtil.b(p[2]), LppUtil.name(p[1])));
        });

        LppUtil.each("item_additions/foods", (e, p) -> {
            LppUtil.needMin(e, p, 8);
            if (!LppRegistry.claim(e, p[0], "item")) return;
            AddedItems.Food food = new AddedItems.Food(LppUtil.i(p[3]), LppUtil.f(p[4]), LppUtil.b(p[5]), LppUtil.b(p[6]),
                    LppUtil.i(p[7]), LppUtil.b(p[2]), LppUtil.name(p[1]));
            food.effects.addAll(LppEffects.parseAll(e, p, 8));
            LppRegistry.addItem(p[0], food);
        });

        LppUtil.each("item_additions/thrown", (e, p) -> {
            LppUtil.needMin(e, p, 7);
            if (!LppRegistry.claim(e, p[0], "item")) return;
            float chance = p.length > 7 ? LppUtil.f(p[7]) : 0.0F;
            LppDrops.DropList drops = new LppDrops.DropList();
            for (int k = 8; k < p.length; k++) drops.addField(e, p[k]);
            LppRegistry.addItem(p[0], new AddedItems.ThrowableItem(LppUtil.b(p[2]), LppUtil.f(p[3]), LppUtil.f(p[4]), LppUtil.f(p[5]),
                    LppUtil.f(p[6]), chance, drops, LppUtil.name(p[1])));
        });

        LppUtil.each("item_additions/bows", (e, p) -> {
            LppUtil.needMin(e, p, 9);
            if (!LppRegistry.claim(e, p[0], "item")) return;
            int durability = Math.max(1, LppUtil.i(p[2]));
            float dmg = Math.max(0, LppUtil.f(p[3]));
            int draw = LppUtil.i(p[4]);
            int arrows = LppUtil.i(p[5]);
            int ench = Math.max(1, LppUtil.i(p[6]));
            ItemStack repair = null;
            if (!p[7].equalsIgnoreCase("none")) {
                Item r = LppUtil.item(p[7]);
                if (r == null) throw LppUtil.skip("unknown repair item '" + p[7] + "'");
                repair = new ItemStack(r, 1, metaOr(p, 8));
            }
            String ammoName = p.length > 9 ? p[9] : "minecraft:arrow";
            Item ammo = LppUtil.item(ammoName);
            if (ammo == null) throw LppUtil.skip("unknown ammo item '" + ammoName + "'");
            if (ammo != net.minecraft.init.Items.ARROW && !(ammo instanceof AddedItems.ThrowableItem)) {
                throw LppUtil.skip("ammo must be arrow or a thrown item");
            }
            LppRegistry.addItem(p[0], new AddedItems.Bow(LppUtil.name(p[1]), dmg, draw, durability, arrows, ench, repair, ammo,
                    p.length > 10 ? p[10] : "random.bow"));
            LppRegistry.bows.add(p[0]);
        });

        LppUtil.each("item_additions/guns", (e, p) -> {
            LppUtil.needMin(e, p, 11);
            if (!LppRegistry.claim(e, p[0], "item")) return;
            int durability = Math.max(1, LppUtil.i(p[2]));
            float dmg = Math.max(0, LppUtil.f(p[3]));
            int wait = LppUtil.i(p[4]);
            int bullets = LppUtil.i(p[5]);
            int ench = Math.max(1, LppUtil.i(p[6]));
            ItemStack repair = null;
            if (!p[7].equalsIgnoreCase("none")) {
                Item r = LppUtil.item(p[7]);
                if (r == null) throw LppUtil.skip("unknown repair item '" + p[7] + "'");
                repair = new ItemStack(r, 1, metaOr(p, 8));
            }
            Item ammo = net.minecraft.init.Items.ARROW;
            if (!p[9].equalsIgnoreCase("none")) {
                ammo = LppUtil.item(p[9]);
                if (ammo == null) throw LppUtil.skip("unknown ammo item '" + p[9] + "'");
            } else {
                bullets = 0;
            }
            Item shot = LppUtil.item(p[10]);
            if (!(shot instanceof AddedItems.ThrowableItem)) throw LppUtil.skip("shot '" + p[10] + "' is not a thrown item");
            LppRegistry.addItem(p[0], new AddedItems.Gun(LppUtil.name(p[1]), dmg, wait, durability, bullets, ench, repair, ammo,
                    (AddedItems.ThrowableItem) shot, p.length > 11 ? p[11] : "random.bow"));
        });

        LppUtil.each("item_additions/multitools", (e, p) -> {
            LppUtil.need(e, p, 10);
            if (!LppRegistry.claim(e, p[0], "item")) return;
            ItemStack repair = null;
            if (!p[8].equalsIgnoreCase("none")) {
                Item r = LppUtil.item(p[8]);
                if (r == null) throw LppUtil.skip("unknown repair item '" + p[8] + "'");
                repair = new ItemStack(r, 1, metaOr(p, 9));
            }
            LppRegistry.addItem(p[0], new AddedItems.MultiTool(p[2].split("-"), LppUtil.i(p[3]), Math.max(1, LppUtil.i(p[4])),
                    LppUtil.f(p[5]), LppUtil.f(p[6]), LppUtil.i(p[7]), repair, LppUtil.name(p[1])));
        });

        LppUtil.each("item_additions/materials", (e, p) -> {
            LppUtil.need(e, p, 9);
            Item item = LppUtil.item(p[0]);
            if (item == null) throw LppUtil.skip("unknown material item '" + p[0] + "'");
            int meta = LppUtil.i(p[1]);
            if (meta < 0) meta = 32767;
            String key = matKey(item, meta);
            if (toolMats.containsKey(key)) {
                LootPlusPlus.duplicate(e, "material " + p[0]);
                return;
            }
            String[] s = p[8].split("-");
            if (s.length != 4) throw LppUtil.skip("armour strengths need 4 parts");
            int harvest = LppUtil.i(p[2]);
            int durability = LppUtil.i(p[3]);
            float eff = LppUtil.f(p[4]);
            float dmg = LppUtil.f(p[5]);
            int ench = LppUtil.i(p[6]);
            int armDur = LppUtil.i(p[7]);
            int helm = LppUtil.i(s[0]), chest = LppUtil.i(s[1]), legs = LppUtil.i(s[2]), boots = LppUtil.i(s[3]);
            String name = "lpp_" + item.getRegistryName().getPath() + "_" + meta;
            ToolMaterial tm = EnumHelper.addToolMaterial(name, harvest, durability, eff, dmg, ench);
            if (tm == null) throw LppUtil.skip("could not create tool material");
            tm.setRepairItem(new ItemStack(item, 1, meta));
            toolMats.put(key, tm);
            ArmorMaterial am = EnumHelper.addArmorMaterial(name, LootPlusPlus.MODID + ":" + name, armDur,
                    new int[]{boots, legs, chest, helm}, ench, SoundEvents.ITEM_ARMOR_EQUIP_GENERIC, 0.0F);
            if (am != null) {
                am.setRepairItem(new ItemStack(item, 1, meta));
                armorMats.put(key, am);
            }
        });

        tool("swords", 4, (e, p, m) -> new AddedItems.Sword(m, LppUtil.f(p[3]), LppUtil.name(p[1])), 4);
        tool("pickaxes", 3, (e, p, m) -> new AddedItems.Pickaxe(m, LppUtil.name(p[1])), 3);
        tool("axes", 3, (e, p, m) -> new AddedItems.Axe(m, LppUtil.name(p[1])), 3);
        tool("shovels", 3, (e, p, m) -> new AddedItems.Shovel(m, LppUtil.name(p[1])), 3);
        tool("hoes", 3, (e, p, m) -> new AddedItems.Hoe(m, LppUtil.name(p[1])), 3);

        armor("helmets", EntityEquipmentSlot.HEAD);
        armor("chestplates", EntityEquipmentSlot.CHEST);
        armor("leggings", EntityEquipmentSlot.LEGS);
        armor("boots", EntityEquipmentSlot.FEET);
    }

    private interface ToolFactory {
        Item make(LppConfig.Entry e, String[] p, ToolMaterial m);
    }

    private static void tool(String file, int min, ToolFactory f, int metaIdx) {
        LppUtil.each("item_additions/" + file, (e, p) -> {
            LppUtil.needMin(e, p, min);
            Item mat = LppUtil.item(p[2]);
            if (mat == null) throw LppUtil.skip("unknown material item '" + p[2] + "'");
            ToolMaterial m = findMat(toolMats, mat, metaOr(p, metaIdx));
            if (m == null) throw LppUtil.skip("no material for '" + p[2] + "' (is it in item_additions/materials?)");
            if (!LppRegistry.claim(e, p[0], "item")) return;
            LppRegistry.addItem(p[0], f.make(e, p, m));
        });
    }

    private static void armor(String file, EntityEquipmentSlot slot) {
        LppUtil.each("item_additions/" + file, (e, p) -> {
            LppUtil.needMin(e, p, 4);
            Item mat = LppUtil.item(p[2]);
            if (mat == null) throw LppUtil.skip("unknown material item '" + p[2] + "'");
            ArmorMaterial m = findMat(armorMats, mat, metaOr(p, 4));
            if (m == null) throw LppUtil.skip("no material for '" + p[2] + "' (is it in item_additions/materials?)");
            if (!LppRegistry.claim(e, p[0], "item")) return;
            LppRegistry.addItem(p[0], new AddedItems.Armor(m, slot, LppUtil.name(p[1]), p[3].trim()));
        });
    }
}
