package org.imagine.vector.editor.ui.palette;

import com.mastfrog.abstractions.Wrapper;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.filechooser.FileFilter;
import org.imagine.awt.dnd.PaintKeyDropSupport;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import com.mastfrog.geometry.util.PooledTransform;
import org.imagine.io.ByteArrayReadChannel;
import org.imagine.io.KeyBinaryReader;
import org.imagine.io.KeyBinaryWriter;
import org.imagine.nbutil.filechooser.FileChooserBuilder;
import org.imagine.nbutil.filechooser.FileKinds;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.io.HashInconsistencyBehavior;
import org.imagine.vector.editor.ui.io.VectorIO;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.openide.awt.Mnemonics;
import org.openide.filesystems.FileUtil;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
final class ShapeTileFactory<S extends ShapeElement> extends AbstractTileFactory<S, ShapeEntryTransferHandler> {

    private final PaletteBackend<S> storage;
    private final int prefDim;

    public ShapeTileFactory(PaletteBackend<S> storage) {
        this(storage, 128);
    }

    public ShapeTileFactory(PaletteBackend<S> storage, int prefDim) {
        super(ShapeElement.class);
        this.storage = storage;
        this.prefDim = prefDim;
    }

    @Override
    public Dimension getPreferredTileSize() {
        return new Dimension(prefDim, prefDim);
    }

    @Override
    public PaletteBackend<S> storage() {
        return storage;
    }

    @Override
    public Tile<? super S> createTile(String tileName) {
        return new ShapeTile(tileName);
    }

    @Override
    public Transferable createTransferable(Tile<S> comp) {
        return getTransferHandler().createTransferable(comp);
    }

    @Override
    protected ShapeEntryTransferHandler createTransferHandler() {
        return new ShapeEntryTransferHandler((comp) -> {
            return ((ShapeTile) comp).item();
        });
    }

    @Override
    protected boolean isOurTile(Component tile) {
        return tile instanceof ShapeTile;
    }

    @Override
    protected Collection<? extends DataFlavor> supportedDataFlavors() {
        Set<DataFlavor> flavors = new HashSet<>(ShapeEntryTransferHandler.flavors());
        flavors.addAll(PaintKeyDropSupport.allFlavors());
        return flavors;
    }

    @Override
    protected Collection<? extends DataFlavor> paletteSupportedDataFlavors() {
        return new HashSet<>(ShapeEntryTransferHandler.flavors());
    }

    @Override
    protected int handleAddToPalette(Set<DataFlavor> supportedFlavors, Transferable xfer, Component target) {
        if (supportedFlavors.isEmpty()) {
            return -1;
        }
        try (InputStream in = (InputStream) xfer.getTransferData(supportedFlavors.iterator().next())) {
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                FileUtil.copy(in, out);
                VectorIO vio = new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.WARN);
                KeyBinaryReader reader = new KeyBinaryReader(new ByteArrayReadChannel(out.toByteArray()));
                reader.readMagicAndSize();
                ShapeEntry entry = ShapeEntry.read(vio, reader);
                entry.changed();
                Rectangle2D.Double bds = new Rectangle2D.Double();
                entry.addToBounds(bds);
                if (bds.x != 0 || bds.y != 0) {
                    PooledTransform.withTranslateInstance(-bds.x, -bds.y, entry::applyTransform);
//                    AffineTransform xform = AffineTransform.getTranslateInstance(-bds.x, -bds.y);
//                    entry.applyTransform(xform);
                    entry.changed();
                }
                storage().save(entry.getName(), (S) entry, (thrown, str) -> {
                    if (thrown != null) {
                        Exceptions.printStackTrace(thrown);
                    }
                });
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return -1;
    }

