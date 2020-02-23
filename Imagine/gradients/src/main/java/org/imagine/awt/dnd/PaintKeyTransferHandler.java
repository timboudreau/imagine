/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Function;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import static org.imagine.awt.dnd.PaintKeyDropSupport.allFlavors;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
class PaintKeyTransferHandler extends TransferHandler {

    private final Function<? super JComponent, ? extends PaintKey<?>> keyForComponent;

    public PaintKeyTransferHandler(Function<? super JComponent, ? extends PaintKey<?>> keyForComponent) {
        this.keyForComponent = keyForComponent;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        PaintKey<?> key = keyForComponent.apply(c);
        setDragImage(Accessor.thumbnailForPaintKey(key, 120, 90));
        if (key != null) {
            return PaintKeyDropSupport.createTransferrable(key);
        }
        return null;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        Set<DataFlavor> flavors = allFlavors();
        flavors.retainAll(Arrays.asList(transferFlavors));
        return !flavors.isEmpty();
    }
}
