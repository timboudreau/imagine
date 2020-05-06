/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions.impl;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import net.java.dev.imagine.api.io.LoadSupport;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import net.java.dev.imagine.ui.common.ImageEditorFactory;
import org.netbeans.paint.api.editing.LayerFactory;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages("IMAGINE_NATIVE=Imagine Native Format")
@ServiceProvider(service = ImageEditorFactory.class, position = 0)
public class NativeImageEditorFactory extends ImageEditorFactory {

    public NativeImageEditorFactory() {
        super(Bundle.IMAGINE_NATIVE());
    }

    @Override
    public void openNew(Dimension size, BackgroundStyle bg, LayerFactory layerFactory) {
        for (ImageEditorFactory f : Lookup.getDefault().lookupAll(ImageEditorFactory.class)) {
            if (!(f instanceof NativeImageEditorFactory)) {
                f.openNew(size, bg, layerFactory);
                return;
            }
        }
        throw new IllegalStateException("No image editor factories installed");
    }

    @Override
    public void openNew(Dimension dim, BackgroundStyle bg) {
        for (ImageEditorFactory f : Lookup.getDefault().lookupAll(ImageEditorFactory.class)) {
            if (!(f instanceof NativeImageEditorFactory)) {
                f.openNew(dim, bg);
                return;
            }
        }
        throw new IllegalStateException("No image editor factories installed");
    }

    @Override
    public boolean openExisting(File file) {
        for (LoadSupport<?, ?, ?> supp : Lookup.getDefault().lookupAll(LoadSupport.class)) {
            if (file.getName().endsWith("." + supp.fileExtension())) {
                return open(file.toPath(), supp);
            }
        }
        return false;
    }

    @Override
    public boolean canOpen(File file) {
        for (LoadSupport supp : Lookup.getDefault().lookupAll(LoadSupport.class)) {
            if (file.getName().endsWith("." + supp.fileExtension())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isUserVisible() {
        return false;
    }

    private boolean open(Path toPath, LoadSupport<?, ?, ?> supp) {
        try {
            supp.open(toPath);
            return true;
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        }
        return false;
    }
}
