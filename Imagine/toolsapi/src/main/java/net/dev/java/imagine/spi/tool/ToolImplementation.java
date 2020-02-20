package net.dev.java.imagine.spi.tool;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashSet;
import java.util.Set;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.api.tool.aspects.LookupContentsContributor;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

/**
 * Provides the functionality of a Tool.  A single logical Tool (one name,
 * action, icon, display name) can have multiple implementations.  Each
 * implementation is sensitive to some object type which may be present in
 * the active Layer's lookup.  If an instance of that type is present, then
 * a ToolImplementation may be constructed to drive the tool.
 * <p/>
 * ToolImplementation subclasses <u>must</u> have a 1-argument constructor
 * which takes an instance of the type it is sensitive to.  To register a tool,
 * implement ToolImplementation and annotate it with the &#064;Tool annotation.
 * <p/>
 * <i><b>Note:</b> For many commonn cases, it is not necessary to subclass
 * ToolImplementation</i> - you can simply subclass MouseAdapter or similar, and
 * add the annotation &#064;Tool, give it a constructor which takes an instance
 * of the type specified in the annotation, and one will be generated.  Subclassing
 * ToolImplementation is useful when more complex enablement or attaching logic
 * is needed.
 *
 * @author Tim Boudreau
 */
public abstract class ToolImplementation<T> implements Attachable, LookupContentsContributor {

    protected final T obj;
    private volatile Lookup lkp;

    protected ToolImplementation(T obj) {
        this.obj = obj;
    }

    /**
     * Provide some additional objects which should be present in the 
     * Tool's lookup.  
     * @param addTo a Set which objects can be contributed to, and which
     * will appear in Tool.getLookup() which this ToolImplementation is in use.
     * <p/>
     * In particular, if you are implementing WidgetAction or any Swing listener
     * interface, you can implement it in a separate class and simply add
     * it to the passed set.
     */
    public void createLookupContents(Set<? super Object> addTo) {
        //do nothing
    }

    protected Lookup additionalLookup() {
        return Lookup.EMPTY;
    }

    /**
     * Get this Tool's lookup, which contains any listeners or other
     * relevant objects which can interoperate with the selected layer.
     * @return 
     */
    public final Lookup getLookup() {
        if (lkp == null) {
            Set<Object> s = new HashSet<Object>();
            createLookupContents(s);
            synchronized (this) {
                if (lkp == null) {
                    if (this instanceof MouseListener) {
                        s.add(new MouseListenerProxy(this));
                    }
                    if (this instanceof MouseMotionListener) {
                        s.add(new MouseMotionListenerProxy(this));
                    }
                    if (this instanceof KeyListener) {
                        s.add(new KeyListenerProxy(this));
                    }
                    if (this instanceof MouseWheelListener) {
                        s.add(new MouseWheelListenerProxy(this));
                    }
                    lkp = Lookups.fixed(s.toArray(new Object[s.size()]));
                    Lookup addtl = additionalLookup();
                    if (addtl != Lookup.EMPTY) {
                        lkp = new ProxyLookup(lkp, addtl);
                    }
                }
            }
        }
        return lkp;
    }

    /**
     * Optional method to add listeners, fetch other objects, etc., from the
     * layer this ToolImplementation is currently operating on.
     * 
     * @param layer The layer (or other lookup provider) which is the context
     * in which this implementation is operating.
     */
    public void attach(Lookup.Provider layer) {
        //do nothing
    }
    
    /**
     * Optional method to detach listeners, etc. when this ToolImplementation is
     * no longer in use.
     */
    public void detach() {
        //do nothing
    }
    
    //Proxy classes to avoid exposing the ToolImplementation itself in its own lookup
    private static class MouseListenerProxy implements MouseListener {
        private final MouseListener delegate;

        public MouseListenerProxy(ToolImplementation<?> delegate) {
            this.delegate = (MouseListener) delegate;
        }
        
        public void mouseReleased(MouseEvent e) {
            delegate.mouseReleased(e);
        }

        public void mousePressed(MouseEvent e) {
            delegate.mousePressed(e);
        }

        public void mouseExited(MouseEvent e) {
            delegate.mouseExited(e);
        }

        public void mouseEntered(MouseEvent e) {
            delegate.mouseEntered(e);
        }

        public void mouseClicked(MouseEvent e) {
            delegate.mouseClicked(e);
        }
    }
    
    private static class KeyListenerProxy implements KeyListener {
        private final KeyListener keyListener;

        public KeyListenerProxy(ToolImplementation<?> keyListener) {
            this.keyListener = (KeyListener) keyListener;
        }

        public void keyTyped(KeyEvent e) {
            keyListener.keyTyped(e);
        }

        public void keyReleased(KeyEvent e) {
            keyListener.keyReleased(e);
        }

        public void keyPressed(KeyEvent e) {
            keyListener.keyPressed(e);
        }
    }
    
    private static class MouseMotionListenerProxy implements MouseMotionListener {
        private final MouseMotionListener mml;

        public MouseMotionListenerProxy(ToolImplementation<?> mml) {
            this.mml = (MouseMotionListener) mml;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            mml.mouseDragged(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            mml.mouseMoved(e);
        }
    }
    
    private static class MouseWheelListenerProxy implements MouseWheelListener {
        private final MouseWheelListener whl;

        public MouseWheelListenerProxy(ToolImplementation<?> whl) {
            this.whl = (MouseWheelListener) whl;
        }

        public void mouseWheelMoved(MouseWheelEvent e) {
            whl.mouseWheelMoved(e);
        }
    }
    
    private static final class AttachableProxy implements Attachable {
        private final ToolImplementation<?> impl;
        AttachableProxy(ToolImplementation<?> impl) {
            this.impl = impl;
        }

        @Override
        public void attach(Provider on) {
            impl.attach(on);
        }

        @Override
        public void detach() {
            impl.detach();
        }
        
    }
}
