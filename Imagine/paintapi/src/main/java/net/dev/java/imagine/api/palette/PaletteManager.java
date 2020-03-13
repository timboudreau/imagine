/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.api.palette;

import com.mastfrog.function.TriConsumer;
import java.awt.event.ActionEvent;
import java.util.function.BiConsumer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import net.dev.java.imagine.spi.palette.PaletteHandler;
import org.openide.awt.StatusDisplayer;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
public final class PaletteManager {

    private static <T> PaletteSaver<? super T> saver(Class<T> type) {
        for (PaletteHandler h : Lookup.getDefault().lookupAll(PaletteHandler.class)) {
            TriConsumer<String, T, BiConsumer<Throwable, String>> saver = h.saver(type);
            if (saver != null) {
                return new PaletteSaver<>(saver, h.displayName());
            }
        }
        return null;
    }

    public static <T> Action createSaveToPaletteAction(T object, Class<T> type) {
        return createSaveToPaletteAction(object, type, false);
    }

    public static <T> Action createSaveToPaletteAction(T object, Class<T> type, boolean askForName) {
        PaletteSaver<? super T> saver = saver(type);
        if (saver != null) {
            return new SaveToPaletteAction<>(saver, object);
        }
        return null;
    }

    @Messages({
        "# {0} - paletteName",
        "saveTo=Save To {0} Palette",
        "# {0} - itemName",
        "# {1} - paletteName",
        "savedTo=Saved {0} To {1} Palette"
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
            saver.accept(object, (thrown, name) -> {
                if (thrown != null) {
                    Exceptions.printStackTrace(thrown);
                } else if (name != null) {
                    StatusDisplayer.getDefault().setStatusText(
                            Bundle.savedTo(name, saver.displayName()));
                }
            });
        }
    }
}
