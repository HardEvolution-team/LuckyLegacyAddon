package com.electrifiedded.luckylegacyaddon.convert.fixers.value;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DropWalker {
    private final Stats st;

    private static final java.util.regex.Pattern STRAY_AT = java.util.regex.Pattern.compile("@ *([-+]?[0-9]+(?:[.][0-9]+)?)(?=@|$)");
    private static final java.util.regex.Pattern LUCK_TAIL = java.util.regex.Pattern.compile("@(?:luck|chance)=[^@]*$");
    private static final java.util.regex.Pattern NAME_QUOTE = java.util.regex.Pattern.compile("\\s*([A-Za-z_][\\w:.]*)\\s*(\".*\")\\s*");
    private static final java.util.regex.Pattern RIDING_NO_EQUALS = java.util.regex.Pattern.compile("(?s)[Rr]iding\\s*(\\(.*)");
    private static final java.util.regex.Pattern BARE_ENTITY_ID = java.util.regex.Pattern.compile("[A-Za-z_][\\w:.]*");
    private static final java.util.regex.Pattern NBT_SINGLE_PAIR = java.util.regex.Pattern.compile("[A-Za-z_]\\w*=[^(),\\[\\]\"]*");

    public DropWalker(Stats st) {
        this.st = st;
    }

    private static final class P {
        String k;
        String v;
        P(String k, String v) { this.k = k; this.v = v; }
        String key() { return k == null ? "" : k.trim().toLowerCase(Locale.ROOT); }
        @Override public String toString() { return k == null ? v : k + "=" + v; }
    }

    private static List<P> parsePairs(String inner) {
        List<P> out = new ArrayList<>();
        for (String t : Scan.split(inner, ',')) {
            int eq = t.indexOf('=');
            if (eq <= 0) { out.add(new P(null, t)); continue; }
            String name = t.substring(0, eq);
            if (name.indexOf('(') >= 0 || name.indexOf('[') >= 0 || name.indexOf('"') >= 0) { out.add(new P(null, t)); continue; }
            out.add(new P(name, t.substring(eq + 1)));
        }
        return out;
    }

    private static String joinPairs(List<P> ps) {
        StringBuilder sb = new StringBuilder();
        for (P p : ps) {
            if (sb.length() > 0 || false) sb.append(',');
            sb.append(p);
        }
        return sb.toString();
    }

    private static P find(List<P> ps, String lowerKey) {
        for (P p : ps) if (p.key().equals(lowerKey)) return p;
        return null;
    }

    private static boolean isCompound(String v) {
        return Scan.wrapped(v.trim(), '(', ')');
    }

    private static boolean isList(String v) {
        return Scan.wrapped(v.trim(), '[', ']');
    }


    public String processLine(String line, int level) {
        try {
            line = removeUnmatchedClosers(line);
            java.util.regex.Matcher lm = LUCK_TAIL.matcher(line);
            int at = Scan.firstTopLevel(line, '@');
            if (lm.find() && (at < 0 || lm.start() < at)) at = lm.start();
            String body = at < 0 ? line : line.substring(0, at);
            String suffix = at < 0 ? "" : fixStrayAt(line.substring(at));
            if (suffix.indexOf('"') >= 0) { suffix = suffix.replace("\"", ""); st.count("luck-suffix-quote-removed"); }
            if (level == 0) body = cancelHashOutsideQuotes(body);
            body = rebuildNestedDrops(body);
            body = repairDropsTail(body);
            body = removeUnmatchedClosers(body);
            body = TextRules.apply(body, st);
            body = ChestFix.convertAll(body, st);
            return processDrop(body, level) + suffix;
        } catch (RuntimeException e) {
            System.out.println("[LuckyLegacyAddon] ValueFixer: kept line unchanged after " + e + " : " + line);
            return line;
        }
    }

    static String removeUnmatchedClosers(String s) {
        StringBuilder out = new StringBuilder();
        java.util.ArrayList<Character> stack = new java.util.ArrayList<>();
        boolean inQ = false;
        int removed = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == (char) 92 && i + 1 < s.length()) { out.append(c).append(s.charAt(i + 1)); i++; continue; }
            if (c == '"') { inQ = !inQ; out.append(c); continue; }
            if (!inQ) {
                if (c == '(' || c == '[' || c == '{') stack.add(c);
                else if (c == ')' || c == ']' || c == '}') {
                    char want = c == ')' ? '(' : c == ']' ? '[' : '{';
                    if (!stack.contains(want)) { removed++; continue; }
                    while (!stack.isEmpty()) {
                        char top = stack.remove(stack.size() - 1);
                        if (top == want) break;
                    }
                }
            }
            out.append(c);
        }
        if (removed > 0) return out.toString();
        return s;
    }

    private String repairDropsTail(String body) {
        int idx = body.toLowerCase(Locale.ROOT).indexOf("drops=[");
        if (idx < 0) return body;
        String head = body.substring(0, idx + "drops=".length());
        String tail = body.substring(idx + "drops=".length());
        if (!badQuoting(tail)) return body;
        st.count("nested-drops-rebalanced");
        return head + repairNested(tail);
    }

    private String rebuildNestedDrops(String body) {
        int idx = body.toLowerCase(Locale.ROOT).indexOf("drops=[");
        if (idx < 0) return body;
        if (!badQuoting(body.substring(idx)) && !unbalanced(body.substring(idx))) return body;
        try {
            String rebuilt = rebuildLine(body);
            if (unbalanced(rebuilt) || badQuoting(rebuilt)) {
                st.count("nested-drops-rebuild-rejected");
                return body;
            }
            st.count("nested-drops-rebuilt");
            return rebuilt;
        } catch (RuntimeException e) {
            st.count("nested-drops-rebuild-failed");
            return body;
        }
    }

    private String rebuildLine(String line) {
        String t = line.trim();
        if (t.toLowerCase(Locale.ROOT).startsWith("group")) return rebuildGroup(t);
        return rebuildProps(t);
    }

    private String rebuildGroup(String text) {
        int open = text.indexOf('(');
        int close = open < 0 ? -1 : matchBracket(text, open);
        if (open < 0) throw new IllegalStateException("group without bracket");
        if (close < 0) st.count("nested-unclosed-group-closed");
        List<String> items = new ArrayList<>();
        for (String d : splitTop(close < 0 ? text.substring(open + 1) : text.substring(open + 1, close), ';')) {
            if (!d.trim().isEmpty()) items.add(rebuildLine(d));
        }
        String rest = close < 0 ? "" : text.substring(close + 1).trim();
        if (rest.startsWith(",")) rest = rest.substring(1);
        String restOut = rest.isEmpty() || isJunk(rest) ? "" : "," + rebuildProps(rest);
        return text.substring(0, open + 1) + Scan.join(items, ";") + ")" + restOut;
    }

    private String rebuildProps(String t) {
        List<String> parts = new ArrayList<>();
        for (String p : splitTop(t, ',')) {
            if (p.trim().isEmpty()) continue;
            if (p.indexOf('=') < 0 && !parts.isEmpty() && oddQuotes(parts.get(parts.size() - 1))) {
                parts.set(parts.size() - 1, parts.get(parts.size() - 1) + "," + p);
                st.count("nested-quoted-comma-joined");
                continue;
            }
            parts.add(p);
        }
        List<String> out = new ArrayList<>();
        for (String p : parts) out.add(rebuildProp(p));
        return Scan.join(out, ",");
    }

    private String rebuildProp(String p) {
        int eq = p.indexOf('=');
        if (eq <= 0) return p.trim();
        String name = p.substring(0, eq).trim();
        String key = name.toLowerCase(Locale.ROOT);
        String val = p.substring(eq + 1).trim();
        if ((key.equals("nbttag") || key.equals("tileentity")) && val.startsWith("(")) {
            return name + "=" + rebuildCompound(val);
        }
        return name + "=" + leafValue(val);
    }

    private static String leafValue(String v) {
        String s = stripQuotes(v);
        return hasTopLevel(s, ',') || hasTopLevel(s, ';') ? "\"" + s + "\"" : v;
    }

    private String rebuildCompound(String v) {
        int close = matchBracket(v, 0);
        if (close < 0) st.count("nested-unclosed-compound-closed");
        List<String> tags = new ArrayList<>();
        for (String tag : splitTop(close < 0 ? v.substring(1) : v.substring(1, close), ',')) {
            if (!tag.trim().isEmpty()) tags.add(rebuildTag(tag));
        }
        return "(" + Scan.join(tags, ",") + ")" + (close < 0 ? "" : junkOr(v.substring(close + 1)));
    }

    private String rebuildTag(String tag) {
        int eq = tag.indexOf('=');
        if (eq <= 0) return tag.trim();
        String key = tag.substring(0, eq).trim();
        String val = tag.substring(eq + 1).trim();
        if (!key.equalsIgnoreCase("drops") || !val.startsWith("[")) return tag;
        int close = matchBracket(val, 0);
        if (close < 0) st.count("nested-unclosed-drops-closed");
        List<String> els = new ArrayList<>();
        for (String e : splitDropElements(close < 0 ? val.substring(1) : val.substring(1, close))) {
            String raw = stripQuotes(e);
            if (raw.isEmpty()) { st.count("nested-empty-drop-removed"); continue; }
            els.add("\"" + Scan.escape(rebuildLine(javaUnescape(raw))) + "\"");
        }
        return key + "=[" + Scan.join(els, ",") + "]" + (close < 0 ? "" : junkOr(val.substring(close + 1)));
    }

    private static List<String> splitDropElements(String inner) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\\\*\"\\s*,\\s*\\\\*\"").matcher(inner);
        List<String> out = new ArrayList<>();
        int start = 0;
        while (m.find()) {
            if (depthAt(inner, m.start()) != 0) continue;
            out.add(inner.substring(start, m.start()));
            start = m.end();
        }
        out.add(inner.substring(start));
        return out;
    }

    private static List<String> splitTop(String s, char sep) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
            else if (c == sep && depth == 0) { out.add(s.substring(start, i)); start = i + 1; }
        }
        out.add(s.substring(start));
        return out;
    }

    private static int matchBracket(String s, int open) {
        StringBuilder stack = new StringBuilder();
        stack.append(s.charAt(open));
        for (int i = open + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') { stack.append(c); continue; }
            if (c != ')' && c != ']' && c != '}') continue;
            char want = c == ')' ? '(' : c == ']' ? '[' : '{';
            int k = stack.lastIndexOf(String.valueOf(want));
            if (k < 0) continue;
            stack.setLength(k);
            if (stack.length() == 0) return i;
        }
        return -1;
    }

    private static int depthAt(String s, int end) {
        int depth = 0;
        for (int i = 0; i < end; i++) {
            char c = s.charAt(i);
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') depth--;
        }
        return depth;
    }

    private static boolean hasTopLevel(String s, char sep) {
        return splitTop(s, sep).size() > 1;
    }

    private static boolean oddQuotes(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == '"') n++;
        return (n & 1) == 1;
    }

    private static String stripQuotes(String v) {
        int a = 0, b = v.length();
        while (a < b && (v.charAt(a) == '"' || v.charAt(a) == '\\' || Character.isWhitespace(v.charAt(a)))) a++;
        while (b > a && (v.charAt(b - 1) == '"' || v.charAt(b - 1) == '\\' || Character.isWhitespace(v.charAt(b - 1)))) b--;
        return v.substring(a, b);
    }

    private static boolean isJunk(String rest) {
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c != '"' && c != ']' && c != ')' && !Character.isWhitespace(c)) return false;
        }
        return true;
    }

    private String junkOr(String rest) {
        for (int i = 0; i < rest.length(); i++) {
            char c = rest.charAt(i);
            if (c != '"' && c != ']' && c != ')' && !Character.isWhitespace(c)) return rest;
        }
        if (!rest.isEmpty()) st.count("nested-junk-removed");
        return "";
    }

    private static String javaUnescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '\\') { sb.append(c); continue; }
            if (i + 1 >= s.length()) break;
            char n = s.charAt(++i);
            switch (n) {
                case 'n': sb.append('\n'); break;
                case 't': sb.append('\t'); break;
                case 'r': sb.append('\r'); break;
                case 'b': sb.append('\b'); break;
                case 'f': sb.append('\f'); break;
                default: sb.append(n);
            }
        }
        return sb.toString();
    }

    private String fixStrayAt(String suffix) {
        java.util.regex.Matcher m = STRAY_AT.matcher(suffix);
        if (!m.find()) return suffix;
        StringBuilder sb = new StringBuilder();
        boolean hasChance = suffix.contains("@chance=");
        int last = 0;
        m.reset();
        while (m.find()) {
            sb.append(suffix, last, m.start());
            if (!hasChance) {
                sb.append("@chance=").append(m.group(1));
                hasChance = true;
                st.count("stray-at-chance->@chance");
            } else {
                st.count("stray-at-number-removed");
            }
            last = m.end();
        }
        sb.append(suffix.substring(last));
        return sb.toString();
    }

    private String cancelHashOutsideQuotes(String s) {
        if (s.indexOf("'#'") < 0) return s;
        StringBuilder sb = new StringBuilder();
        boolean inQ = false;
        boolean changed = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && !Scan.canceled(s, i)) inQ = !inQ;
            if (!inQ && c == '\'' && s.startsWith("'#'", i)) {
                sb.append('#');
                i += 2;
                changed = true;
                continue;
            }
            sb.append(c);
        }
        if (changed) st.count("cancel-hash-outside-quotes");
        return sb.toString();
    }

    private String processDrop(String item, int level) {
        String t = item.trim();
        if (t.toLowerCase(Locale.ROOT).startsWith("group")) return processGroup(item, level);
        return processProps(item, level);
    }

    private String processGroup(String item, int level) {
        int open = item.indexOf('(');
        if (open < 0) return item;
        int close = matchingClose(item, open);
        if (close < 0) {
            String body = item.substring(open + 1);
            List<String> outOpen = new ArrayList<>();
            for (String d : Scan.split(body, ';')) outOpen.add(processDrop(d, level));
            st.count("unclosed-group-body-converted");
            return item.substring(0, open + 1) + Scan.join(outOpen, ";");
        }
        String head = item.substring(0, open + 1);
        String inner = item.substring(open + 1, close);
        String rest = item.substring(close + 1);
        List<String> out = new ArrayList<>();
        for (String d : Scan.split(inner, ';')) out.add(processDrop(d, level));
        StringBuilder sb = new StringBuilder(head).append(Scan.join(out, ";")).append(')');
        String props = rest.startsWith(",") ? rest.substring(1) : rest;
        if (!props.trim().isEmpty() && !props.startsWith("@")) {
            sb.append(',').append(processProps(props, level));
        } else {
            sb.append(rest);
        }
        return sb.toString();
    }

    private static int matchingClose(String s, int open) {
        int depth = 0;
        boolean inQ = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && !Scan.canceled(s, i)) inQ = !inQ;
            if (inQ) continue;
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                depth--;
                if (depth == 0) return c == ')' ? i : -1;
            }
        }
        return -1;
    }


    private String processProps(String item, int level) {
        List<P> ps = parsePairs(item);
        P typeP = find(ps, "type");
        String type = typeP == null ? "" : Scan.unquote(typeP.v).trim().toLowerCase(Locale.ROOT);
        P idP = find(ps, "id");
        P nbtP = find(ps, "nbttag");
        if (nbtP == null) nbtP = find(ps, "tileentity");
        if (nbtP != null && NBT_SINGLE_PAIR.matcher(nbtP.v.trim()).matches()) {
            nbtP.v = "(" + nbtP.v.trim() + ")";
            st.count("NBTTag-unwrapped-pair");
        }
        boolean entity = type.equals("entity");
        if (idP != null && type.equals("command")) idP.v = CommandFix.apply(idP.v, st);
        if (idP != null && type.equals("sound")) {
            String snd = Tables.sound(Scan.unquote(idP.v).trim());
            if (snd != null) { idP.v = snd; st.count("sound-name"); }
        }
        boolean itemDrop = type.isEmpty() || type.equals("item");

        if (entity && idP != null) {
            String id = Scan.unquote(idP.v).trim();
            List<P> nbt = nbtP != null && isCompound(nbtP.v) ? parsePairs(inner(nbtP.v)) : new ArrayList<>();
            Ent e = fixEntity(id, nbt, level);
            if (!e.id.equals(id)) {
                idP.v = e.id;
            }
            if (nbtP != null && isCompound(nbtP.v)) nbtP.v = "(" + joinPairs(e.nbt) + ")";
            else if (!e.nbt.isEmpty() && nbtP == null) ps.add(new P("NBTTag", "(" + joinPairs(e.nbt) + ")"));
        } else if (nbtP != null && isCompound(nbtP.v)) {
            nbtP.v = fixCompound(nbtP.v, level);
        }
        if (itemDrop && idP != null) fixItemDrop(ps, idP, nbtP);
        if (entity && idP != null) {  }
        return joinPairs(ps);
    }

    private static String inner(String compound) {
        String t = compound.trim();
        return t.substring(1, t.length() - 1);
    }


    private void fixItemDrop(List<P> ps, P idP, P nbtP) {
        String id = itemName(idP.v);
        P dmg = find(ps, "damage");
        if (dmg == null) dmg = find(ps, "meta");
        if (dmg == null) return;
        Integer d = litInt(dmg.v);
        if (isPotion(id)) {
            Potion pot = potion(dmg.v);
            if (pot == null) return;
            String newId = pot.splash && id.equals("potion") ? "splash_potion" : id;
            idP.v = Scan.isQuoted(idP.v.trim()) ? "\"" + newId + "\"" : newId;
            ps.remove(dmg);
            setNbtPair(ps, nbtP, "Potion", pot.nbtValue());
            st.count("potion-damage->Potion-tag");
        } else if (d != null && (id.equals("spawn_egg") || id.equals("monster_egg"))) {
            String ent = Tables.EGG.get(d);
            if (ent == null) return;
            idP.v = "spawn_egg";
            ps.remove(dmg);
            setNbtPair(ps, nbtP, "EntityTag", "(id=\"minecraft:" + ent + "\")");
            st.count("spawn-egg-damage->EntityTag");
        }
    }

    private static void setNbtPair(List<P> ps, P nbtP, String key, String value) {
        if (nbtP != null && isCompound(nbtP.v)) {
            List<P> inner = parsePairs(inner(nbtP.v));
            if (find(inner, key.toLowerCase(Locale.ROOT)) == null) inner.add(new P(key, value));
            nbtP.v = "(" + joinPairs(inner) + ")";
        } else if (nbtP == null) {
            ps.add(new P("NBTTag", "(" + key + "=" + value + ")"));
        }
    }

    private static String itemName(String idText) {
        String n = Scan.unquote(idText).trim().toLowerCase(Locale.ROOT).replace("minecraft:", "");
        if (n.equals("373")) return "potion";
        if (n.equals("383")) return "spawn_egg";
        return n;
    }

    private static boolean isPotion(String name) {
        return name.equals("potion") || name.equals("splash_potion") || name.equals("lingering_potion");
    }

    private static final class Potion {
        String valueText;
        boolean splash;
        String nbtValue() { return "\"minecraft:" + valueText + "\""; }
    }

    private static Potion potion(String dmgText) {
        String t = dmgText.trim();
        int base = 0;
        List<Integer> choices = null;
        for (String term : Scan.split(t, '+')) {
            term = term.trim();
            if (term.matches("[0-9]+")) base += Integer.parseInt(term);
            else if (term.startsWith("#randList(") && term.endsWith(")") && choices == null) {
                choices = new ArrayList<>();
                for (String c : Scan.split(term.substring("#randList(".length(), term.length() - 1), ',')) {
                    if (!c.trim().matches("[0-9]+")) return null;
                    choices.add(Integer.parseInt(c.trim()));
                }
            } else return null;
        }
        Potion p = new Potion();
        if (choices == null) {
            String n = Tables.potionType(base);
            if (n == null) return null;
            p.valueText = n;
            p.splash = (base & 0x4000) != 0;
        } else {
            List<String> names = new ArrayList<>();
            for (int c : choices) {
                String n = Tables.potionType(base + c);
                if (n == null) return null;
                names.add(n);
            }
            p.valueText = "#randList(" + Scan.join(names, ",") + ")";
            p.splash = (base & 0x4000) != 0 || (choices.get(0) & 0x4000) != 0;
        }
        return p;
    }

    private static Integer litInt(String v) {
        v = v.trim();
        if (v.matches("-?\\d+")) {
            try { return Integer.parseInt(v); } catch (NumberFormatException e) { return null; }
        }
        return null;
    }


    private String fixCompound(String comp, int level) {
        if (!isCompound(comp)) return comp;
        List<P> ps = parsePairs(inner(comp));
        fixPairs(ps, level);
        return "(" + joinPairs(ps) + ")";
    }

    private void fixPairs(List<P> ps, int level) {
        repairBareTokens(ps);
        for (int i = 0; i < ps.size(); i++) {
            P p = ps.get(i);
            String key = p.key();
            String v = p.v;
            if (p.k == null) continue;
            if (key.startsWith("commandlist") || key.equals("command")) {
                p.v = CommandFix.apply(v, st);
                continue;
            }
            if (key.equals("amplifier")) {
                Integer n = litInt(v);
                if (n != null && (n < -128 || n > 127)) {
                    p.v = Integer.toString(n.byteValue());
                    st.count("amplifier-byte-wrap");
                }
                continue;
            }
            switch (key) {
                case "drops":
                case "impact":
                    if (v.trim().startsWith("[") && (isList(v) ? (unbalanced(v) || badQuoting(v)) : true)) {
                        p.v = repairNested(v);
                        st.count("nested-drops-rebalanced");
                    } else if (isList(v)) p.v = fixDropList(v, level);
                    continue;
                case "entityid": {
                    String id = Scan.unquote(v).trim();
                    if (!id.isEmpty() && id.indexOf('#') < 0) {
                        String m = Tables.entity(id);
                        String nid = m != null ? m : id;
                        ps.set(i, new P("SpawnData", "(id=" + nid + ")"));
                        st.count("spawner-EntityId->SpawnData");
                    }
                    continue;
                }
                case "items":
                case "handitems":
                case "armoritems":
                    if (isList(v)) { p.v = fixItemList(v, level); continue; }
                    break;
                case "item":
                case "saddleitem":
                case "armoritem":
                case "buy":
                case "buyb":
                case "sell":
                case "potion":
                case "fireworksitem":
                    if (isCompound(v)) { p.v = fixItemCompound(v, level); continue; }
                    break;
                case "passengers":
                    if (isList(v)) { p.v = fixPassengers(v, level); continue; }
                    break;
                case "equipment":
                    if (isList(v)) {
                        List<P> tmp = new ArrayList<>();
                        tmp.add(p);
                    }
                    break;
                default:
                    break;
            }
            if (isCompound(v)) p.v = fixCompound(v, level);
            else if (isList(v)) p.v = fixList(v, level);
        }
    }

    private void repairBareTokens(List<P> ps) {
        repairRidingTokens(ps);
        for (int i = ps.size() - 1; i >= 0; i--) {
            P p = ps.get(i);
            if (p.k != null || p.v.indexOf('=') >= 0) continue;
            java.util.regex.Matcher m = NAME_QUOTE.matcher(p.v);
            if (m.matches()) {
                ps.set(i, new P(m.group(1), m.group(2)));
                st.count("nbt-name-without-equals");
            } else {
                st.count("bare-token-kept-unconverted");
            }
        }
    }

    private void repairRidingTokens(List<P> ps) {
        for (int i = 0; i < ps.size(); i++) {
            P p = ps.get(i);
            if (p.k != null) continue;
            String t = p.v.trim();
            java.util.regex.Matcher m = RIDING_NO_EQUALS.matcher(t);
            if (m.matches()) {
                ps.set(i, new P("Riding", m.group(1)));
                st.count("Riding-missing-equals");
            } else if (t.equalsIgnoreCase("riding")) {
                ps.set(i, new P("Riding", "1b"));
                st.count("bare-Riding-flag->Riding=1b");
            }
        }
    }

    static boolean unbalanced(String s) {
        int depth = 0;
        boolean inQ = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '"' && !Scan.canceled(s, i)) inQ = !inQ;
            if (inQ) continue;
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') { depth--; if (depth < 0) return true; }
        }
        return inQ || depth != 0;
    }

    private static char closerOf(char open) {
        return open == '(' ? ')' : open == '[' ? ']' : open == '{' ? '}' : open == '"' ? '"' : 0;
    }

    static String repairNested(String s) {
        final char bs = (char) 92;
        StringBuilder out = new StringBuilder();
        java.util.ArrayList<Character> stack = new java.util.ArrayList<>();
        int strDepth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == bs && i + 1 < s.length()) { out.append(c).append(s.charAt(i + 1)); i++; continue; }
            if (c == '"') {
                boolean closer = !stack.isEmpty() && stack.get(stack.size() - 1) == '"' && nextIsDelimiter(s, i + 1);
                if (closer) {
                    stack.remove(stack.size() - 1);
                    strDepth--;
                    out.append(escapes(strDepth)).append('"');
                } else {
                    out.append(escapes(strDepth)).append('"');
                    stack.add('"');
                    strDepth++;
                }
                continue;
            }
            if (c == '(' || c == '[' || c == '{') { stack.add(c); out.append(c); continue; }
            if (c == ')' || c == ']' || c == '}') {
                int at = -1;
                for (int k = stack.size() - 1; k >= 0; k--) {
                    char f = stack.get(k);
                    if (f != '"' && closerOf(f) == c) { at = k; break; }
                }
                if (at < 0) { out.append(c); continue; }
                while (stack.size() > at + 1) {
                    char f = stack.remove(stack.size() - 1);
                    if (f == '"') { strDepth--; out.append(escapes(strDepth)).append('"'); }
                    else out.append(closerOf(f));
                }
                stack.remove(stack.size() - 1);
                out.append(c);
                continue;
            }
            out.append(c);
        }
        while (!stack.isEmpty()) {
            char f = stack.remove(stack.size() - 1);
            if (f == '"') { strDepth--; out.append(escapes(strDepth)).append('"'); }
            else out.append(closerOf(f));
        }
        return out.toString();
    }

    private static String escapes(int depth) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < (1 << depth) - 1; n++) sb.append((char) 92);
        return sb.toString();
    }

    private static boolean nextIsDelimiter(String s, int from) {
        int j = from;
        while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;
        return j >= s.length() || ",])}".indexOf(s.charAt(j)) >= 0;
    }

    static boolean badQuoting(String s) {
        boolean inQ = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c != '"' || Scan.canceled(s, i)) continue;
            if (!inQ) { inQ = true; continue; }
            inQ = false;
            int j = i + 1;
            while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;
            if (j < s.length() && ",])}".indexOf(s.charAt(j)) < 0) return true;
        }
        return false;
    }

    private String fixList(String list, int level) {
        List<String> els = Scan.split(inner(list), ',');
        List<String> out = new ArrayList<>();
        for (String e : els) {
            String t = e.trim();
            if (isCompound(t)) out.add(fixCompound(t, level));
            else if (isList(t)) out.add(fixList(t, level));
            else out.add(e);
        }
        return "[" + Scan.join(out, ",") + "]";
    }

    private String fixItemList(String list, int level) {
        List<String> els = Scan.split(inner(list), ',');
        List<String> out = new ArrayList<>();
        for (String e : els) {
            String t = e.trim();
            out.add(isCompound(t) ? fixItemCompound(t, level) : e);
        }
        return "[" + Scan.join(out, ",") + "]";
    }

    private String fixItemCompound(String comp, int level) {
        if (!isCompound(comp)) return comp;
        List<P> ps = parsePairs(inner(comp));
        fixPairs(ps, level);
        P id = find(ps, "id");
        if (id != null) {
            if (find(ps, "count") == null) {
                ps.add(new P("Count", "1b"));
                st.count("item-Count-added");
            }
            String name = itemName(id.v);
            P dmg = find(ps, "damage");
            Potion pot = dmg != null && isPotion(name) ? potion(dmg.v) : null;
            if (pot != null) {
                id.v = pot.splash && name.equals("potion") ? "splash_potion" : name;
                ps.remove(dmg);
                P tag = find(ps, "tag");
                if (tag != null && isCompound(tag.v)) {
                    List<P> tp = parsePairs(inner(tag.v));
                    if (find(tp, "potion") == null) tp.add(new P("Potion", pot.nbtValue()));
                    tag.v = "(" + joinPairs(tp) + ")";
                } else if (tag == null) {
                    ps.add(new P("tag", "(Potion=" + pot.nbtValue() + ")"));
                }
                st.count("potion-damage->Potion-tag");
            }
        }
        return "(" + joinPairs(ps) + ")";
    }

    private String fixDropList(String list, int level) {
        List<String> els = Scan.split(inner(list), ',');
        List<String> out = new ArrayList<>();
        for (String e : els) {
            String t = e.trim();
            if (Scan.isQuoted(t)) {
                String innerText = Scan.unescape(t.substring(1, t.length() - 1));
                String fixed = processLine(innerText, level + 1);
                if (!fixed.equals(innerText)) out.add("\"" + Scan.escape(fixed) + "\"");
                else out.add(e);
            } else out.add(e);
        }
        return "[" + Scan.join(out, ",") + "]";
    }


    private static final class Ent {
        String id;
        List<P> nbt;
    }

    private String fixPassengers(String list, int level) {
        List<String> els = Scan.split(inner(list), ',');
        List<String> out = new ArrayList<>();
        for (String e : els) {
            String t = e.trim();
            if (!isCompound(t)) { out.add(e); continue; }
            List<P> ps = parsePairs(inner(t));
            P id = find(ps, "id");
            if (id == null) { out.add(fixCompound(t, level)); continue; }
            ps.remove(id);
            String rawId = Scan.unquote(id.v).trim();
            Ent ent = fixEntity(rawId, ps, level);
            out.add(entityCompound(ent));
        }
        return "[" + Scan.join(out, ",") + "]";
    }

    private static String entityCompound(Ent e) {
        List<P> all = new ArrayList<>();
        all.add(new P("id", e.id));
        all.addAll(e.nbt);
        return "(" + joinPairs(all) + ")";
    }

    private Ent fixEntity(String id, List<P> nbt, int level) {
        String mapped = Tables.entity(id);
        String newId = mapped != null ? mapped : id;
        if (mapped != null) st.count("entity-id");
        repairRidingTokens(nbt);
        P riding = findRidingCompound(nbt);
        List<P> vps = null;
        String vehicleId = null;
        if (riding != null) {
            vps = parsePairs(inner(riding.v));
            P vid = find(vps, "id");
            if (vid == null && !vps.isEmpty() && vps.get(0).k == null && BARE_ENTITY_ID.matcher(vps.get(0).v.trim()).matches()) {
                vps.set(0, new P("id", vps.get(0).v.trim()));
                vid = find(vps, "id");
                st.count("Riding-bare-id->id");
            }
            if (vid != null) {
                vehicleId = Scan.unquote(vid.v).trim();
                vps.remove(vid);
                nbt.remove(riding);
            } else {
                vps = null;
                st.count("Riding-without-vehicle-kept");
            }
        }
        fixPairs(nbt, level);
        String lid = newId.toLowerCase(Locale.ROOT).replace("minecraft:", "");

        P eq = find(nbt, "equipment");
        if (eq != null && isList(eq.v)) {
            List<String> items = Scan.split(inner(eq.v), ',');
            List<String> fixed = new ArrayList<>();
            for (String s : items) {
                String t = s.trim();
                fixed.add(isCompound(t) ? fixItemCompound(t, level) : "()");
            }
            while (fixed.size() < 5) fixed.add("()");
            int at = nbt.indexOf(eq);
            nbt.remove(eq);
            nbt.add(at, new P("HandItems", "[" + fixed.get(0) + ",()]"));
            nbt.add(at + 1, new P("ArmorItems", "[" + fixed.get(1) + "," + fixed.get(2) + "," + fixed.get(3) + "," + fixed.get(4) + "]"));
            st.count("Equipment->HandItems/ArmorItems");
        }
        P dc = find(nbt, "dropchances");
        if (dc != null && isList(dc.v) && (eq != null || find(nbt, "handitems") != null)) {
            List<String> v = new ArrayList<>(Scan.split(inner(dc.v), ','));
            while (v.size() < 5) v.add("0.085F");
            int at = nbt.indexOf(dc);
            nbt.remove(dc);
            nbt.add(at, new P("HandDropChances", "[" + v.get(0).trim() + ",0.085F]"));
            nbt.add(at + 1, new P("ArmorDropChances", "[" + v.get(1).trim() + "," + v.get(2).trim() + "," + v.get(3).trim() + "," + v.get(4).trim() + "]"));
            st.count("DropChances->Hand/ArmorDropChances");
        }
        if (lid.equals("falling_block")) {
            P tile = find(nbt, "tile");
            if (tile != null && find(nbt, "tileid") == null && find(nbt, "block") == null) {
                tile.k = "TileID";
                st.count("falling-Tile->TileID");
            }
        }
        if (lid.equals("skeleton")) {
            P stype = find(nbt, "skeletontype");
            if (stype != null) {
                nbt.remove(stype);
                if (stype.v.trim().matches("1b?|1B")) { newId = "wither_skeleton"; st.count("SkeletonType->wither_skeleton"); }
            }
        }
        if (lid.equals("zombie")) {
            P iv = find(nbt, "isvillager");
            if (iv != null && (iv.v.trim().equals("1") || iv.v.trim().equals("true") || iv.v.trim().equals("1b"))) {
                newId = "zombie_villager";
                nbt.remove(iv);
                st.count("IsVillager->zombie_villager");
            }
        }
        if (lid.equals("horse")) {
            P ty = find(nbt, "type");
            if (ty != null) {
                Integer n = litInt(ty.v.replaceAll("[bBsS]$", ""));
                if (n != null) {
                    String[] ids = {"horse", "donkey", "mule", "zombie_horse", "skeleton_horse"};
                    if (n >= 1 && n <= 4) { newId = ids[n]; st.count("horse-Type->entity-id"); }
                }
            }
        }
        Ent r = new Ent();
        r.id = newId;
        r.nbt = nbt;
        if (vps != null) {
            P existing = find(vps, "passengers");
            String me = entityCompound(r);
            if (existing != null && isList(existing.v)) {
                String others = inner(existing.v).trim();
                existing.v = others.isEmpty() ? "[" + me + "]" : "[" + me + "," + others + "]";
            } else {
                vps.add(new P("Passengers", "[" + me + "]"));
            }
            st.count("Riding->Passengers");
            return fixEntityNoRidingReorder(vehicleId, vps, level);
        }
        return r;
    }

    private static P findRidingCompound(List<P> ps) {
        for (P p : ps) if (p.k != null && p.key().equals("riding") && isCompound(p.v)) return p;
        return null;
    }

    private Ent fixEntityNoRidingReorder(String id, List<P> nbt, int level) {
        return fixEntity(id, nbt, level);
    }
}
