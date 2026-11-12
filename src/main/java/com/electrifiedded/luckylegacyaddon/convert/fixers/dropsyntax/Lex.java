package com.electrifiedded.luckylegacyaddon.convert.fixers.dropsyntax;

import java.util.ArrayList;
import java.util.List;

public final class Lex {
    private Lex() {}

    public static final class Split {
        public final List<String> parts = new ArrayList<>();
        public int strayAt = -1;
    }

    private static boolean isOpen(char c) { return c == '(' || c == '[' || c == '{'; }
    private static boolean isClose(char c) { return c == ')' || c == ']' || c == '}'; }

    public static Split split(String line, char sep) {
        Split r = new Split();
        List<Integer> pts = new ArrayList<>();
        pts.add(-1);
        int tier = 0;
        boolean inQuotes = false;
        boolean broke = false;
        for (int a = 0; a < line.length(); a++) {
            char c = line.charAt(a);
            boolean canceled = a > 0 && line.charAt(a - 1) == '\\';
            if (canceled) continue;
            if (c == '"') inQuotes = !inQuotes;
            if (isOpen(c) && !inQuotes) tier++;
            if (isClose(c) && !inQuotes) tier--;
            if (tier < 0) {
                pts.add(a);
                r.strayAt = a;
                broke = true;
                break;
            }
            if (c == sep && tier == 0 && !inQuotes) pts.add(a);
        }
        if (!broke) pts.add(line.length());
        for (int i = 0; i < pts.size() - 1; i++) r.parts.add(line.substring(pts.get(i) + 1, pts.get(i + 1)));
        return r;
    }

