package net.java.dev.imagine.layers.text.widget;

import java.awt.Dimension;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import org.netbeans.paint.api.editing.LayerFactory;
import org.openide.util.NbBundle;
import org.openide.util.lookup.ServiceProvider;

/**
 * A factory for text layers
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=LayerFactory.class)
public class TextLayerFactory extends LayerFactory {
    public TextLayerFactory() {
        super("text", NbBundle.getMessage(TextLayerFactory.class, "TEXT_LAYER"));
    }

    @Override
    public LayerImplementation createLayer(String name, RepaintHandle handle, Dimension size) {
        return new TextLayer(this, name, handle, size);
    }

    @Override
    public boolean canConvert(Layer other) {
        return false;
    }

    @Override
    public LayerImplementation convert(Layer other) {
        throw new UnsupportedOperationException("No.");
    }
}
