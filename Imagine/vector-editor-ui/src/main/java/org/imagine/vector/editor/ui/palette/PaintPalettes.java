package org.imagine.vector.editor.ui.palette;

import com.mastfrog.abstractions.Wrapper;
import com.mastfrog.util.preconditions.Exceptions;
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
import org.imagine.io.ByteArrayReadChannel;
import org.imagine.io.KeyBinaryReader;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.io.HashInconsistencyBehavior;
import org.imagine.vector.editor.ui.io.VectorIO;
import static org.imagine.vector.editor.ui.palette.PaintPalettes.shapeMimeTypes;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Tim Boudreau
 */
public final class PaintPalettes {

    public static Collection<? extends DataFlavor> shapeMimeTypes() {
        return ShapeEntryTransferHandler.flavors();
    }

    public static void addToShapePalette(ShapeElement el) {
        PaletteBackend<ShapeElement> stor = PaintStorage.get("shapes", ShapePaletteStorage::new);
        stor.save(null, el, (thrown, name) -> {
            if (thrown != null) {
                Exceptions.printStackTrace(thrown);
            }
        });
    }

    public static void addToPaintPalette(PaintKey<?> key) {
        PaletteBackend<PaintKey<?>> stor = PaintStorage.get("paints", PaintStorage::new);
        stor.save(null, key, (thrown, name) -> {
            if (thrown != null) {
                Exceptions.printStackTrace(thrown);
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
