/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.palette;

import java.awt.BasicStroke;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import org.imagine.awt.key.PaintKey;
import org.imagine.vector.editor.ui.ShapeEntry;

/**
 * Written for demo purposes, but may be useful to copy data into for organizing
 * the palette and not altering the real palette data until a save.
 *
 * @author Tim Boudreau
 */
public final class InMemoryShapePaletteBackend implements PaletteBackend<ShapeEntry> {

    private final Map<String, ShapeEntry> entries = new HashMap<>();
    private final List<Listener<? super ShapeEntry>> listeners = new ArrayList<>();
    static int six = 0;

    public InMemoryShapePaletteBackend setAsDefault() {
        PaletteStorage.CACHE.put("shapes", this);
        return this;
    }

    public void addRandom() {
        ShapeEntry en = createEntry();
        addEntry(en);
    }

    public void addEntry(ShapeEntry e) {
        entries.put(e.getName(), e);
        EventQueue.invokeLater(() -> {
            for (Listener<? super ShapeEntry> l : listeners) {
                l.onItemAdded(e.getName(), e);
            }
        });
    }

    ShapeEntry createEntry() {
        String name = "shape-" + six++;
        PathIteratorWrapper shp = Demo.randomShape();
        ShapeEntry e = new ShapeEntry(shp, PaintKey.forPaint(Demo.randomColor()), PaintKey.forPaint(Demo.randomColor()), true, true, new BasicStroke(2), name);
        return e;
    }

    @Override
    public PaletteBackend<ShapeEntry> allNames(BiConsumer<? super Throwable, ? super List<String>> names) {
        EventQueue.invokeLater(() -> {
            List<String> all = new ArrayList<>(entries.keySet());
            Collections.sort(all);
            names.accept(null, all);
        });
        return this;
    }

    @Override
    public void load(String name, BiConsumer<? super Throwable, ? super ShapeEntry> c) {
        ShapeEntry se = entries.get(name);
        if (se != null) {
            EventQueue.invokeLater(() -> {
                c.accept(null, se);
            });
        }
    }

    @Override
    public void save(String name, ShapeEntry obj, BiConsumer<? super Throwable, ? super String> bi) {
        if (name == null) {
            name = obj.getName();
        }
        ShapeEntry e = entries.get(name);
        entries.put(name, obj);
        String nm = name;
        EventQueue.invokeLater(() -> {
            for (Listener<? super ShapeEntry> l : listeners) {
                if (e != null) {
                    l.onItemChanged(nm, obj);
                } else {
                    l.onItemAdded(nm, obj);
                }
            }
        });
    }

    @Override
    public void delete(String name, BiConsumer<? super Throwable, Boolean> c) {
        ShapeEntry e = entries.remove(name);
        EventQueue.invokeLater(() -> {
            c.accept(null, e != null);
            for (Listener<? super ShapeEntry> l : listeners) {
                l.onItemDeleted(name);
            }
        });
    }

    @Override
    public PaletteBackend<ShapeEntry> listen(Listener<? super ShapeEntry> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public PaletteBackend<ShapeEntry> unlisten(Listener<? super ShapeEntry> listener) {
        listeners.remove(listener);
        return this;
    }

}
