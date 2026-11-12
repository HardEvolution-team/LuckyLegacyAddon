package com.electrifiedded.luckylegacyaddon.convert;

import com.github.junrar.Archive;
import com.github.junrar.rarfile.FileHeader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

final class RarSupport {
    private RarSupport() {}

    static int rarVersion(byte[] b) {
        if (b.length < 8 || b[0] != 'R' || b[1] != 'a' || b[2] != 'r' || b[3] != '!' || b[4] != 0x1A || b[5] != 0x07) return 0;
        return b[6] == 0 ? 4 : b[6] == 1 ? 5 : 0;
    }

    static void read(File f, Map<String, byte[]> out) throws IOException {
        try (InputStream in = java.nio.file.Files.newInputStream(f.toPath())) {
            read(in.readAllBytes(), out);
        }
    }

    static void read(byte[] data, Map<String, byte[]> out) throws IOException {
        int ver = rarVersion(data);
        if (ver == 5) throw new IOException("RAR5 archives are not supported (junrar reads RAR 1.5-4.x only); re-pack the add-on as .zip");
        if (ver == 0) throw new IOException("not a RAR archive");
        try (Archive a = new Archive(new ByteArrayInputStream(data))) {
            FileHeader fh;
            while ((fh = a.nextFileHeader()) != null) {
                if (fh.isDirectory()) continue;
                String name = fh.getFileName();
                if (name == null || name.isEmpty()) continue;
                try (InputStream is = a.getInputStream(fh)) {
                    out.put(VirtualAddon.clean(name), VirtualAddon.readAll(is));
                }
            }
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("RAR read failed: " + e, e);
        }
    }
}
