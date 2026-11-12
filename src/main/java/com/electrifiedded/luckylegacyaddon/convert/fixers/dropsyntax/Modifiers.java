package com.electrifiedded.luckylegacyaddon.convert.fixers.dropsyntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Modifiers {
    public String core = "";
    public String luck;
    public String chance;
    public final List<String> extraDrops = new ArrayList<>();
    public int changes;
    public final List<String> notes = new ArrayList<>();
    public boolean dangling;

    private static final Pattern MOD = Pattern.compile("@([A-Za-z]{2,8})\\s*([=-]?)");
    private static final Pattern LUCK_VAL = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?(?:[-+]\\d+)*");
    private static final Pattern CHANCE_VAL = Pattern.compile("[-+]?(?:\\d+\\.?\\d*|\\.\\d+)");
    private static final Pattern BARE_MOD = Pattern.compile("^\\s*#?(luck|chance|hance|cance|chanse)\\s*=");
    private static final Pattern NEW_DROP = Pattern.compile("^\\s*(?:(?i:id|type)\\s*=|(?i:group)\\s*[(:])");
    private static final Pattern PROP_CONT = Pattern.compile("^\\s*,\\s*[A-Za-z_]\\w*\\s*=");

    static char kind(String name) {
        String n = name.toLowerCase(Locale.ENGLISH);
        if (n.equals("luck") || n.equals("lucky") || n.equals("luk") || n.equals("lcuk") || n.equals("luc")) return 'l';
        if (n.equals("chance")) return 'c';
        if (n.length() >= 4 && n.charAt(0) == 'c' && dist(n, "chance") <= 2) return 'c';
        return 0;
    }

    static int dist(String a, String b) {
        int[][] d = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) d[i][0] = i;
        for (int j = 0; j <= b.length(); j++) d[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + c);
            }
        }
        return d[a.length()][b.length()];
    }

    private static final Pattern TAIL_NO_AT = Pattern.compile(
            "(?s)^(.*?[)\\]\"\\s])((?:\\s*@?(?:luck|chance)\\s*=\\s*[-+]?\\d*\\.?\\d+\\s*)+)$");

    public static Modifiers parse(String s) {
        return parse(s, false);
    }

    public static Modifiers parse(String s, boolean ignoreQuotes) {
        Modifiers m = new Modifiers();
        m.ignoreQuotes = ignoreQuotes;
        m.run(s);
        return m;
    }

    private boolean ignoreQuotes;

    public String suffix() {
        return (luck == null ? "" : "@luck=" + luck) + (chance == null ? "" : "@chance=" + chance);
    }

    private void run(String s) {
        Matcher tm = TAIL_NO_AT.matcher(s);
        if (tm.matches() && Pattern.compile("(?<![@\\w])(?:luck|chance)\\s*=").matcher(tm.group(2)).find()) {
            s = tm.group(1) + tm.group(2).replaceAll("(?<![@\\w])(luck|chance)", "@$1");
            changes++;
            notes.add("added missing '@' before trailing modifier");
        }
        StringBuilder core = new StringBuilder();
        int n = s.length();
        int i = 0;
        boolean inQ = false;
        boolean sawMod = false;
        while (i < n) {
            char c = s.charAt(i);
            boolean canceled = i > 0 && s.charAt(i - 1) == '\\';
            if (!canceled && c == '"') inQ = !inQ;
            if ((ignoreQuotes || !inQ) && c == '@' && !canceled) {
                Matcher mm = MOD.matcher(s);
                mm.region(i, n);
                if (mm.lookingAt() && kind(mm.group(1)) != 0) {
                    char k = kind(mm.group(1));
                    boolean irregular = !(mm.group(1).equals(k == 'l' ? "luck" : "chance") && mm.group(2).equals("="));
                    int vs = mm.end();
                    if (mm.group(2).equals("-")) vs = mm.end() - 1;
                    if (irregular) {
                        changes++;
                        notes.add("irregular modifier '" + s.substring(i, Math.min(n, i + 14)) + "'");
                    }
                    sawMod = true;
                    String val = null;
                    int ve = vs;
                    if (vs < n && s.charAt(vs) == '(') {
                        int close = Lex.matching(s, vs);
                        if (close > 0) {
                            val = s.substring(vs + 1, close).trim();
                            ve = close + 1;
                            changes++;
                        }
                    }
                    if (val == null) {
                        Matcher vm = (k == 'l' ? LUCK_VAL : CHANCE_VAL).matcher(s);
                        vm.region(vs, n);
                        if (vm.lookingAt()) {
                            val = vm.group();
                            ve = vm.end();
                            if (k == 'l' && val.matches("[-+]?\\d+(?:[-+]\\d+)+")) {
                                Matcher t = Pattern.compile("[-+]?\\d+").matcher(val);
                                long sum = 0;
                                while (t.find()) sum += Long.parseLong(t.group().replace("+", ""));
                                notes.add("evaluated luck '" + val + "' = " + sum);
                                val = Long.toString(sum);
                                changes++;
                            }
                        }
                    }
                    if (val == null) {
                        int e = ve;
                        while (e < n && s.charAt(e) != '@') e++;
                        String junk = s.substring(vs, e).trim();
                        if (junk.isEmpty() && e >= n) dangling = true;
                        else if (junk.startsWith("#")) {
                            val = junk;
                            ve = e;
                        } else {
                            changes++;
                            notes.add("dropped modifier with bad value '" + junk + "'");
                            ve = e;
                        }
                        if (val == null) {
                            i = ve;
                            continue;
                        }
                    }
                    String prevLuck = luck;
                    String prevChance = chance;
                    if (k == 'l') luck = val; else chance = val;
                    String rest = s.substring(ve);
                    if (rest.isEmpty() || rest.startsWith("@")) {
                        i = ve;
                        continue;
                    }
                    if (rest.charAt(0) == ';' || rest.charAt(0) == ')' || rest.charAt(0) == ']') {
                        String after = rest.replaceFirst("^[;)\\]\\s]*", "");
                        if (!after.isEmpty() && after.charAt(0) != '@' && !BARE_MOD.matcher(after).find()) {
                            luck = prevLuck;
                            chance = prevChance;
                            changes++;
                            notes.add("modifier inside drop text removed");
                        } else {
                            changes++;
                        }
                        i = ve;
                        continue;
                    }
                    if (BARE_MOD.matcher(rest).find()) {
                        String fixed = rest.replaceFirst("^\\s*#?(?:hance|cance|chanse)", "chance");
                        fixed = fixed.replaceFirst("^\\s*#?", "");
                        s = s.substring(0, ve) + "@" + fixed;
                        n = s.length();
                        changes++;
                        notes.add("glued modifier restored");
                        i = ve;
                        continue;
                    }
                    if (NEW_DROP.matcher(rest).find()) {
                        extraDrops.add(rest.trim());
                        changes++;
                        notes.add("glued drop split into its own line");
                        i = n;
                        break;
                    }
                    if (PROP_CONT.matcher(rest).find()) {
                        changes++;
                        notes.add("properties after modifier moved back into the drop");
                        i = ve;
                        continue;
                    }
                    int e = ve;
                    boolean q2 = false;
                    while (e < n) {
                        char d = s.charAt(e);
                        if (d == '"' && !(e > 0 && s.charAt(e - 1) == '\\')) q2 = !q2;
                        if (!q2 && d == '@') break;
                        e++;
                    }
                    String junk = s.substring(ve, e).trim();
                    changes++;
                    int glued = junk.indexOf("type=");
                    if (glued < 0) glued = junk.indexOf("ID=");
                    if (glued >= 0 && NEW_DROP.matcher(junk.substring(glued)).find()) {
                        extraDrops.add(s.substring(ve + s.substring(ve).indexOf(junk) + glued).trim());
                        notes.add("glued drop split into its own line");
                        i = n;
                        break;
                    }
                    if (!junk.isEmpty() && !junk.equals(",") && !junk.equals(";")) notes.add("dropped text after modifier '" + abbreviate(junk) + "'");
                    i = e;
                    continue;
                }
            }
            if (sawMod && !Character.isWhitespace(c)) {
                changes++;
                sawMod = false;
            }
            core.append(c);
            i++;
        }
        this.core = core.toString().trim();
    }

    private static String abbreviate(String x) { return x.length() > 40 ? x.substring(0, 40) + "..." : x; }
}
