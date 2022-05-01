/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.io;

import com.mastfrog.util.strings.Strings;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Arrays;

/**
 *
 * @author Tim Boudreau
 */
public final class KeyStringWriter implements KeyWriter<String> {

    private final StringBuilder sb;
    public static final byte INT_ARRAY_PREFIX = (byte) 'i';
    public static final byte BYTE_ARRAY_PREFIX = (byte) 'b';
    public static final byte LONG_ARRAY_PREFIX = (byte) 'l';

    public static final byte MAGIC_1 = (byte) 'k';
    public static final byte MAGIC_2 = (byte) '!';

    public KeyStringWriter(StringBuilder sb) {
        this(sb, MAGIC_1, MAGIC_2);
    }

    public KeyStringWriter(StringBuilder sb, byte magic1, byte magic2) {
        this.sb = sb;
        writeByte(magic1);
        writeByte(magic2);
    }

    public KeyStringWriter() {
        this(new StringBuilder(256));
    }

    @Override
    public KeyWriter finishRecord() {
        return this;
    }

    public int size() {
        return sb.length();
    }

    @Override
    public byte[] toByteArray() {
        return sb.toString().getBytes(UTF_8);
    }

    @Override
    public String get() {
        return sb.toString();
    }

    @Override
    public String toString() {
        return "StringWriter at " + sb.length() + "\n" + sb;
    }

    @Override
    public KeyWriter writeInt(int value) {
        int oldLength = sb.length();
        Strings.appendPaddedHex(value, sb);
        assert sb.length() == oldLength + 8 :
                "16 characters should always be inserted for an int"
                + " but " + (sb.length() - oldLength) + " were: "
                + " '" + sb.subSequence(oldLength, sb.length()) + "'";
        return this;
    }

    @Override
    public KeyWriter writeLong(long value) {
        int oldLength = sb.length();
        appendPaddedHex(value);
        assert sb.length() == oldLength + 16 :
                "16 characters should always be inserted for an int"
                + " but " + (sb.length() - oldLength) + " were: "
                + " '" + sb.subSequence(oldLength, sb.length()) + "'";
        return this;
    }

    private void appendPaddedHex(long val) {
        String s = Long.toHexString(val);
        if (s.length() < 16) {
            char[] chars = new char[16 - s.length()];
            Arrays.fill(chars, '0');
            sb.append(chars);
        }
        sb.append(s);
    }

    @Override
    public KeyWriter writeIntArray(int[] ints) {
        writeByte(INT_ARRAY_PREFIX);
        writeInt(ints.length);
        sb.append(Strings.toPaddedHex(ints, ""));
        return this;
    }

    @Override
    public KeyWriter writeLongArray(long[] ints) {
        writeByte(LONG_ARRAY_PREFIX);
        writeInt(ints.length);
        sb.append(Strings.toPaddedHex(ints, ""));
        return this;
    }

    @Override
    public KeyWriter writeByte(byte b) {
        int oldPos = sb.length();
        Strings.appendPaddedHex(b, sb);
        assert sb.length() == oldPos + 2 : "Two characters "
                + "should always be inserted for a byte";
        return this;
    }

    @Override
    public KeyWriter writeByteArray(byte[] bytes) {
        writeByte(BYTE_ARRAY_PREFIX);
        writeInt(bytes.length);
        sb.append(Strings.toPaddedHex(bytes, ""));
        return this;
    }
}
