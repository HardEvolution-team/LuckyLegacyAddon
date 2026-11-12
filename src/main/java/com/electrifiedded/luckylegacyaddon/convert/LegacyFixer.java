package com.electrifiedded.luckylegacyaddon.convert;

import com.electrifiedded.luckylegacyaddon.convert.fixers.*;

public final class LegacyFixer {
    private LegacyFixer() {}

    static final String[] LUCKY_TXT = {"drops.txt", "bow_drops.txt", "sword_drops.txt", "potion_drops.txt", "luck_crafting.txt",
            "natural_gen.txt", "properties.txt", "recipes.txt", "structures.txt", "plugin_init.txt"};

    static final Fixer[] FIXERS = {new DropSyntaxFixer(), new ValueFixer(), new StructureFixer(), new ResourceFixer()};

    public static void fix(Converted c) {
        for (String f : LUCKY_TXT) {
            String t = c.text(f);
            if (t == null) continue;
            t = t.replace("\r\n", "\n").replace('\r', '\n');
            c.putText(f, t);
        }
        for (Fixer f : FIXERS) {
            try {
                f.fix(c);
            } catch (Throwable t) {
                AddonConverter.err("fixer " + f.name() + " failed on " + c.name, t);
            }
        }
        if (!c.files.containsKey("pack.mcmeta")) {
            c.putText("pack.mcmeta", "{\"pack\":{\"pack_format\":3,\"description\":\"" + c.name.replace("\"", "") + "\"}}\n");
        }
    }
}
