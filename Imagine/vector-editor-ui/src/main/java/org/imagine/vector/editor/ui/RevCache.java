package org.imagine.vector.editor.ui;

import java.util.function.Supplier;
import net.java.dev.imagine.api.vector.Versioned;
import org.imagine.vector.editor.ui.spi.ShapeInfo;

/**
 *
 * @author Tim Boudreau
 */
final class RevCache implements Supplier<ShapeInfo> {

    private final Supplier<Object> supp;
    private final Supplier<ShapeInfo> info;
    private int lastRev = -1;
    private ShapeInfo lastInfo;

    RevCache(Supplier<Object> supp, Supplier<ShapeInfo> info) {
        this.supp = supp;
        this.info = info;
    }

    @Override
    public ShapeInfo get() {
        int r = rev();
        if (lastInfo == null || r != lastRev) {
            lastInfo = info.get();
            lastRev = r;
        }
        return lastInfo;
    }

    private int rev() {
        Object o = supp.get();
        if (o instanceof Versioned) {
            return ((Versioned) o).rev() + System.identityHashCode(o);
        } else {
            return o.hashCode();
        }
    }
}
