package com.electrifiedded.luckylegacyaddon.convert.fixers;

import com.electrifiedded.luckylegacyaddon.convert.Converted;
import com.electrifiedded.luckylegacyaddon.convert.fixers.structure.LuckyStructRepair;
import com.electrifiedded.luckylegacyaddon.convert.fixers.structure.SchematicRepair;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class StructureFixer implements Fixer {
    private static final String PLACEHOLDER = "_lucky_legacy_missing.schematic";

    @Override public String name() { return "Structure"; }

    @Override
    public void fix(Converted addon) {
        try {
            fixLuckyStructs(addon);
        } catch (Throwable t) {
            System.err.println("[LuckyLegacyAddon] StructureFixer (.luckystruct) failed on " + addon.name + ": " + t);
        }
        String txt = addon.text("structures.txt");
        if (txt == null) return;
        try {
            fixImpl(addon, txt);
        } catch (Throwable t) {
            System.err.println("[LuckyLegacyAddon] StructureFixer failed on " + addon.name + ": " + t);
        }
    }

    private static void fixLuckyStructs(Converted a) {
        for (String key : new ArrayList<>(a.files.keySet())) {
            if (!key.startsWith("structures/") || !key.toLowerCase().endsWith(".luckystruct")) continue;
            String txt = new String(a.files.get(key), StandardCharsets.ISO_8859_1);
            LuckyStructRepair.Out o = LuckyStructRepair.repair(txt);
            if (o.converted == 0) continue;
            a.files.put(key, o.text.getBytes(StandardCharsets.ISO_8859_1));
            log(a, key.substring("structures/".length()) + ": " + o.converted + " block row(s) tile entity -> (Luck=N) form, e.g. " + o.examples.get(0));
        }
    }

    private static void log(Converted a, String s) {
        System.out.println("[LuckyLegacyAddon] " + a.name + ": " + s);
    }

    static List<String> readLines(String txt) {
        List<String> out = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new StringReader(txt))) {
            StringBuilder cur = new StringBuilder();
            boolean open = false;
            String l;
            while ((l = br.readLine()) != null) {
                l = l.trim();
                if (l.startsWith("/") || l.isEmpty()) continue;
                if (l.endsWith("\\")) {
                    cur.append(l, 0, l.length() - 1).append(' ');
                    open = true;
                    continue;
                }
                cur.append(l);
                out.add(cur.toString().trim());
                cur.setLength(0);
                open = false;
            }
            if (open && cur.length() > 0) out.add(cur.toString().trim());
        } catch (Exception ignored) {
        }
        return out;
    }

    private void fixImpl(Converted a, String txt) {
        Map<String, SchematicRepair.Out> repaired = new HashMap<>();
        List<String> outLines = new ArrayList<>();
        boolean placeholderUsed = false;

        for (String line : readLines(txt)) {
            List<String[]> props = new ArrayList<>();
            for (String p : line.split(",")) {
                int eq = p.indexOf('=');
                if (eq < 0) {
                    if (!p.trim().isEmpty()) log(a, "structures.txt: dropped malformed property '" + p.trim() + "'");
                    continue;
                }
                String k = p.substring(0, eq).trim(), v = p.substring(eq + 1).trim();
                if (k.isEmpty()) continue;
                if (v.isEmpty()) {
                    log(a, "structures.txt: dropped empty property '" + k + "'");
                    continue;
                }
                props.add(new String[] {k, v});
            }
            String[] idP = find(props, "id"), fileP = find(props, "file");
            if (idP == null || fileP == null) {
                log(a, "structures.txt: dropped line without ID/file: " + line);
                continue;
            }
            String wanted = unquote(fileP[1]);
            String key = resolve(a, wanted);
            String newFile;
            if (key == null) {
                log(a, "structures.txt: structure file '" + wanted + "' not found -> empty placeholder for ID " + idP[1]);
                placeholderUsed = true;
                newFile = PLACEHOLDER;
                key = "structures/" + PLACEHOLDER;
            } else {
                newFile = key.substring("structures/".length());
                if (!newFile.equals(wanted)) log(a, "structures.txt: file '" + wanted + "' -> '" + newFile + "'");
            }
            if (!key.equals("structures/" + PLACEHOLDER) && !key.toLowerCase().endsWith(".luckystruct")) {
                SchematicRepair.Out o = repaired.get(key);
                if (o == null && !repaired.containsKey(key)) {
                    o = SchematicRepair.repair(a.files.get(key));
                    repaired.put(key, o);
                    if (o == null) {
                        log(a, "structure file '" + newFile + "' is unreadable -> replaced by empty placeholder");
                    } else if (o.data != null) {
                        a.files.put(key, o.data);
                        log(a, "repaired schematic " + newFile + ": " + o.notes);
                    }
                }
                if (o == null) {
                    placeholderUsed = true;
                    newFile = PLACEHOLDER;
                } else if (o.cropped) {
                    adjustCenter(props, o);
                }
            }
            fileP[1] = newFile;
            StringBuilder sb = new StringBuilder();
            for (String[] p : props) sb.append(sb.length() == 0 ? "" : ",").append(p[0]).append('=').append(p[1]);
            outLines.add(sb.toString());
        }
        if (placeholderUsed) a.files.put("structures/" + PLACEHOLDER, SchematicRepair.placeholder());

        String res = String.join("\n", outLines) + (outLines.isEmpty() ? "" : "\n");
        if (!res.equals(txt)) a.putText("structures.txt", res);
    }

    private static String[] find(List<String[]> props, String key) {
        for (String[] p : props) if (p[0].equalsIgnoreCase(key)) return p;
        return null;
    }

    private static String unquote(String s) {
        s = s.trim();
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) s = s.substring(1, s.length() - 1);
        return s;
    }

    private static void adjustCenter(List<String[]> props, SchematicRepair.Out o) {
        shiftCenter(props, "centerX", o.oldW / 2, o.offX, true);
        shiftCenter(props, "centerY", 0, o.offY, false);
        shiftCenter(props, "centerZ", o.oldL / 2, o.offZ, true);
    }

    private static void shiftCenter(List<String[]> props, String key, int dflt, int off, boolean always) {
        String[] p = find(props, key);
        if (p == null) {
            if (off == 0 && !always) return;
            if (off == 0) {
            }
            props.add(new String[] {key, Integer.toString(dflt - off)});
            return;
        }
        String v = p[1].trim();
        try {
            p[1] = Integer.toString(Integer.parseInt(v) - off);
            return;
        } catch (NumberFormatException ignored) {
        }
        try {
            String suffix = "";
            String num = v;
            char c = v.charAt(v.length() - 1);
            if ("fFdD".indexOf(c) >= 0) {
                suffix = String.valueOf(c);
                num = v.substring(0, v.length() - 1);
            }
            p[1] = (Double.parseDouble(num) - off) + suffix;
        } catch (Exception ignored) {
        }
    }


    private static String nz(String s) {
        return s.replace('\\', '/').trim().toLowerCase().replace(' ', '_');
    }

    private static String dirOf(String p) {
        int i = p.lastIndexOf('/');
        return i < 0 ? "" : p.substring(0, i);
    }

    private static String baseOf(String p) {
        return p.substring(p.lastIndexOf('/') + 1);
    }

    private static String stemOf(String base) {
        int i = base.lastIndexOf('.');
        return i < 0 ? base : base.substring(0, i);
    }

    static String resolve(Converted a, String wanted) {
        String w = wanted.replace('\\', '/').trim();
        while (w.startsWith("/") || w.startsWith("./")) w = w.startsWith("/") ? w.substring(1) : w.substring(2);
        if (w.isEmpty()) return null;
        String exact = "structures/" + w;
        if (a.files.containsKey(exact)) return exact;

        TreeSet<String> keys = new TreeSet<>();
        for (String k : a.files.keySet()) {
            if (k.startsWith("structures/") && (k.endsWith(".schematic") || k.endsWith(".luckystruct"))) keys.add(k);
        }
        String wk = nz(exact);
        for (String k : keys) if (nz(k).equals(wk)) return k;

        String alt = w.toLowerCase().startsWith("structures/") ? nz(w.substring(11)) : nz("structures/" + w);
        for (String k : keys) if (nz(k).equals("structures/" + alt) || nz(k).equals(alt)) return k;

        String wdir = dirOf(wk), wstem = stemOf(baseOf(wk));
        String wstemBase = wstem.contains(".") ? wstem.substring(0, wstem.indexOf('.')) : wstem;
        for (String k : keys) {
            String nk = nz(k);
            if (!dirOf(nk).equals(wdir)) continue;
            String ks = stemOf(baseOf(nk));
            if (ks.equals(wstemBase) || ks.endsWith("_" + wstem) || ks.replace("_", "").equals(wstem.replace("_", ""))) return k;
        }
        String best = null;
        int bestScore = -1;
        for (String k : keys) {
            String nk = nz(k);
            String ks = stemOf(baseOf(nk));
            if (!(ks.equals(wstem) || ks.equals(wstemBase))) continue;
            int score = commonPrefix(dirOf(nk), wdir);
            if (score > bestScore) {
                bestScore = score;
                best = k;
            }
        }
        return best;
    }

    private static int commonPrefix(String a, String b) {
        String[] x = a.split("/"), y = b.split("/");
        int n = 0;
        while (n < x.length && n < y.length && x[n].equals(y[n])) n++;
        return n;
    }
}
