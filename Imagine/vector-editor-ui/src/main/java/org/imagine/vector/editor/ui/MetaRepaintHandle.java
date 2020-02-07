/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Cursor;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.java.dev.imagine.spi.image.RepaintHandle;

/**
 *
 * @author Tim Boudreau
 */
public class MetaRepaintHandle implements RepaintHandle {

    private final List<Reference<RepaintHandle>> handles = new ArrayList<>();

    public void add(RepaintHandle h) {
        WeakReference<RepaintHandle> ref = new WeakReference<>(h);
        handles.add(ref);
    }

    private void iterate(Consumer<RepaintHandle> c) {
        for (Iterator<Reference<RepaintHandle>> it = handles.iterator(); it.hasNext();) {
            Reference<RepaintHandle> ref = it.next();
            RepaintHandle h = ref.get();
            if (h == null) {
                it.remove();
            } else {
                c.accept(h);
            }
        }
    }

    @Override
    public void repaintArea(int x, int y, int w, int h) {
        iterate(rh -> rh.repaintArea(x, y, w, h));
    }

    @Override
    public void setCursor(Cursor cursor) {
        iterate(rh -> rh.setCursor(cursor));
    }
}
