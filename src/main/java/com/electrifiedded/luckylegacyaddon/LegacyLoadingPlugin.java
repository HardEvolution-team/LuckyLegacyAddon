package com.electrifiedded.luckylegacyaddon;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("LuckyLegacyAddon")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public class LegacyLoadingPlugin implements IFMLLoadingPlugin {
    @Override public String[] getASMTransformerClass() { return new String[0]; }
    @Override public String getModContainerClass() { return null; }
    @Override public String getSetupClass() { return null; }
    @Override public String getAccessTransformerClass() { return null; }

    @Override
    public void injectData(Map<String, Object> data) {
        java.io.File mcDir = (java.io.File) data.get("mcLocation");
        try {
            com.electrifiedded.luckylegacyaddon.convert.AddonConverter.run(mcDir);
        } catch (Throwable t) {
            System.err.println("[LuckyLegacyAddon] add-on conversion failed");
            t.printStackTrace();
        }
    }
}
