package com.electrifiedded.luckylegacyaddon.convert.fixers.resource;

import com.electrifiedded.luckylegacyaddon.convert.Converted;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PluginIds {
    private PluginIds() {}

    private static final Map<String, String> USED = new HashMap<>();
    private static final String[] RESERVED = {"lucky_block", "lucky_sword", "lucky_bow", "lucky_potion"};

    public static final String[] ROLES = {"block", "sword", "bow", "potion"};

    public static boolean knownBlockOrItem(String p) {
        for (String r : RESERVED) if (r.equals(p)) return true;
        synchronized (USED) {
            return USED.containsKey(p);
        }
    }

    public static final class Ids {
        public String block, sword, bow, potion;

        public String get(String role) {
            switch (role) {
                case "block": return block;
                case "sword": return sword;
                case "bow": return bow;
                default: return potion;
            }
        }

        public void set(String role, String v) {
            switch (role) {
                case "block": block = v; break;
                case "sword": sword = v; break;
                case "bow": bow = v; break;
                default: potion = v; break;
            }
        }
    }

    static String sanitize(String v) {
        String s = v.trim().toLowerCase(Locale.ROOT);
        int c = s.indexOf(':');
        if (c >= 0) s = s.substring(c + 1);
        return s.replaceAll("[^a-z0-9_]+", "_").replaceAll("^_+|_+$", "");
    }

    public static String camel(String id) {
        StringBuilder sb = new StringBuilder();
        for (String p : id.split("_")) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase(Locale.ROOT));
        }
        if (sb.length() == 0) return id;
        sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    public static Ids process(Converted c) {
        String txt = c.text("plugin_init.txt");
        if (txt == null) return null;
        Map<String, String> kv = new LinkedHashMap<>();
        StringBuilder extra = new StringBuilder();
        for (String raw : txt.split("\n")) {
            String l = raw.trim();
            if (l.isEmpty() || l.startsWith("/")) continue;
            int eq = l.indexOf('=');
            if (eq <= 0) {
                log(c, "plugin_init.txt: dropped line without '=': " + l);
                continue;
            }
            String k = l.substring(0, eq).trim().toLowerCase(Locale.ROOT);
            String v = l.substring(eq + 1).trim();
            switch (k) {
                case "id": case "block_id": case "blockid": case "block": k = "block"; break;
                case "sword_id": case "swordid": case "sword": k = "sword"; break;
                case "bow_id": case "bog_id": case "bowid": case "bow": k = "bow"; break;
                case "potion_id": case "potionid": case "potion": k = "potion"; break;
                default: extra.append(l).append('\n'); continue;
            }
            if (v.isEmpty()) continue;
            kv.put(k, v);
        }
        Ids ids = new Ids();
        for (String role : ROLES) {
            String v = kv.get(role);
            if (v == null) continue;
            String s = sanitize(v);
            if (s.isEmpty()) {
                log(c, "plugin_init.txt: empty " + role + " id '" + v + "' ignored");
                continue;
            }
            if (!s.equals(v)) log(c, "plugin_init.txt: " + role + " id '" + v + "' -> '" + s + "'");
            ids.set(role, s);
        }
        if (ids.block == null) {
            ids.block = "lucky_block_" + sanitize(c.name);
            log(c, "plugin_init.txt: no block id, using " + ids.block);
        }
        for (String role : ROLES) {
            String id = ids.get(role);
            if (id == null) continue;
            String uniq = unique(id, c.name + "#" + role);
            if (!uniq.equals(id)) {
                log(c, "id '" + id + "' (" + role + ") already registered by another add-on -> '" + uniq + "'");
                rename(c, id, uniq);
                ids.set(role, uniq);
            }
        }
        StringBuilder out = new StringBuilder();
        for (String role : ROLES) {
            String id = ids.get(role);
            if (id == null) continue;
            out.append(role.equals("block") ? "block_id" : role + "_id").append('=').append(id).append('\n');
        }
        out.append(extra);
        String res = out.toString();
        if (!res.equals(txt)) c.putText("plugin_init.txt", res);
        return ids;
    }

    public static String reserve(String id, String owner) {
        return unique(id, owner);
    }

    private static String unique(String id, String owner) {
        synchronized (USED) {
            String cand = id;
            int n = 2;
            while (true) {
                boolean reserved = false;
                for (String r : RESERVED) if (r.equals(cand)) reserved = true;
                String o = USED.get(cand);
                if (!reserved && (o == null || o.equals(owner))) {
                    USED.put(cand, owner);
                    return cand;
                }
                cand = id + "_" + n++;
            }
        }
    }

    public static void rename(Converted c, String from, String to) {
        Pattern file = Pattern.compile("^(.*/)?" + Pattern.quote(from) + "(?=[._]|$)(.*)$");
        Pattern ref = Pattern.compile("(?<![A-Za-z0-9_])" + Pattern.quote(from) + "(?![A-Za-z0-9])");
        Map<String, byte[]> add = new LinkedHashMap<>();
        List<String> del = new ArrayList<>();
        for (Map.Entry<String, byte[]> e : c.files.entrySet()) {
            String path = e.getKey();
            if (!path.startsWith("assets/")) continue;
            Matcher m = file.matcher(path);
            if (m.matches()) {
                del.add(path);
                add.put((m.group(1) == null ? "" : m.group(1)) + to + m.group(2), e.getValue());
            }
        }
        for (String d : del) c.files.remove(d);
        c.files.putAll(add);
        String fc = camel(from), tc = camel(to);
        for (String path : new ArrayList<>(c.files.keySet())) {
            String lp = path.toLowerCase(Locale.ROOT);
            boolean assetText = path.startsWith("assets/") && (lp.endsWith(".json") || lp.endsWith(".mcmeta") || lp.endsWith(".lang"));
            boolean luckyTxt = lp.equals("natural_gen.txt") || lp.equals("recipes.txt") || lp.equals("luck_crafting.txt")
                    || lp.equals("drops.txt") || lp.equals("sword_drops.txt") || lp.equals("bow_drops.txt")
                    || lp.equals("potion_drops.txt") || lp.equals("structures.txt");
            if (!assetText && !luckyTxt) continue;
            String t = c.text(path);
            if (t == null) continue;
            String n;
            if (lp.endsWith(".lang")) {
                n = t.replace("tile." + fc + ".", "tile." + tc + ".").replace("item." + fc + ".", "item." + tc + ".");
            } else if (lp.endsWith(".txt")) {
                n = t.replace("lucky:" + from, "lucky:" + to);
            } else {
                n = ref.matcher(t).replaceAll(Matcher.quoteReplacement(to));
            }
            if (!n.equals(t)) c.putText(path, n);
        }
    }

    static void log(Converted c, String s) {
        System.out.println("[LuckyLegacyAddon] [" + c.name + "] " + s);
    }
}
