package org.imagine.vector.editor.ui.palette;

import java.awt.Dimension;
import java.awt.datatransfer.Transferable;
import javax.swing.TransferHandler;

/**
 *
 * @author Tim Boudreau
 */
interface TileFactory<T> {

    Tile<? super T> createTile(String tileName);

    Tile<? super T> createTile(String tileName, T obj);

    Dimension getPreferredTileSize();

    PaletteBackend<? extends T> storage();

    TransferHandler getTransferHandler();

    Transferable createTransferable(Tile<T> obj);
}
