/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.paintui;

import com.mastfrog.function.throwing.io.IOBiFunction;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import net.java.dev.imagine.ui.common.UndoMgr;
import net.java.dev.imagine.ui.common.UIContextLookupProvider;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.api.selection.Selection.Op;
import net.dev.java.imagine.api.tool.Tool;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.netbeans.paint.api.editor.IO;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.spi.image.PictureImplementation;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import net.java.dev.imagine.ui.actions.spi.Resizable;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.paint.api.components.FileChooserUtils;
import org.netbeans.paintui.PictureScene.PI;
import net.java.dev.imagine.ui.actions.spi.Selectable;
import net.java.dev.imagine.ui.common.ImageEditorFactory;
import net.java.dev.imagine.ui.common.RecentFiles;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.ContextLog;
import org.imagine.editor.api.ImageEditor;
import org.imagine.editor.api.Zoom;
import org.netbeans.api.visual.widget.Scene.SceneListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.UndoRedo;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 *
 * @author Timothy Boudreau
 */
@Messages({
    // XXX pasted in from bundle file - a number of these are probably
    // unused, or if used, are used from other modules and should be moved
    // there
    "# {0} - formatName",
    "UnsavedImageNameFormat=Image {0}",
    "# {0} - fileName",
    "MSG_SaveFailed=Could not write to file {0}",
    "# {0} - fileName",
    "MSG_Overwrite={0} exists.  Overwrite?",
    "# {0} - fileName",
    "MSG_Saved=Saved image to {0}",
    "LBL_CreateSelection=Create Selection",
    "LBL_ClearSelection=Clear Selection",
    "LBL_ChangeSelection=Change Selection",
    "bounds=Bounds",
    "visible=Visible",
    "opacity=Opacity",
    "size=Size",
    "name=Name",
    "# {0} - changed",
    "LBL_Change=Change {0}",
    "MSG_ADD_NEW_LAYER=Add New Layer",
    "# {0} - layerName",
    "MSG_DUPLICATE_LAYER=Duplicate Layer {0}",
    "MSG_FLATTEN_LAYERS=Flatten Layers",
    "# {0} - layerName",
    "LAYER_NAME=Layer {0}",
    "# {0} - layerName",
    "# {1} - position",
    "MSG_MOVE_LAYER=Move {0} to Position {1}",
    "# {0} - layerName",
    "MSG_DELETE_LAYER=Delete Layer {0}",
    "# {0} - layerName",
    "MSG_ACTIVATE_LAYER=Activate Layer {0}",
    "MSG_CLEAR_ACTIVE_LAYER=Clear active layer",
    "MSG_UNHIBERNATING=Reloading images",
    "# {0} - fileName",
    "MSG_SAVED=Saved {0}",
    "LBL_PASTED_LAYER=From Clipboard",
    "LAYER_CLIPBOARD_NAME=Layer",
    "# {0} - cutTarget",
    "CUT=Cut {0}",
    "LBL_UNKNOWN_UNDOABLE_OP=Unknown Operation",
    "CTL_PaintAction=Image",
    "TRANSPARENT=Transparent",
    "WHITE=White",
    "RASTER_IMAGES=Raster Images (png, jpg, gif, ...)",
    "# {0} - imageCount",
    "OPENING_IMAGES=Opening {0} images",
    "# {0} - fileExtension",
    "UNKNOWN_FILE_EXTENSION=Not a known image file extension: {0}",
    "TTL_UNKNOWN_FILE_EXTENSION=Could Not Save"
})
@TopComponent.Description(preferredID = "PaintTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_NEVER)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.netbeans.paintui.PaintTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_PaintAction",
        preferredID = "PaintTopComponent")
