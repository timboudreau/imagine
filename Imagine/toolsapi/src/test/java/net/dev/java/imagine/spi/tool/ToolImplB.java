/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.dev.java.imagine.spi.tool;

import net.dev.java.imagine.spi.tool.ToolsTest.Layer;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class ToolImplB extends ToolImplementation<B> {
    static ToolImplB IMPL;

    public ToolImplB (B obj) {
        super(obj);
        IMPL = this;
        System.out.println("CREATED " + this + " for " + obj);
    }

    @Override
    public void attach(Lookup.Provider layer) {
        System.out.println(this + " attached to " + layer);
        super.attach(layer);
        ToolsTest.attachedToB = (Layer) layer;
    }

    @Override
    public void detach() {
        ToolsTest.attachedToB = null;
        super.detach();
    }
    
}
