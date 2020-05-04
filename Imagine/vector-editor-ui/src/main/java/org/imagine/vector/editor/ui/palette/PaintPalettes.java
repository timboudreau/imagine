package org.imagine.vector.editor.ui.palette;

import com.mastfrog.abstractions.Wrapper;
import com.mastfrog.util.preconditions.Exceptions;
import java.awt.EventQueue;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.activation.DataHandler;
import javax.swing.JComponent;
import org.imagine.awt.dnd.PaintKeyDropSupport;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import org.imagine.editor.api.util.ActivationOrder;
import org.imagine.io.ByteArrayReadChannel;
import org.imagine.io.KeyBinaryReader;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.io.HashInconsistencyBehavior;
import org.imagine.vector.editor.ui.io.VectorIO;
import static org.imagine.vector.editor.ui.palette.PaintPalettes.shapeMimeTypes;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.openide.awt.StatusDisplayer;
import org.openide.filesystems.FileUtil;
import org.openide.util.Mutex;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
public final class PaintPalettes {

    private static final ActivationOrder<String> order = new ActivationOrder(3, s -> s.hashCode());

    private static final Runnable OPENER;

    static {
        OPENER = order.sequenceBuilder()
                .add(PaintsPaletteTC.PREFERRED_ID,
                        () -> {
                            PaintsPaletteTC tc = PaintsPaletteTC.getInstance();
                            if (tc != null) {
                                tc.ensureOpenWithoutUpdateOrder(true);
                            }
                        }).add(ShapesPaletteTC.PREFERRED_ID,
                        () -> {
                            ShapesPaletteTC tc = ShapesPaletteTC.getInstance();
                            if (tc != null) {
                                tc.ensureOpenWithoutUpdateOrder(true);
                            }
                        }).build();
    }

    static void activated(AbstractPaletteTC paletteTC) {
        order.activated(paletteTC.preferredID());
    }

    public static void openPalettes() {
        Mutex.EVENT.readAccess(OPENER);
    }

    public static void closePalettes() {
        PaintsPaletteTC.closePalette();
        ShapesPaletteTC.closePalette();
    }

    public static Collection<? extends DataFlavor> shapeMimeTypes() {
        return ShapeEntryTransferHandler.flavors();
    }

    @Messages(
            {"# {0} - shapeName",
                "SAVED_SHAPE=Saved Shape {0} to palette"})
    public static void addToShapePalette(ShapeElement el) {
        PaletteBackend<ShapeElement> stor = PaintStorage.get("shapes", ShapePaletteStorage::new);
        stor.save(null, el, (thrown, name) -> {
            if (thrown != null) {
                Exceptions.printStackTrace(thrown);
            } else {
                EventQueue.invokeLater(() -> {
                    StatusDisplayer.getDefault().setStatusText(Bundle.SAVED_SHAPE(name));
                    ShapesPaletteTC.getInstance().requestVisible();
                });
            }
        });
    }

    @Messages({
        "# {0} - shapeName",
        "SAVED_FILL=Saved Fill {0} to palette"})
    public static void addToPaintPalette(PaintKey<?> key) {
        PaletteBackend<PaintKey<?>> stor = PaintStorage.get("paints", PaintStorage::new);
        stor.save(null, key, (thrown, name) -> {
            if (thrown != null) {
                Exceptions.printStackTrace(thrown);
            } else {
                EventQueue.invokeLater(() -> {
                    StatusDisplayer.getDefault().setStatusText(Bundle.SAVED_FILL(name));
                    PaintsPaletteTC.getInstance().requestVisible();
                });
            }
        });
    }

    public static boolean containsShapeElement(Transferable xfer) {
        Set<DataFlavor> all = new HashSet<>(shapeMimeTypes());
        all.retainAll(Arrays.asList(xfer.getTransferDataFlavors()));
        return !all.isEmpty();
    }

    public static boolean containsPaintKey(Transferable xfer) {
        Set<DataFlavor> all = new HashSet<>(PaintKeyDropSupport.allFlavors());
        all.retainAll(Arrays.asList(xfer.getTransferDataFlavors()));
        return !all.isEmpty();
    }

    public static Transferable createTransferable(ShapeElement el) {
        ShapeEntry en = el instanceof ShapeEntry ? (ShapeEntry) el
                : Wrapper.find(el, ShapeEntry.class);
        return new DataHandler(new ShapeEntryTransferHandler.ShapeEntryDataSource(en));
    }

    public static Transferable createTransferable(PaintKey<?> key) {
        return PaintKeyDropSupport.createTransferrable(key);
    }

    public static ShapeEntry shapeElementFromTransferable(Transferable xfer) throws IOException {
        try {
            try (InputStream in = (InputStream) xfer.getTransferData(xfer.getTransferDataFlavors()[0])) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    FileUtil.copy(in, out);
                    try (ByteArrayReadChannel c = new ByteArrayReadChannel(out.toByteArray())) {
                        KeyBinaryReader kbr = new KeyBinaryReader(c);
                        kbr.readMagicAndSize();
                        VectorIO vio = new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.WARN);
                        return ShapeEntry.read(vio, kbr);
                    }
                }
            }
        } catch (UnsupportedFlavorException e) {
            throw new IOException(e);
        }
    }

    public static PaintKey<?> paintKeyFromTransferable(Transferable xfer) throws IOException {
        try {
            try (InputStream in = (InputStream) xfer.getTransferData(xfer.getTransferDataFlavors()[0])) {
                try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                    FileUtil.copy(in, out);
                    return PaintKeyIO.read(out.toByteArray());
                }
            }
        } catch (UnsupportedFlavorException e) {
            throw new IOException(e);
        }
    }

    public static JComponent createPaintPaletteComponent() {
        PaletteBackend<? extends PaintKey<?>> backend = PaletteStorage.get("paints", PaintStorage::new);
        PaintKeyTileFactory paintTiles = new PaintKeyTileFactory(backend);
        PaletteItemsPanel panel = new PaletteItemsPanel(paintTiles);
        return panel;
    }

    public static JComponent createShapesPaletteComponent() {
        PaletteBackend<ShapeElement> stor = PaletteStorage.get("shapes", ShapePaletteStorage::new);
        ShapeTileFactory shapeTiles = new ShapeTileFactory(stor);
        PaletteItemsPanel panel = new PaletteItemsPanel(shapeTiles);
        return panel;
    }

    private PaintPalettes() {
        throw new AssertionError();
    }
}
