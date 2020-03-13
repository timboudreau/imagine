/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.palette;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
public final class InMemoryPaintPaletteBackend implements PaletteBackend<PaintKey<?>> {

    private final Map<String, PaintKey<?>> entries = new HashMap<>();
    private final List<Listener<? super PaintKey<?>>> listeners = new ArrayList<>();

    static int INDICES;

    public InMemoryPaintPaletteBackend setAsDefault() {
        PaletteStorage.CACHE.put("paints", this);
        return this;
    }

    public void addRandom() {
        addEntry(createEntry());
    }

    private String newName() {
        return "paint-" + INDICES++;
    }

    public void addEntry(PaintKey<?> e) {
        String name = newName();
        for (Map.Entry<String, PaintKey<?>> en : entries.entrySet()) {
            if (e.equals(en.getValue())) {
                return;
            }
        }
        entries.put(name, e);
        EventQueue.invokeLater(() -> {
            for (Listener<? super PaintKey<?>> l : listeners) {
                l.onItemAdded(name, e);
            }
        });
    }

    PaintKey<?> createEntry() {
        return Demo.randomPaint();
    }

    @Override
    public PaletteBackend<PaintKey<?>> allNames(BiConsumer<? super Throwable, ? super List<String>> names) {
        EventQueue.invokeLater(() -> {
            List<String> all = new ArrayList<>(entries.keySet());
            Collections.sort(all);
            names.accept(null, all);
        });
        return this;
    }

    @Override
    public void load(String name, BiConsumer<? super Throwable, ? super PaintKey<?>> c) {
        PaintKey<?> se = entries.get(name);
        if (se != null) {
            EventQueue.invokeLater(() -> {
                c.accept(null, se);
            });
        }
    }

    @Override
    public void save(String name, PaintKey<?> obj, BiConsumer<? super Throwable, ? super String> bi) {
        if (name == null) {
            name = newName();
        }
        PaintKey<?> e = entries.get(name);
        entries.put(name, obj);
        String nm = name;
        EventQueue.invokeLater(() -> {
            for (Listener<? super PaintKey<?>> l : listeners) {
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
        PaintKey<?> e = entries.remove(name);
        EventQueue.invokeLater(() -> {
            c.accept(null, e != null);
            for (Listener<? super PaintKey<?>> l : listeners) {
                l.onItemDeleted(name);
            }
        });
    }

    @Override
    public PaletteBackend<PaintKey<?>> listen(Listener<? super PaintKey<?>> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public PaletteBackend<PaintKey<?>> unlisten(Listener<? super PaintKey<?>> listener) {
        listeners.remove(listener);
        return this;
    }

}
