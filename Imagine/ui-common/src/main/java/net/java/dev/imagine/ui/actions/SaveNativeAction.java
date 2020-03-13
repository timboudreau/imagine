/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.api.io.SaveSupport;
import static net.java.dev.imagine.ui.actions.SaveAsNativeAction.performSave;
import static net.java.dev.imagine.ui.actions.SaveAsNativeAction.showFileChooser;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;

/**
 *
 * @author Tim Boudreau
 */
public class SaveNativeAction extends GenericContextSensitiveAction<Picture> {

    public SaveNativeAction() {
        super("ACT_SaveNative", Picture.class);
	setIcon(
              ImageUtilities.loadImage (
	      "net/java/dev/imagine/ui/actions/save24.png")); //NOI18N
    }

    @Override
    protected <T> boolean checkEnabled(Collection<? extends T> coll, Class<T> clazz) {
        if (clazz == Picture.class) {
            for (T obj : coll) {
                Picture p = (Picture) obj;
                if (p.associatedFile() == null) {
                    return false;
                }
                if (!SaveSupport.canSave(p)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void performAction(Picture t) {
        Path path = t.associatedFile();
        if (path == null) {
            path = showFileChooser(t);
        }
        boolean preexisting = Files.exists(path);
        if (path != null) {
            try {
                // XXX make a backup first, and restore on failure?
                performSave(t, path);
            } catch (IOException | RuntimeException | Error ex) {
                Exceptions.printStackTrace(ex);
                // don't leave turds
                if (!preexisting && Files.exists(path)) {
                    try {
                        Files.delete(path);
                    } catch (IOException ex1) {
                        Exceptions.printStackTrace(ex1);
                    }
                }
            }
        }
    }
}
