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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import org.netbeans.api.visual.widget.Scene;
import org.netbeans.api.visual.widget.Scene.SceneListener;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.StatusDisplayer;
import org.openide.awt.UndoRedo;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.nodes.Node;
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
    "OPENING_IMAGES=Opening {0} images"
})
@TopComponent.Description(preferredID = "PaintTopComponent",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@TopComponent.Registration(mode = "editor", openAtStartup = false)
@ActionID(category = "Window", id = "org.netbeans.paintui.PaintTopComponent")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(displayName = "#CTL_PaintAction",
        preferredID = "PaintTopComponent")
public final class PaintTopComponent extends TopComponent implements
        ChangeListener, LookupListener, IO, Selectable, Resizable {

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

    public PaintTopComponent() {
        this(new PictureScene());
        init();
    }

    @Override
    public void resizePicture(int w, int h) {
        for (LayerImplementation l : canvas.getPicture().getLayers()) {
            l.resize(w, h);
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
        setDisplayName(origin.getName());
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
        String displayName = NbBundle.getMessage(
                PaintTopComponent.class,
                "UnsavedImageNameFormat", //NOI18N
                new Object[]{new Integer(ct++)}
        );
        setDisplayName(displayName);
        PictureImplementation picture = canvas.getPicture();
        stateChanged(new ChangeEvent(picture));
        setPreferredSize(new Dimension(500, 500));

        setLayout(new BorderLayout());
        InnerPanel inner = new InnerPanel(canvas.createView(), canvas);
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

        InnerPanel(JComponent inner, Scene scene) {
            this.inner = inner;
            add(inner);
            setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        public Dimension getPreferredSize() {
            return prefSize == null
                    ? inner.getPreferredSize() : new Dimension(prefSize);
//            return inner.getPreferredSize();
        }

        @Override
        public void doLayout() {
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
//        /throw new UnsupportedOperationException("Not yet implemented");
        invalidate();
        revalidate();
        repaint();
        canvas.getScene().validate();
    }

    @Override
    public UndoRedo getUndoRedo() {
        return undoManager;
    }

    private LayerImplementation lastActiveLayer = null;

    @Override
    public void stateChanged(ChangeEvent e) {
        PI picture = canvas.getPicture();
        LayerImplementation layerImpl = picture.getActiveLayer();
        if (layerImpl != lastActiveLayer && lastActiveLayer != null) {
            SurfaceImplementation lastSurface = lastActiveLayer.getSurface();
            if (lastSurface != null && lastActiveLayer != null) {
                //Force it to dispose its undo data
                lastActiveLayer.getSurface().setTool(null);
            }
        }
        //What the heck was this?
        // Ah, a way for layers to provide their own palettes
        /*
        Collection <? extends TopComponent> tcs = layerImpl.getLookup().lookupAll (TopComponent.class);
        for (TopComponent tc : tcs) {
            tc.open();
            tc.requestVisible();
        }
        layerTopComponents.removeAll(tcs);
        for (TopComponent old : layerTopComponents) {
            old.close();
        }
        layerTopComponents.clear();
        for (TopComponent nue : tcs) {
            layerTopComponents.add (nue);
        }
         */

        List l = new ArrayList(10);
        l.add(this);
        l.addAll(Arrays.asList(this, canvas.getZoom(),
                getUndoRedo(), picture.getPicture()));
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
            UIContextLookupProvider.setLayerAndSelection(layer.getLookup(),
                    canvas.getLookup());
        } else {
            UIContextLookupProvider.setLayerAndSelection(canvas.getLookup(), null);
        }
//        UIContextLookupProvider.setLayer(layerImpl.getLookup());
        System.err.println("Lookup contents set to " + UIContextLookupProvider.theLookup().lookupAll(Object.class));
        updateActiveTool();
    }

    public void save() throws IOException {
        if (this.file != null) {
            doSave(file);
        } else {
            saveAs();
        }
    }

    public void saveAs() throws IOException {
        JFileChooser ch = FileChooserUtils.getFileChooser("image");
        if (ch.showSaveDialog(this) == JFileChooser.APPROVE_OPTION
                && ch.getSelectedFile() != null) {

            File f = ch.getSelectedFile();
            if (!f.getPath().endsWith(".png")) { //NOI18N
                f = new File(f.getPath() + ".png"); //NOI18N
            }
            if (!f.exists()) {
                if (!f.createNewFile()) {
                    String failMsg = NbBundle.getMessage(
                            PaintTopComponent.class,
                            "MSG_SaveFailed", new Object[]{f.getPath()} //NOI18N
                    );
                    JOptionPane.showMessageDialog(this, failMsg);
                    return;
                }
            } else {
                String overwriteMsg = NbBundle.getMessage(
                        PaintTopComponent.class,
                        "MSG_Overwrite", new Object[]{f.getPath()} //NOI18N
                );
                if (JOptionPane.showConfirmDialog(this, overwriteMsg)
                        != JOptionPane.OK_OPTION) {

                    return;
                }
            }
            doSave(f);
        }
    }

    private void doSave(File f) throws IOException {
        BufferedImage img = canvas.toImage();
        ImageIO.write(img, "png", f);
        this.file = f;
        String statusMsg = NbBundle.getMessage(PaintTopComponent.class,
                "MSG_Saved", new Object[]{f.getPath()}); //NOI18N
        StatusDisplayer.getDefault().setStatusText(statusMsg);
        setDisplayName(f.getName());
        updateActivatedNode(f);
        StatusDisplayer.getDefault().setStatusText(NbBundle.getMessage(PaintTopComponent.class,
                "MSG_SAVED", f.getPath())); //NOI18N
    }

    @Override
    public void open() {
        //Rare case where we *do* want to do this
        super.open();
        requestActive();
    }

    @Override
    protected void componentActivated() {
        active = true;
        startListening();
        System.out.println("activated, update active tool");
        updateActiveTool();
        stateChanged(null);
        canvas.getView().requestFocus();
//        for (TopComponent tc : layerTopComponents) {
//            tc.open();
//        }
    }

    @Override
    protected void componentDeactivated() {
        active = false;
        stopListening();
        System.out.println("deactivated, set active tool null");
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
        updateActiveTool();
    }

    private void setActiveTool(Tool tool) {
        System.out.println("set active tool " + tool);
        canvas.setActiveTool(tool);
    }

    private void startListening() {
        System.out.println("  start listening for active tool changes");
        tools = Utilities.actionsGlobalContext().lookupResult(Tool.class);
        tools.addLookupListener(this);
    }

    private void stopListening() {
        System.out.println("  stop listening for active tool changes");
        tools.removeLookupListener(this);
        tools = null;
    }

    private void updateActiveTool() {
        System.out.println("update active tool");
        if (isActive()) {
            Collection<? extends Tool> oneOrNone = tools == null
                    ? Collections.emptySet() : tools.allInstances();
            setActiveTool(oneOrNone.isEmpty() ? null : (Tool) oneOrNone.iterator().next());
        } else {
            System.out.println("  not active, no update");
        }
    }

    @Override
    protected void componentClosed() {
        if (UIContextLookupProvider.lookup(PaintTopComponent.class) == this) {
            UIContextLookupProvider.set(new Object[0]);
            if (lastActiveLayer != null) {
                lastActiveLayer.getSurface().setTool(null);
            }
        }

        super.componentClosed();
        undoManager.discardAllEdits();
    }

    public void reload() throws IOException {
    }

    public boolean canReload() {
        Node[] n = getActivatedNodes();
        if (n.length == 1) {
            DataObject dob = (DataObject) n[0].getCookie(DataObject.class);
            return dob.getPrimaryFile().isValid();
        }
        return false;
    }

    boolean isActive() {
        return UIContextLookupProvider.lookup(PaintTopComponent.class)
                == this;
//        return TopComponent.getRegistry().getActivated() == this;
    }
}
