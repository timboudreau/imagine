/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.api.io.SaveSupport;
import net.java.dev.imagine.ui.common.RecentFiles;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
public class SaveAsNativeAction extends GenericContextSensitiveAction<Picture> {

    public SaveAsNativeAction() {
        super("ACT_SaveAsNative", Picture.class);
        setIcon(
                ImageUtilities.loadImage(
                        "net/java/dev/imagine/ui/actions/save24.png")); //NOI18N
    }

    @Override
    protected <T> boolean checkEnabled(Collection<? extends T> coll, Class<T> clazz) {
        if (clazz == Picture.class) {
            for (T obj : coll) {
                Picture p = (Picture) obj;
                if (!SaveSupport.canSave(p)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void performAction(Picture t) {
        Path path = showFileChooser(t);
        boolean preexisting = Files.exists(path);
        if (path != null) {
            try {
                // XXX make a backup first, and restore on failure?
                performSave(t, path);
                t.associateFile(path);
                RecentFiles.getDefault().add(RecentFiles.Category.IMAGINE_NATIVE, path);
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

    static long performSave(Picture t, Path path) throws IOException {
        return SaveSupport.save(t, path);
    }

    static Path showFileChooser(Picture t) {
        Collection<? extends SaveSupport> all = Lookup.getDefault().lookupAll(SaveSupport.class);

        FileChooserBuilder fcb = new FileChooserBuilder("imagineNative")
                .addDefaultFileFilters();

        boolean anyFound = false;
        for (SaveSupport save : all) {
            fcb.addFileFilter(new SaveSupportFilter(save));
            anyFound = true;
        }
        if (!anyFound) {
            JOptionPane.showMessageDialog(WindowManager.getDefault().getMainWindow(),
                    NbBundle.getMessage(SaveAsNativeAction.class, "MSG_CannotSave"));
            return null;
        }
        fcb.setTitle(NbBundle.getMessage(SaveAsNativeAction.class, "TTL_SaveAsNative"));
        fcb.setDefaultWorkingDirectory(new File(System.getProperty("user.home")));
        fcb.setFilesOnly(true);
        fcb.setFileHiding(true);
        File file = fcb.showSaveDialog();
        return file == null ? null : file.toPath();
    }

    private static class SaveSupportFilter extends FileFilter {

        private final SaveSupport supp;

        public SaveSupportFilter(SaveSupport supp) {
            this.supp = supp;
        }

        @Override
        public boolean accept(File f) {
            return !f.isHidden() && (f.getName().endsWith("." + supp.fileExtension())
                    || f.isDirectory());
        }

        @Override
        public String getDescription() {
            return supp.displayName() + " (." + supp.fileExtension() + ")";
        }

    }

}
