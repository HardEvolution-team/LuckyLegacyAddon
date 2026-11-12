package com.electrifiedded.luckylegacyaddon.convert.fixers;

import com.electrifiedded.luckylegacyaddon.convert.Converted;
import com.electrifiedded.luckylegacyaddon.convert.fixers.value.DropWalker;
import com.electrifiedded.luckylegacyaddon.convert.fixers.value.Stats;

public class ValueFixer implements Fixer {
    private static final String[] DROP_FILES = {"drops.txt", "bow_drops.txt", "sword_drops.txt", "potion_drops.txt"};

    @Override public String name() { return "Value"; }

    @Override
    public void fix(Converted addon) {
        for (String file : DROP_FILES) {
            String text = addon.text(file);
            if (text == null) continue;
            Stats st = new Stats();
            String out = process(text, st);
            if (!out.equals(text)) {
                addon.putText(file, out);
                System.out.println("[LuckyLegacyAddon] ValueFixer " + addon.name + "/" + file + ": " + st);
            }
        }
    }

    static String process(String text, Stats st) {
        DropWalker walker = new DropWalker(st);
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder();
        StringBuilder logical = null;
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String cur = raw.trim();
            boolean last = i == lines.length - 1;
            if (cur.isEmpty() || cur.startsWith("/")) {
                if (logical == null) { out.append(raw); if (!last) out.append('\n'); }
                continue;
            }
            if (cur.endsWith("\\")) {
                if (logical == null) logical = new StringBuilder();
                logical.append(cur, 0, cur.length() - 1).append("");
                String piece = logical.toString().trim();
                logical.setLength(0);
                logical.append(piece);
                continue;
            }
            String line = logical == null ? cur : logical.append(cur).toString();
            logical = null;
            out.append(walker.processLine(line, 0));
            if (!last) out.append('\n');
        }
        if (logical != null) out.append(walker.processLine(logical.toString(), 0));
        return out.toString();
    }
}
