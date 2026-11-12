package com.electrifiedded.luckylegacyaddon.convert.fixers.value;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class CommandFix {
    private CommandFix() {}

    private static final Set<String> SOURCES = new HashSet<>(Arrays.asList("master", "music", "record", "weather", "block", "hostile",
            "neutral", "player", "ambient", "voice"));
    private static final Pattern PLAY = Pattern.compile("(?<![A-Za-z_])playsound\s+([^\s\"]+)(\s+)([^\s\"]*)");
    private static final Pattern SUMMON = Pattern.compile("(?<![A-Za-z_])summon\s+([A-Za-z_:]+)(?=\s|$|\")");
    private static final Pattern SUMMON_WITH_NBT = Pattern.compile("(?<![A-Za-z_])summon\\s+([A-Za-z_:]+)(\\s+[~^\\-+\\d.]+){3}\\s*\\{");

    static String apply(String cmd, Stats st) {
        if (cmd.indexOf("playsound") < 0 && cmd.indexOf("summon") < 0) return cmd;
        String o = cmd;
        Matcher m = PLAY.matcher(cmd);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String snd = m.group(1);
            String mapped = Tables.sound(snd);
            String src = SOURCES.contains(m.group(3)) ? "" : "master ";
            String rep = "playsound " + (mapped != null ? mapped : snd) + " " + src + m.group(3);
            if (m.group(3).isEmpty()) rep = "playsound " + (mapped != null ? mapped : snd) + " master";
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        cmd = sb.toString();
        cmd = rideSummons(cmd, st);
        cmd = dropGroups(cmd, st);
        m = SUMMON.matcher(cmd);
        sb = new StringBuffer();
        while (m.find()) {
            String id = m.group(1);
            String mapped = id.equalsIgnoreCase("LightningBolt") ? "lightning_bolt" : Tables.entity(id);
            m.appendReplacement(sb, Matcher.quoteReplacement("summon " + (mapped != null ? mapped : id)));
        }
        m.appendTail(sb);
        cmd = sb.toString();
        if (!cmd.equals(o)) st.count("command-syntax");
        return cmd;
    }

    static String rideSummons(String cmd, Stats st) {
        if (cmd.indexOf("Riding") < 0) return cmd;
        Matcher m = SUMMON_WITH_NBT.matcher(cmd);
        StringBuilder out = new StringBuilder();
        int last = 0;
        while (m.find()) {
            int brace = m.end() - 1;
            int close = closeIndex(cmd, brace);
            if (close < 0) continue;
            String comp = cmd.substring(brace, close + 1);
            List<String[]> es = entries(comp);
            if (es == null || indexOfKey(es, "Riding") < 0) continue;
            Ride r = ride(bareName(m.group(1)), comp, st);
            if (r == null) {
                st.count("Riding-command-kept");
                continue;
            }
            out.append(cmd, last, m.start(1)).append(r.id).append(cmd, m.end(1), brace).append(deep(r.nbt, st, 0));
            last = close + 1;
            m.region(last, cmd.length());
        }
        out.append(cmd.substring(last));
        return out.toString();
    }

    static String dropGroups(String cmd, Stats st) {
        if (cmd.indexOf("group(") < 0) return cmd;
        StringBuilder out = new StringBuilder();
        int last = 0;
        int from = 0;
        while (true) {
            int idx = cmd.indexOf("group(", from);
            if (idx < 0) break;
            boolean esc = idx >= 2 && cmd.startsWith("\\\"", idx - 2);
            boolean plain = !esc && idx >= 1 && cmd.charAt(idx - 1) == '"';
            if (!esc && !plain) { from = idx + 6; continue; }
            String w = esc ? "\\\"" : "\"";
            int start = esc ? idx - 2 : idx - 1;
            int close = closeIndex(cmd, idx + 5);
            if (close < 0 || !cmd.startsWith(w, close + 1)) {
                close = cmd.indexOf(")" + w, idx + 6);
                if (close < 0) {
                    st.count("drop-string-kept");
                    from = idx + 6;
                    continue;
                }
            }
            int end = close + 1 + w.length();
            String x = cmd.substring(idx, close + 1);
            String e = Scan.unescape(x);
            int[] closed = {0};
            e = closeDropEnds(e, closed);
            if (closed[0] > 0) st.count("drop-closers-inserted");
            String e2 = new DropWalker(st).processLine(e, 1);
            out.append(cmd, last, start);
            if (e2.equals(e)) out.append(cmd, start, end);
            else {
                out.append(w).append(Scan.escape(e2)).append(w);
                st.count("drop-string-converted");
            }
            last = end;
            from = end;
        }
        out.append(cmd.substring(last));
        return out.toString();
    }

    static String closeDropEnds(String g, int[] changed) {
        StringBuilder out = new StringBuilder();
        List<Character> stack = new ArrayList<>();
        boolean quoted = false;
        for (int i = 0; i < g.length(); i++) {
            char c = g.charAt(i);
            if (c == '\\' && i + 1 < g.length()) { out.append(c).append(g.charAt(++i)); continue; }
            if (quoted) {
                if (c == '"') quoted = false;
                out.append(c);
                continue;
            }
            if (c == '"') quoted = true;
            else if (c == '(' || c == '[' || c == '{') stack.add(c);
            else if (c == ')' || c == ']' || c == '}') {
                if (!stack.isEmpty()) stack.remove(stack.size() - 1);
            } else if (c == ';' && stack.size() > 1) {
                for (int k = stack.size() - 1; k >= 1; k--) {
                    char o = stack.get(k);
                    out.append(o == '(' ? ')' : o == '[' ? ']' : '}');
                    changed[0]++;
                }
                stack.subList(1, stack.size()).clear();
            }
            out.append(c);
        }
        for (int k = stack.size() - 1; k >= 1; k--) {
            char o = stack.get(k);
            out.append(o == '(' ? ')' : o == '[' ? ']' : '}');
            changed[0]++;
        }
        return out.toString();
    }

    private static final class Ride {
        final String id;
        final String nbt;
        Ride(String id, String nbt) { this.id = id; this.nbt = nbt; }
    }

    private static Ride ride(String id, String comp, Stats st) {
        String curId = mapId(id);
        String cur = comp;
        boolean esc = comp.contains("\\\"");
        boolean changed = false;
        for (int guard = 0; guard < 4096; guard++) {
            List<String[]> es = entries(cur);
            if (es == null) return null;
            if (indexOfKey(es, "Riding") < 0) break;
            if (indexOfKey(es, "id") < 0) es.add(0, new String[]{"id", wrapLike("", curId, esc)});
            int ri = indexOfKey(es, "Riding");
            String[] riding = es.get(ri);
            if (!riding[1].startsWith("{")) return null;
            int ii = indexOfKey(es, "id");
            es.get(ii)[1] = wrapLike(es.get(ii)[1], curId, esc);
            List<String[]> rider = new ArrayList<>(es);
            rider.remove(ri);
            String riderText = build(rider);
            List<String[]> ves = entries(riding[1]);
            if (ves == null) return null;
            int vi = indexOfKey(ves, "id");
            if (vi < 0) return null;
            String vid = mapId(bareName(ves.get(vi)[1]));
            ves.get(vi)[1] = wrapLike(ves.get(vi)[1], vid, esc);
            int pi = indexOfKey(ves, "Passengers");
            if (pi >= 0) {
                String pv = ves.get(pi)[1];
                if (!pv.startsWith("[") || closeIndex(pv, 0) != pv.length() - 1) return null;
                String others = pv.substring(1, pv.length() - 1).trim();
                ves.get(pi)[1] = "[" + riderText + (others.isEmpty() ? "" : "," + others) + "]";
            } else {
                ves.add(new String[]{"Passengers", "[" + riderText + "]"});
            }
            cur = build(ves);
            curId = vid;
            changed = true;
            st.count("Riding->Passengers");
        }
        return changed ? new Ride(curId, cur) : null;
    }

    private static String deep(String comp, Stats st, int depth) {
        if (depth > 64) return comp;
        List<String[]> es = entries(comp);
        if (es == null) return comp;
        if (indexOfKey(es, "Riding") >= 0) {
            String id = find(es, "id");
            if (id != null) {
                Ride r = ride(bareName(id), comp, st);
                if (r != null) return deep(r.nbt, st, depth + 1);
            }
            st.count("Riding-command-kept");
        }
        for (String[] e : es) {
            if (e[1].startsWith("{")) e[1] = deep(e[1], st, depth + 1);
            else if (e[1].startsWith("[") && closeIndex(e[1], 0) == e[1].length() - 1) {
                List<String> items = new ArrayList<>();
                for (String it : splitTop(e[1].substring(1, e[1].length() - 1))) {
                    String t = it.trim();
                    items.add(t.startsWith("{") ? deep(t, st, depth + 1) : t);
                }
                e[1] = "[" + String.join(",", items) + "]";
            }
        }
        return build(es);
    }

    private static String mapId(String name) {
        String mapped = Tables.entity(name);
        return mapped != null ? mapped : name;
    }

    private static String find(List<String[]> es, String key) {
        int i = indexOfKey(es, key);
        return i < 0 ? null : es.get(i)[1];
    }

    private static int indexOfKey(List<String[]> es, String key) {
        for (int i = 0; i < es.size(); i++) if (es.get(i)[0].equals(key)) return i;
        return -1;
    }

    private static String bareName(String s) {
        String t = s.trim();
        if (t.length() >= 4 && t.startsWith("\\\"") && t.endsWith("\\\"")) return t.substring(2, t.length() - 2);
        if (t.length() >= 2 && t.startsWith("\"") && t.endsWith("\"")) return t.substring(1, t.length() - 1);
        return t;
    }

    private static String wrapLike(String orig, String name, boolean escaped) {
        if (orig.startsWith("\\\"")) return "\\\"" + name + "\\\"";
        if (orig.startsWith("\"")) return "\"" + name + "\"";
        if (orig.isEmpty()) return escaped ? "\\\"" + name + "\\\"" : "\"" + name + "\"";
        return name;
    }

    private static String build(List<String[]> es) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < es.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(es.get(i)[0]).append(':').append(es.get(i)[1]);
        }
        return sb.append('}').toString();
    }

    private static List<String[]> entries(String comp) {
        if (comp.length() < 2 || comp.charAt(0) != '{' || closeIndex(comp, 0) != comp.length() - 1) return null;
        List<String[]> out = new ArrayList<>();
        for (String part : splitTop(comp.substring(1, comp.length() - 1))) {
            String t = part.trim();
            if (t.isEmpty()) continue;
            int colon = t.indexOf(':');
            if (colon < 0) return null;
            out.add(new String[]{bareName(t.substring(0, colon)), t.substring(colon + 1).trim()});
        }
        return out;
    }

    private static List<String> splitTop(String s) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            int q = quoteLen(s, i);
            if (q > 0) {
                quoted = !quoted;
                i += q - 1;
            } else if (quoted) {
                continue;
            } else if (c == '{' || c == '[' || c == '(') depth++;
            else if (c == '}' || c == ']' || c == ')') depth--;
            else if (c == ',' && depth == 0) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    private static int quoteLen(String s, int i) {
        int j = i;
        while (j < s.length() && s.charAt(j) == '\\') j++;
        return j < s.length() && s.charAt(j) == '"' ? j - i + 1 : 0;
    }

    private static int closeIndex(String s, int open) {
        int depth = 0;
        boolean quoted = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            int q = quoteLen(s, i);
            if (q > 0) {
                quoted = !quoted;
                i += q - 1;
            } else if (quoted) {
                continue;
            } else if (c == '{' || c == '[' || c == '(') depth++;
            else if (c == '}' || c == ']' || c == ')') {
                depth--;
                if (depth == 0) return i;
            }
        }
        return -1;
    }
}
