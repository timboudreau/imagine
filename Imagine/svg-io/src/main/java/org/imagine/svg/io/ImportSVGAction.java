package org.imagine.svg.io;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.api.io.LoadSupport;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.nbutil.filechooser.FileChooserBuilder;
import org.imagine.nbutil.filechooser.FileKinds;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.VectorLayerFactory;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionRegistration;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;

@ActionID(
        category = "File",
        id = "org.imagine.svg.io.ImportSVGAction"
)
@ActionRegistration(
        displayName = "#CTL_ImportSVGAction"
)
@ActionReference(path = "Menu/File", position = 1850)
@Messages({
    "CTL_ImportSVGAction=Import SVG",
    "IMPORT=Import"
})
public final class ImportSVGAction implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
        File[] files = new FileChooserBuilder(ExportSVGAction.class)
                .setFileFilter(new SvgFileFilter())
                .setApproveText(Bundle.IMPORT()).setTitle(Bundle.CTL_ImportSVGAction())
                .setFileKinds(FileKinds.FILES_ONLY).setFileHiding(true)
                .showMultiOpenDialog();
        System.out.println("\n\n\nBEGIN SVG LOAD");
        if (files != null && files.length > 0) {
            VectorLayerFactory vlf = Lookup.getDefault().lookup(VectorLayerFactory.class);
            LoadSupport<?, ?, ?> loader = Lookup.getDefault().lookup(LoadSupport.class);
            if (vlf == null) {
                return;
            }
            if (loader == null) {
                return;
            }
            // XXX this currently needs to be in the event thread, because
            // paintui2's Picture implementation will construct a PictureScene, and
            // visual library prohibits manipulating it from anything but the event
            // thread.  Those two things need to get detangled.
            for (File f : files) {
                try {
                    Path p = f.toPath();
                    SvgLoader ldr = new SvgLoader(p);
                    String name = fileName(p);
                    BiFunction<Dimension, BiConsumer<RepaintHandle, Function<List<LayerImplementation>, Picture>>, Void> func
                            = loader.create(true);
                    Picture pic = ldr.load(func, vlf, name);
                    if (pic != null) {
                        pic.associateFile(p);
                    }
                } catch (Exception | Error ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    private static String fileName(Path p) {
        String s = p.getFileName().toString();
        int ix = s.lastIndexOf('.');
        if (ix > 0) {
            return s.substring(0, ix);
        }
        return s;
    }
}
