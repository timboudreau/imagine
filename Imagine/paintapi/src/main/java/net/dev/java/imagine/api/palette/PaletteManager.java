/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.palette;

import java.awt.event.ActionEvent;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import net.dev.java.imagine.spi.palette.PaletteHandler;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
public final class PaletteManager {

    private static <T> PaletteSaver<? super T> saver(T object) {
        for (PaletteHandler h : Lookup.getDefault().lookupAll(PaletteHandler.class)) {
            Consumer<T> saver = h.saver(object);
            if (saver != null) {
                return new PaletteSaver<>(saver, h.displayName());
            }
        }
        return null;
    }

    public static <T> Action createSaveToPaletteAction(T object) {
        PaletteSaver<? super T> saver = saver(object);
        if (saver != null) {
            return new SaveToPaletteAction<>(saver, object);
        }
        return null;
    }

    @Messages({
        "# {0} - paletteName",
        "saveTo=Save To {0} Palette"
    })
    private static final class SaveToPaletteAction<T> extends AbstractAction {

        private final PaletteSaver<T> saver;
        private final T object;

        public SaveToPaletteAction(PaletteSaver<T> saver, T object) {
            putValue(NAME, Bundle.saveTo(saver.displayName()));
            this.saver = saver;
            this.object = object;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            saver.accept(object);
        }
    }
}
