package com.electrifiedded.luckylegacyaddon.convert.fixers.value;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class Tables {
    private Tables() {}

    static final Map<String, String> ENTITY = new HashMap<>();
    static final Set<String> ENTITY_VALID = new HashSet<>(Arrays.asList(
            "item", "xp_orb", "area_effect_cloud", "elder_guardian", "wither_skeleton", "stray", "egg", "leash_knot", "painting", "arrow",
            "snowball", "fireball", "small_fireball", "ender_pearl", "eye_of_ender_signal", "potion", "xp_bottle", "item_frame",
            "wither_skull", "tnt", "falling_block", "fireworks_rocket", "husk", "spectral_arrow", "shulker_bullet", "dragon_fireball",
            "zombie_villager", "skeleton_horse", "zombie_horse", "armor_stand", "donkey", "mule", "evocation_fangs", "evocation_illager",
            "vex", "vindication_illager", "illusion_illager", "commandblock_minecart", "boat", "minecart", "chest_minecart",
            "furnace_minecart", "tnt_minecart", "hopper_minecart", "spawner_minecart", "creeper", "skeleton", "spider", "giant", "zombie",
            "slime", "ghast", "zombie_pigman", "enderman", "cave_spider", "silverfish", "blaze", "magma_cube", "ender_dragon", "wither",
            "bat", "witch", "endermite", "guardian", "shulker", "pig", "sheep", "cow", "chicken", "squid", "wolf", "mooshroom", "snowman",
            "ocelot", "villager_golem", "horse", "rabbit", "polar_bear", "llama", "llama_spit", "parrot", "villager", "ender_crystal"));

    static {
        String[][] m = {
                {"XPOrb", "xp_orb"}, {"ThrownEgg", "egg"}, {"ThrownEnderpearl", "ender_pearl"}, {"EyeOfEnderSignal", "eye_of_ender_signal"},
                {"ThrownPotion", "potion"}, {"ThrownExpBottle", "xp_bottle"}, {"ItemFrame", "item_frame"}, {"WitherSkull", "wither_skull"},
                {"PrimedTnt", "tnt"}, {"FallingSand", "falling_block"}, {"FireworksRocketEntity", "fireworks_rocket"},
                {"ArmorStand", "armor_stand"}, {"MinecartRideable", "minecart"}, {"MinecartChest", "chest_minecart"},
                {"MinecartFurnace", "furnace_minecart"}, {"MinecartTNT", "tnt_minecart"}, {"MinecartHopper", "hopper_minecart"},
                {"MinecartSpawner", "spawner_minecart"}, {"MinecartCommandBlock", "commandblock_minecart"},
                {"PigZombie", "zombie_pigman"}, {"CaveSpider", "cave_spider"}, {"LavaSlime", "magma_cube"}, {"EnderDragon", "ender_dragon"},
                {"WitherBoss", "wither"}, {"MushroomCow", "mooshroom"}, {"Ozelot", "ocelot"}, {"VillagerGolem", "villager_golem"},
                {"EntityHorse", "horse"}, {"EnderCrystal", "ender_crystal"}, {"SmallFireball", "small_fireball"}, {"LeashKnot", "leash_knot"},
                {"SnowMan", "snowman"}, {"LuckyProjectile", "lucky:LuckyProjectile"}, {"LuckyPotion", "lucky:LuckyPotion"},
                {"AreaEffectCloud", "area_effect_cloud"}, {"SpectralArrow", "spectral_arrow"}, {"EntityHorse", "horse"},
                {"DragonFireball", "dragon_fireball"}, {"ShulkerBullet", "shulker_bullet"}, {"PolarBear", "polar_bear"},
                {"lootplusplus.ThrownItem", "lootplusplus:thrown_item"}
        };
        for (String[] e : m) ENTITY.put(e[0].toLowerCase(Locale.ROOT), e[1]);
    }

    static String entity(String name) {
        if (name == null || name.isEmpty() || name.indexOf(':') >= 0 || name.indexOf('#') >= 0 || name.indexOf('_') >= 0) return null;
        String lower = name.toLowerCase(Locale.ROOT);
        String mapped = ENTITY.get(lower);
        if (mapped != null) return mapped.equals(name) ? null : mapped;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isUpperCase(c) && i > 0) sb.append('_');
            sb.append(Character.toLowerCase(c));
        }
        String snake = sb.toString();
        if (ENTITY_VALID.contains(snake) && !snake.equals(lower)) return snake;
        return null;
    }

    static final Map<Integer, String> EGG = new HashMap<>();
    static {
        Object[][] e = {{50, "creeper"}, {51, "skeleton"}, {52, "spider"}, {54, "zombie"}, {55, "slime"}, {56, "ghast"},
                {57, "zombie_pigman"}, {58, "enderman"}, {59, "cave_spider"}, {60, "silverfish"}, {61, "blaze"}, {62, "magma_cube"},
                {65, "bat"}, {66, "witch"}, {67, "endermite"}, {68, "guardian"}, {90, "pig"}, {91, "sheep"}, {92, "cow"},
                {93, "chicken"}, {94, "squid"}, {95, "wolf"}, {96, "mooshroom"}, {98, "ocelot"}, {100, "horse"}, {101, "rabbit"},
                {120, "villager"}};
        for (Object[] x : e) EGG.put((Integer) x[0], (String) x[1]);
    }

    static final Map<String, String> CHEST = new HashMap<>();
    static {
        String[][] c = {
                {"dungeonchest", "chests/simple_dungeon"}, {"bonuschest", "chests/spawn_bonus_chest"},
                {"villageblacksmith", "chests/village_blacksmith"}, {"blacksmithchest", "chests/village_blacksmith"},
                {"blacksmith", "chests/village_blacksmith"}, {"mineshaftcorridor", "chests/abandoned_mineshaft"},
                {"pyramiddesertychest", "chests/desert_pyramid"}, {"pyramiddesertchest", "chests/desert_pyramid"},
                {"pyramidjunglechest", "chests/jungle_temple"}, {"pyramidjungledispenser", "chests/jungle_temple_dispenser"},
                {"strongholdcorridor", "chests/stronghold_corridor"}, {"strongholdcrossing", "chests/stronghold_crossing"},
                {"strongholdlibrary", "chests/stronghold_library"}, {"dungeon", "chests/simple_dungeon"},
        };
        for (String[] x : c) CHEST.put(x[0], x[1]);
    }

    static final String[] ENCH_VARS = {
            "#luckySwordEnchantments", "#luckyAxeEnchantments", "#luckyToolEnchantments", "#luckyHelmetEnchantments",
            "#luckyChestplateEnchantments", "#luckyLeggingsEnchantments", "#luckyBootsEnchantments", "#luckyBowEnchantments",
            "#luckyFishingRodEnchantments"};

    static final Map<Integer, String> POTION_BASE = new HashMap<>();
    static final Set<String> POTION_LONG = new HashSet<>(Arrays.asList("regeneration", "swiftness", "fire_resistance", "poison",
            "night_vision", "weakness", "strength", "slowness", "leaping", "water_breathing", "invisibility"));
    static final Set<String> POTION_STRONG = new HashSet<>(Arrays.asList("regeneration", "swiftness", "poison", "healing", "strength",
            "leaping", "harming", "slowness"));
    static {
        Object[][] p = {{1, "regeneration"}, {2, "swiftness"}, {3, "fire_resistance"}, {4, "poison"}, {5, "healing"},
                {6, "night_vision"}, {8, "weakness"}, {9, "strength"}, {10, "slowness"}, {11, "leaping"}, {12, "harming"},
                {13, "water_breathing"}, {14, "invisibility"}};
        for (Object[] x : p) POTION_BASE.put((Integer) x[0], (String) x[1]);
    }

    static final Map<String, String> SOUND = new HashMap<>();
    static {
        String[][] s = {
                {"ambient.weather.thunder", "entity.lightning.thunder"}, {"ambient.weather.rain", "weather.rain"},
                {"random.fizz", "block.fire.extinguish"}, {"random.break", "entity.item.break"}, {"random.levelup", "entity.player.levelup"},
                {"random.explode", "entity.generic.explode"}, {"random.anvil_land", "block.anvil.land"}, {"random.anvil_use", "block.anvil.use"},
                {"random.anvil_break", "block.anvil.break"}, {"random.chestopen", "block.chest.open"}, {"random.chestclosed", "block.chest.close"},
                {"random.click", "ui.button.click"}, {"random.pop", "entity.item.pickup"}, {"random.orb", "entity.experience_orb.pickup"},
                {"random.bow", "entity.arrow.shoot"}, {"random.bowhit", "entity.arrow.hit"}, {"random.burp", "entity.player.burp"},
                {"random.eat", "entity.generic.eat"}, {"random.drink", "entity.generic.drink"}, {"random.door_open", "block.wooden_door.open"},
                {"random.door_close", "block.wooden_door.close"}, {"random.splash", "entity.generic.splash"},
                {"random.fuse", "entity.tnt.primed"}, {"random.glass", "block.glass.break"}, {"random.wood_click", "block.wood_button.click_on"},
                {"item.fireCharge.use", "item.firecharge.use"}, {"fire.fire", "block.fire.ambient"}, {"fire.ignite", "item.flintandsteel.use"},
                {"note.pling", "block.note.pling"}, {"note.bassattack", "block.note.bass"}, {"note.bass", "block.note.bass"},
                {"note.harp", "block.note.harp"}, {"note.hat", "block.note.hat"}, {"note.snare", "block.note.snare"},
                {"note.bd", "block.note.basedrum"},
                {"game.neutral.swim.splash", "entity.generic.splash"}, {"game.player.swim.splash", "entity.generic.splash"},
                {"game.hostile.swim.splash", "entity.generic.splash"}, {"game.neutral.swim", "entity.generic.swim"},
                {"game.player.swim", "entity.generic.swim"}, {"game.tnt.primed", "entity.tnt.primed"},
                {"portal.travel", "block.portal.travel"}, {"portal.portal", "block.portal.ambient"}, {"portal.trigger", "block.portal.trigger"},
                {"liquid.lavapop", "block.lava.pop"}, {"liquid.lava", "block.lava.ambient"}, {"liquid.water", "block.water.ambient"},
                {"tile.piston.in", "block.piston.contract"}, {"tile.piston.out", "block.piston.extend"},
                {"mob.zombie.unfect", "entity.zombie_villager.converted"}, {"mob.zombie.remedy", "entity.zombie_villager.cure"},
                {"mob.zombie.infect", "entity.zombie.infect"}, {"mob.zombie.say", "entity.zombie.ambient"},
                {"mob.blaze.breathe", "entity.blaze.ambient"}, {"mob.ghast.scream", "entity.ghast.scream"},
                {"mob.ghast.moan", "entity.ghast.ambient"}, {"mob.ghast.death", "entity.ghast.death"},
                {"mob.ghast.fireball", "entity.ghast.shoot"}, {"mob.ghast.charge", "entity.ghast.warn"},
                {"mob.enderdragon.growl", "entity.enderdragon.growl"}, {"mob.enderdragon.end", "entity.enderdragon.death"},
                {"mob.enderdragon.wings", "entity.enderdragon.flap"}, {"mob.endermen.portal", "entity.endermen.teleport"},
                {"mob.endermen.scream", "entity.endermen.scream"}, {"mob.villager.yes", "entity.villager.yes"},
                {"mob.villager.no", "entity.villager.no"}, {"mob.villager.idle", "entity.villager.ambient"},
                {"mob.villager.hit", "entity.villager.hurt"}, {"mob.villager.death", "entity.villager.death"},
                {"mob.slime.big", "entity.slime.squish"}, {"mob.slime.small", "entity.small_slime.squish"},
                {"mob.guardian.curse", "entity.elder_guardian.curse"}, {"mob.wither.death", "entity.wither.death"},
                {"mob.wither.spawn", "entity.wither.spawn"}, {"mob.wither.idle", "entity.wither.ambient"},
                {"mob.wither.hurt", "entity.wither.hurt"}, {"mob.wither.shoot", "entity.wither.shoot"},
                {"mob.cat.purr", "entity.cat.purr"}, {"mob.cat.hitt", "entity.cat.hiss"}, {"mob.cat.meow", "entity.cat.ambient"},
                {"mob.cow.hurt", "entity.cow.hurt"}, {"mob.cow.say", "entity.cow.ambient"}, {"mob.chicken.say", "entity.chicken.ambient"},
                {"mob.pig.say", "entity.pig.ambient"}, {"mob.sheep.say", "entity.sheep.ambient"}, {"mob.creeper.say", "entity.creeper.hurt"},
                {"mob.creeper.death", "entity.creeper.death"}, {"mob.skeleton.say", "entity.skeleton.ambient"},
                {"mob.spider.say", "entity.spider.ambient"}, {"mob.wolf.growl", "entity.wolf.growl"}, {"mob.wolf.howl", "entity.wolf.howl"},
                {"mob.wolf.bark", "entity.wolf.ambient"}, {"mob.bat.takeoff", "entity.bat.takeoff"}, {"mob.horse.death", "entity.horse.death"},
                {"mob.irongolem.death", "entity.irongolem.death"}, {"mob.irongolem.hit", "entity.irongolem.hurt"},
                {"mob.silverfish.say", "entity.silverfish.ambient"}, {"mob.skeleton.death", "entity.skeleton.death"},
                {"mob.zombiepig.zpig", "entity.zombie_pig.ambient"}, {"mob.endermen.idle", "entity.endermen.ambient"},
                {"dig.cloth", "block.cloth.break"}, {"dig.glass", "block.glass.break"}, {"dig.grass", "block.grass.break"},
                {"dig.gravel", "block.gravel.break"}, {"dig.sand", "block.sand.break"}, {"dig.snow", "block.snow.break"},
                {"dig.stone", "block.stone.break"}, {"dig.wood", "block.wood.break"},
                {"step.cloth", "block.cloth.step"}, {"step.grass", "block.grass.step"}, {"step.gravel", "block.gravel.step"},
                {"step.ladder", "block.ladder.step"}, {"step.sand", "block.sand.step"}, {"step.snow", "block.snow.step"},
                {"step.stone", "block.stone.step"}, {"step.wood", "block.wood.step"},
        };
        for (String[] x : s) SOUND.put(x[0].toLowerCase(Locale.ROOT), x[1]);
    }

    public static String sound(String name) {
        if (name == null || name.indexOf(':') >= 0 || name.indexOf('#') >= 0) return null;
        String l = name.toLowerCase(Locale.ROOT);
        String m = SOUND.get(l);
        if (m != null) return m;
        if (l.startsWith("records.")) return "record." + l.substring("records.".length());
        return null;
    }

    static String potionType(int damage) {
        int base = damage & 15;
        if (base == 0) {
            int low = damage & 0x3F;
            if (low == 0) return "water";
            if (low == 16) return "awkward";
            if (low == 32) return "thick";
            if (low == 64) return "mundane";
            return null;
        }
        String name = POTION_BASE.get(base);
        if (name == null) return null;
        boolean lng = (damage & 0x20) != 0, strong = (damage & 0x40) != 0;
        if (strong && POTION_STRONG.contains(name)) return "strong_" + name;
        if (lng && POTION_LONG.contains(name)) return "long_" + name;
        return name;
    }
}
