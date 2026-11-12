package com.electrifiedded.luckylegacyaddon.convert.fixers;

import com.electrifiedded.luckylegacyaddon.convert.Converted;
import com.electrifiedded.luckylegacyaddon.convert.fixers.dropsyntax.DropFileRepairer;

import java.util.ArrayList;
import java.util.List;

public class DropSyntaxFixer implements Fixer {
    static final String[] FILES = {"drops.txt", "bow_drops.txt", "sword_drops.txt", "potion_drops.txt"};

    @Override public String name() { return "DropSyntax"; }

    @Override
    public void fix(Converted addon) {
        for (String f : FILES) {
            try {
                String t = addon.text(f);
                if (t == null) continue;
                DropFileRepairer.Output out = DropFileRepairer.repair(t);
                if (!out.changed || out.text.equals(t)) continue;
                addon.putText(f, out.text);
                List<String> parts = new ArrayList<>();
                out.counts.forEach((k, v) -> parts.add(v + "x " + k));
                System.out.println("[LuckyLegacyAddon] DropSyntax: " + addon.name + "/" + f + ": " + String.join("; ", parts));
                for (String e : out.examples) System.out.println("[LuckyLegacyAddon]   e.g. " + e);
            } catch (Throwable t) {
                System.out.println("[LuckyLegacyAddon] DropSyntax: failed on " + addon.name + "/" + f + ": " + t);
            }
        }
    }
}
