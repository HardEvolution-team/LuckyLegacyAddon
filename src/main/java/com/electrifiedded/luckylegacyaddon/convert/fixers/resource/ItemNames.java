package com.electrifiedded.luckylegacyaddon.convert.fixers.resource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ItemNames {
    private ItemNames() {}

    private static Set<String> vanilla;
    private static final Map<String, String> ALIAS = new HashMap<>();
    private static final String[] COLORS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "silver", "cyan",
            "purple", "blue", "brown", "green", "red", "black"};
    private static final String[] DYE_COLORS = {"black", "red", "green", "brown", "blue", "purple", "cyan", "silver", "gray", "pink",
            "lime", "yellow", "light_blue", "magenta", "orange", "white"};
    private static final Pattern NUM = Pattern.compile("-?\\d+");
    private static final Pattern COLORED = Pattern.compile(
            "^([a-z_]+?)_(wool|carpet|stained_glass_pane|stained_glass|stained_hardened_clay|terracotta|dye)$");

    static {
        ALIAS.put("cobweb", "web");
        ALIAS.put("lapis_lazuli", "dye:4");
        ALIAS.put("lapis_lazuli_block", "lapis_block");
        ALIAS.put("lapiz_block", "lapis_block");
        ALIAS.put("lapis", "dye:4");
        ALIAS.put("cooked_fished", "cooked_fish");
        ALIAS.put("raw_fish", "fish");
        ALIAS.put("ink_sac", "dye:0");
        ALIAS.put("cocoa_beans", "dye:3");
        ALIAS.put("bone_meal", "dye:15");
        ALIAS.put("grass_block", "grass");
        ALIAS.put("bricks", "brick_block");
    }

    private static synchronized Set<String> vanilla() {
        if (vanilla != null) return vanilla;
        Set<String> s = new HashSet<>();
        try (InputStream in = ItemNames.class.getResourceAsStream("/mc1122_items.txt")) {
            if (in != null) {
                BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String l;
                while ((l = r.readLine()) != null) if (!l.isBlank()) s.add(l.trim());
            }
        } catch (Throwable ignored) {
        }
        vanilla = s;
        return s;
    }

    public static boolean available() {
        return !vanilla().isEmpty();
    }

    public static String fix(String token) {
        if (token == null) return null;
        String t = token.trim().toLowerCase(Locale.ROOT);
        if (t.isEmpty()) return null;
        if (!available()) return t;
        if (t.startsWith("minecraft:")) t = t.substring("minecraft:".length());
        if (NUM.matcher(t).matches()) return t;
        String meta = null;
        String name = t;
        int c = t.lastIndexOf(':');
        if (c > 0 && NUM.matcher(t.substring(c + 1)).matches()) {
            name = t.substring(0, c);
            meta = t.substring(c + 1);
            if (name.startsWith("minecraft:")) name = name.substring("minecraft:".length());
        }
        int colon = name.indexOf(':');
        if (colon >= 0) {
            String ns = name.substring(0, colon);
            if (ns.equals("lucky")) {
                String p = name.substring(colon + 1);
                return PluginIds.knownBlockOrItem(p) || p.startsWith("lucky_") ? t : null;
            }
            return t;
        }
        if (vanilla().contains(name)) return meta == null ? name : name + ":" + meta;
        String a = ALIAS.get(name);
        if (a != null) {
            if (a.indexOf(':') > 0) return meta == null ? a : null;
            return meta == null ? a : a + ":" + meta;
        }
        Matcher m = COLORED.matcher(name);
        if (m.matches() && meta == null) {
            String col = m.group(1);
            String kind = m.group(2);
            if (col.equals("light_gray")) col = "silver";
            if (kind.equals("dye")) {
                for (int i = 0; i < 16; i++) if (DYE_COLORS[i].equals(col)) return "dye:" + i;
            } else {
                String base = kind.equals("terracotta") ? "stained_hardened_clay" : kind;
                for (int i = 0; i < 16; i++) if (COLORS[i].equals(col)) return base + ":" + i;
            }
        }
        return null;
    }
}
