package net.java.dev.imagine.spi.image.support;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.java.dev.imagine.spi.image.LayerImplementation;

/**
 *
 * Replaceable object which holds all stateful fields for a Layers instance. It
 * is copied whenever an operation is performed which may need to be undone
 * layer. Can create a deep copy of all LayerImpls associated with it if needed.
 */
/**
 *
 * @author Tim Boudreau
 */
final class LayersState implements Iterable<LayerImplementation> {

    private Dimension size = new Dimension();
    private List<LayerImplementation> layers = new LinkedList<LayerImplementation>();
    private LayerImplementation activeLayer = null;
    private AbstractPictureImplementation owner;

    LayersState(LayersState other, boolean deepCopy) {
        this.owner = other.owner;
        this.size = new Dimension(other.size);
        this.activeLayer = other.activeLayer;
        if (deepCopy) {
            deepCopy(other.layers);
        } else {
            layers.addAll(other.layers);
        }
    }

    LayersState(AbstractPictureImplementation owner) {
        this.owner = owner;
    }

    AbstractPictureImplementation getOwner() {
        return owner;
    }

    int layerCount() {
        return layers.size();
    }

    void removeLayer(int ix) {
        LayerImplementation targetLayer = layers.get(ix);
        setLayers(removing(targetLayer));
    }

    void removeLayer(LayerImplementation layer) {
        setLayers(removing(layer));
    }

    LayerImplementation getActiveLayer() {
        assert activeLayer == null || layers.contains(activeLayer);
        return activeLayer;
    }

    void setActiveLayer(LayerImplementation layer) {
        assert layer == null || layers.contains(layer);
        LayerImplementation old = activeLayer;
        activeLayer = layer;
        change(old, activeLayer);
    }

    void restore() {
        owner.setState(this);
        owner.undo().fire();
    }

    public Dimension getSize() {
        return new Dimension(size);
    }

    public void setSize(Dimension d) {
        if (!d.equals(size)) {
            Dimension old = new Dimension(size);
            size.setSize(d);
            change(old, getSize());
        }
    }

    public void setSize(int width, int height) {
        size.setSize(width, height);
    }

    public LayerImplementation[] getLayers() {
        return layers.toArray(new LayerImplementation[layers.size()]);
    }

    public List<LayerImplementation> layers() {
        return new ArrayList<>(layers);
    }

    boolean isEmpty() {
        return layers.isEmpty();
    }

    void becomeDeepCopy() {
        deepCopy(this.layers);
    }

    void addLayer(LayerImplementation layer) {
        assert !layers.contains(layer);
        setLayers(adding(layer));
    }

    void addLayer(int ix, LayerImplementation impl) {
        assert !layers.contains(impl);
        setLayers(adding(impl, ix));
    }

    LayerImplementation getLayer(int ix) {
        return layers.get(ix);
    }

    void deepCopy(java.util.List<LayerImplementation> layers) {
//        LayerImplementation oldActive = activeLayer;
        List<LayerImplementation> newLayers = new LinkedList<LayerImplementation>();
        LayerImplementation newActive = null;
        for (LayerImplementation layer : layers) {
            LayerImplementation nue = layer.clone(false, true);
            if (activeLayer == layer) {
                newActive = nue;
            }
            newLayers.add(nue);
        }
        setLayers(newLayers, newActive);
    }

    @Override
    public Iterator<LayerImplementation> iterator() {
        return Collections.unmodifiableList(layers).iterator();
    }

    void clearLayers() {
        setLayers(Collections.emptyList(), null);
//        LayerImplementation oldActive = activeLayer;
//        setLayers();
//        activeLayer = null;
//        change(oldActive, null);
    }

    List<LayerImplementation> adding(LayerImplementation impl) {
        List<LayerImplementation> result = new ArrayList<>(layers);
        result.add(impl);
        return result;
    }

    List<LayerImplementation> adding(LayerImplementation impl, int ix) {
        List<LayerImplementation> result = new ArrayList<>(layers);
        result.add(ix, impl);
        return result;
    }

    List<LayerImplementation> removing(LayerImplementation impl) {
        List<LayerImplementation> result = new ArrayList<>(layers);
        result.remove(impl);
        return result;
    }

    void setLayers(LayerImplementation... nue) {
        setLayers(Arrays.asList(nue), null);
    }

