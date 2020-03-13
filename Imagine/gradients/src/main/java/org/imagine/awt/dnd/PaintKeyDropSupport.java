package org.imagine.awt.dnd;

import java.awt.Paint;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.activation.DataHandler;
import javax.swing.JComponent;
import javax.swing.TransferHandler;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import org.openide.util.Exceptions;

/**
 *
 * @author Tim Boudreau
 */
public class PaintKeyDropSupport {

    public static <P extends Paint, K extends PaintKey<P>> Iterable<DataFlavor> flavorFor(K key) throws ClassNotFoundException {
        return Collections.singleton(new DataFlavor("application/x-paintkey;kind=" + key.idBase()));
    }
    private static Set<DataFlavor> flavors;

    static String mimeTypeFor(Class<?> type) {
        Map<String, Class<?>> mc = Accessor.allSupportedTypes();
        for (Map.Entry<String, Class<?>> e : mc.entrySet()) {
            if (e.getValue() == type) {
                return "application/x-paintkey;kind=" + e.getKey();
            }
        }
        return null;
    }

    public static Transferable createTransferrable(PaintKey<?> transferrable) {
        DataHandler handler = new DataHandler(new PaintKeyDataSource(transferrable));
        return handler;
    }

    public static DropTargetListener createDropTargetListener(Consumer<PaintKey<?>> consumer) {
        return new PaintKeyDropTargetListener(consumer);
    }

    public static TransferHandler createTransferHandler(Function<? super JComponent, ? extends PaintKey<?>> keyForComponent) {
        return new PaintKeyTransferHandler(keyForComponent);
    }

    public static PaintKey<?> extractPaintKey(Transferable xfer) {
        Set<DataFlavor> flavors = allFlavors();
        List<DataFlavor> curr = Arrays.asList(xfer.getTransferDataFlavors());
        System.out.println("Curr Flavors " + curr);
        System.out.println("ALL: " + flavors);
        flavors.retainAll(curr);
        if (flavors.isEmpty()) {
            System.out.println("no flavors");
            return null;
        }
        DataFlavor fl = flavors.iterator().next();
        Object data = null;
        try {
            data = xfer.getTransferData(fl);
        } catch (UnsupportedFlavorException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        if (data != null) {
            InputStream in = (InputStream) data;
            try {
                byte[] b = new byte[in.available()];
                in.read(b);
                System.out.println("Transferred " + b.length + " bytes");
                return PaintKeyIO.read(b);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
        return null;
    }

    public static Set<DataFlavor> allFlavors() {
        if (flavors != null) {
            return new LinkedHashSet<>(flavors);
        }
        flavors = new LinkedHashSet<>();
        Accessor.allSupportedTypes().entrySet().forEach((e) -> {
            try {
                flavors.add(new DataFlavor(mimeTypeFor(e.getValue())));
            } catch (ClassNotFoundException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
        return new LinkedHashSet<>(flavors);
    }
}