public final class PaintTopComponent extends TopComponent implements
        ChangeListener, LookupListener, IO, Selectable, Resizable, ImageEditor {

    private final PictureScene canvas; //The component the user draws on
    private static int ct = 0; //A counter we use to provide names for new images
    private File file;
    private final UndoRedo.Manager undoManager = new UndoMgr();
    private static final int UNDO_LIMIT = 15;
    private boolean active;
    boolean firstTime = true;
    private Lookup.Result<Tool> tools = null;
    public static DataFlavor LAYER_DATA_FLAVOR = new DataFlavor(Layer.class,
            NbBundle.getMessage(PI.class,
                    "LAYER_CLIPBOARD_NAME")); //NOI18N

    private static final ContextLog CLOG = ContextLog.get("selection");

    static int ids = 0;
    private final int id = ids++;

    public PaintTopComponent() {
        this(new PictureScene());
        init();
    }

    @Override
    public Dimension getAvailableSize() {
        return getSize();
    }

    @Override
    public AspectRatio getPictureAspectRatio() {
        return canvas.aspectRatio();
    }

    public Zoom getZoom() {
        return canvas.getZoom();
    }

    @Override
    public void resizePicture(int w, int h, boolean resizeCanvasOnly) {
        for (LayerImplementation l : canvas.getPicture().getLayers()) {
            l.resize(w, h, resizeCanvasOnly);
        }
        pictureResized(w, h);
        invalidate();
        revalidate();
        repaint();
    }

    @Override
    public boolean canInvertSelection() {
        Selection sel = getLookup().lookup(Selection.class);
        return sel != null && !sel.isEmpty();
    }

    public static PaintTopComponent tcFor(Picture p) {
        Set<TopComponent> tcs = TopComponent.getRegistry().getOpened();
        for (TopComponent tc : tcs) {
            if (tc instanceof PaintTopComponent) {
                PaintTopComponent ptc = (PaintTopComponent) tc;
                if (ptc.canvas.getPicture().getPicture() == p) {
                    return ptc;
                }
            }
        }
        return null;
    }

    public PaintTopComponent(BufferedImage img, File origin) throws IOException {
        this(new PictureScene(img));
        this.canvas.picture().getPicture().associateFile(origin.toPath());
        this.file = origin;
        updateActivatedNode(origin);
        init();
        setDisplayName(fileName(origin.toPath()));
    }

    public PaintTopComponent(Dimension dim, BackgroundStyle backgroundStyle) {
        this(new PictureScene(dim, backgroundStyle));
        init();
    }

    private void init() {
        undoManager.discardAllEdits(); //XXX because we paint it white
        setOpaque(true);
        setBackground(Color.WHITE);
    }

    private void updateActivatedNode(File origin) throws IOException {
        FileObject fob = FileUtil.toFileObject(FileUtil.normalizeFile(origin));
        if (fob != null) {
            DataObject dob = DataObject.find(fob);
            setActivatedNodes(new Node[]{dob.getNodeDelegate()});
        }
    }

    @SuppressWarnings("LeakingThisInConstructor")
    public PaintTopComponent(PictureScene canvas) {
        this.canvas = canvas;
        PictureImplementation picture = canvas.getPicture();
        String displayName;
        Path pth = picture.getPicture().associatedFile();
        if (pth != null) {
            file = pth.toFile();
            displayName = fileName(pth);
            try {
                updateActivatedNode(file);
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        } else {
            displayName = NbBundle.getMessage(
                    PaintTopComponent.class,
                    "UnsavedImageNameFormat", //NOI18N
                    new Object[]{ct++}
            );
        }
        setDisplayName(displayName);
        stateChanged(new ChangeEvent(picture));

        setLayout(new BorderLayout());
        InnerPanel inner = new InnerPanel(canvas.createView(), canvas, canvas.zoom());
        canvas.addSceneListener(inner);
        JScrollPane pane = new JScrollPane(inner);
        pane.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        pane.getViewport().setDoubleBuffered(false);

        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setViewportBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("controlShadow"))); //NOI18N
        add(pane, BorderLayout.CENTER);
        undoManager.setLimit(UNDO_LIMIT);
        picture.addChangeListener(this);
    }

    static class InnerPanel extends JComponent implements SceneListener {

        private final JComponent inner;
        private Dimension prefSize;
        private final PictureScene scene;
        private final Zoom zoom;

        InnerPanel(JComponent inner, PictureScene scene, Zoom zoom) {
            this.inner = inner;
            add(inner);
            setBorder(BorderFactory.createEmptyBorder());
            this.scene = scene;
            this.zoom = zoom;
        }

        @Override
        public Dimension getPreferredSize() {
            return prefSize == null
                    ? inner.getPreferredSize() : new Dimension(prefSize);
//            return inner.getPreferredSize();
        }

        private boolean pristine = true;

        private void ensureZoom() {
            if (pristine) {
                Rectangle r = scene.getBounds();
                if (r == null) {
                    scene.validate();
                    r = scene.getBounds();
                }
                if (r == null) {
                    return;
                }
                // If we are opening an extremely small or large image,
                // adjust the zoom level to fit the image within the visible area
                Dimension pictureSize = r.getSize();
                Dimension currentSize = getParent().getSize();
                System.out.println("\n\nPristine validate " + currentSize + " and " + pictureSize);
                if (currentSize.width != 0 && currentSize.height != 0
                        && pictureSize.width != 0 && pictureSize.height != 0) {
                    pristine = false;
                    int myArea = currentSize.width * currentSize.height;
                    int pictureArea = pictureSize.width * pictureSize.height;
                    if (pictureArea < myArea / 4) {
                        if (pictureSize.width > pictureSize.height) {
                            double widthFactor = (double) currentSize.width / pictureSize.width;
                            zoom.setZoom((float) widthFactor);
                            System.out.println("\n\nSCALE TO WIDTH " + widthFactor + "\n\n");
                        } else {
                            double heightFactor = (double) currentSize.height / pictureSize.height;
                            zoom.setZoom((float) heightFactor);
                            System.out.println("\n\nSCALE TO HEIGHT " + heightFactor + "\n\n");
                        }
                    }
                }
            }
        }

        @Override
        public void doLayout() {
            ensureZoom();
            Dimension d = inner.getPreferredSize();
            int offX = 0;
            int offY = 0;
            if (d.width < getWidth()) {
                offX = (getWidth() - d.width) / 2;
            }
            if (d.height < getHeight()) {
                offY = (getHeight() - d.height) / 2;
            }
            inner.setBounds(offX, offY, d.width, d.height);
        }

        @Override
        public void sceneRepaint() {
        }

        @Override
        public void sceneValidating() {
        }

        @Override
        public void sceneValidated() {
            prefSize = inner.getPreferredSize();
        }
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }

    @Override
    public String preferredID() {
        return this.file == null ? "Image" : this.file.getName(); //NOI18N
    }

    @Override
    public void selectAll() {
        PictureImplementation l = canvas.getPicture();
        if (l.getActiveLayer() == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        LayerImplementation layer = l.getActiveLayer();
        Selection s = layer.getLookup().lookup(Selection.class);
        if (s != null) {
            s.add(new Rectangle(l.getSize()), Op.REPLACE);
        }
        repaint();
    }

    @Override
    public void clearSelection() {
        PictureImplementation l = canvas.getPicture();
        if (l.getActiveLayer() == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        LayerImplementation layer = l.getActiveLayer();
        Selection s = layer.getLookup().lookup(Selection.class);
        if (s != null) {
            s.clear();
            repaint();
        }
    }

    @Override
    public void invertSelection() {
        PictureImplementation l = canvas.getPicture();
        if (l.getActiveLayer() == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        LayerImplementation layer = l.getActiveLayer();
        Selection s = layer.getLookup().lookup(Selection.class);
        if (s != null) {
            Dimension d = l.getSize();
            s.invert(new Rectangle(0, 0, d.width, d.height));
            repaint();
        }
    }

    public void pictureResized(int width, int height) {
        canvas.pictureResized(new Dimension(width, height));
        invalidate();
        revalidate();
        repaint();
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoManager;
    }

    private LayerImplementation lastActiveLayer = null;

    private void clog(String msg) {
        CLOG.log("PTC-" + id + ": " + msg);
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        PI picture = canvas.getPicture();
        LayerImplementation layerImpl = picture.getActiveLayer();
        if (layerImpl != lastActiveLayer && lastActiveLayer != null) {
            SurfaceImplementation lastSurface = lastActiveLayer.getSurface();
            if (lastSurface != null && lastActiveLayer != null) {
                //Force it to dispose its undo data
                clog("stateChanged clear surface tool");
                lastSurface.setTool(null);
            }
        }
        List<Object> l = new ArrayList<>(10);
        l.add(this);
        l.addAll(Arrays.asList(this, canvas.getZoom(),
                getUndoRedo(), picture.getPicture(),
                picture.aspectRatio()));
        Layer layer = null;
        Selection<?> sel = null;
        if (layerImpl != null) {
            l.add(picture.getPicture().getLayers());
            layer = layerImpl.getLookup().lookup(Layer.class);
            if (layer != null) {
                l.add(layer);
            }
            sel = layerImpl.getLookup().lookup(Selection.class);
            if (sel != null) {
                l.add(sel);
            }
            Surface surf
                    = layerImpl.getLookup().lookup(Surface.class); //XXX what is this?

            if (surf != null) {
                l.add(surf);
            }
        }
        if (layerImpl != null) {
            l.addAll(layerImpl.getLookup().lookupAll(Object.class)); //XXX use PRoxyLookup properly
        }
        UIContextLookupProvider.set(l);
        if (layer != null) {
            clog("stateChanged set layer and selection with " + l.size() + " objects "
                    + ", layer lookup and canvas lookup");
            UIContextLookupProvider.setLayerAndSelection(layer.getLookup(),
                    canvas.getLookup());
        } else {
            clog("stateChanged set layer and selection with " + l.size() + " objects "
                    + " canvas lookup and NO layer lookup");
            UIContextLookupProvider.setLayerAndSelection(canvas.getLookup(), null);
        }
//        UIContextLookupProvider.setLayer(layerImpl.getLookup());
//        System.err.println("Lookup contents set to " + UIContextLookupProvider.theLookup().lookupAll(Object.class));
        updateActiveTool();
    }

    public void save(BiConsumer<Exception, Path> c) {
        if (this.file != null) {
            IO_POOL.submit(() -> {
                try {
                    doSave(file);
                    c.accept(null, file.toPath());
                } catch (Exception ex) {
                    c.accept(ex, null);
                }
            });
        } else {
            saveAs(c);
        }
    }

    @Override
    public void saveAs(BiConsumer<Exception, Path> c) {
        JFileChooser ch = FileChooserUtils.getFileChooser("image");
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                && ch.getSelectedFile() != null) {
            File f = ch.getSelectedFile();
            if (!f.exists()) {
                try {
                    if (!f.createNewFile()) {
                        String failMsg = NbBundle.getMessage(
                                PaintTopComponent.class,
                                "MSG_SaveFailed", new Object[]{f.getPath()} //NOI18N
                        );
                        EventQueue.invokeLater(() -> {
                            JOptionPane.showMessageDialog(this, failMsg);
                            c.accept(null, null);
                        });
                        return;
                    }
                } catch (IOException ex) {
                    c.accept(ex, null);
                }
            } else {
                String overwriteMsg = NbBundle.getMessage(
                        PaintTopComponent.class,
                        "MSG_Overwrite", new Object[]{f.getPath()} //NOI18N
                );
                if (JOptionPane.showConfirmDialog(this, overwriteMsg)
                        != JOptionPane.OK_OPTION) {
                    c.accept(null, null);
                    return;
                }
            }
            File ff = f;
            IO_POOL.submit(() -> {
                File realFile;
                try {
                    realFile = doSave(ff);
                } catch (IOException ex) {
                    EventQueue.invokeLater(() -> {
                        c.accept(ex, null);
                    });
                    return;
                }
                EventQueue.invokeLater(() -> {
                    if (realFile == null) {
                        c.accept(null, null);
                    } else {
                        Path p = realFile.toPath();
                        canvas.picture().getPicture().associateFile(p);
                        setDisplayName(fileName(p));
                        RecentFiles.getDefault().add(RecentFiles.Category.IMAGE, p);
                        c.accept(null, p);
                    }
                });
            });
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

    private File findFormatAndSaveImage(File f, IOBiFunction<String, File, File> io) throws IOException {
        String ext = extensionOf(f);
        if (!f.getName().endsWith(ext)) {
            f = new File(f.getPath() + '.' + ext);
        }
        boolean hasReader = ImageIO.getImageReadersBySuffix(ext).hasNext();
        if (!hasReader) {
            JOptionPane.showMessageDialog(this, Bundle.UNKNOWN_FILE_EXTENSION(ext),
                    Bundle.TTL_UNKNOWN_FILE_EXTENSION(), JOptionPane.ERROR_MESSAGE);
            return null;
        }
        return io.apply(ext, f);
    }

    private static String extensionOf(File file) {
        String nm = file.getName();
        int len = nm.length();
        int ix = nm.lastIndexOf('.');
        if (ix != len - 1 && ix > 0) {
            return nm.substring(ix + 1, len);
        }
        return "png";
    }

    private File doSave(File f) throws IOException {
        BufferedImage img = canvas.toImage();
        return findFormatAndSaveImage(f, (fmt, file) -> {
            ImageIO.write(img, fmt, file);
            String statusMsg = NbBundle.getMessage(PaintTopComponent.class,
                    "MSG_Saved", new Object[]{f.getPath()}); //NOI18N
            StatusDisplayer.getDefault().setStatusText(statusMsg);
            this.file = f;
            setDisplayName(fileName(f.toPath()));
            updateActivatedNode(f);
            return file;
        });
    }

    @Override
    public void open() {
        //Rare case where we *do* want to do this
        super.open();
        requestActive();
    }

    @Override
    protected void componentActivated() {
        clog("componentActivated");
        lastActive = new WeakReference<>(this);
        active = true;
        startListening();
        updateActiveTool();
        stateChanged(null);
        canvas.getView().requestFocus();
//        for (TopComponent tc : layerTopComponents) {
//            tc.open();
//        }
    }

    @Override
    protected void componentDeactivated() {
        clog("componentDeactivated");
        active = false;
        stopListening();
//        setActiveTool(null);
//        for (TopComponent tc : layerTopComponents) {
//            tc.close();
//        }
    }

    @Override
    protected void componentShowing() {
        PI p = canvas.getPicture();
        final int layerCount = p.getLayers().size();
//        if (p.hibernated()) {
        p.wakeup(false, new Runnable() {
            int ct = 0;
            ProgressHandle h;

            public void run() {
                if (EventQueue.isDispatchThread()) {
                    invalidate();
                    revalidate();
                    repaint();
                    return;
                }
                ct++;
                if (ct == 1) {
                    h = ProgressHandleFactory.createHandle(NbBundle.getMessage(PaintTopComponent.class,
                            "MSG_UNHIBERNATING")); //NOI18N
                    h.start();
                    h.switchToDeterminate(layerCount);
                }
                if (h != null) {
                    h.progress(ct);
                }
                if (ct == layerCount - 1) {
                    if (h != null) {
                        h.finish();
                    }
                    EventQueue.invokeLater(this);
                }
            }
        });
//        }
    }

    @Override
    protected void componentHidden() {
        canvas.getPicture().hibernate(false);
    }

    @Override
    public void resultChanged(LookupEvent lookupEvent) {
        clog("Update active tool from lookupEvent");
        updateActiveTool();
    }

    private void setActiveTool(Tool tool) {
        clog("Set active tool " + tool);
//        System.out.println("set active tool " + tool);
        canvas.setActiveTool(tool);
    }

    private void startListening() {
//        System.out.println("  start listening for active tool changes");
        tools = Utilities.actionsGlobalContext().lookupResult(Tool.class);
        tools.addLookupListener(this);
    }

    private void stopListening() {
        if (tools != null) {
            tools.removeLookupListener(this);
        }
        tools = null;
    }

    private void updateActiveTool() {
        if (isActive() && tools != null) {
            Collection<? extends Tool> oneOrNone = tools == null
                    ? Collections.emptySet() : tools.allInstances();
            Tool to = oneOrNone.isEmpty() ? null : (Tool) oneOrNone.iterator().next();
            clog("Update active tool " + to);
            setActiveTool(to);
        } else {
            System.out.println("Not active, no update active tool");
        }
    }

    @Override
    protected void componentClosed() {
        stopListening();
        if (isActive()) {
            UIContextLookupProvider.set(new Object[0]);
            if (lastActiveLayer != null) {
                lastActiveLayer.getSurface().setTool(null);
            }
            lastActiveLayer = null;
            UIContextLookupProvider.setLayerAndSelection(Lookup.EMPTY, Lookup.EMPTY);
        }
        canvas.detachForClose();
        // Ensure no soft memory leaks
        undoManager.discardAllEdits();
        canvas.removeChildren();
        active = false;
        removeAll();
        super.componentClosed();
    }

    @Override
    public void reload(BiConsumer<Exception, Path> bi) {
        PI pic = canvas.getPicture();
        Path pth = pic.getPicture().associatedFile();
        if (pth != null) {
            if (!Files.exists(pth)) {
                bi.accept(null, null);
            }
            IO_POOL.submit(() -> {
                File file = pth.toFile();
                for (ImageEditorFactory factory : Lookup.getDefault().lookupAll(ImageEditorFactory.class)) {
                    if (factory.canOpen(file)) {
                        if (factory.openExisting(file)) {
                            RecentFiles.getDefault().add(factory.category(), file.toPath());
                            EventQueue.invokeLater(() -> {
                                undoManager.discardAllEdits();
                                close();
                            });
                        } else {
                            EventQueue.invokeLater(() -> {
                                bi.accept(null, null);
                            });
                        }
                    }
                }
            });
        }
    }

    @Override
    public boolean canReload() {
        Node[] n = getActivatedNodes();
        if (n.length == 1) {
            DataObject dob = (DataObject) n[0].getCookie(DataObject.class);
            return dob.getPrimaryFile().isValid();
        }
        return false;
    }

    boolean isActive() {
        return lastActive != null && lastActive.get() == this
                || UIContextLookupProvider.lookup(PaintTopComponent.class)
                == this;
//        return TopComponent.getRegistry().getActivated() == this;
    }

    private static Reference<PaintTopComponent> lastActive;
}
