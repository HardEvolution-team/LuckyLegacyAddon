package com.electrifiedded.luckylegacyaddon.convert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class VirtualAddon {
    public final String name;
    public final Map<String, byte[]> files = new TreeMap<>();

    public VirtualAddon(String name) {
        this.name = name;
    }

    public static VirtualAddon load(File src) throws IOException {
        String n = src.getName();
        int dot = n.lastIndexOf('.');
        VirtualAddon v = new VirtualAddon(dot > 0 && !src.isDirectory() ? n.substring(0, dot) : n);
        if (src.isDirectory()) {
            readDir(src, "", v.files);
        } else if (n.toLowerCase().endsWith(".rar")) {
            RarSupport.read(src, v.files);
        } else {
            try (ZipFile zf = openZip(src)) {
                Enumeration<? extends ZipEntry> en = zf.entries();
                while (en.hasMoreElements()) {
                    ZipEntry e = en.nextElement();
                    if (e.isDirectory()) continue;
                    try (InputStream in = zf.getInputStream(e)) {
                        v.files.put(clean(e.getName()), readAll(in));
                    }
                }
            }
        }
        return v;
    }

    public static VirtualAddon load(String name, byte[] zipBytes) throws IOException {
        VirtualAddon v = new VirtualAddon(name);
        if (RarSupport.rarVersion(zipBytes) != 0) {
            RarSupport.read(zipBytes, v.files);
            return v;
        }
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(zipBytes), StandardCharsets.UTF_8)) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                if (e.isDirectory()) continue;
                v.files.put(clean(e.getName()), readAll(zin));
            }
        }
        return v;
    }

    private static ZipFile openZip(File f) throws IOException {
        try {
            return new ZipFile(f, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return new ZipFile(f, Charset.forName("GBK"));
        }
    }

    private static void readDir(File dir, String prefix, Map<String, byte[]> out) throws IOException {
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) readDir(f, prefix + f.getName() + "/", out);
            else out.put(prefix + f.getName(), Files.readAllBytes(f.toPath()));
        }
    }

    static String clean(String p) {
        p = p.replace('\\', '/');
        while (p.startsWith("/")) p = p.substring(1);
        return p;
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int r;
        while ((r = in.read(buf)) > 0) bo.write(buf, 0, r);
        return bo.toByteArray();
    }

    public List<String> findRoots() {
        List<String> roots = new ArrayList<>();
        for (String p : files.keySet()) {
            String base = p.substring(p.lastIndexOf('/') + 1);
            if (isRootMarker(base)) {
                String dir = p.contains("/") ? p.substring(0, p.lastIndexOf('/') + 1) : "";
                if (dir.contains("__MACOSX")) continue;
                if (!roots.contains(dir)) roots.add(dir);
            }
        }
        List<String> res = new ArrayList<>();
        for (String r : roots) {
            boolean nested = false;
            for (String o : roots) if (!o.equals(r) && r.startsWith(o)) nested = true;
            if (!nested) res.add(r);
        }
        return res;
    }

    static boolean isRootMarker(String base) {
        return base.equals("plugin_init.txt") || base.equals("drops.txt") || base.equals("properties.txt");
    }

    public VirtualAddon subRoot(String root) {
        VirtualAddon v = new VirtualAddon(name);
        for (Map.Entry<String, byte[]> e : files.entrySet()) {
            String k = e.getKey();
            if (k.startsWith(root) && !k.contains("__MACOSX/") && !k.endsWith(".DS_Store")) v.files.put(k.substring(root.length()), e.getValue());
        }
        return v;
    }

    public Map<String, byte[]> nestedZips() {
        Map<String, byte[]> r = new LinkedHashMap<>();
        for (Map.Entry<String, byte[]> e : files.entrySet()) if (e.getKey().toLowerCase().endsWith(".zip") || e.getKey().toLowerCase().endsWith(".rar")) r.put(e.getKey(), e.getValue());
        return r;
    }

    public boolean hasClasses() {
        for (String p : files.keySet()) if (p.endsWith(".class")) return true;
        return false;
    }
}
