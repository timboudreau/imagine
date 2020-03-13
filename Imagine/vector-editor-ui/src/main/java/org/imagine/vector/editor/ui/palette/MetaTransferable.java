package org.imagine.vector.editor.ui.palette;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A Transferable which wraps multiple other individual transferables.
 *
 * @author Tim Boudreau
 */
public final class MetaTransferable implements Transferable {

    // XXX could use ExTransferable.Multi
    private final List<Transferable> delegates = new ArrayList<>(5);
    private DataFlavor[] flavors;

    public MetaTransferable(Transferable initial) {
        delegates.add(initial);
    }

    public MetaTransferable add(Transferable xfer) {
        if (xfer instanceof MetaTransferable) {
            throw new IllegalArgumentException("Do not next MetaTransferables");
        }
        delegates.add(xfer);
        flavors = null;
        return this;
    }

    public static Transferable find(DataFlavor flavor, Transferable t) {
        if (t instanceof MetaTransferable) {
            return ((MetaTransferable) t).transferableForFlavor(flavor);
        }
        if (Arrays.asList(t.getTransferDataFlavors()).indexOf(flavor) >= 0) {
            return t;
        }
        return null;
    }

    public static <T extends Transferable> T find(Class<T> type, Transferable t) {
        if (type.isInstance(t)) {
            return type.cast(t);
        }
        if (t instanceof MetaTransferable) {
            MetaTransferable mt = (MetaTransferable) t;
            for (Transferable xfer : mt.delegates) {
                if (type.isInstance(xfer)) {
                    return type.cast(xfer);
                }
            }
        }
        return null;
    }

    private Transferable transferableForFlavor(DataFlavor flavor) {
        for (Transferable t : delegates) {
            if (Arrays.asList(t.getTransferDataFlavors()).indexOf(flavor) >= 0) {
                return t;
            }
        }
        return null;
    }

    private DataFlavor[] flavors() {
        if (flavors == null) {
            Set<DataFlavor> theFlavors = new LinkedHashSet<>(delegates.size());
            for (Transferable t : delegates) {
                theFlavors.addAll(Arrays.asList(t.getTransferDataFlavors()));
            }
            this.flavors = theFlavors.toArray(new DataFlavor[theFlavors.size()]);
        }
        return flavors;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        return flavors();
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return Arrays.asList(flavors).indexOf(flavor) >= 0;
    }

    @Override
    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        for (Transferable t : delegates) {
            if (Arrays.asList(t.getTransferDataFlavors()).indexOf(flavor) >= 0) {
                return t.getTransferData(flavor);
            }
        }
        throw new UFE(flavor, "No supporter of " + flavor + " in " + delegates);
    }

    static class UFE extends UnsupportedFlavorException {

        private final String message;

        public UFE(DataFlavor df, String msg) {
            super(df);
            this.message = msg;
        }

        @Override
        public String getMessage() {
            return message;
        }
    }

}
