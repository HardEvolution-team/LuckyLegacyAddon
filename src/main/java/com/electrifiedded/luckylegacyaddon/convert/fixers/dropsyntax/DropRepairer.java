package com.electrifiedded.luckylegacyaddon.convert.fixers.dropsyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DropRepairer {
    public static final class Result {
        public final List<String> lines = new ArrayList<>();
        public final List<String> notes = new ArrayList<>();
        public boolean removed;
        public boolean changed;
    }

    private static final Pattern NAME_EQ = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*\\s*=.*", Pattern.DOTALL);
    private static final Pattern NAME_BRACKET = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)([#(\\[].*)$", Pattern.DOTALL);
    private static final Pattern COLON_STYLE = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*\\s*:.*", Pattern.DOTALL);
    private static final Pattern NUMERIC = Pattern.compile("^[-+]?\\d+(\\.\\d+)?[fFdDsSbB]?$");
    private static final Pattern TOKEN = Pattern.compile("^[\\w:.\\-]+$");
    private static final Pattern DROP_START = Pattern.compile("^\\s*(?:(?i:group)\\s*[(:]|[A-Za-z_][A-Za-z0-9_]*\\s*[=#(\\[])");

    private final List<String> notes = new ArrayList<>();
    private int changes;

    private DropRepairer() {}

    public static Result repair(String line) {
        Result res = new Result();
        try {
            DropRepairer r = new DropRepairer();
            r.line(line, res, 0);
            res.notes.addAll(r.notes);
        } catch (RuntimeException e) {
            res.lines.clear();
            res.lines.add(line);
            res.notes.add("repair aborted: " + e);
        }
        return res;
    }

    private void line(String line, Result res, int depth) {
        String work = line;
        int pre = 0;
        if (Lex.oddQuotes(work)) {
            String e = Lex.escapeInnerQuotes(work);
            if (!Lex.oddQuotes(e)) {
                work = e;
                pre++;
                notes.add("escaped unescaped inner quotes");
            }
        }
        Modifiers m = Modifiers.parse(work, Lex.oddQuotes(work));
        notes.addAll(m.notes);
        changes = m.changes + pre;
        String core = m.core;
        String fixedCore = core.isEmpty() ? null : core(core);
        if (fixedCore == null) {
            notes.add("not a drop: " + abbreviate(line));
        } else {
            String out = fixedCore + m.suffix();
            boolean same = changes == 0 && fixedCore.equals(core) && DropGrammar.check(line) == null;
            if (same) {
                out = line;
            } else {
                res.changed = true;
            }
            String err = DropGrammar.check(out);
            if (err != null) {
                notes.add("unrepairable (" + err + "): " + abbreviate(line));
            } else {
                res.lines.add(out);
            }
        }
        if (changes > 0) res.changed = true;
        for (String extra : m.extraDrops) {
            if (depth > 200) break;
            DropRepairer sub = new DropRepairer();
            Result subRes = new Result();
            sub.line(extra, subRes, depth + 1);
            notes.addAll(sub.notes);
            res.lines.addAll(subRes.lines);
            res.changed = true;
        }
        res.removed = res.lines.isEmpty();
    }

    private static String abbreviate(String x) { return x.length() > 90 ? x.substring(0, 90) + "..." : x; }


    private String core(String s) {
        s = s.trim();
        if (s.isEmpty()) return null;
        if (Lex.oddQuotes(s)) {
            String e = Lex.escapeInnerQuotes(s);
            if (!Lex.oddQuotes(e)) {
                s = e;
                changes++;
                notes.add("escaped unescaped inner quotes");
            }
        }
        Matcher lead = LEAD_NUM.matcher(s);
        if (lead.find()) {
            notes.add("dropped stray numeric prefix '" + s.substring(0, lead.end()).trim() + "'");
            s = s.substring(lead.end());
            changes++;
            if (s.trim().isEmpty()) return null;
        }
        s = splitGlued(s);
        if (s.charAt(0) == '(') {
            int close = Lex.matching(s, 0);
            if (close > 0 && validRemainder(s.substring(close + 1)) && Lex.split(s.substring(1, close), ';').parts.size() > 1) {
                s = "group" + s;
                changes++;
                notes.add("added missing 'group'");
            }
        } else if (GROUP_SPACE.matcher(s).find()) {
            s = s.replaceFirst("^(?i)group\\s+", "");
            changes++;
            notes.add("removed stray word 'group'");
        } else if (GROUP_GLUED.matcher(s).find()) {
            s = "group(" + s.substring(5).replaceFirst("\\(;", ";");
            changes++;
            notes.add("repaired malformed group header");
        }
        if (s.regionMatches(true, 0, "group", 0, 5)) return group(s);
        return props(s);
    }

    private static final Pattern LEAD_NUM = Pattern.compile("^\\s*[-+]?\\d+(?=\\s*(?i:type|id|group)\\s*[=(:])");
    private static final Pattern GLUE_NEXT = Pattern.compile("^\\s*(?i:(?:type|id)\\s*=|group\\s*[(:])");
    private static final Pattern BARE_KEY = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");
    private static final Pattern KEY_NO_EQ = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)([\"(\\[].*)$", Pattern.DOTALL);
    private static final Pattern PAREN_NUM = Pattern.compile("^\\(\\s*([-+]?\\d+(?:\\.\\d+)?)\\s*\\)$");
    private static final Pattern NBT_UNWRAPPED = Pattern.compile("^NBTTag=[A-Za-z_][A-Za-z0-9_]*\\s*=.*", Pattern.DOTALL);
    private static final Pattern NBT_KEY_UPPER = Pattern.compile("^[A-Z][A-Za-z0-9_]*\\s*=.*", Pattern.DOTALL);

    private String splitGlued(String s) {
        StringBuilder b = new StringBuilder();
        int tier = 0;
        boolean inQ = false;
        boolean qPrevEq = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            b.append(c);
            if (i > 0 && s.charAt(i - 1) == '\\') continue;
            boolean glue = false;
            if (c == '"') {
                if (!inQ) qPrevEq = i > 0 && s.charAt(i - 1) == '=';
                inQ = !inQ;
                glue = !inQ && qPrevEq;
            } else if (!inQ && (c == '(' || c == '[' || c == '{')) {
                tier++;
            } else if (!inQ && (c == ')' || c == ']' || c == '}')) {
                if (tier > 0) tier--;
                glue = tier == 0;
            }
            if (glue && i + 1 < s.length() && GLUE_NEXT.matcher(s.substring(i + 1)).find()) {
                b.append(';');
                changes++;
                notes.add("added missing ';' between glued drops");
            }
        }
        return b.toString();
    }

    private String fixNbt(String v, int depth) {
        String t = v.trim();
        if (depth > 20 || t.length() < 2) return v;
        char open = t.charAt(0);
        if (open != '(' && open != '[') return v;
        if (Lex.matching(t, 0) != t.length() - 1) return v;
        List<String> segs = Lex.split(t.substring(1, t.length() - 1), ',').parts;
        boolean keyed = false;
        for (String x : segs) if (NAME_EQ.matcher(x.trim()).matches()) keyed = true;
        boolean changed = false;
        List<String> out = new ArrayList<>();
        for (String seg : segs) {
            String x = seg.trim();
            if (x.isEmpty()) { changed = true; continue; }
            if (open == '(' && keyed && BARE_KEY.matcher(x).matches()) {
                changed = true;
                notes.add("dropped bare NBT key '" + x + "'");
                continue;
            }
            Matcher kn = KEY_NO_EQ.matcher(x);
            if (open == '(' && kn.matches()) {
                changed = true;
                notes.add("added missing '=' in NBT key '" + kn.group(1) + "'");
                out.add(kn.group(1) + "=" + kn.group(2));
                continue;
            }
            if (x.charAt(0) == '(' || x.charAt(0) == '[') {
                String f = fixNbt(x, depth + 1);
                if (!f.equals(x)) changed = true;
                out.add(f);
                continue;
            }
            if (NAME_EQ.matcher(x).matches()) {
                int eq = x.indexOf('=');
                String val = x.substring(eq + 1);
                String f = fixNbt(val, depth + 1);
                if (!f.equals(val)) changed = true;
                out.add(x.substring(0, eq + 1) + f.trim());
                continue;
            }
            out.add(x);
        }
        if (!changed) return v;
        changes++;
        return open + String.join(",", out) + (open == '(' ? ')' : ']');
    }

    private List<String> absorbNbt(List<String> segs) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < segs.size(); i++) {
            String t = segs.get(i).trim();
            if (NBT_UNWRAPPED.matcher(t).matches()) {
                StringBuilder v = new StringBuilder(t.substring(7));
                int j = i + 1;
                while (j < segs.size() && NBT_KEY_UPPER.matcher(segs.get(j).trim()).matches()) {
                    v.append(',').append(segs.get(j).trim());
                    j++;
                }
                changes++;
                notes.add("wrapped unbracketed NBTTag value in '()'");
                out.add("NBTTag=(" + v + ")");
                i = j - 1;
            } else {
                out.add(segs.get(i));
            }
        }
        return out;
    }

    private static final Pattern GROUP_SPACE = Pattern.compile("^(?i)group\\s+[A-Za-z_]\\w*\\s*=");
    private static final Pattern GROUP_GLUED = Pattern.compile("^(?i)group[A-Za-z_]\\w*\\s*=");
    private static final Pattern GROUP_SEG = Pattern.compile("^(?is)group\\s*\\(.*\\)$");

    private String props(String s) {
        if (hasBare(s)) {
            String e = Lex.escapeInnerQuotes(s);
            if (!e.equals(s) && bareCount(e) < bareCount(s)) {
                s = e;
                changes++;
                notes.add("escaped unescaped inner quotes");
            }
        }
        int[] c = {0};
        s = s.startsWith("(") ? Lex.balanceStack(s, c) : Lex.balance(s, c);
        if (c[0] > 0) {
            changes += c[0];
            notes.add("balanced brackets/quotes");
        }
        String tailless = s.replaceFirst("\"+$", "");
        if (tailless.startsWith("(") && Lex.matching(tailless, 0) == tailless.length() - 1) {
            if (!tailless.equals(s)) {
                changes++;
                notes.add("removed trailing stray quote after balanced NBT fragment");
            }
            notes.add("kept balanced NBT fragment without drop head: " + abbreviate(tailless));
            return tailless;
        }
        Lex.Split semi = Lex.split(s, ';');
        if (semi.parts.size() > 1) {
            List<String> segs = new ArrayList<>();
            for (String part : semi.parts) {
                String t = part.trim();
                if (t.isEmpty()) continue;
                if (!segs.isEmpty() && segs.get(segs.size() - 1).endsWith("=")) {
                    segs.set(segs.size() - 1, segs.get(segs.size() - 1) + t);
                } else if (!segs.isEmpty() && !DROP_START.matcher(t).find()) {
                    notes.add("dropped non-drop segment '" + abbreviate(t) + "'");
                } else {
                    segs.add(t);
                }
            }
            changes++;
            if (segs.size() > 1) {
                notes.add("wrapped ';'-separated drops in group(...)");
                List<String> fixed = new ArrayList<>();
                for (String seg : segs) {
                    String f = core(seg);
                    if (f == null) {
                        notes.add("dropped non-drop segment '" + abbreviate(seg) + "'");
                        continue;
                    }
                    fixed.add(f);
                }
                if (fixed.isEmpty()) return null;
                if (fixed.size() == 1) return fixed.get(0);
                return "group(" + String.join(";", fixed) + ")";
            }
            if (segs.isEmpty()) return null;
            s = segs.get(0);
        }
        List<String> segs = absorbNbt(Lex.split(s, ',').parts);
        for (int g = 1; g < segs.size(); g++) {
            if (GROUP_SEG.matcher(segs.get(g).trim()).matches() || segs.get(g).trim().regionMatches(true, 0, "group(", 0, 6)) {
                String left = String.join(",", segs.subList(0, g));
                String right = String.join(",", segs.subList(g, segs.size()));
                String lf = core(left);
                String rf = core(right);
                changes++;
                notes.add("split drop at embedded group(...)");
                if (lf == null) return rf;
                if (rf == null) return lf;
                return "group(" + lf + ";" + rf + ")";
            }
        }
        List<String> out = new ArrayList<>();
        int realEq = 0;
        for (String seg : segs) {
            String t = seg.trim();
            if (t.isEmpty()) {
                changes++;
                continue;
            }
            if (NAME_EQ.matcher(t).matches()) {
                int eq = t.indexOf('=');
                String val = t.substring(eq + 1);
                java.util.regex.Matcher pn = PAREN_NUM.matcher(val);
                if (pn.matches()) {
                    val = pn.group(1);
                    changes++;
                    notes.add("unwrapped parenthesised number in '" + abbreviate(t) + "'");
                }
                out.add(t.substring(0, eq + 1) + fixNbt(val, 0));
                realEq++;
                continue;
            }
            java.util.regex.Matcher nb = NAME_BRACKET.matcher(t);
            if (nb.matches()) {
                out.add(nb.group(1) + "=" + nb.group(2));
                realEq++;
                changes++;
                notes.add("added missing '=' in '" + abbreviate(t) + "'");
                continue;
            }
            if (COLON_STYLE.matcher(t).matches() || NUMERIC.matcher(t).matches()) {
                changes++;
                notes.add("dropped stray property '" + abbreviate(t) + "'");
                continue;
            }
            boolean first = out.isEmpty() || (out.size() == 1 && out.get(0).toLowerCase(Locale.ENGLISH).startsWith("type"));
            if (first && TOKEN.matcher(t).matches()) {
                out.add("ID=" + t);
                changes++;
                notes.add("added missing 'ID=' before '" + abbreviate(t) + "'");
                continue;
            }
            if (!out.isEmpty()) {
                String prev = out.get(out.size() - 1);
                int eq = prev.indexOf('=');
                String val = prev.substring(eq + 1).trim();
                if (eq > 0 && !val.startsWith("\"") && !val.contains("(") && !val.contains("[")) {
                    out.set(out.size() - 1, prev.substring(0, eq + 1) + "\"" + val + "," + t + "\"");
                    changes++;
                    notes.add("merged comma inside value '" + abbreviate(val + "," + t) + "'");
                    continue;
                }
            }
            changes++;
            notes.add("dropped stray text '" + abbreviate(t) + "'");
        }
        if (realEq == 0) return null;
        return String.join(",", out);
    }

    private static boolean hasBare(String s) { return bareCount(s) > 0; }

    private static int bareCount(String s) {
        int n = 0;
        for (String p : Lex.split(s, ',').parts) {
            if (p.indexOf('=') < 0) n++;
        }
        return n;
    }

    private String group(String s) {
        int open = -1;
        Lex.Split colon = Lex.split(s, ':');
        if (colon.strayAt < 0 && colon.parts.get(0).length() == 5) {
            int pos = 5;
            if (colon.parts.size() == 1) open = 5;
            else if (colon.parts.size() == 2) open = 6;
            else if (colon.parts.size() == 3) open = 5 + 1 + colon.parts.get(1).length() + 1;
            if (open >= s.length() || (open >= 0 && s.charAt(open) != '(')) open = -1;
            if (pos != 5) open = -1;
        }
        if (open < 0 && s.length() > 5 && s.charAt(5) == '(') open = 5;
        if (open < 0) return null;
        String prefix = s.substring(0, open + 1);
        int close = Lex.matching(s, open);
        String content;
        String rest;
        if (close < 0) {
            content = s.substring(open + 1);
            rest = "";
            changes++;
            notes.add("group was not closed");
        } else {
            content = s.substring(open + 1, close);
            rest = s.substring(close + 1);
            if (!validRemainder(rest)) {
                List<Integer> cl = Lex.closers(s);
                int found = -1;
                for (int k = cl.size() - 1; k >= 0; k--) {
                    int idx = cl.get(k);
                    if (idx <= close) break;
                    if (validRemainder(s.substring(idx + 1))) {
                        found = idx;
                        break;
                    }
                }
                changes++;
                if (found > 0) {
                    notes.add("group closed too early by a stray ')'");
                    content = s.substring(open + 1, found);
                    rest = s.substring(found + 1);
                } else {
                    notes.add("dropped garbage after group '" + abbreviate(rest) + "'");
                    rest = "";
                }
            }
        }
        int[] c = {0};
        content = close < 0 ? Lex.balanceStack(content, c) : Lex.balance(content, c);
        if (c[0] > 0) {
            changes += c[0];
            notes.add("balanced group content");
        }
        List<String> parts = new ArrayList<>();
        for (String part : Lex.split(content, ';').parts) {
            String t = part.trim();
            if (t.isEmpty()) {
                changes++;
                continue;
            }
            if (!parts.isEmpty() && parts.get(parts.size() - 1).matches("(?s).*=\\s*$")) {
                parts.set(parts.size() - 1, parts.get(parts.size() - 1) + t);
                changes++;
                notes.add("joined value that was separated by ';'");
                continue;
            }
            parts.add(t);
        }
        List<String> fixed = new ArrayList<>();
        for (String part : parts) {
            String f = core(part);
            if (f == null) {
                changes++;
                notes.add("dropped non-drop group entry '" + abbreviate(part) + "'");
                continue;
            }
            fixed.add(f);
        }
        if (fixed.isEmpty()) return null;
        StringBuilder sb = new StringBuilder(prefix);
        sb.append(String.join(";", fixed)).append(')');
        if (!rest.trim().isEmpty()) {
            List<String> pr = new ArrayList<>();
            for (String seg : Lex.split(Lex.balance(rest, null), ',').parts) {
                String t = seg.trim();
                if (t.isEmpty()) continue;
                if (NAME_EQ.matcher(t).matches()) pr.add(t);
                else {
                    java.util.regex.Matcher nb = NAME_BRACKET.matcher(t);
                    if (nb.matches()) pr.add(nb.group(1) + "=" + nb.group(2));
                    else {
                        changes++;
                        notes.add("dropped stray group property '" + abbreviate(t) + "'");
                    }
                }
            }
            if (!pr.isEmpty()) sb.append(',').append(String.join(",", pr));
        }
        return sb.toString();
    }

    private static boolean validRemainder(String r) {
        String t = r.trim();
        if (t.isEmpty()) return true;
        if (t.charAt(0) != ',') return false;
        Lex.Split sp = Lex.split(t, ';');
        if (sp.parts.size() > 1 || sp.strayAt >= 0) return false;
        for (String seg : Lex.split(t, ',').parts) {
            String x = seg.trim();
            if (x.isEmpty()) continue;
            if (!NAME_EQ.matcher(x).matches() && !NAME_BRACKET.matcher(x).matches()) return false;
        }
        return true;
    }
}
