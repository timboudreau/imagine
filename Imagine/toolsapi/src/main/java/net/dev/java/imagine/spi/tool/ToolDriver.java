package net.dev.java.imagine.spi.tool;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.api.tool.aspects.LookupContentsContributor;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;

/**
 * Object which can create instances of ToolImplementation based on some criteria.
 * If you are using the &#064;Tool and &#064;ToolDef annotations, it is extremely
 * unlikely that this class will be useful;  only in the odd case that you want to
 * write your own layer entries and subclass this to customize how the ToolImplementation
 * is constructed might it be necessary.
 *
 * @author Tim Boudreau
 */
public class ToolDriver<T, R> {

    public static final String SENSITIVE_TO_ATTRIBUTE = "sensitiveTo";
    public static final String TYPE_ATTRIBUTE = "type";
    protected final Class<T> sensitiveTo;
    protected final Class<R> implType;

    protected ToolDriver(Class<T> sensitiveTo, Class<R> implType) {
        this.sensitiveTo = sensitiveTo;
        this.implType = implType;
    }

    /**
     * Create a new ToolDriver, getting constructor parameters from FileObject
     * attributes.
     * 
     * @param fo A FileObject
     * @return A ToolDriver, or null if any classes cannot be loaded
     */
    public static ToolDriver create(FileObject fo) {
        String sensitiveType;
        String implType;
        if (!(fo.getAttribute(TYPE_ATTRIBUTE) instanceof String)) {
            Logger.getLogger(ToolDriver.class.getName()).log(Level.WARNING, "Tool defined by {0} does not have a type= attribute", fo.getPath());
            return null;
        } else {
            implType = (String) fo.getAttribute(TYPE_ATTRIBUTE);
        }
        if (!(fo.getAttribute(SENSITIVE_TO_ATTRIBUTE) instanceof String)) {
            Logger.getLogger(ToolDriver.class.getName()).log(Level.WARNING, "Tool defined by {0} does not have a sensitiveTo= attribute", fo.getPath());
            return null;
        } else {
            sensitiveType = (String) fo.getAttribute(SENSITIVE_TO_ATTRIBUTE);
        }
        try {
            ClassLoader cl = Lookup.getDefault().lookup(ClassLoader.class);
            Class<?> type = cl.loadClass(implType);
            Class<?> sensitiveTo = cl.loadClass(sensitiveType);
            return create(sensitiveTo, type);
        } catch (ClassNotFoundException ex) {
            Exceptions.printStackTrace(ex);
            return null;
        }
    }

    //handy hack-the-generics method to avoid unsafe warnings
    private static <T, R> ToolDriver<T, R> create(Class<T> sensitiveTo, Class<R> impl) {
        return new ToolDriver<T, R>(sensitiveTo, impl);
    }

    public String toString() {
        return super.toString() + " sensitiveTo=" + sensitiveTo.getName() + " type=" + implType;
    }

    /**
     * Get the type this ToolDriver needs to find in the selected layer's lookup
     * in order to create a ToolImplementation instance.
     * @return 
     */
    public final Class<T> sensitiveTo() {
        return sensitiveTo;
    }

    /**
     * Get the type that this ToolDriver will instantiate to drive the 
     * created ToolImplementation.  This may be a subclass of ToolImplementation,
     * or something simple such as a MouseListener (in which case, a wrapper
     * ToolImplementation instance will be generated, with this object in its
     * lookup).
     * @return 
     */
    public final Class<R> type() {
        return implType;
    }

    /**
     * Create a ToolImplementation, if possible
     * @param layer A Lookup.Provider, presumably an instance of Layer
     * @return  A ToolImplementation if an instance of <code>sensitiveTo()</code>
     * is present in the Lookup of the passed Lookup.Provider
     */
    public ToolImplementation<T> create(Lookup.Provider layer) {
        T t = layer.getLookup().lookup(sensitiveTo());
        if (t != null) {
            return create(t);
        }
        return null;
    }

    private final Map<T, ToolImplementation<T>> cache
            = new WeakHashMap<>();
    /**
     * Create the ToolImplementation, constructing an instance of R by reflection.
     * @param t An object of type T found in the layer's lookup
     * @return A ToolImplementation, or null
     */
    protected ToolImplementation<T> create(T t) {
        ToolImplementation<T> cached = cache.get(t);
        if (cached != null) {
            return cached;
        }
        try {
            Constructor<R> c = implType.getConstructor(sensitiveTo);
            R result = c.newInstance(t);
            if (result instanceof ToolImplementation) {
                //XXX can do better than unsafe cast?
                ToolImplementation<T> res = (ToolImplementation<T>) result;
                cache.put(t, res);
                return res;
            } else {
                ToolImplementation<T> res = new WrapperImplementation<T>(t, result);
                cache.put(t, res);
                return res;
            }
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            Logger.getLogger(ToolDriver.class.getName()).log(Level.WARNING, "Failed to "
                    + "instantiate " + implType.getName() + " passing instance " + t + " of "
                    + implType.getName() + " to the constructor", ex);
        }
        return null;
    }

    static final class WrapperImplementation<T> extends ToolImplementation<T> {

        private final Object delegate;

        public WrapperImplementation(T obj, Object delegate) {
            super(obj);
            this.delegate = delegate;
        }

        @Override
        public void createLookupContents(Set<? super Object> addTo) {
            if (delegate instanceof LookupContentsContributor) {
                LookupContentsContributor c = (LookupContentsContributor) delegate;
                c.createLookupContents(addTo);
            }
            addTo.add(delegate);
            addTo.add(this);
        }

        @Override
        public void attach(Provider layer, ToolUIContext ctx) {
            super.attach(layer);
            for (Attachable a : getLookup().lookupAll(Attachable.class)) {
                if (a != this) {
                    a.attach(layer, ctx);
                }
            }
        }

        @Override
        public void detach() {
            try {
                for (Attachable a : getLookup().lookupAll(Attachable.class)) {
                    if (a != this) {
                        a.detach();
                    }
                }
            } finally {
                super.detach();
            }
        }
    }
}
