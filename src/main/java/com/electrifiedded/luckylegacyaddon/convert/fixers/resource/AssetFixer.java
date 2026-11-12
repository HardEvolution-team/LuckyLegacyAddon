package com.electrifiedded.luckylegacyaddon.convert.fixers.resource;

import com.electrifiedded.luckylegacyaddon.convert.Converted;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class AssetFixer {
    private AssetFixer() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String STUB_PNG = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z/D/PwAG/gL+DHWJ3gAAAABJRU5ErkJggg==";
    private static Set<String> luckyDefaults;

    private static void log(Converted c, String s) {
        System.out.println("[LuckyLegacyAddon] [" + c.name + "] " + s);
    }

    private static synchronized Set<String> luckyDefaults() {
        if (luckyDefaults != null) return luckyDefaults;
        Set<String> s = new HashSet<>();
        try (InputStream in = AssetFixer.class.getResourceAsStream("/lucky_default_assets.txt")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String l;
                while ((l = r.readLine()) != null) if (!l.isBlank()) s.add(l.trim());
            }
        } catch (Throwable ignored) {
        }
        luckyDefaults = s;
        return s;
    }

    public static void fix(Converted c, PluginIds.Ids ids) {
        removeJunk(c);
        fixLang(c);
        lowercasePaths(c);
        fixJson(c);
        if (ids != null) {
            adoptOrphanItemAssets(c, ids);
            ensureItemAssets(c, ids);
        }
        fixTextureRefs(c, ids);
        if (ids != null) ensureLang(c, ids);
        fixPackMcmeta(c);
    }

    private static void removeJunk(Converted c) {
        List<String> del = new ArrayList<>();
        for (String p : c.files.keySet()) {
            String l = p.toLowerCase(Locale.ROOT);
            String base = l.substring(l.lastIndexOf('/') + 1);
            if (base.equals(".ds_store") || base.equals("thumbs.db") || base.equals("desktop.ini") || l.startsWith("__macosx/")
                    || l.contains("/__macosx/") || base.startsWith("._")) del.add(p);
        }
        for (String d : del) c.files.remove(d);
    }

    private static void fixLang(Converted c) {
        List<String> langs = new ArrayList<>();
        for (String p : c.files.keySet()) if (p.matches("assets/[^/]+/lang/[^/]+\\.lang")) langs.add(p);
        for (String p : langs) {
            String dir = p.substring(0, p.lastIndexOf('/') + 1);
            String base = p.substring(dir.length(), p.length() - 5);
            int u = base.indexOf('_');
            String nb = u > 0 ? base.substring(0, u).toLowerCase(Locale.ROOT) + "_" + base.substring(u + 1).toUpperCase(Locale.ROOT) : base;
            String np = dir + nb + ".lang";
            if (np.equals(p)) continue;
            if (c.files.containsKey(np)) {
                String a = c.text(np), b = c.text(p);
                c.putText(np, a + (a.endsWith("\n") ? "" : "\n") + b);
                c.files.remove(p);
            } else {
                c.files.put(np, c.files.remove(p));
            }
            log(c, "renamed lang file " + p + " -> " + np);
        }
    }

    private static void ensureLang(Converted c, PluginIds.Ids ids) {
        String path = "assets/lucky/lang/en_US.lang";
        String t = c.text(path);
        if (t == null) t = "";
        StringBuilder add = new StringBuilder();
        String[][] roles = {{"block", "tile"}, {"sword", "item"}, {"bow", "item"}, {"potion", "item"}};
        for (String[] r : roles) {
            String id = ids.get(r[0]);
            if (id == null) continue;
            String key = r[1] + "." + PluginIds.camel(id) + ".name=";
            if (t.contains(key) || t.contains(key.substring(0, key.length() - 1))) continue;
            add.append(key).append(title(id)).append('\n');
        }
        if (add.length() > 0) {
            if (!t.isEmpty() && !t.endsWith("\n")) t += "\n";
            c.putText(path, t + add);
            log(c, "added missing lang entries:\n" + add.toString().trim());
        }
    }

    private static String title(String id) {
        StringBuilder sb = new StringBuilder();
        for (String p : id.split("_")) {
            if (p.isEmpty()) continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

    private static JsonElement parse(String text, boolean lenient) throws Exception {
        JsonReader r = new JsonReader(new StringReader(text));
        r.setLenient(lenient);
        JsonElement e = com.google.gson.internal.Streams.parse(r);
        if (!lenient && r.peek() != com.google.gson.stream.JsonToken.END_DOCUMENT) throw new IllegalStateException("trailing data");
        return e;
    }

    private static void fixJson(Converted c) {
        List<String> jsons = new ArrayList<>();
        for (String p : c.files.keySet()) {
            if (p.startsWith("assets/") && (p.endsWith(".json") || p.endsWith(".mcmeta"))) jsons.add(p);
        }
        for (String p : jsons) {
            String t = c.text(p);
            if (t == null) continue;
            byte[] raw = c.files.get(p);
            boolean bom = raw != null && raw.length >= 3 && (raw[0] & 0xFF) == 0xEF && (raw[1] & 0xFF) == 0xBB && (raw[2] & 0xFF) == 0xBF;
            try {
                parse(t, false);
                if (bom) c.putText(p, t);
                continue;
            } catch (Throwable ignored) {
            }
            try {
                JsonElement e = parse(t, true);
                c.putText(p, GSON.toJson(e) + "\n");
                log(c, "re-serialised non strict json " + p);
            } catch (Throwable ex) {
                c.files.remove(p);
                log(c, "removed unparsable json " + p + " (" + ex.getMessage() + ")");
            }
        }
    }

    private static void put(Converted c, String path, String text) {
        if (!c.files.containsKey(path)) {
            c.putText(path, text);
            log(c, "generated missing " + path);
        }
    }

    private static void ensureItemAssets(Converted c, PluginIds.Ids ids) {
        String b = ids.block;
        if (b != null) {
            put(c, "assets/lucky/blockstates/" + b + ".json", "{\n  \"variants\": {\n    \"normal\": { \"model\": \"lucky:" + b + "\" }\n  }\n}\n");
            put(c, "assets/lucky/models/block/" + b + ".json",
                    "{\n  \"parent\": \"block/cube_all\",\n  \"textures\": { \"all\": \"lucky:blocks/" + b + "\" }\n}\n");
            put(c, "assets/lucky/models/item/" + b + ".json", "{\n  \"parent\": \"lucky:block/" + b + "\"\n}\n");
        }
        for (String role : new String[] {"sword", "potion"}) {
            String id = ids.get(role);
            if (id == null) continue;
            put(c, "assets/lucky/models/item/" + id + ".json",
                    "{\n  \"parent\": \"builtin/generated\",\n  \"textures\": { \"layer0\": \"lucky:items/" + id + "\" }\n}\n");
        }
        String bow = ids.bow;
        if (bow != null) {
            put(c, "assets/lucky/models/item/" + bow + ".json",
                    "{\n  \"parent\": \"builtin/generated\",\n  \"textures\": { \"layer0\": \"lucky:items/" + bow + "_standby\" }\n}\n");
            for (int i = 0; i < 3; i++) {
                put(c, "assets/lucky/models/item/" + bow + "_pulling_" + i + ".json",
                        "{\n  \"parent\": \"builtin/generated\",\n  \"textures\": { \"layer0\": \"lucky:items/" + bow + "_pulling_" + i + "\" }\n}\n");
            }
        }
    }

    private static void collectBlockModels(Converted c, String id, Set<String> out) {
        String bs = c.text("assets/lucky/blockstates/" + id + ".json");
        if (bs == null) return;
        try {
            collectModelRefs(parse(bs, true), out);
        } catch (Throwable ignored) {
        }
    }

    private static void collectModelRefs(JsonElement e, Set<String> out) {
        if (e.isJsonObject()) {
            for (Map.Entry<String, JsonElement> en : e.getAsJsonObject().entrySet()) {
                if (en.getKey().equals("model") && en.getValue().isJsonPrimitive()) {
                    String v = en.getValue().getAsString();
                    int col = v.indexOf(':');
                    String ns = col >= 0 ? v.substring(0, col) : "minecraft";
                    String p = col >= 0 ? v.substring(col + 1) : v;
                    if (!ns.equals("minecraft")) out.add("assets/" + ns + "/models/block/" + p + ".json");
                } else {
                    collectModelRefs(en.getValue(), out);
                }
            }
        } else if (e.isJsonArray()) {
            for (JsonElement x : e.getAsJsonArray()) collectModelRefs(x, out);
        }
    }

    private static void fixTextureRefs(Converted c, PluginIds.Ids ids) {
        Set<String> defaults = luckyDefaults();
        Set<String> queue = new java.util.LinkedHashSet<>();
        if (ids != null) {
            if (ids.block != null) {
                collectBlockModels(c, ids.block, queue);
                queue.add("assets/lucky/models/item/" + ids.block + ".json");
            }
            if (ids.sword != null) queue.add("assets/lucky/models/item/" + ids.sword + ".json");
            if (ids.potion != null) queue.add("assets/lucky/models/item/" + ids.potion + ".json");
            if (ids.bow != null) {
                queue.add("assets/lucky/models/item/" + ids.bow + ".json");
                for (int i = 0; i < 3; i++) queue.add("assets/lucky/models/item/" + ids.bow + "_pulling_" + i + ".json");
            }
        }
        Set<String> seen = new HashSet<>();
        Map<String, String> missing = new TreeMap<>();
        List<String> work = new ArrayList<>(queue);
        while (!work.isEmpty()) {
            String p = work.remove(work.size() - 1);
            if (!seen.add(p)) continue;
            String t = c.text(p);
            if (t == null) continue;
            JsonObject o;
            try {
                JsonElement el = parse(t, true);
                if (!el.isJsonObject()) continue;
                o = el.getAsJsonObject();
            } catch (Throwable ex) {
                continue;
            }
            if (o.has("parent") && o.get("parent").isJsonPrimitive()) {
                String par = o.get("parent").getAsString();
                int col = par.indexOf(':');
                if (col >= 0 && !par.startsWith("minecraft:")) {
                    work.add(("assets/" + par.substring(0, col) + "/models/" + par.substring(col + 1) + ".json").toLowerCase(Locale.ROOT));
                }
            }
            if (o.has("textures") && o.get("textures").isJsonObject()) {
                for (Map.Entry<String, JsonElement> te : o.getAsJsonObject("textures").entrySet()) {
                    if (!te.getValue().isJsonPrimitive()) continue;
                    String ref = te.getValue().getAsString();
                    if (ref.startsWith("#")) continue;
                    String tns = "minecraft", tp = ref;
                    int col = ref.indexOf(':');
                    if (col >= 0) {
                        tns = ref.substring(0, col);
                        tp = ref.substring(col + 1);
                    }
                    if (!tns.equals("lucky")) continue;
                    String file = ("assets/" + tns + "/textures/" + tp + ".png").toLowerCase(Locale.ROOT);
                    if (c.files.containsKey(file) || defaults.contains(file)) continue;
                    missing.put(file, p);
                }
            }
        }
        for (Map.Entry<String, String> m : missing.entrySet()) {
            c.files.put(m.getKey(), Base64.getDecoder().decode(STUB_PNG));
            log(c, "stub texture " + m.getKey() + " (referenced by " + m.getValue() + ", not present in the add-on)");
        }
    }

    private static void lowercasePaths(Converted c) {
        List<String> all = new ArrayList<>(c.files.keySet());
        for (String p : all) {
            if (!p.startsWith("assets/") || p.matches("assets/[^/]+/lang/[^/]+")) continue;
            String lp = p.toLowerCase(Locale.ROOT);
            if (lp.equals(p) || c.files.containsKey(lp)) continue;
            c.files.put(lp, c.files.remove(p));
            log(c, "lower-cased asset path " + p);
        }
    }

    private static boolean hasModel(Converted c, String id) {
        return c.files.containsKey("assets/lucky/models/item/" + id + ".json");
    }

    private static void adoptOrphanItemAssets(Converted c, PluginIds.Ids ids) {
        Set<String> owned = new HashSet<>();
        for (String r : PluginIds.ROLES) if (ids.get(r) != null) owned.add(ids.get(r));
        for (String role : new String[] {"sword", "potion", "bow"}) {
            String id = ids.get(role);
            if (id == null) continue;
            if (role.equals("bow") && !hasModel(c, id) && c.files.containsKey("assets/lucky/models/item/" + id + "_standby.json")) {
                c.files.put("assets/lucky/models/item/" + id + ".json", c.files.get("assets/lucky/models/item/" + id + "_standby.json"));
                log(c, "bow model " + id + ".json created from the _standby model");
            }
            if (hasModel(c, id)) continue;
            Set<String> roots = new java.util.TreeSet<>();
            for (String p : c.files.keySet()) {
                if (!p.startsWith("assets/lucky/models/item/") || !p.endsWith(".json")) continue;
                String base = p.substring(p.lastIndexOf('/') + 1, p.length() - 5);
                String root = base.replaceAll("_(pulling_\\d+|standby)$", "");
                if (!root.contains(role) || owned.contains(root) || root.startsWith("lucky_block")) continue;
                if (root.matches(".*\\d$")) continue;
                roots.add(root);
            }
            if (roots.size() == 1) {
                String root = roots.iterator().next();
                log(c, "item assets '" + root + "' belong to " + role + " id '" + id + "' -> renamed");
                PluginIds.rename(c, root, id);
            }
        }
    }

    private static void fixPackMcmeta(Converted c) {
        String t = c.text("pack.mcmeta");
        String desc = c.name.replace("\"", "");
        if (t != null) {
            try {
                JsonElement e = parse(t, true);
                if (e.isJsonObject() && e.getAsJsonObject().has("pack") && e.getAsJsonObject().get("pack").isJsonObject()) {
                    JsonObject pack = e.getAsJsonObject().getAsJsonObject("pack");
                    if (pack.has("description") && pack.get("description").isJsonPrimitive()) desc = pack.get("description").getAsString();
                }
            } catch (Throwable ignored) {
                log(c, "pack.mcmeta was not valid json, rewritten");
            }
        }
        JsonObject pack = new JsonObject();
        pack.addProperty("pack_format", 3);
        pack.addProperty("description", desc);
        JsonObject root = new JsonObject();
        root.add("pack", pack);
        String res = GSON.toJson(root) + "\n";
        if (!res.equals(t)) c.putText("pack.mcmeta", res);
    }
}
