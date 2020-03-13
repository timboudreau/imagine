package org.imagine.vector.editor.ui.palette;

import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.function.Supplier;

/**
 *
 * @author Tim Boudreau
 */
final class TileImageTransferable<T> implements Transferable {

    private final Tile<T> tile;
    private final Supplier<Image> imgSupplier;

    public TileImageTransferable(Tile<T> tile, Supplier<Image> imgSupplier) {
        this.tile = tile;
        this.imgSupplier = imgSupplier;
    }
    private Image image;

    private Image image() {
        if (image == null) {
            image = imgSupplier.get();
        }
        return image;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (isDataFlavorSupported(flavor)) {
            return image();
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor == DataFlavor.imageFlavor;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor};
    }

}
