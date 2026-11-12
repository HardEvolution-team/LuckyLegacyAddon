package com.electrifiedded.luckylegacyaddon.compat;

import com.electrifiedded.luckylegacyaddon.convert.NativeAddons;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.data.IMetadataSection;
import net.minecraft.client.resources.data.MetadataSerializer;
import net.minecraft.util.ResourceLocation;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/** Resource pack over a natively loaded (in-memory) legacy add-on; nothing is read from or written to disk. */
public final class MemoryResourcePack implements IResourcePack {
    private final NativeAddons.Addon addon;
    private final Map<String, byte[]> lower = new HashMap<>();
    private final Set<String> domains = new HashSet<>();

    private MemoryResourcePack(NativeAddons.Addon a) {
        addon = a;
        for (Map.Entry<String, byte[]> e : a.files.entrySet()) {
            String k = e.getKey().toLowerCase(Locale.ROOT);
            lower.putIfAbsent(k, e.getValue());
            if (k.startsWith("assets/")) {
                int i = k.indexOf('/', 7);
                if (i > 7) domains.add(k.substring(7, i));
            }
        }
    }

    public static Object of(NativeAddons.Addon a) {
        // 1.8 assets: models/sounds go through the ArchaicLib 1.8.9 engine
        return com.electrifiedded.archaiclib.v189.LegacyModelHooks189.wrap(new MemoryResourcePack(a));
    }

    private static String path(ResourceLocation l) {
        return ("assets/" + l.getNamespace() + "/" + l.getPath()).toLowerCase(Locale.ROOT);
    }

    @Override public InputStream getInputStream(ResourceLocation l) throws IOException {
        byte[] b = lower.get(path(l));
        if (b == null) throw new FileNotFoundException(l.toString());
        return new ByteArrayInputStream(b);
    }
    @Override public boolean resourceExists(ResourceLocation l) { return lower.containsKey(path(l)); }
    @Override public Set<String> getResourceDomains() { return domains; }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends IMetadataSection> T getPackMetadata(MetadataSerializer s, String section) throws IOException {
        byte[] b = lower.get("pack.mcmeta");
        String json = b != null ? new String(b, java.nio.charset.StandardCharsets.UTF_8)
                : "{\"pack\":{\"pack_format\":3,\"description\":\"" + addon.name + "\"}}";
        try {
            com.google.gson.JsonObject o = new com.google.gson.JsonParser().parse(json).getAsJsonObject();
            return s.parseMetadataSection(section, o);
        } catch (Exception e) {
            return null;
        }
    }
    @Override public BufferedImage getPackImage() throws IOException { throw new FileNotFoundException("pack.png"); }
    @Override public String getPackName() { return addon.name; }
}
