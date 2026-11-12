package com.electrifiedded.luckylegacyaddon.convert.fixers.value;

import java.util.ArrayList;
import java.util.List;

final class Scan {
    private Scan() {}

    static boolean canceled(String s, int i) {
        return i > 0 && s.charAt(i - 1) == '\\';
    }

    static List<String> split(String s, char sep) {
        List<String> out = new ArrayList<>();
        int depth = 0, start = 0;
        boolean inQ = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (canceled(s, i)) continue;
            if (c == '"') inQ = !inQ;
            if (!inQ) {
                if (c == '(' || c == '[' || c == '{') depth++;
                else if (c == ')' || c == ']' || c == '}') depth--;
                if (c == sep && depth == 0) {
                    out.add(s.substring(start, i));
                    start = i + 1;
                }
            }
        }
        out.add(s.substring(start));
        return out;
    }

    static int firstTopLevel(String s, char c0) {
        int depth = 0;
        boolean inQ = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (canceled(s, i)) continue;
            if (c == '"') inQ = !inQ;
            if (!inQ) {
                if (c == c0 && depth == 0) return i;
                if (c == '(' || c == '[' || c == '{') depth++;
                else if (c == ')' || c == ']' || c == '}') depth--;
            }
        }
        return -1;
    }

    static boolean wrapped(String s, char open, char close) {
        if (s.length() < 2 || s.charAt(0) != open || s.charAt(s.length() - 1) != close) return false;
        int depth = 0;
        boolean inQ = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (canceled(s, i)) continue;
            if (c == '"') inQ = !inQ;
            if (inQ) continue;
            if (c == '(' || c == '[' || c == '{') depth++;
            else if (c == ')' || c == ']' || c == '}') {
                depth--;
                if (depth == 0 && i != s.length() - 1) return false;
                if (depth < 0) return false;
            }
        }
        return depth == 0;
    }

    static boolean isQuoted(String s) {
        return s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"';
    }

    static String unquote(String s) {
        s = s.trim();
        return isQuoted(s) ? s.substring(1, s.length() - 1) : s;
    }

    static String unescape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                switch (n) {
                    case '"': sb.append('"'); i++; continue;
                    case '\\': sb.append('\\'); i++; continue;
                    case 'n': sb.append('\n'); i++; continue;
                    case 't': sb.append('\t'); i++; continue;
                    case 'r': sb.append('\r'); i++; continue;
                    case '\'': sb.append('\''); i++; continue;
                    default: break;
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                case '\r': sb.append("\\r"); break;
                default: sb.append(c);
            }
        }
        return sb.toString();
    }

    static String join(List<String> l, String sep) {
        return String.join(sep, l);
    }
}
