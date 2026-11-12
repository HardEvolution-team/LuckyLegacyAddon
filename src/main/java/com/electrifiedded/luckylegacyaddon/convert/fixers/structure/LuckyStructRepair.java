package com.electrifiedded.luckylegacyaddon.convert.fixers.structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class LuckyStructRepair {
    private LuckyStructRepair() {}

    private static final Pattern BARE_PAIR = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*=[^=()\\[\\]{}\"]*$");

    public static final class Out {
        public final String text;
        public final int converted;
        public final List<String> examples;

        Out(String text, int converted, List<String> examples) {
            this.text = text;
            this.converted = converted;
            this.examples = examples;
        }
    }

    public static Out repair(String text) {
        String[] lines = text.split("\n", -1);
        StringBuilder out = new StringBuilder(text.length());
        List<String> examples = new ArrayList<>();
        String section = "";
        int converted = 0;
        StringBuilder pending = null;
        int pendingFrom = 0;
        for (int i = 0; i < lines.length; i++) {
            String raw = lines[i];
            String t = raw.trim();
            boolean last = i == lines.length - 1;
            if (t.isEmpty() || t.startsWith("/")) {
                if (pending == null) {
                    out.append(raw);
                    if (!last) out.append('\n');
                }
                continue;
            }
            if (pending == null && t.startsWith(">")) {
                section = t.toLowerCase(Locale.ENGLISH);
                out.append(raw);
                if (!last) out.append('\n');
                continue;
            }
            if (t.endsWith("\\")) {
                if (pending == null) {
                    pending = new StringBuilder();
                    pendingFrom = i;
                }
                pending.append(t, 0, t.length() - 1).append(' ');
                continue;
            }
            String logical;
            int from = i;
            if (pending != null) {
                pending.append(t);
                logical = pending.toString().trim();
                from = pendingFrom;
                pending = null;
            } else {
                logical = t;
            }
            String fixed = section.equals(">blocks") ? fixBlockRow(logical) : null;
            if (fixed != null) {
                if (examples.size() < 3) examples.add(logical + "  ->  " + fixed);
                converted++;
                out.append(fixed);
            } else {
                for (int k = from; k <= i; k++) {
                    out.append(lines[k]);
                    if (k < lines.length - 1) out.append('\n');
                }
                continue;
            }
            if (!last) out.append('\n');
        }
        if (pending != null) {
            for (int k = pendingFrom; k < lines.length; k++) {
                out.append(lines[k]);
                if (k < lines.length - 1) out.append('\n');
            }
        }
        return new Out(out.toString(), converted, examples);
    }

    private static final Pattern COORD = Pattern.compile("^-?\\d+(\\.\\d+)?$");

    static String fixBlockRow(String row) {
        int[][] span = columnSpans(row);
        if (span.length < 6) return null;
        for (int c = 0; c < 3; c++) {
            if (!COORD.matcher(row.substring(span[c][0], span[c][1]).trim()).matches()) return null;
        }
        int s = span[5][0], e = span[5][1];
        String value = row.substring(s, e);
        String v = value.trim();
        String nv = normalizeTileEntity(v);
        if (nv.equals(v)) return null;
        int lead = value.indexOf(v);
        return row.substring(0, s) + value.substring(0, lead) + nv + value.substring(lead + v.length()) + row.substring(e);
    }

    static String normalizeTileEntity(String v) {
        String x = v;
        if (x.length() > 7 && x.regionMatches(true, 0, "NBTTag=", 0, 7)) {
            x = x.substring(7).trim();
        }
        if (x.startsWith("(") && x.endsWith(")")) return x;
        if (BARE_PAIR.matcher(x).matches()) return "(" + x + ")";
        return x.equals(v) ? v : x;
    }

    static int[][] columnSpans(String row) {
        List<int[]> spans = new ArrayList<>();
        int depth = 0, start = 0;
        boolean inQuote = false;
        for (int i = 0; i < row.length(); i++) {
            char c = row.charAt(i);
            if (i > 0 && row.charAt(i - 1) == '\\') continue;
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote) {
                if (c == '(' || c == '[' || c == '{') depth++;
                else if (c == ')' || c == ']' || c == '}') depth = Math.max(0, depth - 1);
                else if (c == ',' && depth == 0) {
                    spans.add(new int[] {start, i});
                    start = i + 1;
                }
            }
        }
        spans.add(new int[] {start, row.length()});
        return spans.toArray(new int[0][]);
    }
}
