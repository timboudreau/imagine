/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.minidesigner;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.spi.tool.ToolElement;
import net.java.dev.imagine.api.image.Surface;
import org.imagine.geometry.EqPointDouble;
import org.imagine.nbutil.SingleUseWindow;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.netbeans.paint.api.components.explorer.Customizable;
import org.netbeans.paint.tools.spi.PathCreator;
import static org.netbeans.paint.tools.spi.PathCreator.REGISTRATION_PATH;
import org.openide.modules.Places;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.RequestProcessor;

/**
 *
 * @author Tim Boudreau
 */
@ToolElement(folder = REGISTRATION_PATH, name = "Path Segment Designer", position = 200)
public class GenericPathCreator implements PathCreator, Customizable {

    private PathSegmentModel mdl = new PathSegmentModel();

    {
        initDefaults(mdl);
    }

    private static final Map<Surface, PathSegmentModel> modelForSurface = new WeakHashMap<>();
    private static final String CACHE_SUBDIR = "pathsegments";
    private static final String LASTFILE = "last.pathsegment";

    @Override
    public void init(Surface surface) {
        PathSegmentModel mdl = modelForSurface.get(surface);
        if (mdl != null) {
            this.mdl = mdl;
        } else {
            modelForSurface.put(surface, this.mdl);
            load(this.mdl);
        }
    }

    private void load(PathSegmentModel mdl) {
        if (bufferRef.get() != null) {
            return;
        }
        File fl = Places.getCacheSubdirectory(CACHE_SUBDIR);
        ByteBuffer buf = null;
        synchronized (GenericPathCreator.class) {
            if (fl.exists()) {
                File last = new File(fl, LASTFILE);
                if (last.exists() && last.length() > 0) {
                    try (FileChannel channel = FileChannel.open(last.toPath(), StandardOpenOption.READ)) {
                        int len = (int) channel.size();
                        if (len < 70 * 1024) {
                            // wtf, should be tiny
                            buf = ByteBuffer.allocate(len);
                            channel.read(buf);
                            buf.flip();
                        }
                    } catch (IOException ex) {
                        Exceptions.printStackTrace(ex);
                        last.delete();
                    }
                }
            }
        }
        if (buf != null) {
            try {
                PathSegmentModel loaded = PathSegmentModel.read(buf);
                mdl.replaceFrom(loaded);
                System.out.println("Loaded model from " + new File(fl, LASTFILE).getAbsolutePath());
            } catch (IOException ex) {
                Logger.getLogger(GenericPathCreator.class.getName()).log(Level.INFO, "Exception loading cached model", ex);
            }
        }
    }

    @Override
    public void reset() {
        mdl.clear();
        initDefaults(mdl);
    }
    
    private static void initDefaults(PathSegmentModel mdl) {
        mdl.addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 25, 10, 50, 0);
        mdl.addQuadTo(new EqPointDouble(0, 0), new EqPointDouble(100, 0), 75, -10, 100, 0);
    }

    private static boolean isDefault(PathSegmentModel mdl) {
        PathSegmentModel nue = new PathSegmentModel();
        initDefaults(nue);
        return nue.equals(mdl);
    }

    @Override
    public Shape create(boolean close, boolean commit, List<EqPointDouble> points, EqPointDouble nextProposal) {
        if (points.size() < 2) {
            return new Rectangle();
        }
        Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        EqPointDouble start = points.get(0);
        path.moveTo(start.x, start.y);

        for (int i = 1; i < points.size(); i++) {
            EqPointDouble end = points.get(i);
            double max = mdl.maxDestinationScale();
            mdl.apply(path, start, end, i == 1, 1D / max);
            start = end;
        }
        return path;
    }

    @Override
    public JComponent getCustomizer() {
        return c.getComponent();
    }

    private final C c = new C();
    private static final RequestProcessor SAVE_THREAD_POOL = new RequestProcessor("gp-save", 1);
    private final AtomicReference<ByteBuffer> bufferRef = new AtomicReference<>();

    private void enqueueSave(PathSegmentModel mdl) {
        if (!mdl.isEmpty() && !isDefault(mdl)) {
            int len = mdl.sizeInBytes();
            ByteBuffer buf = ByteBuffer.allocate(len);
            mdl.write(buf);
            bufferRef.set(buf);
            saveTask.schedule(5000);
        }
    }

    private void doSave() {
        ByteBuffer buf = bufferRef.getAndSet(null);
        if (buf != null) {
            File dir = Places.getCacheSubdirectory(CACHE_SUBDIR);
            synchronized (GenericPathCreator.class) {
                if (!dir.exists()) {
                    if (!dir.mkdir()) {
                        throw new IllegalStateException("Could not create " + dir.getAbsolutePath());
                    }
                }
                File last = new File(dir, LASTFILE);
                try (FileChannel outputChannel = FileChannel.open(last.toPath(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)) {
                    buf.flip();
                    outputChannel.write(buf);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
                System.out.println("Saved last model to " + last.getAbsolutePath());
            }
        }
    }

    private final RequestProcessor.Task saveTask = SAVE_THREAD_POOL.create(this::doSave);

    @Messages({"Path=Path",
        "PathSegment=Path Segment"
    })
    final class C implements Customizer<PathSegmentModel> {

        private Reference<JComponent> ref;

        @Override
        public JComponent getComponent() {
            if (ref != null) {
                JComponent cus = ref.get();
                if (cus != null) {
                    return cus;
                }
            }
            GenericDesignCustomizer cus = new GenericDesignCustomizer(mdl);
            cus.setMinimumSize(new Dimension(400, 300));
            cus.onEdit(mdl -> {
                System.out.println("PopOut");
                JPanel panel = new JPanel(new VerticalFlowLayout());
                GenericDesignCustomizer popoutCustomizer = new GenericDesignCustomizer(mdl);
                JScrollPane pane = new JScrollPane(popoutCustomizer.satelliteView());
                JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, popoutCustomizer,
                        pane);
                panel.add(split);
                SingleUseWindow.popOut(Bundle.PathSegment(), panel);
            });
            cus.onHide(GenericPathCreator.this::enqueueSave);
            ref = new SoftReference<>(cus);
            return cus;
        }

        @Override
        public String getName() {
            return Bundle.Path();
        }

        @Override
        public PathSegmentModel get() {
            return mdl;
        }
    }
}
