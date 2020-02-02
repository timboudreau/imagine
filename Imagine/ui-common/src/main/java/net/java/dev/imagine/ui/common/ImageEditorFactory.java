/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.common;

import java.awt.Dimension;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 *
 * @author Tim Boudreau
 */
public abstract class ImageEditorFactory {

    private final String name;

    public ImageEditorFactory(String name) {
        this.name = name;
    }

    public final String name() {
        return name;
    }

    @Override
    public String toString() {
        return name();
    }

    public boolean supportsBackgroundStyles() {
        return true;
    }

    public final boolean equals(Object o) {
        return o != null && (o == this || o.getClass() == getClass());
    }

    public final int hashCode() {
        return getClass().hashCode();
    }

    public abstract void openNew(Dimension dim, BackgroundStyle bg);

    public abstract void openExisting(File file);

    public abstract boolean canOpen(File file);

    public void openMany(File[] files, Consumer<Set<File>> unopened) {
        Set<File> result = new HashSet<>();
        for (File f : files) {
            if (canOpen(f)) {
                openExisting(f);
            } else {
                result.add(f);
            }
        }
        if (!result.isEmpty()) {
            unopened.accept(result);
        }
    }
}
