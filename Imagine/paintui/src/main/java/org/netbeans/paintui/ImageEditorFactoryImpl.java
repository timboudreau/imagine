package org.netbeans.paintui;

import com.mastfrog.function.state.Bool;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import net.java.dev.imagine.ui.common.ImageEditorFactory;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.editing.LayerFactory;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=ImageEditorFactory.class)
public class ImageEditorFactoryImpl extends ImageEditorFactory {

    private final Set<String> fmts = new HashSet<>();
    private static final RequestProcessor IMAGE_OPEN
            = new RequestProcessor("Open images", 2, true);

    public ImageEditorFactoryImpl() {
        super(NbBundle.getMessage(ImageEditorFactoryImpl.class, "RASTER_IMAGES"));
        for (String fmt : ImageIO.getReaderFormatNames()) {
            fmts.add(fmt.toLowerCase());
        }
    }

    @Override
    public void openNew(Dimension dim, BackgroundStyle bg, LayerFactory layerFactory) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void openNew(Dimension dim, BackgroundStyle bg) {
        PaintTopComponent ptc = new PaintTopComponent(dim, bg);
        ptc.open();
        ptc.requestActive();
    }

    @Override
    public boolean openExisting(File file) {
        Bool result = Bool.create();
        load(file, (err, img, origin) -> {
            if (err != null) {
                Exceptions.printStackTrace(err);
                return;
            }
            PaintTopComponent last;
            try {
                last = new PaintTopComponent(img, origin);
                last.setDisplayName(origin.getName());
                last.open();
                last.requestActive();
                result.set();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        });
        return result.getAsBoolean();
    }

    private void load(File file, OpenConsumer img) {
        IMAGE_OPEN.submit(() -> {
            try {
                BufferedImage bi = ImageIO.read(file);
                // Loaded PNGs are slow to render;  unpack the data.
                if (bi.getType() != GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE) {
                    BufferedImage nue = new BufferedImage(bi.getWidth(),
                            bi.getHeight(),
                            GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
                    Graphics2D g2d = (Graphics2D) nue.createGraphics();

                    g2d.drawRenderedImage(bi,
                            AffineTransform.getTranslateInstance(0,
                                    0));
                    g2d.dispose();
                    bi = nue;
                }
            } catch (IOException ex) {
                img.accept(ex, null, file);
            }
        });
    }

    interface OpenConsumer {

        void accept(Exception ex, BufferedImage img, File origin);
    }

    @Override
    public void openMany(File[] files, Consumer<Set<File>> unopenedConsumer) {
        if (files.length == 0) {
            return;
        }
        final Iterator<File> iter = Arrays.asList(files).iterator();
        Map<File, BufferedImage> images = new LinkedHashMap<>(files.length);
        Set<File> unopened = Collections.synchronizedSet(new HashSet<>());
        OpenConsumer c = new OpenConsumer() {
            @Override
            public void accept(Exception t, BufferedImage u, File origin) {
                if (t != null) {
                    Exceptions.printStackTrace(t);
                    unopened.add(origin);
                } else if (u != null) {
                    images.put(origin, u);
                }
                if (iter.hasNext()) {
                    load(iter.next(), this);
                } else {
                    EventQueue.invokeLater(() -> {
                        for (Iterator<Map.Entry<File, BufferedImage>> it = images.entrySet().iterator(); it.hasNext();) {
                            Map.Entry<File, BufferedImage> e = it.next();
                            PaintTopComponent last;
                            try {
                                last = new PaintTopComponent(e.getValue(), e.getKey());
                                last.setDisplayName(e.getKey().getName());
                                last.open();
                                if (!it.hasNext()) {
                                    last.requestActive();
                                }
                            } catch (IOException ex) {
                                Exceptions.printStackTrace(ex);
                                unopened.add(e.getKey());
                            }
                        }
                        if (!unopened.isEmpty()) {
                            unopenedConsumer.accept(unopened);
                        }
                    });
                }
            }
        };
        load(iter.next(), c);
    }

    @Override
    public boolean canOpen(File f) {
        if (!f.isFile()) {
            return false;
        }
        String nm = f.getName();
        ImageIO.getReaderFileSuffixes();
        int ix = nm.lastIndexOf('.'); //NOI18N
        if (ix != -1 && ix != nm.length() - 1) {
            String s = nm.substring(ix + 1);
            return (fmts.contains(s.toLowerCase())
                    || fmts.contains(s.toUpperCase())) && f.isFile()
                    && f.canRead();
        }
        return false;
    }

}
