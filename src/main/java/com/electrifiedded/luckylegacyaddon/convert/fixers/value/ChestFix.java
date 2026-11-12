package com.electrifiedded.luckylegacyaddon.convert.fixers.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class ChestFix {
    private ChestFix() {}

    private static final String START = "#chest(";

    static String convertAll(String s, Stats st) {
        int from = 0;
        while (true) {
            int i = s.indexOf(START, from);
            if (i < 0) return s;
            if (inQuotes(s, i)) { from = i + 1; continue; }
            int open = i + START.length() - 1;
            int close = matching(s, open);
            if (close < 0) return s;
            String compound = s.substring(open, close + 1);
            String json = null;
            try {
                json = toJson(compound);
            } catch (RuntimeException e) {
                System.out.println("[LuckyLegacyAddon] ValueFixer: could not convert #chest(...): " + e);
            }
            if (json == null) {
                from = close + 1;
                continue;
            }
            String rep = "#customChestLootTable(\"" + Scan.escape(json) + "\")";
            s = s.substring(0, i) + rep + s.substring(close + 1);
            st.count("chest-contents->loot-table-json");
            from = i + rep.length();
        }
    }

    private static boolean inQuotes(String s, int upTo) {
        boolean inQ = false;
        for (int i = 0; i < upTo; i++) if (s.charAt(i) == '"' && !Scan.canceled(s, i)) inQ = !inQ;
        return inQ;
    }

    private static int matching(String s, int open) {
        int depth = 0;
        boolean inQ = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Scan.canceled(s, i)) continue;
            if (c == '"') inQ = !inQ;
            if (inQ) continue;
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }

    private static List<String[]> pairs(String compound) {
        List<String[]> out = new ArrayList<>();
        String inner = compound.substring(1, compound.length() - 1);
        for (String t : Scan.split(inner, ',')) {
            if (t.trim().isEmpty()) continue;
            int eq = t.indexOf('=');
            if (eq < 0) throw new IllegalArgumentException("no '=' in " + t);
            out.add(new String[] {t.substring(0, eq).trim(), t.substring(eq + 1).trim()});
        }
        return out;
    }

    private static String get(List<String[]> ps, String key) {
        for (String[] p : ps) if (p[0].equalsIgnoreCase(key)) return p[1];
        return null;
    }

    private static Integer intOf(String v) {
        if (v == null) return null;
        v = v.trim();
        if (v.matches("-?\\d+[bBsS]?")) return Integer.parseInt(v.replaceAll("[bBsS]$", ""));
        return null;
    }

    static String toJson(String compound) {
        List<String[]> top = pairs(compound);
        String contents = get(top, "contents");
        if (contents == null || !Scan.wrapped(contents, '[', ']')) throw new IllegalArgumentException("no contents list");
        StringBuilder entries = new StringBuilder();
        for (String itemText : Scan.split(contents.substring(1, contents.length() - 1), ',')) {
            if (itemText.trim().isEmpty()) continue;
            if (!Scan.wrapped(itemText.trim(), '(', ')')) throw new IllegalArgumentException("bad item " + itemText);
            List<String[]> it = pairs(itemText.trim());
            String id = Scan.unquote(get(it, "id") == null ? "" : get(it, "id"));
            if (id.isEmpty()) { System.out.println("[LuckyLegacyAddon] ValueFixer: #chest item without id skipped"); continue; }
            if (id.indexOf('#') >= 0) throw new IllegalArgumentException("bad id " + id);
            if (id.indexOf(':') < 0) id = "minecraft:" + id;
            Integer min = intOf(get(it, "minAmount")), max = intOf(get(it, "maxAmount")), weight = intOf(get(it, "weight"));
            Integer fixedCount = intOf(get(it, "Count"));
            if (min == null && max == null && fixedCount != null) { min = fixedCount; max = fixedCount; }
            int mn = min == null ? 1 : min, mx = max == null ? 1 : max;
            if (mn == 0) mn = 1;
            if (mx == 0) mx = 2;
            if (mx < mn) mx = mn;
            int w = weight == null ? 1 : weight;
            Integer dmg = intOf(get(it, "Damage") != null ? get(it, "Damage") : get(it, "damage"));
            String tag = get(it, "tag") != null ? get(it, "tag") : get(it, "NBTTag");
            StringBuilder fn = new StringBuilder();
            fn.append("{\"function\":\"minecraft:set_count\",\"count\":");
            if (mn == mx) fn.append(mn);
            else fn.append("{\"min\":").append(mn).append(",\"max\":").append(mx).append('}');
            fn.append('}');
            if (dmg != null && dmg != 0) fn.append(",{\"function\":\"minecraft:set_data\",\"data\":").append(dmg).append('}');
            if (tag != null) {
                String snbt = null;
                try {
                    snbt = Snbt.convert(tag);
                } catch (RuntimeException e) {  }
                if (snbt != null) fn.append(",{\"function\":\"minecraft:set_nbt\",\"tag\":").append(Json.str(snbt)).append('}');
            }
            if (entries.length() > 0) entries.append(',');
            entries.append("{\"type\":\"item\",\"name\":").append(Json.str(id)).append(",\"weight\":").append(w)
                    .append(",\"functions\":[").append(fn).append("]}");
        }
        if (entries.length() == 0) throw new IllegalArgumentException("empty");
        String rolls = "12";
        String total = get(top, "maxTotalAmount");
        if (total != null) {
            Integer n = intOf(total);
            if (n != null) rolls = String.valueOf(n);
            else {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("#rand\\((\\d+),(\\d+)\\)").matcher(total);
                if (m.matches()) rolls = "{\"min\":" + m.group(1) + ",\"max\":" + m.group(2) + "}";
            }
        }
        return "{\"pools\":[{\"rolls\":" + rolls + ",\"entries\":[" + entries + "]}]}";
    }

    static final class Json {
        static String str(String s) {
            StringBuilder sb = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                if (c == '"' || c == '\\') sb.append('\\').append(c);
                else if (c == '\n') sb.append("\\n");
                else if (c < 0x20) sb.append(String.format(Locale.ROOT, "\\u%04x", (int) c));
                else sb.append(c);
            }
            return sb.append('"').toString();
        }
    }

    static final class Snbt {
        static String convert(String v) {
            v = v.trim();
            if (v.indexOf('#') >= 0 && !(Scan.isQuoted(v))) throw new IllegalArgumentException("dynamic");
            if (Scan.wrapped(v, '(', ')')) {
                StringBuilder sb = new StringBuilder("{");
                for (String t : Scan.split(v.substring(1, v.length() - 1), ',')) {
                    if (t.trim().isEmpty()) continue;
                    int eq = t.indexOf('=');
                    if (eq < 0) throw new IllegalArgumentException("no =");
                    if (sb.length() > 1) sb.append(',');
                    sb.append(t.substring(0, eq).trim()).append(':').append(convert(t.substring(eq + 1)));
                }
                return sb.append('}').toString();
            }
            if (Scan.wrapped(v, '[', ']')) {
                StringBuilder sb = new StringBuilder("[");
                for (String t : Scan.split(v.substring(1, v.length() - 1), ',')) {
                    if (t.trim().isEmpty()) continue;
                    if (sb.length() > 1) sb.append(',');
                    sb.append(convert(t));
                }
                return sb.append(']').toString();
            }
            if (Scan.isQuoted(v)) {
                if (v.indexOf('#') >= 0) throw new IllegalArgumentException("dynamic");
                return quote(Scan.unescape(v.substring(1, v.length() - 1)));
            }
            if (v.matches("-?\\d+(\\.\\d+)?[bBsSfFdDlL]?|true|false")) return v;
            return quote(v);
        }

        private static String quote(String s) {
            s = s.replace('$', '§');
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }
}
