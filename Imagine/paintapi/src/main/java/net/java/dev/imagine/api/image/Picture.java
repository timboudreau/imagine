/*
 * Picture.java
 *
 * Created on October 14, 2005, 11:11 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.image;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.Accessor;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import org.imagine.editor.api.Zoom;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.utils.ConvertList;
import org.openide.util.ChangeSupport;

/**
 * An ordered stack of one or more images which compose an overall image.
 * Individual layers are separately edited.
 * <p/>
 * To implement Picture, provide an implementation of PictureImplementation.
 * An implementation is provided by Imagine's editor;  to get the currently
 * edited picture, simply call Utilities.actionsGlobalContext().lookup(Picture.class).
 *
 * @author Timothy Boudreau
 */
public final class Picture implements Iterable<Layer> {
    public static final int POSITION_BOTTOM = -1;
    public static final int POSITION_TOP = -2;
    final PictureImplementation impl;
    private Path file;

    static {
        Layer.init();
    }

    Picture (PictureImplementation impl) {
        if (Accessor.pictureFor(impl) != null) {
            throw new IllegalStateException ("Constructing a second " + //NOI18N
                    "Picture for " + impl); //NOI18N
        }
        this.impl = impl;
    }

    public Picture associateFile(Path file) {
        this.file = file;
        return this;
    }

    public Path associatedFile() {
        return file;
    }

    public Iterator<Layer> iterator() {
        return getLayers().iterator();
    }

    /**
     * Get the size of this picture.
     * @return
     */
    public Dimension getSize() {
        return impl.getSize ();
    }

    /**
     * Get the layers that make up this picture
     * @return A list of layers.
     */
    public List <Layer> getLayers() {
//        List <LayerImplementation> impls = impl.getLayers();
//        List <Layer> result = new ArrayList <Layer> (impls.size());
//        for (LayerImplementation curr : impls) {
//            result.add (Accessor.layerFor(curr));
//        }
//        return result;
        return new ConvertList<Layer, LayerImplementation>(Layer.class,
                LayerImplementation.class, impl.getLayers(), CVT);
    }

    static ConvertList.Converter<Layer, LayerImplementation> CVT = new ConvertList.Converter<Layer, LayerImplementation>() {

        @Override
        public Layer convert(LayerImplementation r) {
            return r == null ? null : Accessor.INVERSE.layerFor(r);
        }

        @Override
        public LayerImplementation unconvert(Layer t) {
            return t == null ? null : Accessor.DEFAULT.getImpl(t);
        }
    };

    /**
     * Move a layer to a different position in the order of layers.
     * @param layer The layer
     * @param pos The new index of the layer
     */
    public void move (Layer layer, int pos) {
        impl.move (Accessor.DEFAULT.getImpl(layer), pos);
    }

    /**
     * Remove a layer from this picture.
     * @param layer
     */
    public void delete (Layer layer) {
        impl.delete(Accessor.DEFAULT.getImpl(layer));
    }

    /**
     * Add a new empty layer of the default type at the specified index.
     * @param index The index
     * @return A new layer
     */
    public Layer add (int index) {
        return Accessor.INVERSE.layerFor (impl.add (index));
    }

    /**
     * Add a layer to this picture at the specified index.
     * @param index
     * @param layer
     */
    public void add (int index, Layer layer) {
        impl.add (index,
                Accessor.DEFAULT.getImpl(layer));
    }

    /**
     * Create a duplicate of an existing layer.
     * @param toClone
     * @return
     */
    public Layer duplicate (Layer toClone) {
        return Accessor.layerFor(
            impl.duplicate (Accessor.DEFAULT.getImpl (toClone)));
    }

    final ChangeSupport supp = new ChangeSupport(this);
    volatile boolean listening;
    public void addChangeListener (ChangeListener cl) {
        supp.addChangeListener(cl);
        if (!listening) {
            impl.addChangeListener (implListener);
        }
    }

    public void removeChangeListener (ChangeListener cl) {
        supp.removeChangeListener(cl);
        if (!supp.hasListeners()) {
            impl.removeChangeListener(implListener);
        }
    }

    private final CL implListener = new CL();
    private final class CL implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }

    }

    /**
     * Set the layer currently being edited.
     * @param layer A layer that belongs to this picture or null
     */
    public void setActiveLayer (Layer layer) {
        assert layer == null || getLayers().contains(layer);
        impl.setActiveLayer (layer == null ? null : Accessor.DEFAULT.getImpl(layer));
    }

    /**
     * Get the currently-being-edited layer
     * @return A layer or null
     */
    public Layer getActiveLayer() {
        return Accessor.layerFor(impl.getActiveLayer());
    }

    /**
     * Collapse the contents of all layers into a single layer
     */
    public void flatten() {
        impl.flatten();
    }

    /**
     * Copy the content of the current layer or all layers to the clipboard
     * in some fashion
     * @param allLayers Whether only the active layer or all layers should be
     * copied
     * @return A transferable
     */
    public Transferable copy(Clipboard clipboard, boolean allLayers) {
        return impl.copy(clipboard, allLayers);
    }

    /**
     * Cut the content of the current layer or all layers to the clipboard
     * in some fashion
     * @param allLayers Whether only the active layer or all layers should be
     * copied
     * @return A transferable
     */
    public Transferable cut (Clipboard clipboard, boolean allLayers) {
        return impl.cut (clipboard, allLayers);
    }

    /**
     * Paste the content of the clipboard into a new layer in the current image
     * @return
     */
    public boolean paste(Clipboard clipboard) {
        return impl.paste(clipboard);
    }

    /**
     * Paint the current contents of this Picture object to the supplied
     * Graphics2D context.
     * <p>
     * If a bounding rectangle is supplied, this method should assume that the
     * call is to paint a thumbnail, and that low quality rendering settings
     * should be used.  If the rectangle is null, then the image should be
     * rendered at full quality and full size (size will actually be determined
     * by the AffineTransform the Graphics is currently using, which will not
     * be modified if the rectangle is null).
     * @param g A graphics context
     * @param r A bounding rectangle if painting a thumbnail image, or null
     *  if full quality painting is desired
     */
    public boolean paint (RenderingGoal goal, Graphics2D g, Rectangle bounds, boolean showSelection, Zoom zoom) {
        return impl.paint (goal, g, bounds, showSelection, zoom);
    }

    /**
     * Get an object that can be used to instruct the picture that its
     * UI representation should repaint some region.
     * @return A RepaintHandle
     */
    public RepaintHandle getRepaintHandle() {
        return impl.getMasterRepaintHandle();
    }
}
