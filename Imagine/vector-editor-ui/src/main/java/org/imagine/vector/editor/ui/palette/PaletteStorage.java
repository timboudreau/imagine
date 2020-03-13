package org.imagine.vector.editor.ui.palette;

import java.awt.EventQueue;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public abstract class PaletteStorage<T> implements PaletteBackend<T> {

    private static final RequestProcessor PALETTE_IO
            = new RequestProcessor("palette-io", 1);
    static final Map<String, PaletteBackend<?>> CACHE = new ConcurrentHashMap<>();
    private final Class<? super T> type;
    private final Set<Listener<? super T>> listeners = new WeakSet<>();

    static <T> PaletteBackend<T> get(String key, Supplier<PaletteStorage<T>> supp) {
        PaletteBackend<T> result = (PaletteBackend<T>) CACHE.get(key);
        if (result != null) {
            return result;
        }
        result = supp.get();
        CACHE.put(key, result);
        return result;
    }

    PaletteStorage(Class<? super T> type) {
        this.type = type;
        new Exception(type.getName()).printStackTrace();
    }

    Listener<T> listenerProxy = new Listener<T>() {
        @Override
        public void onItemAdded(String name, T item) {
            if (!listeners.isEmpty()) {
                EventQueue.invokeLater(() -> {
                    for (Listener<? super T> l : listeners) {
                        l.onItemAdded(name, item);
                    }
                });
            }
        }

        @Override
        public void onItemDeleted(String name) {
            if (!listeners.isEmpty()) {
                EventQueue.invokeLater(() -> {
                    for (Listener<? super T> l : listeners) {
                        l.onItemDeleted(name);
                    }
                });
            }
        }

        @Override
        public void onItemChanged(String name, T imet) {
            if (!listeners.isEmpty()) {
                EventQueue.invokeLater(() -> {
                    for (Listener<? super T> l : listeners) {
                        l.onItemChanged(name, imet);
                    }
                });
            }
        }
    };

    @Override
    public PaletteStorage<T> allNames(BiConsumer<? super Throwable, ? super List<String>> names) {
        PALETTE_IO.submit(() -> {
            List<String> result = new ArrayList<>();
            try {
                FileObject fo = folder();
                String ext = type.getSimpleName();
                for (FileObject child : fo.getChildren()) {
                    if (ext.equals(child.getExt())) {
                        result.add(child.getName());
                    }
                }
            } catch (Exception | Error ex) {
                names.accept(ex, result);
            }
        });
        return this;
    }

    @Override
    public PaletteStorage<T> listen(Listener<? super T> listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public PaletteStorage<T> unlisten(Listener<? super T> listener) {
        listeners.remove(listener);
        return this;
    }

    @Override
    public void delete(String name, BiConsumer<? super Throwable, Boolean> c) {
        PALETTE_IO.submit(() -> {
            try {
                Path pth = path(name);
                if (pth != null && Files.exists(pth)) {
                    Files.delete(pth);
                    listenerProxy.onItemDeleted(name);
                }
                c.accept(null, true);
            } catch (Exception | Error ex) {
                c.accept(ex, false);
            }
        });
    }

    @Override
    public final void save(String name, T obj, BiConsumer<? super Throwable, ? super String> bi) {
        PALETTE_IO.submit(() -> {
            try {
                Path pth = path(name);
                boolean existed = Files.exists(pth);
                saveImpl(name, obj);
                try {
                    bi.accept(null, name);
                } finally {
                    if (existed) {
                        listenerProxy.onItemChanged(name, obj);
                    } else {
                        listenerProxy.onItemAdded(name, obj);
                    }
                }
            } catch (Exception | Error ex) {
                bi.accept(ex, null);
            }
        });
    }

    protected abstract void saveImpl(String name, T obj) throws IOException;

    @Override
    public void load(String name, BiConsumer<? super Throwable, ? super T> c) {
        PALETTE_IO.submit(() -> {
            try {
                c.accept(null, loadImpl(name));
            } catch (Exception | Error e) {
                c.accept(e, null);
            }
        });
    }

    protected abstract T loadImpl(String name) throws IOException;

    protected FileChannel readChannel(String name) throws IOException {
        Path path = path(name);
        if (Files.exists(path)) {
            return FileChannel.open(path, StandardOpenOption.READ);
        }
        return null;
    }

    protected FileChannel writeChannel(String name) throws IOException {
        Path path = path(name);
        return FileChannel.open(path, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private Path path(String name) throws IOException {
        FileObject fld = folder();
        String nm = itemName(name);
        return FileUtil.toFile(fld).toPath().resolve(nm);
    }

    private String itemName(String name) {
        return name + "." + type.getSimpleName();
    }

    private static FileObject palettesDir() throws IOException {
        FileObject root = FileUtil.getConfigRoot();
        FileObject result = root.getFileObject("imaginePalettes");
        if (result == null) {
            result = root.createFolder("imaginePalettes");
        }
        return result;
    }

    protected FileObject folder() throws IOException {
        FileObject dir = palettesDir();
        FileObject result = dir.getFileObject(type.getName().replace('.', '-'));
        if (result == null) {
            result = dir.createFolder(type.getName().replace('.', '-'));
        }
        return result;
    }
}
