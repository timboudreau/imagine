/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.dnd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.function.Consumer;
import static org.imagine.awt.dnd.PaintKeyDropSupport.allFlavors;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
final class PaintKeyDropTargetListener implements DropTargetListener {

    private final Consumer<PaintKey<?>> consumer;

    public PaintKeyDropTargetListener(Consumer<PaintKey<?>> consumer) {
        this.consumer = consumer;
    }

    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
        Set<DataFlavor> flavors = allFlavors();
        flavors.retainAll(dtde.getCurrentDataFlavorsAsList());
        if (!flavors.isEmpty()) {
            dtde.acceptDrag(dtde.getDropAction());
        } else {
            dtde.rejectDrag();
        }
    }

    @Override
    public void dragOver(DropTargetDragEvent dtde) {

    }

    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // do nothing
    }

    @Override
    public void dragExit(DropTargetEvent dte) {
        // do nothing
    }

    @Override
    public void drop(DropTargetDropEvent dtde) {
        Set<DataFlavor> flavors = allFlavors();
        flavors.retainAll(dtde.getCurrentDataFlavorsAsList());
        if (!flavors.isEmpty()) {
            try {
                DataFlavor flavor = flavors.iterator().next();
                Transferable xfer = dtde.getTransferable();
                InputStream in = (InputStream) xfer.getTransferData(flavor);
                byte[] b = new byte[in.available()];
                in.read(b);

                PaintKey<?> key = PaintKeyIO.read(b);
                consumer.accept(key);
            } catch (UnsupportedFlavorException | IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }
}
