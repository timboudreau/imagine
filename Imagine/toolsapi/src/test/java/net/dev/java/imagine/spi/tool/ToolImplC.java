package net.dev.java.imagine.spi.tool;

import net.dev.java.imagine.spi.tool.ToolsTest.Layer;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class ToolImplC extends ToolImplementation<B> {

    static ToolImplC IMPL;

    public ToolImplC(B obj) {
        super(obj);
        IMPL = this;
        System.out.println("CREATED " + this + " for " + obj);
    }

    @Override
    public void attach(Lookup.Provider layer) {
        System.out.println(this + " attached to " + layer);
        super.attach(layer);
        ToolsTest.attachedToC = (Layer) layer;
    }

    @Override
    public void detach() {
        ToolsTest.attachedToC = null;
        super.detach();
    }

    public String toString() {
        return super.toString() + " attached to " + ToolsTest.attachedToC;
    }
}
