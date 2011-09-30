/*
 * VectorLayerFactory.java
 *
 * Created on October 25, 2006, 10:40 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.paint.vectorlayers;

import java.awt.Dimension;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.RepaintHandle;
import org.netbeans.paint.api.editing.LayerFactory;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 * Factory for shape-based vector data layers.
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=LayerFactory.class)
@Messages("LBL_VectorLayerFactory=Vector")
public final class VectorLayerFactory extends LayerFactory {

    public VectorLayerFactory () {
        super ("vector", NbBundle.getMessage(VectorLayerFactory.class, //NOI18N
                "LBL_VectorLayerFactory")); //NOI18N
    }

    public LayerImplementation createLayer(String name, RepaintHandle handle,
                                           Dimension size) {
        VLayerImpl result = new VLayerImpl(this, handle, name, size);
        result.setName(name);
        return result;
    }
        
    public boolean canConvert(Layer other) {
        return false;
    }

    public LayerImplementation convert(Layer other) {
        throw new UnsupportedOperationException();
    }
}
