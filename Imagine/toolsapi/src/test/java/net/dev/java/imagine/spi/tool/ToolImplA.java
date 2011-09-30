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
public class ToolImplA extends ToolImplementation<A> {
    static ToolImplA IMPL;
    public ToolImplA(A obj) {
        super (obj);
        IMPL = this;
        System.out.println("CREATED " + this);
    }

    @Override
    public void attach(Lookup.Provider layer) {
        System.out.println(this + " attached to " + layer);
        ToolsTest.attachedToA = (Layer) layer;
        super.attach(layer);
    }

    @Override
    public void detach() {
        super.detach();
        ToolsTest.attachedToA = null;
    }
    
    
    
}
