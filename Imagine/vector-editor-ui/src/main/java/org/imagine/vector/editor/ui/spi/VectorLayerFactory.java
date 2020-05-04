/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.spi;

import org.netbeans.paint.api.editing.LayerFactory;

/**
 *
 * @author Tim Boudreau
 */
public abstract class VectorLayerFactory extends LayerFactory {

    protected VectorLayerFactory(String name, String displayName) {
        super(name, displayName);
    }
}
