package com.electrifiedded.luckylegacyaddon.convert.fixers.dropsyntax;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DropFileRepairer {
    public static final class Output {
        public String text;
        public boolean changed;
        public final Map<String, Integer> counts = new LinkedHashMap<>();
        public final List<String> examples = new ArrayList<>();
    }

    private static final Pattern LEADING_NUM = Pattern.compile("^\\s*([-+]?\\d+(?:\\.\\d+)?)");

    private DropFileRepairer() {}

    private static final class Item {
        final boolean comment;
        String text;
        Item(boolean comment, String text) { this.comment = comment; this.text = text; }
    }

    public static Output repair(String text) {
        Output out = new Output();
        boolean endsNl = text.endsWith("\n");
        String[] phys = text.split("\n", -1);
        int cnt = phys.length - (endsNl ? 1 : 0);
        List<Item> items = new ArrayList<>();
        StringBuilder cur = null;
        for (int i = 0; i < cnt; i++) {
            String t = phys[i].trim();
            if (t.startsWith("/") || t.isEmpty()) {
                if (cur == null) items.add(new Item(true, phys[i]));
                continue;
            }
            if (t.endsWith("\\")) {
                if (cur == null) cur = new StringBuilder();
                cur.append(t, 0, t.length() - 1).append("");
                String joined = cur.toString();
                cur = new StringBuilder(joined.trim());
                continue;
            }
            String line = cur == null ? t : cur.append(t).toString();
            cur = null;
            items.add(new Item(false, line));
        }
        if (cur != null) items.add(new Item(false, cur.toString()));

        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            if (it.comment) continue;
            int joined = 0;
            while (i + 1 < items.size() && isOpen(it.text) && !items.get(i + 1).comment
                    && !DROP_START.matcher(items.get(i + 1).text).find() && !isDanglingNumber(it.text)) {
                it.text = it.text + items.get(i + 1).text;
                items.remove(i + 1);
                joined++;
            }
            if (joined > 0) {
                note(out, "joined", "re-joined hard-wrapped drop (" + joined + " line(s)): " + abbreviate(it.text));
                out.changed = true;
            }
        }

        List<Item> merged = new ArrayList<>();
        for (Item it : items) {
            if (it.comment) {
                merged.add(it);
                continue;
            }
            Item prev = lastDrop(merged);
            if (prev != null && (prev.text.endsWith("@luck=") || prev.text.endsWith("@chance="))) {
                Matcher m = LEADING_NUM.matcher(it.text);
                if (m.find()) {
                    prev.text = prev.text + m.group(1);
                    String rest = it.text.substring(m.end()).trim();
                    note(out, "glued", "value of dangling modifier taken from next line: " + abbreviate(it.text));
                    out.changed = true;
                    if (!rest.isEmpty() && !rest.startsWith("/") && !rest.startsWith("@")) {
                        it.text = rest;
                    } else if (rest.startsWith("@")) {
                        prev.text = prev.text + rest;
                        continue;
                    } else {
                        continue;
                    }
                }
            }
            Modifiers mod = Modifiers.parse(it.text);
            if (mod.core.isEmpty() && (mod.luck != null || mod.chance != null) && mod.extraDrops.isEmpty()) {
                if (prev != null) {
                    prev.text = prev.text + mod.suffix();
                    note(out, "attached", "modifier-only line attached to previous drop: " + abbreviate(it.text));
                } else {
                    note(out, "removed", "orphan modifier line removed: " + abbreviate(it.text));
                }
                out.changed = true;
                continue;
            }
            merged.add(it);
        }

        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Item it : merged) {
            List<String> lines = new ArrayList<>();
            if (it.comment) {
                lines.add(it.text);
            } else {
                DropRepairer.Result r = DropRepairer.repair(it.text);
                if (!r.removed) for (String n : r.notes) note(out, ruleOf(n), n);
                if (r.removed) {
                    lines.add("// [LuckyLegacyAddon] removed (not a valid drop): " + abbreviate(it.text));
                    out.changed = true;
                    note(out, "removed", "removed line: " + abbreviate(it.text));
                } else {
                    lines.addAll(r.lines);
                    if (r.changed) out.changed = true;
                }
            }
            for (String l : lines) {
                if (!first) sb.append('\n');
                sb.append(l);
                first = false;
            }
        }
        if (endsNl) sb.append('\n');
        out.text = sb.toString();
        return out;
    }

    private static final Pattern DROP_START =
            Pattern.compile("^\\s*(?:(?i:group)\\s*[(:]|[A-Za-z_][A-Za-z0-9_]*\\s*=|@(?:luck|chance)=)");

    private static boolean isDanglingNumber(String s) { return s.endsWith("@luck=") || s.endsWith("@chance="); }

    static boolean isOpen(String s) {
        int[] c = {0};
        Lex.balance(s, c);
        String t = s.trim();
        if (t.isEmpty()) return false;
        int tier = 0;
        boolean inQ = false;
        for (int i = 0; i < t.length(); i++) {
            char ch = t.charAt(i);
            if (i > 0 && t.charAt(i - 1) == '\\') continue;
            if (ch == '"') inQ = !inQ;
            else if (!inQ && (ch == '(' || ch == '[' || ch == '{')) tier++;
            else if (!inQ && (ch == ')' || ch == ']' || ch == '}') && tier > 0) tier--;
        }
        if (tier > 0 || inQ) return true;
        char last = t.charAt(t.length() - 1);
        return last == '=' || last == ',';
    }

    private static Item lastDrop(List<Item> items) {
        for (int i = items.size() - 1; i >= 0; i--) {
            Item it = items.get(i);
            if (!it.comment) return it;
            if (!it.text.contains("[LuckyLegacyAddon]")) continue;
        }
        return null;
    }

    private static void note(Output out, String rule, String msg) {
        out.counts.merge(rule, 1, Integer::sum);
        if (out.examples.size() < 12) out.examples.add(msg);
    }

    private static String ruleOf(String n) {
        int c = n.indexOf(' ');
        int q = n.indexOf('\'');
        String base = q > 0 ? n.substring(0, q) : n;
        if (base.startsWith("not a drop") || base.startsWith("unrepairable")) return "removed";
        return base.length() > 50 ? base.substring(0, 50) : base.trim();
    }

    private static String abbreviate(String x) { return x.length() > 100 ? x.substring(0, 100) + "..." : x; }
}
