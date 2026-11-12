package com.electrifiedded.luckylegacyaddon.loot;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class LppConfig {
    public static final class Entry {
        public final String addon;
        public final String file;
        public final int line;
        public final String text;

        Entry(String addon, String file, int line, String text) {
            this.addon = addon;
            this.file = file;
            this.line = line;
            this.text = text;
        }

        public String where() {
            return addon + " " + file + ".txt:" + line;
        }

        public String[] parts() {
            return text.split("_____", -1);
        }
    }

    public static final class Addon {
        public final File file;
        public final String name;
        /** In-memory (natively loaded, fixed) files; null for a plain on-disk add-on. */
        public final Map<String, byte[]> files;

        Addon(File f) {
            this.file = f;
            this.name = f.getName();
            this.files = null;
        }

        Addon(com.electrifiedded.luckylegacyaddon.convert.NativeAddons.Addon a) {
            this.file = a.origin;
            this.name = a.name;
            this.files = a.files;
        }
    }

    private static final Map<String, List<Entry>> FILES = new HashMap<>();
    private static final List<Addon> ADDONS = new ArrayList<>();

    private LppConfig() {}

    public static List<Addon> addons() {
        return ADDONS;
    }

    public static List<Entry> get(String key) {
        List<Entry> l = FILES.get(key);
        return l == null ? Collections.<Entry>emptyList() : l;
    }

    public static void load(File mcDir) {
        List<com.electrifiedded.luckylegacyaddon.convert.NativeAddons.Addon> nat =
                new ArrayList<>(com.electrifiedded.luckylegacyaddon.convert.NativeAddons.all());
        nat.sort((a, b) -> {
            boolean ta = a.name.startsWith("_"), tb = b.name.startsWith("_");
            if (ta != tb) return ta ? 1 : -1;
            return a.name.compareToIgnoreCase(b.name);
        });
        for (com.electrifiedded.luckylegacyaddon.convert.NativeAddons.Addon a : nat) {
            try {
                boolean any = false;
                for (Map.Entry<String, byte[]> e : a.files.entrySet()) {
                    if (!isConfigPath(e.getKey())) continue;
                    add(a.name, e.getKey(), new java.io.ByteArrayInputStream(e.getValue()));
                    any = true;
                }
                if (any) ADDONS.add(new Addon(a));
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Could not read add-on {}: {}", a.name, t.toString());
            }
        }
        LootPlusPlus.LOG.info("[Loot++] Found Loot++ configuration in {} add-on(s)", ADDONS.size());
    }

    /** Old on-disk scan, kept for reference (add-ons are now served by NativeAddons). */
    static void loadFromDisk(File mcDir) {
        File dir = new File(mcDir, "addons/lucky_block");
        File[] files = dir.listFiles();
        if (files == null) {
            LootPlusPlus.LOG.info("[Loot++] No add-on folder at {}", dir);
            return;
        }
        Arrays.sort(files, (a, b) -> {
            boolean ta = a.getName().startsWith("_"), tb = b.getName().startsWith("_");
            if (ta != tb) return ta ? 1 : -1;
            return a.getName().compareToIgnoreCase(b.getName());
        });
        for (File f : files) {
            try {
                if (f.isDirectory()) {
                    loadDir(f);
                } else if (f.getName().endsWith(".zip") || f.getName().endsWith(".jar")) {
                    loadZip(f);
                }
            } catch (Throwable t) {
                LootPlusPlus.LOG.warn("[Loot++] Could not read add-on {}: {}", f.getName(), t.toString());
            }
        }
        LootPlusPlus.LOG.info("[Loot++] Found Loot++ configuration in {} add-on(s)", ADDONS.size());
    }

    private static boolean isConfigPath(String name) {
        return name.startsWith("config/") && name.endsWith(".txt") && name.indexOf('/', 7) > 0;
    }

    private static void loadZip(File f) throws Exception {
        try (ZipFile zf = new ZipFile(f)) {
            boolean any = false;
            List<String> names = new ArrayList<>();
            Enumeration<? extends ZipEntry> en = zf.entries();
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!e.isDirectory() && isConfigPath(e.getName())) names.add(e.getName());
            }
            Collections.sort(names);
            for (String n : names) {
                try (InputStream in = zf.getInputStream(zf.getEntry(n))) {
                    add(f.getName(), n, in);
                    any = true;
                }
            }
            if (any) ADDONS.add(new Addon(f));
        }
    }

    private static void loadDir(File d) throws Exception {
        File cfg = new File(d, "config");
        if (!cfg.isDirectory()) return;
        boolean any = false;
        File[] cats = cfg.listFiles();
        if (cats == null) return;
        Arrays.sort(cats);
        for (File cat : cats) {
            File[] fs = cat.listFiles();
            if (!cat.isDirectory() || fs == null) continue;
            Arrays.sort(fs);
            for (File fl : fs) {
                String n = "config/" + cat.getName() + "/" + fl.getName();
                if (fl.isFile() && isConfigPath(n)) {
                    try (InputStream in = new java.io.FileInputStream(fl)) {
                        add(d.getName(), n, in);
                        any = true;
                    }
                }
            }
        }
        if (any) ADDONS.add(new Addon(d));
    }

    private static void add(String addon, String path, InputStream in) throws Exception {
        String key = path.substring(7, path.length() - 4);
        List<Entry> list = FILES.computeIfAbsent(key, k -> new ArrayList<>());
        BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        int no = 0;
        while ((line = br.readLine()) != null) {
            no++;
            if (no == 1 && line.startsWith("﻿")) line = line.substring(1);
            String t = line.endsWith("\r") ? line.substring(0, line.length() - 1) : line;
            if (t.trim().isEmpty() || t.charAt(0) == '#') continue;
            list.add(new Entry(addon, key, no, t));
        }
    }
}