    void setLayers(List<LayerImplementation> newLayers) {
        setLayers(newLayers, null);
    }

    void setLayers(List<LayerImplementation> newLayers, LayerImplementation explicitNewActiveLayer) {
        LayerImplementation oldActiveLayer = activeLayer;
        int oldActiveIndex = oldActiveLayer == null ? -1 : this.layers.indexOf(oldActiveLayer);
        if (!newLayers.equals(layers)) {
            List<LayerImplementation> oldLayers = new ArrayList<>(layers);
            this.layers.clear();
            this.layers.addAll(newLayers);

            LayerImplementation newActiveLayer = null;
            if (explicitNewActiveLayer != null) {
                newActiveLayer = explicitNewActiveLayer;
            } else if (oldActiveLayer == null && newLayers.size() == 1) {
                newActiveLayer = newLayers.get(0);
            } else if (oldActiveLayer != null && newLayers.indexOf(oldActiveLayer) < 0) {
                if (newLayers.size() == 1) {
                    newActiveLayer = newLayers.get(0);
                } else if (oldActiveIndex >= 0 && newLayers.size() >= 1) {
                    int newActiveIndex = Math.min(newLayers.size() - 1, oldActiveIndex);
                    newActiveLayer = newLayers.get(newActiveIndex);
                } else if (newLayers.size() > 1) {
                    newActiveLayer = newLayers.get(newLayers.size() - 1);
                }
            } else if (oldActiveLayer != null && newLayers.contains(oldActiveLayer)) {
                newActiveLayer = oldActiveLayer;
            } else if (newLayers.size() > 0) {
                newActiveLayer = newLayers.get(newLayers.size() - 1);
            }

            boolean activeLayerChanged = newActiveLayer != oldActiveLayer;
            if (activeLayerChanged) {
                activeLayer = newActiveLayer;
            }
            Set<LayerImplementation> removed = new HashSet<>(oldLayers);
            removed.removeAll(newLayers);
            Set<LayerImplementation> added = new HashSet<>(newLayers);
            added.removeAll(oldLayers);
            Set<LayerImplementation> retained = new HashSet<>(newLayers);
            retained.retainAll(oldLayers);
            change(oldLayers, newLayers, oldActiveLayer, newActiveLayer);
//            if (activeLayerChanged) {
//                change(oldActiveLayer, activeLayer);
//            }
        }
    }

    private void change(LayerImplementation old, LayerImplementation nue) {
        if (old != nue) { //may both be null
            Observer obs = owner.getObserver(this);
            if (obs != null) {
                obs.activeLayerChanged(old, nue);
            }
        }
    }

    private void change(List<LayerImplementation> oldLayers, List<LayerImplementation> newLayers,
            LayerImplementation oldActiveLayer,
            LayerImplementation newActiveLayer) {
        Observer obs = owner.getObserver(this);
        if (obs != null) { //if this is not the current state object for the image, will be
            Set<LayerImplementation> removed = new HashSet<>(oldLayers);
            removed.removeAll(newLayers);
            Set<LayerImplementation> added = new HashSet<>(newLayers);
            added.removeAll(oldLayers);
            Set<LayerImplementation> retained = new HashSet<>(newLayers);
            retained.retainAll(oldLayers);
            obs.layersChanged(oldLayers, newLayers, removed, added, retained, oldActiveLayer, newActiveLayer);
        }
    }

    private void change(Dimension old, Dimension nue) {
        if (!old.equals(nue)) {
            Observer obs = owner.getObserver(this);
            if (obs != null) {
                obs.sizeChanged(old, nue);
            }
        }
    }

    int indexOf(LayerImplementation i) {
        return layers.indexOf(i);
    }

    boolean isSameLayers(LayersState st) {
        return st.layers.equals(layers);
    }

    interface Observer {

        void layersChanged(List<LayerImplementation> old, List<LayerImplementation> nue,
                Set<LayerImplementation> removed, Set<LayerImplementation> added,
                Set<LayerImplementation> retained, LayerImplementation oldActiveLayer,
                LayerImplementation newActiveLayer);

        void activeLayerChanged(LayerImplementation old, LayerImplementation nue);

        void sizeChanged(Dimension old, Dimension nue);
    }
}
