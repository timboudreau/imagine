package net.java.dev.imagine.spi.image.support;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.selection.PictureSelection;
import net.dev.java.imagine.api.selection.Selection;
import net.java.dev.imagine.api.image.Hibernator;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import static net.java.dev.imagine.spi.image.PictureImplementation.POSITION_BOTTOM;
import static net.java.dev.imagine.spi.image.PictureImplementation.POSITION_TOP;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.netbeans.paint.api.editing.LayerFactory;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.util.RasterConverter;
import org.openide.util.ChangeSupport;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Base class for PictureImplementation which implements common logic.
 *
 * @author Tim Boudreau
 */
public abstract class AbstractPictureImplementation extends PictureImplementation {

    private final AtomicBoolean hibernated = new AtomicBoolean();
    protected static final RequestProcessor HIBERNATOR_POOL = new RequestProcessor("Hibernate", 3, true);
    protected final PictureSelection psel = new PictureSelection();
    private final AtomicReference<LayersState> state = new AtomicReference<>(new LayersState(this));
    private final Obs observer = new Obs();
    private final ChangeSupport changes = new ChangeSupport(this);
    private final UndoSupport undoables = new UndoSupport(
            changes::fireChange, this::state);

    protected AbstractPictureImplementation(Dimension dim) {
        state.get().setSize(dim);
    }

    public final PictureSelection getSelection() {
        return psel;
    }

    /**
     * Turns on undo support - enable at the end
     */
    protected final void initialized() {
        undoables.enable();
    }

    protected boolean initFromImage(BufferedImage img) {
        LayerImplementation layer = getActiveLayer();
        if (layer != null) {
            Surface surface = layer.getLayer().getSurface();
            if (surface != null) {
                Graphics2D g = layer.getSurface().getGraphics();
                try {
                    g.drawRenderedImage(img, AffineTransform.getTranslateInstance(0, 0));
                    return true;
                } catch (NullPointerException e) {
                    Exceptions.printStackTrace(e);
                    return false;
                } finally {
                    g.dispose();
                }
            }
        }
        return false;
    }

    protected final void updateSize(Dimension dim) {
        state().setSize(dim);
    }

    protected final LayerImplementation getLayer(int ix) {
        return state().getLayer(ix);
    }

    @Override
    public final List<LayerImplementation> getLayers() {
        return Collections.unmodifiableList(Arrays.asList(state().getLayers()));
    }

    @Override
    public final LayerImplementation getActiveLayer() {
        return state().getActiveLayer();
    }

    LayersState.Observer getObserver(LayersState requester) {
        if (requester == state.get()) {
            return observer;
        }
        System.out.println("DID NOT GET AN OBSERVER");
        return null;
    }

    LayersState state() {
        return state.get();
    }

    void setState(LayersState st) {
        LayersState old;
        if ((old = state.getAndSet(st)) != st) {
            LayerImplementation oldActive = old == null ? null
                    : old.getActiveLayer();
            LayerImplementation newActive = st.getActiveLayer();
            if (old == null || !old.isSameLayers(st)) {
                List<LayerImplementation> oldLayers = old == null ? Collections.emptyList()
                        : old.layers();
                List<LayerImplementation> newLayers = st.layers();

                Set<LayerImplementation> removed = new HashSet<>(oldLayers);
                removed.removeAll(newLayers);
                Set<LayerImplementation> added = new HashSet<>(newLayers);
                added.removeAll(oldLayers);
                Set<LayerImplementation> retained = new HashSet<>(newLayers);
                retained.retainAll(oldLayers);
                observer.layersChanged(oldLayers, newLayers, removed, added, retained, oldActive, newActive);
            } else {
                if (oldActive != newActive) {
                    observer.activeLayerChanged(oldActive, newActive);
                }
            }
        }
    }

    UndoSupport undo() {
        return undoables;
    }

    protected Set<DataFlavor> copyTypes() {
        return Collections.emptySet();
    }

