package org.imagine.vector.editor.ui;

import java.awt.Dimension;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import org.imagine.utils.painting.RepaintHandle;
import org.netbeans.paint.api.editing.LayerFactory;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=LayerFactory.class, position=10000)
public class VectorLayerFactory extends LayerFactory {

    @Messages("vector=Vector (resolution independent)")
    public VectorLayerFactory() {
        super("vector2", Bundle.vector());
    }

    @Override
    public LayerImplementation createLayer(String name, RepaintHandle handle, Dimension size) {
        return new VectorLayer(name, handle, size, this);
    }

    @Override
    public boolean canConvert(Layer other) {
        return false;
    }

    @Override
    public LayerImplementation convert(Layer other) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
