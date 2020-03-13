/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.palette;

import com.mastfrog.abstractions.Wrapper;
import java.awt.Image;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Function;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import org.imagine.awt.dnd.PaintKeyDropSupport;
import org.imagine.io.ByteArrayReadChannel;
import org.imagine.io.KeyBinaryReader;
import org.imagine.io.KeyBinaryWriter;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.io.HashInconsistencyBehavior;
import org.imagine.vector.editor.ui.io.VectorIO;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
public class ShapeEntryTransferHandler extends TransferHandler {

    private final Function<JComponent, ShapeElement> converter;
    private static final String MIME_TYPE = "application/x-shapeElement";

    public static Collection<? extends DataFlavor> flavors() {
        try {
            return Collections.singleton(new DataFlavor(MIME_TYPE));
        } catch (ClassNotFoundException ex) {
            throw new AssertionError(ex);
        }
    }

    public ShapeEntryTransferHandler(Function<JComponent, ShapeElement> converter) {
        super(MIME_TYPE);
        this.converter = converter;
    }

    @Override
    protected Transferable createTransferable(JComponent c) {
        ShapeElement el = converter.apply(c);
        ShapeEntry en = el instanceof ShapeEntry ? (ShapeEntry) el
                : Wrapper.find(el, ShapeEntry.class);
        return new DataHandler(new ShapeEntryDataSource(en));
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
        Set<DataFlavor> paintKeyTypes = PaintKeyDropSupport.allFlavors();
        for (DataFlavor f : transferFlavors) {
            if (MIME_TYPE.equals(f.getMimeType())) {
                return true;
            }
            if (paintKeyTypes.contains(f)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Point getDragImageOffset() {
        return new Point(5, 5);
    }

    @Override
    public Image getDragImage() {
        return super.getDragImage();
    }

    static final class ShapeEntryDataSource implements DataSource {

        private ShapeEntry entry;

        public ShapeEntryDataSource() {

        }

        public ShapeEntryDataSource(ShapeEntry entry) {
            this.entry = entry;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            KeyBinaryWriter kbw = new KeyBinaryWriter();
            VectorIO vio = new VectorIO();
            entry.writeTo(vio, kbw);
            kbw.finishRecord();
            return new ByteArrayInputStream(kbw.toByteArray());
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return new ByteArrayOutputStream() {
                @Override
                public void close() throws IOException {
                    super.close();
                    ByteArrayReadChannel c = new ByteArrayReadChannel(toByteArray());
                    KeyBinaryReader kbr = new KeyBinaryReader(c);
                    kbr.readMagicAndSize();
                    VectorIO vio = new VectorIO().setHashInconsistencyBehavior(HashInconsistencyBehavior.WARN);
                    entry = ShapeEntry.read(vio, kbr);
                }
            };
        }

        @Override
        public String getContentType() {
            return MIME_TYPE;
        }

        @Override
        @Messages("shape=Shape")
        public String getName() {
            return Bundle.shape();
        }
    }

}