    protected Object createClipboardContents(DataFlavor flavor) {
        throw new UnsupportedOperationException("Unsupported data type "
                + flavor);
    }

    public void repaintArea(int x, int y, int w, int h) {
        getMasterRepaintHandle().repaintArea(x, y, w, h);
    }

    protected void flattenImpl() {
        if (state().layerCount() < 2) {
            return;
        }
        String msg = NbBundle.getMessage(AbstractPictureImplementation.class,
                "MSG_FLATTEN_LAYERS");
        String layerName = NbBundle.getMessage(AbstractPictureImplementation.class,
                "DEFAULT_FLATTENED_LAYER_NAME", state().layerCount());
        LayerImplementation nue = LayerFactory.getDefault().createLayer(
                layerName,
                getMasterRepaintHandle(), getSize());

        SurfaceImplementation surface = nue.getSurface();
        if (surface == null) {
            Logger.getLogger("global").log(Level.SEVERE,
                    "Tried to flatten image but default layer factory"
                    + "provides a layer instance with no surface to "
                    + "paint into");
        } else {
            undoables.inUndoableOperation(false, msg, () -> {
                paint(surface.getGraphics(), null, true);
                state().setLayers(nue);
            });
        }
    }

    @Override
    public final void flatten() {
        flattenImpl();
    }

    @Override
    public final void add(int ix, LayerImplementation l) {
        if (l == null) {
            throw new NullPointerException("Null layer"); //NOI18N
        }
        if (ix == POSITION_BOTTOM) {
            ix = 0;
        } else if (ix == POSITION_TOP) {
            ix = state().layerCount() - 1;
        }
        l.addRepaintHandle(getMasterRepaintHandle());
        state().addLayer(ix, l);
        Rectangle r = l.getBounds();
        getMasterRepaintHandle().repaintArea(r.x, r.y, r.width, r.height);
        setActiveLayer(l);
    }

    @Override
    public final void move(LayerImplementation layer, final int pos) {
        LayersState st = state();
        int oldPos = st.indexOf(layer);
        if (oldPos == -1) {
            throw new IllegalArgumentException();
        }
        if (oldPos == pos) {
            return;
        }
        int op = oldPos;
        undoables.inUndoableOperation(false,
                NbBundle.getMessage(AbstractPictureImplementation.class,
                        "MSG_MOVE_LAYER",
                        layer.getName(), "" + pos), () -> {
            st.removeLayer(op);
            int p = pos;
            if (p > op) {
                p--;
            }
            st.addLayer(p, layer);
        });
    }

    @Override
    public final void delete(LayerImplementation layer) {
        LayersState st = state();
        int ix = st.indexOf(layer);
        if (ix == -1) {
            throw new IllegalArgumentException();
        }
        undoables.inUndoableOperation(true, NbBundle.getMessage(
                AbstractPictureImplementation.class,
                "MSG_DELETE_LAYER", layer.getName()), () -> {
            st.removeLayer(layer);
            layer.removeRepaintHandle(getMasterRepaintHandle());
            if (st.getActiveLayer() == layer) {
                if (ix != 0) {
                    st.setActiveLayer(st.getLayer(ix - 1));
                } else {
                    if (st.layerCount() > 0) {
                        st.setActiveLayer(st.getLayer(0));
                    } else {
                        st.setActiveLayer(null);
                    }
                }
            }
        });
    }

    @Override
    public final void setActiveLayer(LayerImplementation l) {
        LayersState st = state();
        if (l != st.getActiveLayer()) {
            String msg = l == null ? NbBundle.getMessage(AbstractPictureImplementation.class,
                    "MSG_CLEAR_ACTIVE_LAYER") : NbBundle.getMessage(AbstractPictureImplementation.class,
                            "MSG_ACTIVATE_LAYER");
            undoables.inUndoableOperation(false, msg, () -> {
                /*
                    Selection oldSelection = old == null ? storedSelection : old.getLookup().lookup(Selection.class);
                    if (oldSelection != null) {
                    Selection newSelection = l == null ? null : l.getLookup().lookup(Selection.class);
                    if (newSelection != null) {
                    newSelection.translateFrom(oldSelection);
                    oldSelection.clearNoUndo();
                    storedSelection = null;
                    } else {
                    storedSelection = oldSelection;
                    }
                    }
                 */
                st.setActiveLayer(l);
            });
        }
    }