    @Override
    protected int handleDropOnTile(Set<DataFlavor> supportedFlavors, Transferable xfer, Component comp) {
        Set<DataFlavor> shapeTransferTypes = new HashSet<>(ShapeEntryTransferHandler.flavors());
        shapeTransferTypes.retainAll(supportedFlavors);
        if (!shapeTransferTypes.isEmpty()) {
        } else {
            Set<DataFlavor> flavors = PaintKeyDropSupport.allFlavors();
            flavors.retainAll(supportedFlavors);
            try {
                try (InputStream in = (InputStream) xfer.getTransferData(flavors.iterator().next())) {
                    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                        FileUtil.copy(in, out);
                        PaintKey<?> key = PaintKeyIO.read(out.toByteArray());
                        ShapeTile shapeTile = (ShapeTile) comp;
                        ShapeElement item = shapeTile.item();
                        String name = shapeTile.getName();
                        item = item.copy();
                        item.setFill(key);
                        storage().save(name, (S) item, (thrown, nm) -> {
                            if (thrown != null) {
                                Exceptions.printStackTrace(thrown);
                            }
                        });
                        return DnDConstants.ACTION_COPY;
                    }
                }
            } catch (UnsupportedFlavorException | IOException ex) {
                Exceptions.printStackTrace(ex);
                return -1;
            }
        }
        return -1;
    }

    @Override
    protected void populateTileMenu(JPopupMenu menu, Tile<S> tile) {
        JMenuItem item = new JMenuItem(new SaveShapeAction(tile.item()));
        Mnemonics.setLocalizedText(item, Bundle.Save());
        menu.add(item, 0);
    }

    @Override
    protected void populatePaletteMenu(JPopupMenu menu, JComponent palette) {
        JMenuItem item = new JMenuItem(new LoadShapeAction());
        Mnemonics.setLocalizedText(item, Bundle.load());
        menu.add(item, 0);
    }

    @Messages({"load=&Load...", "loadShapeApproveText=Load Shape to Palette"})
    private final class LoadShapeAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            File file = new FileChooserBuilder("saveShapes")
                    .setDefaultWorkingDirectory(new File(System.getProperty("user.home")))
                    .setTitle(Bundle.SaveFill())
                    .setFileFilter(new ShapeFileFilter())
                    .setFileKinds(FileKinds.FILES_ONLY)
                    .setFileHiding(true)
                    .setApproveText(Bundle.loadShapeApproveText())
                    .showSaveDialog();
            if (file != null) {
                try (FileChannel c = FileChannel.open(file.toPath(), READ)) {
                    VectorIO io = new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.WARN);
                    KeyBinaryReader kbr = new KeyBinaryReader(c);
                    int size = kbr.readMagicAndSize();
                    ShapeEntry entry = ShapeEntry.read(io, kbr);
                    String name = file.getName();
                    int ix = name.lastIndexOf('.');
                    if (ix > 0) {
                        name = name.substring(0, ix);
                    }
                    storage().save(name, (S) entry, (thrown, nm) -> {
                        if (thrown != null) {
                            Exceptions.printStackTrace(thrown);
                        }
                    });
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    @Messages("shapeFiles=Shape Files")
    private static final class ShapeFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isHidden()) {
                return false;
            }
            return f.isDirectory() || f.getName().endsWith(".ishape");
        }

        @Override
        public String getDescription() {
            return Bundle.shapeFiles();
        }
    }

    private static final class SaveShapeAction extends AbstractAction {

        private final ShapeEntry el;

        SaveShapeAction(ShapeElement key) {
            super(Bundle.Save());
            this.el = key instanceof ShapeEntry ? (ShapeEntry) key
                    : Wrapper.find(key, ShapeEntry.class);
            setEnabled(el != null);
        }

        private static String fileName(File file) {
            String s = file.getName();
            int ix = s.lastIndexOf('.');
            if (ix < 0) {
                return s;
            }
            return s.substring(0, ix);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File file = new FileChooserBuilder("saveShapes")
                    .setDefaultWorkingDirectory(new File(System.getProperty("user.home")))
                    .setTitle(Bundle.SaveFill())
                    .setFileKinds(FileKinds.FILES_ONLY)
                    .setFileFilter(new ShapeFileFilter())
                    .setFileHiding(true)
                    .showSaveDialog();
            if (file != null) {
                if (!file.getName().endsWith(".ishape")) {
                    file = new File(file.getParent(), file.getName() + ".ishape");
                }
                try (FileChannel c = FileChannel.open(file.toPath(), CREATE, TRUNCATE_EXISTING, WRITE)) {
                    ShapeEntry toSave = el.duplicate();
                    toSave.setName(fileName(file));
                    Rectangle2D r = toSave.shape().getBounds();
                    toSave.translate(-r.getX(), -r.getY());

                    VectorIO io = new VectorIO();
                    KeyBinaryWriter w = new KeyBinaryWriter();
                    toSave.writeTo(io, w);
                    w.finishRecord();
                    for (ByteBuffer buf : w.get()) {
                        buf.flip();
                        c.write(buf);
                    }
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

}
