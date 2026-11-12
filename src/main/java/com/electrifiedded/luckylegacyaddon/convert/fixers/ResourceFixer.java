package com.electrifiedded.luckylegacyaddon.convert.fixers;

import com.electrifiedded.luckylegacyaddon.convert.Converted;
import com.electrifiedded.luckylegacyaddon.convert.fixers.resource.AssetFixer;
import com.electrifiedded.luckylegacyaddon.convert.fixers.resource.PluginIds;
import com.electrifiedded.luckylegacyaddon.convert.fixers.resource.TextFiles;

public class ResourceFixer implements Fixer {
    @Override public String name() { return "Resource"; }

    @Override
    public void fix(Converted addon) {
        PluginIds.Ids ids = null;
        try {
            ids = PluginIds.process(addon);
        } catch (Throwable t) {
            err(addon, "plugin_init", t);
        }
        try {
            TextFiles.extraDropFiles(addon, ids);
        } catch (Throwable t) {
            err(addon, "drop files", t);
        }
        try {
            TextFiles.dropFileJunk(addon);
        } catch (Throwable t) {
            err(addon, "drop file junk", t);
        }
        try {
            TextFiles.properties(addon);
        } catch (Throwable t) {
            err(addon, "properties", t);
        }
        try {
            TextFiles.luckCrafting(addon);
        } catch (Throwable t) {
            err(addon, "luck_crafting", t);
        }
        try {
            TextFiles.recipes(addon);
        } catch (Throwable t) {
            err(addon, "recipes", t);
        }
        try {
            TextFiles.naturalGen(addon, ids);
        } catch (Throwable t) {
            err(addon, "natural_gen", t);
        }
        try {
            AssetFixer.fix(addon, ids);
        } catch (Throwable t) {
            err(addon, "assets", t);
        }
    }

    private static void err(Converted c, String what, Throwable t) {
        System.out.println("[LuckyLegacyAddon] [" + c.name + "] ResourceFixer " + what + " failed: " + t);
    }
}
