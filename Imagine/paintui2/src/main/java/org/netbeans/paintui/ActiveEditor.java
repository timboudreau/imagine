package org.netbeans.paintui;

import java.awt.EventQueue;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;

/**
 *
 * @author Tim Boudreau
 */
public final class ActiveEditor {

    private static Reference<PaintTopComponent> active;

    private static PaintTopComponent current() {
        return active == null ? null : active.get();
    }

    public static void setActiveEditor(PaintTopComponent comp) {
        PaintTopComponent prev = current();
        if (prev == comp) {
            return;
        }
        active = new WeakReference<>(comp);
        activeEditorChanged(prev, comp);
    }

    public static boolean closed(PaintTopComponent comp) {
        PaintTopComponent prev = current();
        if (prev == comp) {
            active = null;
            activeEditorChanged(comp, null);
            return true;
        }
        return false;
    }

    public static boolean isActive(PaintTopComponent comp) {
        return current() == comp && comp != null && comp.isDisplayable();
    }

    private static void activeEditorChanged(PaintTopComponent old, PaintTopComponent nue) {
        EventQueue.invokeLater(() -> {
            if (old != null) {
                old.resignActiveEditor();
            }
            if (nue != null) {
                nue.becomeActiveEditor();
            }
        });
    }
}