    @Override
    public final LayerImplementation add(int index) {
        LayersState st = state();
        LayerImplementation[] result = new LayerImplementation[1];
        undoables.inUndoableOperation(false, NbBundle.getMessage(AbstractPictureImplementation.class,
                "MSG_ADD_NEW_LAYER", st.layerCount()), () -> {
            String layerName = NbBundle.getMessage(AbstractPictureImplementation.class,
                    "DEFAULT_LAYER_NAME", st.layerCount());
            LayerFactory factory;
            if (st.isEmpty()) {
                factory = LayerFactory.getDefault();
            } else {
                LayerImplementation l = index >= st.layerCount()
                        ? st.getLayer(st.layerCount() - 1)
                        : st.getLayer(index);

                factory = l.getLookup().lookup(LayerFactory.class);
                if (factory == null) {
                    factory = LayerFactory.getDefault();
                }
            }

            result[0] = factory.createLayer(layerName, getMasterRepaintHandle(), //XXX
                    st.getSize());
            switch (index) {
                case POSITION_TOP:
                    st.addLayer(result[0]);
                    break;
                case POSITION_BOTTOM:
                    st.addLayer(0, result[0]);
                    break;
                default:
                    st.addLayer(index, result[0]);
                    break;
            }
            setActiveLayer(result[0]);
        });
        return st.getLayer(st.layerCount() - 1);
    }

    @Override
    public final LayerImplementation duplicate(LayerImplementation toClone) {
        LayersState localState = state();
        int ix = localState.indexOf(toClone);
        if (ix == -1) {
            throw new IllegalArgumentException();
        }
        String msg = NbBundle.getMessage(
                AbstractPictureImplementation.class,
                "MSG_DUPLICATE_LAYER", toClone.getName());

        LayerImplementation[] result = new LayerImplementation[1];
        undoables.inUndoableOperation(false, msg, () -> {
            result[0] = localState.getLayer(ix).clone(true, true);
            localState.addLayer(ix, result[0]);
            setActiveLayer(result[0]);
        });
        return result[0];
    }

    @Override
    public void hibernate() {
        setHibernated(true, false, null);
    }

    @Override
    public void hibernate(boolean immediately) {
        setHibernated(true, immediately, null);
    }

    @Override
    public final void wakeup(boolean immediate, Runnable run) {
        setHibernated(false, immediate, run);
    }

    protected void onHibernate() {

    }

    protected void onUnhibernate() {

    }

    protected boolean isHibernated() {
        return hibernated.get();
    }

    @Override
    public final void wakeup(boolean immediately) {
        wakeup(immediately, null);
    }

    void setHibernated(boolean val, boolean immediate, Runnable run) {
        if (hibernated.compareAndSet(!val, val)) {
            Runnable doit = () -> {
                if (val) {
                    onHibernate();
                }
                try {
                    LayersState localState = state();
                    for (LayerImplementation layer : localState) {
                        Hibernator hibernator = layer.getLookup()
                                .lookup(Hibernator.class);
                        if (hibernator != null) {
                            if (val) {
                                hibernator.hibernate();
                            } else {
                                hibernator.wakeup(immediate, null);
                            }
                        }
                    }
                } finally {
                    if (run != null) {
                        run.run();
                    }
                }
                if (!val) {
                    onUnhibernate();
                }
            };
            if (immediate) {
                doit.run();
            } else {
                HIBERNATOR_POOL.submit(doit);
            }
        }
    }

    protected abstract void onLayersChanged(List<LayerImplementation> old,
            List<LayerImplementation> nue,
            Set<LayerImplementation> removed,
            Set<LayerImplementation> added,
            Set<LayerImplementation> retained,
            LayerImplementation oldActiveLayer,
            LayerImplementation newActiveLayer);

