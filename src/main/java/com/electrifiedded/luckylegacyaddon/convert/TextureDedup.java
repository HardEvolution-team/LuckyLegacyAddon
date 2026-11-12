package com.electrifiedded.luckylegacyaddon.convert;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Add-ons share one resource namespace, so a texture path shipped by two add-ons with different pictures
 * (e.g. Diamond-Wolf repainting astral.arm_cannon_sides) silently overrides the other add-on's texture.
 * The later add-on's copy is renamed to {@code <name>_dupeN} and that add-on's own models/blockstates are
 * pointed at it, so every add-on keeps its own look. Identical copies are left shared.
 */
final class TextureDedup {
    /** texture file path -> content hash of the first add-on that shipped it. */
    private static final Map<String, String> FIRST = new HashMap<>();
    /** path -> hashes already given a dupe name (same picture in several later add-ons reuses one name). */
    private static final Map<String, Map<String, String>> DUPES = new HashMap<>();

    private TextureDedup() {}

    static synchronized void apply(String addon, Map<String, byte[]> files) {
        Map<String, String> renamedIds = new LinkedHashMap<>();
        List<String> textures = new ArrayList<>();
        for (String k : files.keySet()) if (isTexture(k)) textures.add(k);
        for (String path : textures) {
            byte[] data = files.get(path);
            String h = hash(data);
            String first = FIRST.putIfAbsent(path, h);
            if (first == null || first.equals(h)) continue;
            Map<String, String> byHash = DUPES.computeIfAbsent(path, p -> new HashMap<>());
            String newPath = byHash.get(h);
            if (newPath == null) {
                String base = path.substring(0, path.length() - 4);
                newPath = base + "_dupe" + (byHash.size() + 1) + ".png";
                byHash.put(h, newPath);
            }
            files.remove(path);
            files.put(newPath, data);
            byte[] meta = files.remove(path + ".mcmeta");
            if (meta != null) files.put(newPath + ".mcmeta", meta);
            renamedIds.put(textureId(path), textureId(newPath));
        }
        if (renamedIds.isEmpty()) return;
        int refs = 0;
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String k = e.getKey();
            if (!k.endsWith(".json") || !(k.contains("/models/") || k.contains("/blockstates/"))) continue;
            String text = new String(e.getValue(), StandardCharsets.UTF_8);
            String out = text;
            for (Map.Entry<String, String> r : renamedIds.entrySet()) {
                out = replaceRef(out, r.getKey(), r.getValue());
            }
            if (!out.equals(text)) {
                e.setValue(out.getBytes(StandardCharsets.UTF_8));
                refs++;
            }
        }
        AddonConverter.log("[" + addon + "] " + renamedIds.size() + " texture(s) clash with an earlier add-on, renamed to *_dupeN ("
                + refs + " model file(s) updated), e.g. " + renamedIds.entrySet().iterator().next());
    }

    /** Replaces a quoted texture reference, qualified ("ns:path") or, for minecraft, unqualified. */
    private static String replaceRef(String text, String id, String newId) {
        String out = text.replace("\"" + id + "\"", "\"" + newId + "\"");
        int c = id.indexOf(':');
        String ns = id.substring(0, c);
        String path = id.substring(c + 1), newPath = newId.substring(newId.indexOf(':') + 1);
        if ("minecraft".equals(ns)) out = out.replace("\"" + path + "\"", "\"" + newPath + "\"");
        return out;
    }

    private static boolean isTexture(String k) {
        return k.startsWith("assets/") && k.contains("/textures/") && k.endsWith(".png");
    }

    /** assets/ns/textures/items/x.png -> ns:items/x */
    private static String textureId(String path) {
        String rest = path.substring("assets/".length());
        String ns = rest.substring(0, rest.indexOf('/'));
        String p = rest.substring(ns.length() + "/textures/".length(), rest.length() - 4);
        return ns + ":" + p;
    }

    private static String hash(byte[] d) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            StringBuilder sb = new StringBuilder();
            for (byte b : md.digest(d)) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(java.util.Arrays.hashCode(d));
        }
    }
}
