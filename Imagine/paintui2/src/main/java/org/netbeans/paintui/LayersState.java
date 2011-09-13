package org.netbeans.paintui;

import java.awt.Dimension;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.netbeans.paintui.PictureScene.PI;

/**
 *
 * Replaceable object which holds all stateful fields for a
 * Layers instance.  It is copied whenever an operation is performed
 * which may need to be undone layer.  Can create a deep copy of
 * all LayerImpls associated with it if needed.
 */
/**
 *
 * @author Tim Boudreau
 */
final class LayersState implements Iterable<LayerImplementation> {
    private Dimension size = new Dimension();
    private List<LayerImplementation> layers = new LinkedList<LayerImplementation>();
    private LayerImplementation activeLayer = null;
    private PictureScene.PI owner;

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

    LayersState(PictureScene.PI owner) {
        this.owner = owner;
    }
    
    PI getOwner() {
        return owner;
    }
    
    void removeLayer(int ix) {
        LayerImplementation l = layers.get(ix);
        layers.remove(ix);
        if (l == activeLayer) {
            activeLayer = null;
        }
        change();
    }
    
    void removeLayer(LayerImplementation layer) {
        layers.remove(layer);
        if (layer == activeLayer) {
            activeLayer = null;
        }
        change();
    }
    
    LayerImplementation getActiveLayer() {
        assert activeLayer == null || layers.contains(activeLayer);
        return activeLayer;
    }
    
    void setActiveLayer(LayerImplementation layer) {
        assert layer == null || layers.contains(layer);
        activeLayer = layer;
    }

    void restore() {
        owner.setState(this);
        owner.fire();
    }
    
    public Dimension getSize() {
        return new Dimension(size);
    }
    
    public void setSize(Dimension d) {
        size.setSize(d);
    }
    
    public void setSize(int width, int height) {
        size.setSize(width, height);
    }
    
    public LayerImplementation[] getLayers() {
        return layers.toArray(new LayerImplementation[layers.size()]);
    }
    
    boolean isEmpty() {
        return layers.isEmpty();
    }

    void becomeDeepCopy() {
        deepCopy(this.layers);
    }
    
    void addLayer(LayerImplementation layer) {
        assert !layers.contains(layer);
        layers.add(layer);
        change();
    }
    
    void addLayer(int ix, LayerImplementation impl) {
        assert !layers.contains(impl);
        layers.add(ix, impl);
        change();
    }
    
    int layerCount() {
        return layers.size();
    }
    
    LayerImplementation getLayer(int ix) {
        return layers.get(ix);
    }

    void deepCopy(java.util.List<LayerImplementation> layers) {
        List<LayerImplementation> newLayers = new LinkedList<LayerImplementation>();
        for (LayerImplementation layer : layers) {
            LayerImplementation nue = layer.clone(false, true);
            if (activeLayer == layer) {
                activeLayer = nue;
            }
            newLayers.add(nue);
        }
        this.layers = newLayers;
        change();
    }

    @Override
    public Iterator<LayerImplementation> iterator() {
        return Collections.unmodifiableList(layers).iterator();
    }
    
    void clearLayers() {
        layers.clear();
        change();
    }
    
    void setLayers(LayerImplementation... layers) {
        this.layers.clear();
        this.layers.addAll(Arrays.asList(layers));
        activeLayer = layers.length == 0 ? null : layers[layers.length - 1];
        change();
    }
    
    private void change() {
        Observer obs = owner.getObserver(this);
        if (obs != null) { //if this is not the current state object for the image, will be
            obs.layersChanged(Collections.unmodifiableList(layers));
        }
    }
    
    int indexOf(LayerImplementation i) {
        return layers.indexOf(i);
    }
    
    interface Observer {
        void layersChanged(List<LayerImplementation> layers);
    }
}