    protected abstract void onActiveLayerChanged(LayerImplementation old, LayerImplementation nue);

    protected abstract void onSizeChanged(Dimension old, Dimension nue);

    @Override
    public final void addChangeListener(ChangeListener cl) {
        changes.addChangeListener(cl);
    }

    @Override
    public final void removeChangeListener(ChangeListener cl) {
        changes.removeChangeListener(cl);
    }

    @Override
    public final Dimension getSize() {
        return state().getSize();
    }

    class Obs implements LayersState.Observer {

        @Override
        public void activeLayerChanged(LayerImplementation old, LayerImplementation nue) {
            System.out.println("active layer changed");
            psel.activeLayerChanged(old, nue);
            onActiveLayerChanged(old, nue);
            changes.fireChange();
        }

        @Override
        public void sizeChanged(Dimension old, Dimension nue) {
            if (!Objects.equals(old, nue)) {
                onSizeChanged(old == null ? new Dimension() : old,
                        nue);
                changes.fireChange();
            }
        }

        @Override
        public void layersChanged(List<LayerImplementation> old,
                List<LayerImplementation> nue,
                Set<LayerImplementation> removed,
                Set<LayerImplementation> added,
                Set<LayerImplementation> retained,
                LayerImplementation oldActiveLayer,
                LayerImplementation newActiveLayer) {
            onLayersChanged(old, nue, removed, added, retained,
                    oldActiveLayer, newActiveLayer);
            changes.fireChange();
        }
    }

    @Override
    public Transferable copy(Clipboard clipboard, boolean allLayers) {
        TransferableImpl sel = new TransferableImpl(this, allLayers, false);
        return sel;
    }

    @Override
    public Transferable cut(Clipboard clipboard, boolean allLayers) {
        TransferableImpl sel = new TransferableImpl(this, allLayers, true);
        return sel;
    }

    @Override
    public boolean paste(Clipboard clipboard) {
        if (clipboard.getContents(this) != null) {
            for (DataFlavor flavor : clipboard.getAvailableDataFlavors()) {
                if (DataFlavor.imageFlavor.equals(flavor)) {
                    try {
                        Image img = (Image) clipboard.getData(flavor);
                        if (!(img instanceof BufferedImage)) {
                            int w = img.getWidth(null);
                            int h = img.getHeight(null);
                            LayerImplementation current = getActiveLayer();
                            Selection sel = current == null ? null
                                    : getActiveLayer().getLookup().lookup(Selection.class);
                            Shape clip = sel == null ? null : sel.asShape();
                            if (clip == null) {
                                clip = this.psel.toShape();
                            }
                            if (clip != null && current != null) {
                                Point loc = current.getBounds().getLocation();
                                if (loc.x != 0 || loc.y != 0) {
                                    AffineTransform xform
                                            = AffineTransform.getTranslateInstance(-loc.x, -loc.y);
                                    clip = xform.createTransformedShape(clip);
                                }
                            }
                            String name = NbBundle.getMessage(AbstractPictureImplementation.class,
                                    "LBL_PASTED_LAYER", state().layerCount()); //NOI18N
                            LayerImplementation newLayer = RasterConverter.getLayerFactory().createLayer(name,
                                    getMasterRepaintHandle(), new Dimension(w, h));
                            SurfaceImplementation surf = newLayer.getSurface();
                            Graphics2D g = surf.getGraphics();
                            try {
                                if (clip != null) {
                                    g.setClip(clip);
                                }
                                GraphicsUtils.setHighQualityRenderingHints(g);
                                g.drawImage(img,
                                        AffineTransform.getTranslateInstance(0,
                                                0), null);
                            } finally {
                                g.dispose();
                            }
                            add(0, newLayer);
                        }
                        return true;
                    } catch (UnsupportedFlavorException | IOException ex) {
                        Exceptions.printStackTrace(ex);
                    }
                }
            }
        }
        return false;
    }
}
