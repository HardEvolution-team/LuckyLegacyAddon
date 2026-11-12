package com.electrifiedded.luckylegacyaddon.convert.fixers.structure;

import com.electrifiedded.luckylegacyaddon.convert.fixers.structure.Nbt.ListVal;
import com.electrifiedded.luckylegacyaddon.convert.fixers.structure.Nbt.Tag;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class SchematicRepair {
    private SchematicRepair() {}

    public static final int LIMIT = 100000;

    public static final class Out {
        public byte[] data;
        public boolean changed;
        public int w, l, h;
        public int oldW, oldL, oldH;
        public int offX, offY, offZ;
        public boolean cropped;
        public final StringBuilder notes = new StringBuilder();

        void note(String s) {
            if (notes.length() > 0) notes.append("; ");
            notes.append(s);
        }
    }


    private static final Set<String> ENTITIES = new HashSet<>(Arrays.asList(("item,xp_orb,area_effect_cloud,elder_guardian,wither_skeleton,stray,egg,leash_knot,"
            + "painting,arrow,snowball,fireball,small_fireball,ender_pearl,eye_of_ender_signal,potion,xp_bottle,item_frame,wither_skull,tnt,falling_block,"
            + "fireworks_rocket,husk,spectral_arrow,shulker_bullet,dragon_fireball,zombie_villager,skeleton_horse,zombie_horse,armor_stand,donkey,mule,"
            + "evocation_fangs,evocation_illager,vex,vindication_illager,illusion_illager,commandblock_minecart,boat,minecart,chest_minecart,"
            + "furnace_minecart,tnt_minecart,hopper_minecart,spawner_minecart,creeper,skeleton,spider,giant,zombie,slime,ghast,zombie_pigman,enderman,"
            + "cave_spider,silverfish,blaze,magma_cube,ender_dragon,wither,bat,witch,endermite,guardian,shulker,pig,sheep,cow,chicken,squid,wolf,"
            + "mooshroom,snowman,ocelot,villager_golem,horse,rabbit,polar_bear,llama,llama_spit,parrot,villager,ender_crystal").split(",")));

    private static final Map<String, String> ENTITY_LEGACY = map("mushroomcow", "mooshroom", "cavespider", "cave_spider", "primedtnt", "tnt",
            "minecartrideable", "minecart", "minecartchest", "chest_minecart", "minecartfurnace", "furnace_minecart", "minecarttnt", "tnt_minecart",
            "minecarthopper", "hopper_minecart", "minecartspawner", "spawner_minecart", "minecartcommandblock", "commandblock_minecart",
            "endercrystal", "ender_crystal", "entityhorse", "horse", "ozelot", "ocelot", "pigzombie", "zombie_pigman", "lavaslime", "magma_cube",
            "villagergolem", "villager_golem", "enderdragon", "ender_dragon", "witherboss", "wither", "xporb", "xp_orb", "fallingsand", "falling_block",
            "thrownenderpearl", "ender_pearl", "thrownpotion", "potion", "thrownegg", "egg", "thrownexpbottle", "xp_bottle", "eyeofendersignal",
            "eye_of_ender_signal", "fireworksrocketentity", "fireworks_rocket", "smallfireball", "small_fireball", "witherskull", "wither_skull",
            "dragonfireball", "dragon_fireball", "leashknot", "leash_knot", "armorstand", "armor_stand", "itemframe", "item_frame");

    private static final Set<String> TILES = new HashSet<>(Arrays.asList(("furnace,chest,trapped_chest,ender_chest,jukebox,dispenser,dropper,sign,mob_spawner,"
            + "noteblock,piston,brewing_stand,enchanting_table,end_portal,beacon,skull,daylight_detector,hopper,comparator,flower_pot,banner,"
            + "structure_block,end_gateway,command_block,shulker_box,bed").split(",")));

    private static final Map<String, String> TILE_LEGACY = map("control", "command_block", "music", "noteblock", "recordplayer", "jukebox", "trap",
            "dispenser", "cauldron", "brewing_stand", "enchanttable", "enchanting_table", "airportal", "end_portal", "dldetector", "daylight_detector",
            "flowerpot", "flower_pot", "enderchest", "ender_chest", "mobspawner", "mob_spawner", "endgateway", "end_gateway", "structureblock",
            "structure_block", "trappedchest", "trapped_chest", "brewingstand", "brewing_stand");

    private static Map<String, String> map(String... kv) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(kv[i], kv[i + 1]);
        return m;
    }

    private static String strip(String id) {
        String s = id.trim().toLowerCase();
        if (s.startsWith("minecraft:")) s = s.substring(10);
        return s;
    }

    static String entityId(String id, Map<String, Tag> nbt) {
        if (id == null) return null;
        String low = id.trim().toLowerCase();
        if (low.contains(":") && !low.startsWith("minecraft:")) return low.startsWith("lucky:") ? id : null;
        String s = strip(id);
        if (s.equals("entityhorse") || s.equals("horse")) {
            int t = nbt == null ? 0 : intOf(nbt.get("Type"), 0);
            if (nbt != null && nbt.containsKey("Type") && nbt.get("Type").num() != null) {
                switch (t) {
                    case 1: return "minecraft:donkey";
                    case 2: return "minecraft:mule";
                    case 3: return "minecraft:zombie_horse";
                    case 4: return "minecraft:skeleton_horse";
                    default: break;
                }
            }
            return "minecraft:horse";
        }
        if (s.equals("skeleton") && nbt != null && intOf(nbt.get("SkeletonType"), 0) == 1) return "minecraft:wither_skeleton";
        if (s.equals("guardian") && nbt != null && intOf(nbt.get("Elder"), 0) == 1) return "minecraft:elder_guardian";
        String m = ENTITY_LEGACY.get(s.replace("_", ""));
        if (m != null) s = m;
        return ENTITIES.contains(s) ? "minecraft:" + s : null;
    }

    static String tileId(String id) {
        if (id == null) return null;
        String low = id.trim().toLowerCase();
        if (low.equals("luckyblockdata") || low.equals("lucky:luckyblockdata")) return id;
        if (low.contains(":") && !low.startsWith("minecraft:")) return null;
        String s = strip(id);
        String m = TILE_LEGACY.get(s.replace("_", ""));
        if (m != null) s = m;
        return TILES.contains(s) ? "minecraft:" + s : null;
    }


    static int intOf(Tag t, int def) {
        return t != null && t.num() != null ? t.num().intValue() : def;
    }

    private static String str(Map<String, Tag> m, String k) {
        Tag t = m.get(k);
        return t != null && t.type == Nbt.STRING ? (String) t.value : null;
    }

    private static ListVal list(Map<String, Tag> m, String k, int elemType, Out out) {
        Tag t = m.get(k);
        if (t != null && t.type == Nbt.LIST) {
            ListVal l = (ListVal) t.value;
            if (l.elemType == elemType) return l;
            if (l.items.isEmpty()) {
                l.elemType = elemType;
                return l;
            }
            if (!l.items.isEmpty()) out.note("bad list " + k);
        } else if (t != null) {
            out.note("bad list " + k);
        }
        ListVal nl = new ListVal(elemType);
        m.put(k, new Tag(Nbt.LIST, nl));
        out.changed = true;
        return nl;
    }


    public static Out repair(byte[] in) {
        Nbt.Result r = Nbt.read(in);
        if (r == null || r.root == null) return null;
        try {
            return repair0(r);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Out repair0(Nbt.Result r) throws Exception {
        Out out = new Out();
        Map<String, Tag> root = r.root;
        if (r.truncated) {
            out.changed = true;
            out.note("truncated file salvaged");
        }
        if (!r.wasGzip) {
            out.changed = true;
            out.note("not gzip");
        }
        int w = intOf(root.get("Width"), 0), l = intOf(root.get("Length"), 0), h = intOf(root.get("Height"), 0);
        if (w <= 0 || l <= 0 || h <= 0 || (long) w * l * h > 200_000_000L) return null;
        out.oldW = w;
        out.oldL = l;
        out.oldH = h;
        int n = w * l * h;

        byte[] blocks = fit(bytes(root, "Blocks"), n, out, "Blocks");
        byte[] data = fit(bytes(root, "Data"), n, out, "Data");
        byte[] add = bytes(root, "AddBlocks");
        int fixedIds = 0;
        boolean maskedData = false;
        boolean[] modded = moddedIds(root);
        for (int i = 0; i < n; i++) {
            int id = blocks[i] & 255;
            if (add != null && (i >> 1) < add.length) {
                int nib = (i & 1) == 0 ? add[i >> 1] & 15 : (add[i >> 1] >> 4) & 15;
                if (nib != 0) id |= nib << 8;
            }
            boolean foreign = id > 255 || (modded != null && id < modded.length && modded[id]);
            if (foreign) {
                if (blocks[i] != 0 || data[i] != 0 || id > 255) fixedIds++;
                blocks[i] = 0;
                data[i] = 0;
            } else if ((data[i] & ~15) != 0) {
                data[i] = (byte) (data[i] & 15);
                out.changed = true;
                maskedData = true;
            }
        }
        if (maskedData) out.note("data values masked to 4 bits");
        if (fixedIds > 0) {
            out.changed = true;
            out.note(fixedIds + " mod/unknown blocks -> air");
        }
        if (add != null) {
            root.remove("AddBlocks");
            out.changed = true;
        }

        ListVal ents = list(root, "Entities", Nbt.COMPOUND, out);
        ListVal tiles = list(root, "TileEntities", Nbt.COMPOUND, out);

        if ((long) w * l * h > LIMIT) {
            cropPlan(out, blocks, w, l, h);
            int nw = out.w, nl = out.l, nh = out.h;
            byte[] nb = new byte[nw * nl * nh], nd = new byte[nw * nl * nh];
            for (int y = 0; y < nh; y++) {
                for (int z = 0; z < nl; z++) {
                    int src = ((y + out.offY) * l + (z + out.offZ)) * w + out.offX;
                    int dst = (y * nl + z) * nw;
                    System.arraycopy(blocks, src, nb, dst, nw);
                    System.arraycopy(data, src, nd, dst, nw);
                }
            }
            blocks = nb;
            data = nd;
            out.cropped = true;
            out.changed = true;
            out.note("cropped " + w + "x" + l + "x" + h + " -> " + nw + "x" + nl + "x" + nh + " (offset " + out.offX + "," + out.offY + "," + out.offZ + ")");
            w = nw;
            l = nl;
            h = nh;
            root.remove("Biomes");
            root.remove("TileTicks");
            shift(ents, tiles, out, w, l, h);
        }
        out.w = w;
        out.l = l;
        out.h = h;

        root.put("Width", new Tag(Nbt.SHORT, (short) w));
        root.put("Length", new Tag(Nbt.SHORT, (short) l));
        root.put("Height", new Tag(Nbt.SHORT, (short) h));
        root.put("Blocks", new Tag(Nbt.BYTE_ARRAY, blocks));
        root.put("Data", new Tag(Nbt.BYTE_ARRAY, data));
        if (!root.containsKey("Materials")) root.put("Materials", new Tag(Nbt.STRING, "Alpha"));

        fixEntities(ents, out);
        fixTiles(tiles, out);

        if (!out.changed) {
            out.data = null;
        } else {
            out.data = Nbt.writeGzip(root, "Schematic");
        }
        return out;
    }

    private static boolean[] moddedIds(Map<String, Tag> root) {
        Tag t = root.get("BlockIDs");
        if (t == null || t.type != Nbt.COMPOUND) return null;
        boolean[] m = new boolean[4096];
        for (Map.Entry<String, Tag> e : Nbt.compound(t.value).entrySet()) {
            if (e.getValue().type != Nbt.STRING) continue;
            String name = ((String) e.getValue().value).toLowerCase();
            if (name.startsWith("minecraft:")) continue;
            try {
                int id = Integer.parseInt(e.getKey().trim());
                if (id >= 0 && id < m.length) m[id] = true;
            } catch (NumberFormatException ignored) {
            }
        }
        return m;
    }

    private static byte[] bytes(Map<String, Tag> m, String k) {
        Tag t = m.get(k);
        return t != null && t.type == Nbt.BYTE_ARRAY ? (byte[]) t.value : null;
    }

    private static byte[] fit(byte[] a, int n, Out out, String what) {
        if (a == null) {
            out.changed = true;
            out.note(what + " missing");
            return new byte[n];
        }
        if (a.length != n) {
            out.changed = true;
            out.note(what + " length " + a.length + " != " + n);
            return Arrays.copyOf(a, n);
        }
        return a;
    }

    private static void cropPlan(Out out, byte[] blocks, int w, int l, int h) {
        int x0 = w, y0 = h, z0 = l, x1 = -1, y1 = -1, z1 = -1;
        int i = 0;
        for (int y = 0; y < h; y++) {
            for (int z = 0; z < l; z++) {
                for (int x = 0; x < w; x++, i++) {
                    if (blocks[i] != 0) {
                        if (x < x0) x0 = x;
                        if (x > x1) x1 = x;
                        if (y < y0) y0 = y;
                        if (y > y1) y1 = y;
                        if (z < z0) z0 = z;
                        if (z > z1) z1 = z;
                    }
                }
            }
        }
        if (x1 < 0) {
            x0 = 0;
            x1 = 0;
            y0 = 0;
            y1 = 0;
            z0 = 0;
            z1 = 0;
        }
        x1++;
        y1++;
        z1++;
        int cx = Math.max(x0, Math.min(x1 - 1, w / 2)), cz = Math.max(z0, Math.min(z1 - 1, l / 2));
        while ((long) (x1 - x0) * (y1 - y0) * (z1 - z0) > LIMIT) {
            int lx = x1 - x0, ly = y1 - y0, lz = z1 - z0;
            if (lx >= lz && lx >= ly * 2) {
                int nlen = Math.max(1, Math.min(lx - 1, (int) (lx * 0.93)));
                int na = Math.max(x0, Math.min(cx - nlen / 2, x1 - nlen));
                x0 = na;
                x1 = na + nlen;
            } else if (lz >= ly * 2) {
                int nlen = Math.max(1, Math.min(lz - 1, (int) (lz * 0.93)));
                int na = Math.max(z0, Math.min(cz - nlen / 2, z1 - nlen));
                z0 = na;
                z1 = na + nlen;
            } else {
                int nlen = Math.max(1, Math.min(ly - 1, (int) (ly * 0.93)));
                y1 = y0 + nlen;
            }
        }
        out.offX = x0;
        out.offY = y0;
        out.offZ = z0;
        out.w = x1 - x0;
        out.h = y1 - y0;
        out.l = z1 - z0;
    }

    private static void shift(ListVal ents, ListVal tiles, Out out, int w, int l, int h) {
        for (Iterator<Object> it = tiles.items.iterator(); it.hasNext(); ) {
            Map<String, Tag> t = Nbt.compound(it.next());
            int x = intOf(t.get("x"), 0) - out.offX, y = intOf(t.get("y"), 0) - out.offY, z = intOf(t.get("z"), 0) - out.offZ;
            if (x < 0 || y < 0 || z < 0 || x >= w || y >= h || z >= l) {
                it.remove();
                continue;
            }
            t.put("x", new Tag(Nbt.INT, x));
            t.put("y", new Tag(Nbt.INT, y));
            t.put("z", new Tag(Nbt.INT, z));
        }
        for (Iterator<Object> it = ents.items.iterator(); it.hasNext(); ) {
            Map<String, Tag> e = Nbt.compound(it.next());
            Tag pos = e.get("Pos");
            if (pos != null && pos.type == Nbt.LIST && ((ListVal) pos.value).items.size() == 3 && ((ListVal) pos.value).elemType == Nbt.DOUBLE) {
                ListVal p = (ListVal) pos.value;
                double x = (Double) p.items.get(0) - out.offX, y = (Double) p.items.get(1) - out.offY, z = (Double) p.items.get(2) - out.offZ;
                if (x < 0 || y < 0 || z < 0 || x >= w || y >= h + 2 || z >= l) {
                    it.remove();
                    continue;
                }
                p.items.set(0, x);
                p.items.set(1, y);
                p.items.set(2, z);
            }
            String[] k = {"TileX", "TileY", "TileZ"};
            int[] off = {out.offX, out.offY, out.offZ};
            for (int a = 0; a < 3; a++) {
                Tag t = e.get(k[a]);
                if (t != null && t.num() != null) e.put(k[a], new Tag(Nbt.INT, t.num().intValue() - off[a]));
            }
        }
    }


    private static void fixEntities(ListVal ents, Out out) {
        int removed = 0, mapped = 0;
        for (Iterator<Object> it = ents.items.iterator(); it.hasNext(); ) {
            Map<String, Tag> e = Nbt.compound(it.next());
            String id = str(e, "id");
            String nid = entityId(id, e);
            if (nid == null) {
                it.remove();
                removed++;
                out.changed = true;
                continue;
            }
            if (!nid.equals(id)) {
                e.put("id", new Tag(Nbt.STRING, nid));
                out.changed = true;
                mapped++;
            }
        }
        if (removed > 0) out.note(removed + " unknown entities removed");
        if (mapped > 0) out.note(mapped + " entity ids renamed");
    }

    private static void fixTiles(ListVal tiles, Out out) {
        int removed = 0, mapped = 0;
        for (Iterator<Object> it = tiles.items.iterator(); it.hasNext(); ) {
            Map<String, Tag> t = Nbt.compound(it.next());
            String id = str(t, "id");
            String nid = tileId(id);
            if (nid == null) {
                it.remove();
                removed++;
                out.changed = true;
                continue;
            }
            if (!nid.equals(id)) {
                t.put("id", new Tag(Nbt.STRING, nid));
                out.changed = true;
                mapped++;
            }
            if (nid.equals("minecraft:mob_spawner") && fixSpawner(t)) out.changed = true;
        }
        if (removed > 0) out.note(removed + " unknown tile entities removed");
        if (mapped > 0) out.note(mapped + " tile entity ids renamed");
    }

    private static boolean fixSpawner(Map<String, Tag> t) {
        boolean ch = false;
        String legacy = str(t, "EntityId");
        Tag sd = t.get("SpawnData");
        if (legacy != null) {
            String nid = entityId(legacy, null);
            Map<String, Tag> spawn = sd != null && sd.type == Nbt.COMPOUND ? Nbt.compound(sd.value) : new LinkedHashMap<>();
            if (nid != null && !spawn.containsKey("id")) spawn.put("id", new Tag(Nbt.STRING, nid));
            if (nid != null) t.put("SpawnData", new Tag(Nbt.COMPOUND, spawn));
            t.remove("EntityId");
            ch = true;
        } else if (sd != null && sd.type == Nbt.COMPOUND) {
            Map<String, Tag> spawn = Nbt.compound(sd.value);
            if (spawn.containsKey("id")) {
                String old = str(spawn, "id");
                String nid = entityId(old, spawn);
                if (nid == null) {
                    t.remove("SpawnData");
                    ch = true;
                } else if (!nid.equals(old)) {
                    spawn.put("id", new Tag(Nbt.STRING, nid));
                    ch = true;
                }
            } else if (spawn.isEmpty() || !spawn.containsKey("id")) {
                if (legacy == null) {
                    t.remove("SpawnData");
                    ch = true;
                }
            }
        }
        Tag sp = t.get("SpawnPotentials");
        if (sp != null && sp.type == Nbt.LIST && ((ListVal) sp.value).elemType == Nbt.COMPOUND) {
            ListVal l = (ListVal) sp.value;
            for (Iterator<Object> it = l.items.iterator(); it.hasNext(); ) {
                Map<String, Tag> e = Nbt.compound(it.next());
                Map<String, Tag> ent;
                if (e.containsKey("Entity") && e.get("Entity").type == Nbt.COMPOUND) {
                    ent = Nbt.compound(e.get("Entity").value);
                } else {
                    String type = str(e, "Type");
                    Tag props = e.get("Properties");
                    ent = props != null && props.type == Nbt.COMPOUND ? Nbt.compound(props.value) : new LinkedHashMap<>();
                    if (type != null) ent.put("id", new Tag(Nbt.STRING, type));
                    e.remove("Type");
                    e.remove("Properties");
                    e.put("Entity", new Tag(Nbt.COMPOUND, ent));
                    ch = true;
                }
                String old = str(ent, "id");
                String nid = entityId(old, ent);
                if (nid == null) {
                    it.remove();
                    ch = true;
                } else if (!nid.equals(old)) {
                    ent.put("id", new Tag(Nbt.STRING, nid));
                    ch = true;
                }
            }
        }
        return ch;
    }

    public static byte[] placeholder() {
        try {
            Map<String, Tag> m = new LinkedHashMap<>();
            m.put("Width", new Tag(Nbt.SHORT, (short) 1));
            m.put("Length", new Tag(Nbt.SHORT, (short) 1));
            m.put("Height", new Tag(Nbt.SHORT, (short) 1));
            m.put("Materials", new Tag(Nbt.STRING, "Alpha"));
            m.put("Blocks", new Tag(Nbt.BYTE_ARRAY, new byte[1]));
            m.put("Data", new Tag(Nbt.BYTE_ARRAY, new byte[1]));
            m.put("Entities", new Tag(Nbt.LIST, new ListVal(Nbt.COMPOUND)));
            m.put("TileEntities", new Tag(Nbt.LIST, new ListVal(Nbt.COMPOUND)));
            return Nbt.writeGzip(m, "Schematic");
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