    public static boolean oddQuotes(String s) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) == '"' && !(i > 0 && s.charAt(i - 1) == '\\')) n++;
        }
        return (n & 1) == 1;
    }

    public static String balance(String s, int[] changed) {
        StringBuilder out = new StringBuilder();
        List<Character> stack = new ArrayList<>();
        boolean inQ = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean canceled = i > 0 && s.charAt(i - 1) == '\\';
            if (!canceled) {
                if (c == '"') inQ = !inQ;
                else if (!inQ && isOpen(c)) stack.add(c);
                else if (!inQ && isClose(c)) {
                    if (stack.isEmpty()) {
                        if (changed != null) changed[0]++;
                        continue;
                    }
                    stack.remove(stack.size() - 1);
                }
            }
            out.append(c);
        }
        if (inQ) {
            out.append('"');
            if (changed != null) changed[0]++;
        }
        for (int i = stack.size() - 1; i >= 0; i--) {
            char o = stack.get(i);
            out.append(o == '(' ? ')' : o == '[' ? ']' : '}');
            if (changed != null) changed[0]++;
        }
        return out.toString();
    }

    public static String balanceStack(String s, int[] changed) {
        StringBuilder out = new StringBuilder();
        List<Character> stack = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean canceled = i > 0 && s.charAt(i - 1) == '\\';
            if (canceled) {
                out.append(c);
                continue;
            }
            char top = stack.isEmpty() ? 0 : stack.get(stack.size() - 1);
            boolean inQ = top == '"' || top == 'q' || top == 'e';
            if (c == '"' && top == 'e') {
                if (closesQuote(s, i + 1)) stack.remove(stack.size() - 1);
                out.append("\\\"");
                continue;
            }
            if (c == '"') {
                int qk = Math.max(stack.lastIndexOf('"'), stack.lastIndexOf('q'));
                if (!inQ && i == s.length() - 1 && qk < 0 && afterCloser(s, i)) {
                    if (changed != null) changed[0]++;
                    continue;
                }
                if (!inQ && qk >= 0 && afterCloser(s, i)) {
                    while (stack.size() - 1 > qk) {
                        out.append(closerOf(stack.remove(stack.size() - 1)));
                        if (changed != null) changed[0]++;
                    }
                    stack.remove(qk);
                    out.append(c);
                } else if (!inQ && prevNonBlank(s, i) == '=') {
                    if (stack.contains('"')) {
                        stack.add('e');
                        out.append("\\\"");
                        if (changed != null) changed[0]++;
                    } else {
                        stack.add('q');
                        out.append(c);
                    }
                } else if (!inQ) {
                    stack.add('"');
                    out.append(c);
                } else if (closesQuote(s, i + 1)) {
                    stack.remove(stack.size() - 1);
                    out.append(c);
                } else if (top == '"' && opensNestedQuote(s, i)) {
                    stack.add('"');
                    out.append(c);
                } else {
                    out.append("\\\"");
                    if (changed != null) changed[0]++;
                }
                continue;
            }
            if (top == 'q' || top == 'e' || !isOpenOrClose(c)) {
                out.append(c);
                continue;
            }
            if (isOpen(c)) {
                stack.add(c);
                out.append(c);
                continue;
            }
            char want = c == ')' ? '(' : c == ']' ? '[' : '{';
            int k = stack.lastIndexOf(want);
            if (k < 0) {
                if (changed != null) changed[0]++;
                continue;
            }
            while (stack.size() - 1 > k) {
                out.append(closerOf(stack.remove(stack.size() - 1)));
                if (changed != null) changed[0]++;
            }
            stack.remove(k);
            out.append(c);
        }
        while (!stack.isEmpty()) {
            out.append(closerOf(stack.remove(stack.size() - 1)));
            if (changed != null) changed[0]++;
        }
        return out.toString();
    }

    private static boolean isOpenOrClose(char c) { return isOpen(c) || isClose(c); }

    private static char prevNonBlank(String s, int p) {
        int q = p - 1;
        while (q >= 0 && Character.isWhitespace(s.charAt(q))) q--;
        return q >= 0 ? s.charAt(q) : 0;
    }

    private static char closerOf(char open) {
        return open == '(' ? ')' : open == '[' ? ']' : open == '{' ? '}' : '"';
    }

    private static boolean afterCloser(String s, int p) {
        int q = p - 1;
        while (q >= 0 && s.charAt(q) == ' ') q--;
        return q >= 0 && isClose(s.charAt(q));
    }

    private static boolean opensNestedQuote(String s, int p) {
        int q = p - 1;
        while (q >= 0 && s.charAt(q) == ' ') q--;
        if (q < 0) return false;
        char c = s.charAt(q);
        return c == '[' || c == '(' || c == '=' || c == ',';
    }

    private static boolean closesQuote(String s, int p) {
        int q = p;
        while (q < s.length() && Character.isWhitespace(s.charAt(q))) q++;
        if (q >= s.length()) return true;
        char c = s.charAt(q);
        if (c == ')' || c == ']' || c == '}' || c == ';' || c == '@' || c == '"') return true;
        if (c == ',') {
            int r = q + 1;
            while (r < s.length() && s.charAt(r) == ' ') r++;
            int st = r;
            while (r < s.length() && (Character.isLetterOrDigit(s.charAt(r)) || s.charAt(r) == '_')) r++;
            return r > st && r < s.length() && s.charAt(r) == '=';
        }
        return false;
    }

    public static int matching(String s, int open) {
        int tier = 0;
        boolean inQ = false;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean canceled = i > 0 && s.charAt(i - 1) == '\\';
            if (canceled) continue;
            if (c == '"') inQ = !inQ;
            else if (!inQ && isOpen(c)) tier++;
            else if (!inQ && isClose(c)) {
                tier--;
                if (tier == 0) return i;
            }
        }
        return -1;
    }

    public static List<Integer> closers(String s) {
        List<Integer> r = new ArrayList<>();
        boolean inQ = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean canceled = i > 0 && s.charAt(i - 1) == '\\';
            if (canceled) continue;
            if (c == '"') inQ = !inQ;
            else if (!inQ && c == ')') r.add(i);
        }
        return r;
    }

    public static String escapeInnerQuotes(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            boolean canceled = i > 0 && s.charAt(i - 1) == '\\';
            if (c == '"' && !canceled && i > 0 && s.charAt(i - 1) == '=') {
                out.append(c);
                i++;
                int valStart = out.length();
                boolean closed = false;
                boolean escaped = false;
                while (i < n) {
                    char d = s.charAt(i);
                    if (d == '"' && s.charAt(i - 1) == '=' && !(i > 1 && s.charAt(i - 2) == '\\')) {
                        int ins = -1;
                        for (int k = valStart; k < out.length(); k++) {
                            char e = out.charAt(k);
                            if (e == ')' || e == ';' || (e == ',' && isNameEq(out, k + 1))) {
                                ins = k;
                                break;
                            }
                        }
                        if (ins < 0) out.append('"'); else out.insert(ins, '"');
                        closed = true;
                        break;
                    }
                    if (d == '\\' && i + 1 < n) {
                        out.append(d).append(s.charAt(i + 1));
                        i += 2;
                        continue;
                    }
                    if (d == '"') {
                        if (isTerminator(s, i + 1)) {
                            out.append(d);
                            i++;
                            closed = true;
                            break;
                        }
                        out.append("\\\"");
                        escaped = true;
                        i++;
                        continue;
                    }
                    out.append(d);
                    i++;
                }
                if (i >= n && escaped && !closed) return s;
                continue;
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static boolean isNameEq(CharSequence s, int p) {
        int q = p;
        while (q < s.length() && s.charAt(q) == ' ') q++;
        int st = q;
        while (q < s.length() && (Character.isLetterOrDigit(s.charAt(q)) || s.charAt(q) == '_')) q++;
        return q > st && q < s.length() && s.charAt(q) == '=';
    }

    private static boolean isTerminator(String s, int p) {
        if (p >= s.length()) return true;
        char c = s.charAt(p);
        if (c == ';' || c == ')' || c == '@') return true;
        if (c == ',') {
            int q = p + 1;
            while (q < s.length() && s.charAt(q) == ' ') q++;
            if (q >= s.length()) return true;
            int st = q;
            while (q < s.length() && (Character.isLetterOrDigit(s.charAt(q)) || s.charAt(q) == '_')) q++;
            return q > st && q < s.length() && s.charAt(q) == '=';
        }
        return false;
    }
}
