package com.electrifiedded.luckylegacyaddon.convert.fixers.resource;

import com.electrifiedded.luckylegacyaddon.convert.Converted;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TextFiles {
    private TextFiles() {}

    private static void log(Converted c, String s) {
        System.out.println("[LuckyLegacyAddon] [" + c.name + "] " + s);
    }

    static List<String> logicalLines(String t) {
        List<String> logical = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean joining = false;
        for (String raw : t.split("\n")) {
            String l = raw.trim();
            if (l.isEmpty()) continue;
            if (l.startsWith("/")) {
                if (!joining) logical.add(l);
                continue;
            }
            if (l.endsWith("\\")) {
                cur.append(l, 0, l.length() - 1).append("");
                String s = cur.toString();
                cur.setLength(0);
                cur.append(s.trim());
                joining = true;
                continue;
            }
            cur.append(l);
            logical.add(cur.toString());
            cur.setLength(0);
            joining = false;
        }
        if (cur.length() > 0) logical.add(cur.toString());
        return logical;
    }

    private static boolean skippable(String l) {
        return l.isEmpty() || l.startsWith("/");
    }

    public static void properties(Converted c) {
        String t = c.text("properties.txt");
        if (t == null) return;
        StringBuilder out = new StringBuilder();
        for (String raw : t.split("\n")) {
            String l = raw.trim();
            if (skippable(l)) {
                if (!l.isEmpty()) out.append(l).append('\n');
                continue;
            }
            int eq = l.indexOf('=');
            if (eq <= 0) {
                log(c, "properties.txt: dropped line without '=': " + l);
                continue;
            }
            String k = l.substring(0, eq).trim();
            String v = l.substring(eq + 1).trim();
            if (k.equalsIgnoreCase("doDropsOnCreativeMode")) {
                k = "doDropsOnCreativeMode";
                if (!v.equalsIgnoreCase("true") && !v.equalsIgnoreCase("false")) {
                    log(c, "properties.txt: bad boolean '" + v + "' for doDropsOnCreativeMode -> false");
                    v = "false";
                }
                v = v.toLowerCase(Locale.ROOT);
            }
            out.append(k).append('=').append(v).append('\n');
        }
        put(c, "properties.txt", t, out.toString());
    }

    private static final Pattern NEG_COLON = Pattern.compile("^(.*[A-Za-z_]):(-\\d+)$");
    private static final Pattern LEAD_INT = Pattern.compile("^(-?\\d+)\\D*$");

    public static void luckCrafting(Converted c) {
        String t = c.text("luck_crafting.txt");
        if (t == null) return;
        StringBuilder out = new StringBuilder();
        for (String raw : t.split("\n")) {
            String l = raw.trim();
            if (skippable(l)) {
                if (!l.isEmpty()) out.append(l).append('\n');
                continue;
            }
            String line = l;
            if (line.indexOf('=') < 0) {
                Matcher m = NEG_COLON.matcher(line);
                if (m.matches()) line = m.group(1) + "=" + m.group(2);
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                log(c, "luck_crafting.txt: dropped unparsable line: " + l);
                continue;
            }
            String name = line.substring(0, eq).trim();
            String val = line.substring(eq + 1).trim();
            Matcher vm = LEAD_INT.matcher(val);
            if (!vm.matches()) {
                log(c, "luck_crafting.txt: dropped line with bad luck value: " + l);
                continue;
            }
            String fixed = ItemNames.fix(name);
            if (fixed == null) {
                log(c, "luck_crafting.txt: dropped unknown item '" + name + "'");
                continue;
            }
            String res = fixed + "=" + vm.group(1);
            if (!res.equals(l)) log(c, "luck_crafting.txt: '" + l + "' -> '" + res + "'");
            out.append(res).append('\n');
        }
        put(c, "luck_crafting.txt", t, out.toString());
    }

    public static void recipes(Converted c) {
        String t = c.text("recipes.txt");
        if (t == null) return;
        StringBuilder out = new StringBuilder();
        for (String l : logicalLines(t)) {
            if (l.isEmpty()) continue;
            if (l.startsWith("/")) {
                out.append(l).append('\n');
                continue;
            }
            String fixed = fixRecipe(c, l);
            if (fixed == null) continue;
            if (!fixed.equals(l)) log(c, "recipes.txt: '" + l + "' -> '" + fixed + "'");
            out.append(fixed).append('\n');
        }
        put(c, "recipes.txt", t, out.toString());
    }

    private static String fixRecipe(Converted c, String line) {
        String[] p = line.split(",");
        for (int i = 0; i < p.length; i++) p[i] = p[i].trim();
        if (p.length == 0 || p[0].isEmpty()) return null;
        if (ItemNames.fix(p[0]) != null) {
            List<String> parts = new ArrayList<>();
            for (String s : p) {
                String f = ItemNames.fix(s);
                if (f == null) {
                    log(c, "recipes.txt: dropped recipe with unknown ingredient '" + s + "': " + line);
                    return null;
                }
                parts.add(f);
            }
            if (parts.size() > 9) {
                log(c, "recipes.txt: dropped shapeless recipe with more than 9 ingredients: " + line);
                return null;
            }
            return String.join(",", parts);
        }
        int pairsStart = -1;
        for (int i = p.length - 1; i > 0; i -= 2) {
            if (p[i - 1].length() == 1 && ItemNames.fix(p[i]) != null) pairsStart = i - 1;
            else break;
        }
        int rows = pairsStart;
        if (pairsStart < 0 || rows < 1 || rows > 3) {
            log(c, "recipes.txt: dropped malformed recipe (bad shape or unknown ingredient): " + line);
            return null;
        }
        int width = p[0].length();
        Set<Character> keys = new HashSet<>();
        for (int i = pairsStart; i + 1 < p.length; i += 2) keys.add(p[i].charAt(0));
        StringBuilder res = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            if (p[r].length() != width || width > 3 || width == 0) {
                log(c, "recipes.txt: dropped recipe with ragged/oversized rows: " + line);
                return null;
            }
            for (char ch : p[r].toCharArray()) {
                if (ch != ' ' && !keys.contains(ch)) {
                    log(c, "recipes.txt: dropped recipe, no ingredient defined for '" + ch + "': " + line);
                    return null;
                }
            }
            res.append(p[r]).append(',');
        }
        for (int i = pairsStart; i + 1 < p.length; i += 2) res.append(p[i]).append(',').append(ItemNames.fix(p[i + 1])).append(',');
        res.setLength(res.length() - 1);
        return res.toString();
    }

    private static final Pattern LUCKY_REF = Pattern.compile("lucky:([A-Za-z0-9_]+)");
    private static final Pattern STRUCT_ID = Pattern.compile("(?i)(?:^|,)\\s*ID\\s*=\\s*([A-Za-z0-9_]+)");

    public static void naturalGen(Converted c, PluginIds.Ids ids) {
        String t = c.text("natural_gen.txt");
        if (t == null) return;
        Set<String> structs = new HashSet<>();
        String st = c.text("structures.txt");
        if (st != null) {
            for (String raw : st.split("\n")) {
                String l = raw.trim();
                if (l.startsWith("/")) continue;
                Matcher m = STRUCT_ID.matcher(l);
                if (m.find()) structs.add(m.group(1));
            }
        }
        StringBuilder out = new StringBuilder();
        for (String tl : logicalLines(t)) {
            if (tl.startsWith("/") || tl.isEmpty() || tl.startsWith(">")) {
                out.append(tl).append('\n');
                continue;
            }
            if (tl.indexOf('=') < 0) {
                log(c, "natural_gen.txt: dropped non-drop line: " + tl);
                continue;
            }
            String n = tl;
            Matcher m = LUCKY_REF.matcher(n);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                String ref = m.group(1);
                String rep = ref;
                if (ids != null && !ref.equals(ids.block) && !ref.equals(ids.sword) && !ref.equals(ids.bow) && !ref.equals(ids.potion)
                        && !PluginIds.knownBlockOrItem(ref) && ref.startsWith("lucky_block")) {
                    rep = ids.block;
                }
                m.appendReplacement(sb, Matcher.quoteReplacement("lucky:" + rep));
            }
            m.appendTail(sb);
            n = sb.toString();
            Matcher bm = Pattern.compile("type=block,ID=([A-Za-z0-9_]+)(?![A-Za-z0-9_:])").matcher(n);
            sb = new StringBuffer();
            while (bm.find()) {
                String id = bm.group(1);
                String rep = structs.contains(id) ? "type=structure,ID=" + id : bm.group(0);
                bm.appendReplacement(sb, Matcher.quoteReplacement(rep));
            }
            bm.appendTail(sb);
            n = sb.toString();
            int at = n.lastIndexOf('@');
            if (at > 0) {
                String head = n.substring(0, at);
                int open = count(head, '('), close = count(head, ')');
                while (close > open && head.endsWith(")")) {
                    head = head.substring(0, head.length() - 1);
                    close--;
                }
                n = head + n.substring(at);
            }
            if (!n.equals(tl)) log(c, "natural_gen.txt: '" + tl + "' -> '" + n + "'");
            out.append(n).append('\n');
        }
        put(c, "natural_gen.txt", t, out.toString());
    }

    private static int count(String s, char ch) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == ch) n++;
        return n;
    }

    public static void dropFileJunk(Converted c) {
        for (String file : new String[] {"bow_drops.txt", "sword_drops.txt", "potion_drops.txt"}) {
            String t = c.text(file);
            if (t == null) continue;
            StringBuilder out = new StringBuilder();
            for (String l : logicalLines(t)) {
                if (!l.startsWith("/") && l.indexOf('=') < 0) {
                    log(c, file + ": dropped non-drop line: " + (l.length() > 60 ? l.substring(0, 60) + "..." : l));
                    continue;
                }
                out.append(l).append('\n');
            }
            put(c, file, t, out.toString());
        }
    }

    public static void extraDropFiles(Converted c, PluginIds.Ids ids) {
        if (ids == null) return;
        String[][] map = {{"sword", "sword_drops.txt", "sword"}, {"bow", "bow_drops.txt", "bow"}, {"potion", "potion_drops.txt", "potion"}};
        for (String[] m : map) {
            String role = m[0], file = m[1];
            if (!c.files.containsKey(file)) continue;
            if (ids.get(role) != null) continue;
            String found = null;
            int cnt = 0;
            for (String path : c.files.keySet()) {
                String pl = path.toLowerCase(Locale.ROOT);
                if (!pl.startsWith("assets/lucky/models/item/") || !pl.endsWith(".json")) continue;
                String base = pl.substring(pl.lastIndexOf('/') + 1, pl.length() - 5);
                if (!base.contains(role) || base.contains("_pulling_")) continue;
                if (base.startsWith("lucky_" + role) && base.length() == ("lucky_" + role).length()) continue;
                found = base;
                cnt++;
            }
            if (cnt == 1) {
                String id = PluginIds.reserve(PluginIds.sanitize(found), c.name + "#" + role);
                ids.set(role, id);
                String pi = c.text("plugin_init.txt");
                if (pi == null) pi = "";
                if (!pi.endsWith("\n") && !pi.isEmpty()) pi += "\n";
                c.putText("plugin_init.txt", pi + role + "_id=" + id + "\n");
                log(c, "plugin_init.txt: " + file + " exists without " + role + "_id, derived '" + id + "' from the item model");
            } else {
                c.files.remove(file);
                log(c, file + " removed: add-on defines no " + role + " item (Lucky would fail with a NullPointerException)");
            }
        }
    }

    private static void put(Converted c, String path, String old, String now) {
        if (!now.equals(old)) c.putText(path, now);
    }
}
