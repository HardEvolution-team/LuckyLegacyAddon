package com.electrifiedded.luckylegacyaddon.convert.fixers.structure;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

public final class Nbt {
    private Nbt() {}

    public static final int BYTE = 1, SHORT = 2, INT = 3, LONG = 4, FLOAT = 5, DOUBLE = 6, BYTE_ARRAY = 7, STRING = 8, LIST = 9, COMPOUND = 10,
            INT_ARRAY = 11, LONG_ARRAY = 12;

    public static final class Tag {
        public final int type;
        public Object value;

        public Tag(int type, Object value) {
            this.type = type;
            this.value = value;
        }

        public Number num() {
            return value instanceof Number ? (Number) value : null;
        }
    }

    public static final class ListVal {
        public int elemType;
        public final List<Object> items = new ArrayList<>();

        public ListVal(int elemType) {
            this.elemType = elemType;
        }
    }

    public static final class Result {
        public Map<String, Tag> root;
        public boolean truncated;
        public boolean wasGzip;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Tag> compound(Object o) {
        return (Map<String, Tag>) o;
    }

    public static Result read(byte[] data) {
        try {
            byte[] raw;
            Result r = new Result();
            if (data.length >= 2 && (data[0] & 255) == 0x1f && (data[1] & 255) == 0x8b) {
                raw = inflate(new GZIPInputStream(new ByteArrayInputStream(data)), r);
                r.wasGzip = true;
            } else if (data.length >= 2 && (data[0] & 0x0f) == 8 && (((data[0] & 255) << 8) | (data[1] & 255)) % 31 == 0) {
                raw = inflate(new InflaterInputStream(new ByteArrayInputStream(data)), r);
            } else {
                raw = data;
            }
            if (raw.length < 3 || raw[0] != COMPOUND) return null;
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(raw));
            in.readByte();
            in.readUTF();
            r.root = readCompound(in, r);
            return r;
        } catch (Throwable t) {
            return null;
        }
    }

    private static byte[] inflate(InputStream in, Result r) {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        try {
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
        } catch (IOException e) {
            r.truncated = true;
        }
        return bo.toByteArray();
    }

    private static Map<String, Tag> readCompound(DataInputStream in, Result r) {
        Map<String, Tag> m = new LinkedHashMap<>();
        try {
            while (true) {
                int t = in.readByte();
                if (t == 0) break;
                String name = in.readUTF();
                m.put(name, new Tag(t, readPayload(in, t, r, name, m)));
            }
        } catch (IOException e) {
            r.truncated = true;
        }
        return m;
    }

    private static Object readPayload(DataInputStream in, int t, Result r, String name, Map<String, Tag> parent) throws IOException {
        switch (t) {
            case BYTE: return in.readByte();
            case SHORT: return in.readShort();
            case INT: return in.readInt();
            case LONG: return in.readLong();
            case FLOAT: return in.readFloat();
            case DOUBLE: return in.readDouble();
            case BYTE_ARRAY: {
                int n = in.readInt();
                if (n < 0) throw new IOException("neg");
                int avail = in.available();
                byte[] b = new byte[n];
                if (avail < n) {
                    in.readFully(b, 0, avail);
                    if (name != null && parent != null) parent.put(name, new Tag(BYTE_ARRAY, b));
                    r.truncated = true;
                    throw new EOFException();
                }
                in.readFully(b);
                return b;
            }
            case STRING: return in.readUTF();
            case LIST: {
                int et = in.readByte();
                int n = in.readInt();
                ListVal l = new ListVal(et);
                if (n > 0 && et == 0) throw new IOException("bad list");
                for (int i = 0; i < n; i++) l.items.add(readPayload(in, et, r, null, null));
                return l;
            }
            case COMPOUND: return readCompound(in, r);
            case INT_ARRAY: {
                int n = in.readInt();
                int[] a = new int[n];
                for (int i = 0; i < n; i++) a[i] = in.readInt();
                return a;
            }
            case LONG_ARRAY: {
                int n = in.readInt();
                long[] a = new long[n];
                for (int i = 0; i < n; i++) a[i] = in.readLong();
                return a;
            }
            default: throw new IOException("bad tag " + t);
        }
    }

    public static byte[] writeGzip(Map<String, Tag> root, String rootName) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(new GZIPOutputStream(bo))) {
            out.writeByte(COMPOUND);
            out.writeUTF(rootName);
            writeCompound(out, root);
        }
        return bo.toByteArray();
    }

    private static void writeCompound(DataOutputStream out, Map<String, Tag> m) throws IOException {
        for (Map.Entry<String, Tag> e : m.entrySet()) {
            out.writeByte(e.getValue().type);
            out.writeUTF(e.getKey());
            writePayload(out, e.getValue().type, e.getValue().value);
        }
        out.writeByte(0);
    }

    private static void writePayload(DataOutputStream out, int t, Object v) throws IOException {
        switch (t) {
            case BYTE: out.writeByte((Byte) v); break;
            case SHORT: out.writeShort((Short) v); break;
            case INT: out.writeInt((Integer) v); break;
            case LONG: out.writeLong((Long) v); break;
            case FLOAT: out.writeFloat((Float) v); break;
            case DOUBLE: out.writeDouble((Double) v); break;
            case BYTE_ARRAY: out.writeInt(((byte[]) v).length); out.write((byte[]) v); break;
            case STRING: out.writeUTF((String) v); break;
            case LIST: {
                ListVal l = (ListVal) v;
                out.writeByte(l.items.isEmpty() && l.elemType == 0 ? 0 : l.elemType);
                out.writeInt(l.items.size());
                for (Object o : l.items) writePayload(out, l.elemType, o);
                break;
            }
            case COMPOUND: writeCompound(out, compound(v)); break;
            case INT_ARRAY: {
                int[] a = (int[]) v;
                out.writeInt(a.length);
                for (int x : a) out.writeInt(x);
                break;
            }
            case LONG_ARRAY: {
                long[] a = (long[]) v;
                out.writeInt(a.length);
                for (long x : a) out.writeLong(x);
                break;
            }
            default: throw new IOException("bad tag " + t);
        }
    }
}
