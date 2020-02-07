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
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 *
 * @author Timothy Boudreau
 */
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

    private PaintTopComponent(PictureScene canvas) {
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
        JScrollPane pane = new JScrollPane(new InnerPanel(canvas.createView()));
        pane.getViewport().setScrollMode(JViewport.BLIT_SCROLL_MODE);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setViewportBorder(BorderFactory.createMatteBorder(1, 0, 0, 0,
                UIManager.getColor("controlShadow"))); //NOI18N
        add(pane, BorderLayout.CENTER);
        undoManager.setLimit(UNDO_LIMIT);
        picture.addChangeListener(this);
    }
    private static final int UNDO_LIMIT = 15;

    static class InnerPanel extends JComponent {

        private final JComponent inner;

        InnerPanel(JComponent inner) {
            this.inner = inner;
            add(inner);
            setBorder(BorderFactory.createEmptyBorder());
        }

        public Dimension getPreferredSize() {
            return inner.getPreferredSize();
        }

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
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_NEVER;
    }

    @Override
    public String preferredID() {
        return this.file == null ? "Image" : this.file.getName(); //NOI18N
    }

    public void selectAll() {
        PictureImplementation l = canvas.getPicture();
        if (l.getActiveLayer() == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        LayerImplementation layer = l.getActiveLayer();
        Selection s = layer.getLookup().lookup(Selection.class);
        s.add(new Rectangle(l.getSize()), Op.REPLACE);
        repaint();
    }

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

    private final UndoRedo.Manager undoManager = new UndoMgr();

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

    public void stateChanged(ChangeEvent e) {
        PictureImplementation picture = canvas.getPicture();
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

    private boolean active;

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
        setActiveTool(null);
//        for (TopComponent tc : layerTopComponents) {
//            tc.close();
//        }
    }

    boolean firstTime = true;

    @Override
    protected void componentShowing() {
        PI p = canvas.getPicture();
        final int layerCount = p.getLayers().size();
        if (p.hibernated()) {
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
        }
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

    private Lookup.Result<Tool> tools = null;

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
