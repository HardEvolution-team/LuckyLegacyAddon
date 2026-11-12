package com.electrifiedded.luckylegacyaddon.convert;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

public class Converted {
    public final String name;
    public final Map<String, byte[]> files = new TreeMap<>();

    public Converted(String name, VirtualAddon src) {
        this.name = name;
        this.files.putAll(src.files);
    }

    public String text(String path) {
        byte[] b = files.get(path);
        return b == null ? null : TextUtil.decode(b);
    }

    public void putText(String path, String text) {
        files.put(path, text.getBytes(StandardCharsets.UTF_8));
    }
}
