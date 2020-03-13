/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.io;

import com.mastfrog.util.strings.Strings;
import java.io.IOException;
import java.util.Arrays;
import static org.imagine.io.KeyStringWriter.BYTE_ARRAY_PREFIX;
import static org.imagine.io.KeyStringWriter.INT_ARRAY_PREFIX;
import static org.imagine.io.KeyStringWriter.LONG_ARRAY_PREFIX;
import static org.imagine.io.KeyStringWriter.MAGIC_1;
import static org.imagine.io.KeyStringWriter.MAGIC_2;

/**
 *
 * @author Tim Boudreau
 */
public final class KeyStringReader implements KeyReader {

    private int cursor;
    private final CharSequence seq;
    private final byte m1, m2;

    public KeyStringReader(CharSequence seq, byte m1, byte m2) throws IOException {
        this(0, seq, m1, m2);
    }

    public KeyStringReader(CharSequence seq) throws IOException {
        this(0, seq);
    }

    public KeyStringReader(int cursor, CharSequence seq) throws IOException {
        this(0, seq, MAGIC_1, MAGIC_2);
    }

    public KeyStringReader(int cursor, CharSequence seq, byte m1, byte m2) throws IOException {
        assert seq != null : "Null char seq";
        assert cursor >= 0 : "negative position";
        this.cursor = cursor;
        this.seq = seq;
        this.m1 = m1;
        this.m2 = m2;
        if (cursor >= seq.length()) {
            throw new IOException("Cursor at or past end of string "
                    + " @ " + cursor + " in " + seq.length());
        }
    }

    public void readMagic() throws IOException {
        byte b1 = readByte();
        byte b2 = readByte();
        if (b1 != m1 || b2 != m2) {
            throw new IOException("Magic numbers do not match - expected "
                    + Strings.toPaddedHex(new byte[]{m1, m2})
                    + " but got "
                    + Strings.toPaddedHex(new byte[]{b1, b2})
            );
        }
    }

    public int cursor() {
        return cursor;
    }

    @Override
    public String toString() {
        char[] indent = new char[cursor];
        Arrays.fill(indent, ' ');
        String s = String.valueOf("String reader at " + cursor
                + " of " + seq.length() + '\n' + seq + '\n' + indent)
                + Character.toString('^');
        return s;
    }

    private String chars(int len) {
        CharSequence result = seq.subSequence(cursor, cursor + len);
        cursor += len;
        return result.toString().toUpperCase();
    }

    @Override
    public int readInt() throws IOException {
        // use Long.parseLong to avoid sign problems
        int result = (int) Long.parseLong(chars(Integer.BYTES * 2), 16);
        return result;
    }

    @Override
    public byte readByte() throws IOException {
        // use Long.parseLong to avoid sign problems
        return (byte) Short.parseShort(chars(2), 16);
    }

    @Override
    public long readLong() throws IOException {
        return parseLongHex(chars(Long.BYTES * 2));
    }

    public static long parseLongHex(String s) {
        // Irritatingly, Long.parseLong() cannot parse extreme
        // values produced by Long.toString() because of the sign,
        // so we need to parse our own
        int max = s.length() - 1;
        long result = 0;
        for (int i = max; i >= 0; i -= 2) {
            char lo = s.charAt(i);
            char hi = s.charAt(i - 1);
            long val = toByte(hi, lo);
            int shift = 8 * ((max - i) / 2);
            result |= val << shift;
        }
        return result;
    }

    public static long toByte(char hi, char lo) {
        long high = toNibble(hi);
        long low = toNibble(lo);
        return (high << 4) | low;
    }

    public static int toNibble(char c) {
        switch (c) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                return (int) (c - (int) '0');
            case 'A':
            case 'a':
                return 10;
            case 'B':
            case 'b':
                return 11;
            case 'C':
            case 'c':
                return 12;
            case 'D':
            case 'd':
                return 13;
            case 'E':
            case 'e':
                return 14;
            case 'F':
            case 'f':
                return 15;
            default:
                throw new NumberFormatException("Not a hex char: " + c);
        }
    }

    @Override
    public int[] readIntArray() throws IOException {
        int marker = readByte();
        if (marker != INT_ARRAY_PREFIX) {
            throw new IOException("Expected byte array marker "
                    + Integer.toHexString(INT_ARRAY_PREFIX & 0xFF)
                    + " but got " + Integer.toHexString(marker & 0xFF));
        }
        int size = readInt();
        if (size == 0) {
            return new int[0];
        } else if (size < 0) {
            throw new IOException("Invalid array length " + size);
        }
        int[] result = new int[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = readInt();
        }
        return result;
    }

    @Override
    public long[] readLongArray() throws IOException {
        int marker = readByte();
        if (marker != LONG_ARRAY_PREFIX) {
            throw new IOException("Expected byte array marker "
                    + Integer.toHexString(LONG_ARRAY_PREFIX & 0xFF)
                    + " but got " + Integer.toHexString(marker & 0xFF));
        }
        int size = readInt();
        if (size == 0) {
            return new long[0];
        } else if (size < 0) {
            throw new IOException("Invalid array length " + size);
        }
        long[] result = new long[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = readLong();
        }
        return result;
    }

    @Override
    public byte[] readByteArray() throws IOException {
        int marker = readByte();
        if (marker != BYTE_ARRAY_PREFIX) {
            throw new IOException("Expected byte array marker "
                    + Integer.toHexString(BYTE_ARRAY_PREFIX & 0xFF)
                    + " but got " + Integer.toHexString(marker & 0xFF));
        }
        int size = readInt();
        if (size == 0) {
            return new byte[0];
        } else if (size < 0) {
            throw new IOException("Invalid array length " + size);
        }
        byte[] result = new byte[size];
        for (int i = 0; i < result.length; i++) {
            result[i] = readByte();
        }
        return result;
    }
}
