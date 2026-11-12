package com.electrifiedded.luckylegacyaddon.convert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class AddonConverter {
    public static final String MARKER = "luckylegacy.marker";

    /** Reads every add-on in addons/lucky_block WITHOUT modifying or writing any file; results go to NativeAddons. */
    public static void run(File mcDir) {
        File lucky = new File(mcDir, "addons/lucky_block");
        lucky.mkdirs();
        log("loading add-ons natively from " + lucky);
        File[] src = lucky.listFiles();
        if (src == null) return;
        Arrays.sort(src);
        Set<String> seen = new HashSet<>();
        for (File f : src) {
            if (isConverted(f)) {
                log("ignoring old converted copy from a previous version: " + f.getName());
                continue;
            }
            try {
                convert(f, f, seen);
            } catch (Throwable t) {
                err("loading of " + f.getName() + " failed", t);
            }
        }
    }

    private static void convert(File f, File outDir, Set<String> seenHashes) throws Exception {
        String lower = f.getName().toLowerCase();
        if (!f.isDirectory() && !(lower.endsWith(".zip") || lower.endsWith(".jar") || lower.endsWith(".rar"))) {
            log("skip (unsupported format): " + f.getName());
            return;
        }
        convertVirtual(VirtualAddon.load(f), f, seenHashes, f.getName());
    }

    private static void convertVirtual(VirtualAddon v, File origin0, Set<String> seen, String origin) throws Exception {
        List<String> roots = v.findRoots();
        boolean any = false;
        for (String root : roots) {
            VirtualAddon sub = v.subRoot(root);
            String name = v.name + (roots.size() > 1 ? "_" + root.replace('/', '_') : "");
            if (!seen.add(hash(sub))) {
                log("duplicate add-on content skipped: " + origin + " (" + root + ")");
                continue;
            }
            Converted c = new Converted(name, sub);
            LegacyFixer.fix(c);
            TextureDedup.apply(safe(name), c.files);
            if (!NativeAddons.add(safe(name), origin0, c.files)) {
                log("add-on " + safe(name) + " already loaded, bundled copy in " + origin + " skipped");
                continue;
            }
            log("loaded " + origin + " as " + safe(name) + " (" + c.files.size() + " files, in memory)");
            any = true;
        }
        for (Map.Entry<String, byte[]> nz : v.nestedZips().entrySet()) {
            try {
                String nn = nz.getKey().substring(nz.getKey().lastIndexOf('/') + 1);
                nn = nn.substring(0, nn.length() - 4);
                convertVirtual(VirtualAddon.load(nn, nz.getValue()), origin0, seen, origin + "!" + nz.getKey());
            } catch (Throwable t) {
                log("nested archive " + nz.getKey() + " is not an add-on: " + t);
            }
        }
        if (!any && v.hasClasses()) log("code add-on (needs a real port, not a Lucky resource add-on): " + origin);
    }

    static String safe(String n) {
        return n.replaceAll("[^A-Za-z0-9._+\\-]", "_");
    }

    private static void write(Converted c, File out, String origin) throws IOException {
        try (ZipOutputStream zo = new ZipOutputStream(new FileOutputStream(out))) {
            zo.setLevel(6);
            put(zo, MARKER, ("converted from " + origin).getBytes(StandardCharsets.UTF_8));
            for (Map.Entry<String, byte[]> e : c.files.entrySet()) put(zo, e.getKey(), e.getValue());
        }
        log("converted " + origin + " -> " + out.getName() + " (" + c.files.size() + " files)");
    }

    private static void put(ZipOutputStream zo, String name, byte[] data) throws IOException {
        zo.putNextEntry(new ZipEntry(name));
        zo.write(data);
        zo.closeEntry();
    }

    private static boolean isConverted(File f) {
        try {
            if (f.isDirectory()) return new File(f, MARKER).exists();
            if (!f.getName().toLowerCase().endsWith(".zip")) return false;
            try (ZipFile z = new ZipFile(f)) {
                return z.getEntry(MARKER) != null;
            }
        } catch (Exception e) {
            return false;
        }
    }

    private static String hash(VirtualAddon v) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        for (Map.Entry<String, byte[]> e : v.files.entrySet()) {
            md.update(e.getKey().getBytes(StandardCharsets.UTF_8));
            md.update(e.getValue());
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static void deleteRec(File f) {
        File[] l = f.listFiles();
        if (l != null) for (File c : l) deleteRec(c);
        f.delete();
    }

    static void log(String s) {
        System.out.println("[LuckyLegacyAddon] " + s);
    }

    static void err(String s, Throwable t) {
        System.err.println("[LuckyLegacyAddon] " + s);
        t.printStackTrace();
    }
}
