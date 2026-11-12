package com.electrifiedded.luckylegacyaddon.convert;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

public final class TextUtil {
    private TextUtil() {}

    public static String decode(byte[] b) {
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) return new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE);
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) return new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE);
        int off = (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) ? 3 : 0;
        try {
            return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(b, off, b.length - off)).toString();
        } catch (CharacterCodingException e) {
            return new String(b, off, b.length - off, Charset.forName("windows-1252"));
        }
    }
}
