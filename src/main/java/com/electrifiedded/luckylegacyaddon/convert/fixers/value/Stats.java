package com.electrifiedded.luckylegacyaddon.convert.fixers.value;

import java.util.Map;
import java.util.TreeMap;

public final class Stats {
    final Map<String, Integer> counts = new TreeMap<>();

    void count(String key) {
        counts.merge(key, 1, Integer::sum);
    }

    boolean isEmpty() {
        return counts.isEmpty();
    }

    @Override
    public String toString() {
        return counts.toString();
    }
}
