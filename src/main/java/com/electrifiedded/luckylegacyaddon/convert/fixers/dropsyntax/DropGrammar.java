package com.electrifiedded.luckylegacyaddon.convert.fixers.dropsyntax;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class DropGrammar {
    private DropGrammar() {}

    private static final Pattern NUM = Pattern.compile("[-+*/\\d.()\\s]+[fFdD]?");

    public static String check(String line) {
        try {
            String[] box = new String[1];
            String s = stripLuckChance(line, box);
            if (box[0] != null) return box[0];
            return checkDrop(s, 0);
        } catch (RuntimeException e) {
            return "exception " + e;
        }
    }

    static String stripLuckChance(String string, String[] err) {
        int[] points = new int[256];
        int count = 0;
        boolean used = true;
        while (true) {
            int base = count > 0 ? (used ? points[count - 1] : points[count - 1] + 1) : 0;
            int chancePoint = string.indexOf("@chance=", base);
            int luckPoint = string.indexOf("@luck=", base);
            int type;
            int len;
            if ((chancePoint >= luckPoint || chancePoint == -1) && (luckPoint != -1 || chancePoint <= -1)) {
                if ((luckPoint >= chancePoint || luckPoint == -1) && (chancePoint != -1 || luckPoint <= -1)) return string;
                type = 1;
                len = 6;
                points[count] = luckPoint;
            } else {
                type = 0;
                len = 8;
                points[count] = chancePoint;
            }
            if (count + 2 >= points.length) return string;
            used = true;
            if (endPoint(string, points[count] + len, "+*()[]\",;/") != string.length()) used = false;
            int curEnd = endPoint(string, points[count] + len, "@");
            String v = string.substring(points[count] + len, curEnd);
            if (v.startsWith("(") && v.endsWith(")")) v = v.substring(1, v.length() - 1);
            if (used) {
                if (!(v.contains("#") || NUM.matcher(v).matches()) || v.trim().isEmpty()) {
                    if (err[0] == null) err[0] = "luck/chance value '" + v + "'";
                }
                string = string.substring(0, points[count]) + string.substring(curEnd);
            }
            count++;
        }
    }

    private static int endPoint(String s, int start, String chars) {
        for (int i = start; i < s.length(); i++) if (chars.indexOf(s.charAt(i)) >= 0) return i;
        return s.length();
    }

    private static boolean tooMany(Lex.Split sp) { return sp.parts.size() > 1020; }

    static String checkDrop(String s, int depth) {
        if (depth > 40) return "nesting too deep";
        if (s.toLowerCase(Locale.ENGLISH).startsWith("group")) {
            Lex.Split colon = Lex.split(s, ':');
            String last = colon.parts.get(colon.parts.size() - 1);
            Lex.Split commas = Lex.split(last, ',');
            String loc = commas.parts.get(0);
            StringBuilder rest = new StringBuilder();
            for (int i = 1; i < commas.parts.size(); i++) {
                if (i > 1) rest.append(',');
                rest.append(commas.parts.get(i));
            }
            String restS = rest.length() == 0 ? "" : "," + rest;
            String inner;
            if (loc.toLowerCase(Locale.ENGLISH).startsWith("group")) {
                if (loc.length() < 7) return "group too short";
                inner = loc.substring(6, loc.length() - 1);
            } else {
                if (loc.length() < 2) return "group body too short";
                inner = loc.substring(1, loc.length() - 1);
            }
            Lex.Split items = Lex.split(inner, ';');
            if (tooMany(items)) return "too many group items";
            for (String d : items.parts) {
                String e = checkDrop(d + restS, depth + 1);
                if (e != null) return e;
            }
            return null;
        }
        Lex.Split props = Lex.split(s, ',');
        if (tooMany(props)) return "too many properties";
        for (String p : props.parts) {
            if (p.indexOf('=') < 0) return "property without '=': " + (p.length() > 40 ? p.substring(0, 40) : p);
        }
        return null;
    }

    static List<String> parts(String s, char c) { return Lex.split(s, c).parts; }
}
