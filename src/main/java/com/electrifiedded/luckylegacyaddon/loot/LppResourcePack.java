package com.electrifiedded.luckylegacyaddon.loot;

import com.electrifiedded.archaiclib.v189.LegacyModelConverter189;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.resources.AbstractResourcePack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@SideOnly(Side.CLIENT)
final class LppResourcePack extends AbstractResourcePack {
    private static final String PREFIX = "assets/" + LootPlusPlus.MODID + "/";

    private static final class Src {
        final File file;
        final String entry;
        byte[] data;

        Src(File f, String e) {
            file = f;
            entry = e;
        }
    }

    static volatile LppResourcePack INSTANCE;

    private final Map<String, Src> index = new HashMap<>();

    boolean has(String path) {
        return index.containsKey(path.toLowerCase(Locale.ROOT));
    }
    private byte[] sounds = "{}".getBytes(StandardCharsets.UTF_8);
    private final byte[] mcmeta = "{\"pack\":{\"pack_format\":3,\"description\":\"Loot++ assets of the Lucky add-ons\"}}".getBytes(StandardCharsets.UTF_8);

    LppResourcePack(File dummy) {
        super(dummy);
        JsonObject merged = new JsonObject();
        for (LppConfig.Addon a : LppConfig.addons()) {
            try {
                if (a.files != null) indexMem(a.files, merged);
                else if (a.file.isDirectory()) indexDir(a.file, a.file, merged);
                else indexZip(a.file, merged);
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Could not index assets of {}: {}", a.name, t.toString());
            }
        }
        INSTANCE = this;
        sounds = LegacyModelConverter189.convertSounds(LootPlusPlus.MODID,
                merged.toString().getBytes(StandardCharsets.UTF_8),
                (ns, path) -> index.containsKey(("assets/" + ns + "/sounds/" + path + ".ogg").toLowerCase(Locale.ROOT)));
    }

    private void mergeSounds(InputStream in, JsonObject merged) {
        try {
            JsonElement el = new JsonParser().parse(new java.io.InputStreamReader(in, StandardCharsets.UTF_8));
            if (el.isJsonObject()) {
                for (Map.Entry<String, JsonElement> en : el.getAsJsonObject().entrySet()) {
                    if (!merged.has(en.getKey())) merged.add(en.getKey(), en.getValue());
                }
            }
        } catch (Throwable t) {
            LootPlusPlus.LOG.warn("[Loot++] Unreadable sounds.json: {}", t.toString());
        }
    }

    private void indexMem(Map<String, byte[]> files, JsonObject merged) {
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String n = e.getKey();
            if (!n.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) continue;
            String key = n.toLowerCase(Locale.ROOT);
            if (key.equals(PREFIX + "sounds.json")) {
                mergeSounds(new ByteArrayInputStream(e.getValue()), merged);
                continue;
            }
            Src s = new Src(null, n);
            s.data = e.getValue();
            index.putIfAbsent(key, s);
        }
    }

    private void indexZip(File f, JsonObject merged) throws IOException {
        try (ZipFile zf = new ZipFile(f)) {
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (e.isDirectory()) continue;
                String n = e.getName();
                if (!n.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) continue;
                String key = n.toLowerCase(Locale.ROOT);
                if (key.equals(PREFIX + "sounds.json")) {
                    try (InputStream in = zf.getInputStream(e)) {
                        mergeSounds(in, merged);
                    }
                    continue;
                }
                index.putIfAbsent(key, new Src(f, n));
            }
        }
    }

    private void indexDir(File root, File dir, JsonObject merged) throws IOException {
        File[] fs = dir.listFiles();
        if (fs == null) return;
        for (File c : fs) {
            if (c.isDirectory()) {
                indexDir(root, c, merged);
                continue;
            }
            String n = root.toURI().relativize(c.toURI()).getPath();
            if (!n.regionMatches(true, 0, PREFIX, 0, PREFIX.length())) continue;
            String key = n.toLowerCase(Locale.ROOT);
            if (key.equals(PREFIX + "sounds.json")) {
                try (InputStream in = new FileInputStream(c)) {
                    mergeSounds(in, merged);
                }
                continue;
            }
            index.putIfAbsent(key, new Src(c, null));
        }
    }

    @Override
    protected InputStream getInputStreamByName(String name) throws IOException {
        String key = name.toLowerCase(Locale.ROOT);
        if (key.equals("pack.mcmeta")) return new ByteArrayInputStream(mcmeta);
        if (key.equals(PREFIX + "sounds.json")) return new ByteArrayInputStream(sounds);
        Src s = index.get(key);
        if (s == null) throw new IOException("Missing resource " + name);
        byte[] raw;
        if (s.data != null) {
            raw = s.data;
        } else if (s.entry == null) {
            try (InputStream in = new FileInputStream(s.file)) {
                raw = readAll(in);
            }
        } else {
            try (ZipFile zf = new ZipFile(s.file)) {
                ZipEntry e = zf.getEntry(s.entry);
                if (e == null) throw new IOException("Missing resource " + name);
                try (InputStream in = zf.getInputStream(e)) {
                    raw = readAll(in);
                }
            }
        }
        return new ByteArrayInputStream(LegacyModelConverter189.convert(key, raw, this::textureExists));
    }

    private boolean textureExists(String namespace, String path) {
        String texture = path.toLowerCase(Locale.ROOT);
        return index.containsKey("assets/" + namespace.toLowerCase(Locale.ROOT) + "/textures/" + texture + ".png");
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    @Override
    protected boolean hasResourceName(String name) {
        String key = name.toLowerCase(Locale.ROOT);
        return key.equals("pack.mcmeta") || key.equals(PREFIX + "sounds.json") || index.containsKey(key);
    }

    @Override
    public Set<String> getResourceDomains() {
        return Collections.singleton(LootPlusPlus.MODID);
    }

    @Override
    public String getPackName() {
        return "Loot++ (Lucky add-ons)";
    }
}
