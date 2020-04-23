package org.imagine.vector.editor.ui.palette;

import static com.mastfrog.util.preconditions.Checks.notNull;
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
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final Logger LOG = Logger.getLogger(PaletteStorage.class.getName());
    private final String suffix;

    static {
        LOG.setLevel(Level.ALL);
    }

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
        suffix = '.' + type.getSimpleName();
    }

    Listener<T> listenerProxy = new Listener<T>() {
        @Override
        public void onItemAdded(String name, T item) {
            LOG.log(Level.FINE, "Palette item added to {0}: {1} with {2}", new Object[]{this, item, name});
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
            LOG.log(Level.FINE, "Palette item deleted to {0}: {1}", new Object[]{this, name});
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
            LOG.log(Level.FINER, "Palette item changed in {0}: {1} to {2}", new Object[]{this, name, imet});
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
                EventQueue.invokeLater(() -> {
                    names.accept(null, result);
                });
            } catch (Exception | Error ex) {
                LOG.log(Level.WARNING, "Exception getting names for " + this, ex);
                names.accept(ex, null);
            } finally {
                LOG.log(Level.FINER, "All palette item names in {0}: {1}", new Object[]{this, result});
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
                LOG.log(Level.WARNING, "Exception deleting from " + this + ": " + name, ex);
                c.accept(ex, false);
            }
        });
    }

    @Override
    public final void save(String name, T obj, BiConsumer<? super Throwable, ? super String> bi) {
        PALETTE_IO.submit(() -> {
            String nm = name == null ? itemName(synthesizeNamePrefix(obj)) : name;
            try {
                Path pth = path(nm);
                boolean existed = Files.exists(pth);
                saveImpl(nm, obj);
                try {
                    bi.accept(null, nm);
                } finally {
                    if (existed) {
                        listenerProxy.onItemChanged(nm, obj);
                    } else {
                        listenerProxy.onItemAdded(nm, obj);
                    }
                }
                LOG.log(Level.FINE, "Saved {0} named {1} as {2}", new Object[]{obj, nm, pth});
            } catch (Exception | Error ex) {
                LOG.log(Level.WARNING, "Exception saving in " + this + ": " + nm + " with " + obj, ex);
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
                LOG.log(Level.WARNING, "Exception loading in " + this + ": " + name, e);
                c.accept(e, null);
            }
        });
    }

    protected abstract String synthesizeNamePrefix(T obj);

    protected abstract T loadImpl(String name) throws IOException;

    protected FileChannel readChannel(String name) throws IOException {
        Path path = path(notNull("name", name));
        LOG.log(Level.FINE, "Create read channel in {0} for {1} with path {2} exists? {3}",
                new Object[]{this, name, path, Files.exists(path)});
        if (Files.exists(path)) {
            return FileChannel.open(path, StandardOpenOption.READ);
        } else {
            LOG.log(Level.WARNING, "Palette requested non existent path {0} for {1}",
                    new Object[]{path, name});
        }
        return null;
    }

    protected FileChannel writeChannel(String name) throws IOException {
        Path path = path(notNull("name", name));
        LOG.log(Level.FINE, "Create write channel in {0} for {1} with path {2} exists? {3}",
                new Object[]{this, name, path, Files.exists(path)});
        return FileChannel.open(path, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }

    private Path path(String name) throws IOException {
        FileObject fld = folder();
        String nm = itemName(name);
        return FileUtil.toFile(fld).toPath().resolve(nm);
    }

    private String itemName(String name) {
        if (!name.endsWith(suffix)) {
            return name + suffix;
        }
        return name;
    }

    private static FileObject palettesDir() throws IOException {
        FileObject root = FileUtil.getConfigRoot();
        FileObject result = root.getFileObject("imaginePalettes");
        if (result == null) {
            result = root.createFolder("imaginePalettes");
            LOG.log(Level.FINE, "Create base palettes folder for {0} as {1}", new Object[]{result});
        }
        return result;
    }

    protected FileObject folder() throws IOException {
        FileObject dir = palettesDir();
        FileObject result = dir.getFileObject(type.getName().replace('.', '-'));
        if (result == null) {
            LOG.log(Level.INFO, "Create sfs folder {0} for {1}", new Object[]{this, dir});
            result = dir.createFolder(type.getName().replace('.', '-'));
        }
        return result;
    }
}
