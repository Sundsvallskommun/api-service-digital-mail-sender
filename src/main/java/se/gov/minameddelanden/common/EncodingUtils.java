package se.gov.minameddelanden.common;

import static java.lang.Math.max;
import static java.lang.Math.min;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnmappableCharacterException;
import java.util.Arrays;

public final class EncodingUtils {

    public static final Charset UTF_8 = StandardCharsets.UTF_8;
    public static final Charset ISO_8859_1 = StandardCharsets.ISO_8859_1;

    private EncodingUtils() { }

    public static byte[] stringToBytes(final String string, final Charset encoding) throws EncodingException {
        try {
            var bb = encoding.newEncoder().encode(CharBuffer.wrap(string));

            return Arrays.copyOf(bb.array(), bb.remaining());
        } catch (CharacterCodingException e) {
            throw new EncodingException(e);
        }
    }

    public static String bytesToString(final byte[] bytes, final Charset encoding) throws EncodingException {
        try {
            return tryBytesToString(bytes, encoding);
        } catch (MalformedInputException e) {
            throw wrapCharacterCodingException(bytes, encoding, e, e.getInputLength());
        } catch (UnmappableCharacterException e) {
            throw wrapCharacterCodingException(bytes, encoding, e, e.getInputLength());
        } catch (CharacterCodingException e) {
            throw new EncodingException(e);
        }
    }

    private static EncodingException wrapCharacterCodingException(final byte[] bytes,
            final Charset encoding, final CharacterCodingException e, final int offset) {
        var message = "Illegal byte " + bytesToHexAroundOffset(bytes, offset, 3) + " at input offset " + offset + " for encoding " + encoding;
        if (!encoding.equals(ISO_8859_1)) {
            message += "\nFor encoding ISO-8859-1 the result would be:\n"+new String(bytes, ISO_8859_1);
        }
        return new EncodingException(message, e);
    }

    private static String tryBytesToString(final byte[] bytes, final Charset encoding) throws CharacterCodingException {
        var charBuffer = encoding.newDecoder().decode(ByteBuffer.wrap(bytes));

        return new String(charBuffer.array(), 0, charBuffer.length());
    }

    public static String bytesToHexAroundOffset(final byte[] bytes, final int offset, final int around) {
        var sb = new StringBuilder();
        for (int i = max(offset - around, 0); i < offset; ++i) {
            sb.append(byteToHex(bytes[i])).append(' ');
        }
        sb.append("->").append(byteToHex(bytes[offset])).append("<-");
        for (int i = offset+1; i < min(offset+1+ around, bytes.length); ++i) {
            sb.append(' ').append(byteToHex(bytes[i]));
        }
        return sb.toString();
    }

    public static String byteToHex(final byte aByte) {
        return String.format("%02X", aByte);
    }

	public static class EncodingException extends RuntimeException {

        private EncodingException(String message, CharacterCodingException e) {
            super(message,e);
        }

        private EncodingException(CharacterCodingException e) {
            super(e);
        }
    }
}
