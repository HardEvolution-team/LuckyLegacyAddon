package com.electrifiedded.luckylegacyaddon.convert;

import java.io.File;
import java.util.*;

/**
 * In-memory view of every legacy add-on: the original files in addons/lucky_block stay untouched,
 * fixed contents live only here and are served to Lucky Block / Loot++ / the resource system on read.
 */
public final class NativeAddons {
    public static final class Addon {
        public final String name;
        public final File origin;
        /** Synthetic (non-existing) file Lucky's PluginLoader is keyed by. */
        public final File key;
        public final Map<String, byte[]> files;

        Addon(String name, File origin, Map<String, byte[]> files) {
            this.name = name;
            this.origin = origin;
            this.key = new File(origin.getPath() + "!" + name);
            this.files = Collections.unmodifiableMap(files);
        }

        public boolean has(String path) {
            if (files.containsKey(path)) return true;
            String dir = path.endsWith("/") ? path : path + "/";
            for (String k : files.keySet()) if (k.startsWith(dir)) return true;
            return false;
        }
    }

    private static final List<Addon> ALL = new ArrayList<>();
    private static final Map<String, Addon> BY_KEY = new HashMap<>();
    private static final Set<String> ORIGINS = new HashSet<>();

    private NativeAddons() {}

    static synchronized boolean add(String name, File origin, Map<String, byte[]> files) {
        for (Addon o : ALL) if (o.name.equalsIgnoreCase(name)) return false; // same add-on bundled inside another one
        Addon a = new Addon(name, origin, files);
        ALL.add(a);
        BY_KEY.put(a.key.getPath(), a);
        ORIGINS.add(norm(origin));
        return true;
    }

    private static String norm(File f) {
        return f.toPath().toAbsolutePath().normalize().toString().toLowerCase(java.util.Locale.ROOT);
    }

    public static synchronized List<Addon> all() { return Collections.unmodifiableList(new ArrayList<>(ALL)); }

    public static Addon byKey(File f) { return f == null ? null : BY_KEY.get(f.getPath()); }

    /** True for an original add-on file that is served virtually (Lucky must not load it directly). */
    public static boolean isOrigin(File f) { return f != null && ORIGINS.contains(norm(f)); }
}
