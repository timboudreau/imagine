package org.imagine.markdown.uiapi;

import java.util.function.IntFunction;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
abstract class ListItemCounter {

    protected int counter;

    public abstract String currentItemText();

    public <T> T enterItem(Supplier<T> run) {
        counter++;
        return run.get();
    }

    public abstract <T> T enterList(Supplier<T> supp);

    public static ListItemCounter create() {
        return new Outer();
    }

    static class Outer extends ListItemCounter {

        private Delegate delegate;

        @Override
        public String currentItemText() {
            if (delegate == null) {
                throw new IllegalStateException("Not in a list");
            }
            return delegate.currentItemText();
        }

        @Override
        public <T> T enterList(Supplier<T> supp) {
            Delegate old = delegate;
            int depth = old == null ? 0 : old.depth() + 1;
            try {
                delegate = new Delegate(depth);
                return supp.get();
            } finally {
                delegate = old;
            }
        }
    }

    static class Delegate extends ListItemCounter {

        private final int depth;

        public Delegate(int depth) {
            this.depth = depth;
        }

        int depth() {
            return depth;
        }

        @Override
        public String currentItemText() {
            return counterForDepth(depth).apply(counter);
        }

        @Override
        public <T> T enterList(Supplier<T> supp) {
            throw new UnsupportedOperationException("Should not be called.");
        }

    }

    static IntFunction<String> counterForDepth(int depth) {
        switch (depth % 3) {
            case 0:
                return ListItemCounter::numerals;
            case 1:
                return ListItemCounter::upperCaseLetters;
            case 2:
                return ListItemCounter::lowerCaseLetters;
            default:
                throw new AssertionError(depth % 3);
        }
    }

    static String numerals(int val) {
        return Integer.toString(val) + '.';
    }

    static String upperCaseLetters(int val) {
        // XXX could repeat, AA, BB, CC if > 26
        char c = (char) (('A' - 1) + (val - 1));
        return Character.toString(c) + '.';
    }

    static String lowerCaseLetters(int val) {
        char c = (char) (('a' - 1) + (val - 1));
        return Character.toString(c) + ".";
    }
}
