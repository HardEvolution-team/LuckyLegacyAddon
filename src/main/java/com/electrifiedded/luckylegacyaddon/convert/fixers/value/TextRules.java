package com.electrifiedded.luckylegacyaddon.convert.fixers.value;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class TextRules {
    private TextRules() {}

    private static final Pattern RANDOM_DASH = Pattern.compile("#random-(\\d+)-(\\d+)");
    private static final Pattern RAND_EQ = Pattern.compile("#rand=\\(");
    private static final Pattern RAND_AMP = Pattern.compile("(?:&(?:rand|ran)|#(?:ran|rnd))\\((?=[^()]*,)");
    private static final Pattern RAND_NO_HASH = Pattern.compile("(?<=[=,(\\[:+*/;-])rand\\((?=\\s*-?[\\d.]+[a-zA-Z]?\\s*,)");
    private static final Pattern AMOUNT_PAIR = Pattern.compile("(?i)(?<![\\w])(amount|delay|damage)=\\((-?\\d+),(-?\\d+)\\)(?=[,;)@]|$)");
    private static final Pattern DAMAGE_RAND = Pattern.compile("(?i)(damage=)#rand(?![\\w(=])");
    private static final Pattern PLAYER_NAME = Pattern.compile("#playerName\\b");
    private static final Pattern CHEST_NAME = Pattern.compile("#chest([A-Za-z]+)(?![A-Za-z(])");
    private static final Pattern LUCKY_VAR = Pattern.compile("#lucky[A-Za-z]*");
    private static final Pattern PAREN_NUMBER = Pattern.compile("(?<![\\w])([A-Za-z_]\\w*)=\\((-?\\d+(?:\\.\\d+)?[a-zA-Z]?)\\)(?=[,;@)\\]]|$)");
    private static final Pattern ID_UNDERSCORE = Pattern.compile("\\((?=id_)id_");

    static String apply(String s, Stats st) {
        if (s.indexOf('#') < 0 && s.indexOf('&') < 0 && s.indexOf("rand(") < 0 && s.indexOf('(') < 0) return s;
        String o = s;
        s = sub(RANDOM_DASH, s, "#rand($1,$2)", st, "rand-syntax");
        s = sub(RAND_EQ, s, "#rand(", st, "rand-syntax");
        s = sub(RAND_AMP, s, "#rand(", st, "rand-syntax");
        s = sub(RAND_NO_HASH, s, "#rand(", st, "rand-syntax");
        s = sub(AMOUNT_PAIR, s, "$1=#rand($2,$3)", st, "rand-pair");
        s = sub(DAMAGE_RAND, s, "$1#randPotionDamage", st, "rand-damage");
        s = sub(PAREN_NUMBER, s, "$1=$2", st, "paren-number");
        s = sub(PLAYER_NAME, s, "#pName", st, "playerName");
        s = fixChestNames(s, st);
        s = fixLuckyVars(s, st);
        s = s.replace("(id_", "(id=");
        if (!s.equals(o)) st.count("text-rules");
        return s;
    }

    private static String sub(Pattern p, String s, String repl, Stats st, String key) {
        Matcher m = p.matcher(s);
        if (!m.find()) return s;
        st.count(key);
        return m.replaceAll(repl);
    }

    private static String fixChestNames(String s, Stats st) {
        Matcher m = CHEST_NAME.matcher(s);
        StringBuffer sb = new StringBuffer();
        boolean any = false;
        while (m.find()) {
            String table = Tables.CHEST.get(m.group(1).toLowerCase(Locale.ROOT));
            if (table == null) { m.appendReplacement(sb, Matcher.quoteReplacement(m.group())); continue; }
            any = true;
            m.appendReplacement(sb, Matcher.quoteReplacement("#chestLootTable(" + table + ")"));
        }
        if (!any) return s;
        m.appendTail(sb);
        st.count("chest-loot-table");
        return sb.toString();
    }

    private static String fixLuckyVars(String s, Stats st) {
        if (s.indexOf("#lucky") < 0) return s;
        Matcher m = LUCKY_VAR.matcher(s);
        StringBuffer sb = new StringBuffer();
        boolean any = false;
        while (m.find()) {
            String tok = m.group();
            String best = null;
            for (String k : Tables.ENCH_VARS) if (k.equals(tok)) { best = k; break; }
            if (best == null && !isOtherLuckyVar(tok)) {
                String low = tok.toLowerCase(Locale.ROOT);
                if (low.startsWith("#luckypickaxe")) best = "#luckyToolEnchantments";
                else {
                    int bd = 4;
                    for (String k : Tables.ENCH_VARS) {
                        int d = lev(low, k.toLowerCase(Locale.ROOT));
                        if (d < bd) { bd = d; best = k; }
                    }
                }
            }
            if (best != null && !best.equals(tok)) { any = true; m.appendReplacement(sb, Matcher.quoteReplacement(best)); }
            else m.appendReplacement(sb, Matcher.quoteReplacement(tok));
        }
        if (!any) return s;
        m.appendTail(sb);
        st.count("enchant-var-typo");
        return sb.toString();
    }

    private static boolean isOtherLuckyVar(String tok) {
        String l = tok.toLowerCase(Locale.ROOT);
        return !l.contains("ench");
    }

    private static int lev(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
