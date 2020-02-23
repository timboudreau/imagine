package org.imagine.awt.util;

/**
 *
 * @author Tim Boudreau
 */
public final class IdPathBuilder {

    private final StringBuilder sb = new StringBuilder(40);

    private StringBuilder startElement() {
        if (sb.length() > 0) {
            sb.append('/');
        }
        return sb;
    }

    public IdPathBuilder add(String s) {
        startElement().append(s);
        return this;
    }

    public IdPathBuilder add(int val) {
        startElement().append(Integer.toString(val, 36));
        return this;
    }

    public IdPathBuilder add(long val) {
        startElement().append(Long.toString(val, 36));
        return this;
    }

    public IdPathBuilder add(byte val) {
        startElement().append(Integer.toString(val, 36));
        return this;
    }

    public IdPathBuilder add(float val) {
        startElement().append(Integer.toString(Float.floatToIntBits(val), 36));
        return this;
    }

    public IdPathBuilder add(double val) {
        startElement().append(Long.toString(Double.doubleToLongBits(val), 36));
        return this;
    }

    public IdPathBuilder add(int[] arr) {
        if (arr == null) {
            return this;
        }
        startElement();
        for (int i = 0; i < arr.length; i++) {
            sb.append(Integer.toString(arr[i], 36));
        }
        sb.append('.').append(Integer.toString(arr.length, 36));
        return this;
    }

    public IdPathBuilder add(long[] arr) {
        if (arr == null) {
            return this;
        }
        startElement();
        for (int i = 0; i < arr.length; i++) {
            sb.append(Long.toString(arr[i], 36));
        }
        sb.append('.').append(Integer.toString(arr.length, 36));
        return this;
    }

    @Override
    public String toString() {
        return sb.toString();
    }

}
