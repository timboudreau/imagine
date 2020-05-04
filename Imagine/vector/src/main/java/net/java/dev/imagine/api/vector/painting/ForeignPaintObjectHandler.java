package net.java.dev.imagine.api.vector.painting;

import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.util.Collection;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import org.openide.util.Lookup;

/**
 * Some libraries, such as Batik, provide their own paint or other
 * implementations.  This allows VectorWrapperGraphics to construct the correct
 * PaintWrapper or Shaped or stroke wrapper for such objects without this
 * library depending directly on the foreign library.
 *
 * @author Tim Boudreau
 */
public abstract class ForeignPaintObjectHandler {

    private static Collection<? extends ForeignPaintObjectHandler> all
            = Lookup.getDefault().lookupAll(ForeignPaintObjectHandler.class);

    public static Primitive getStroke(Stroke stroke) {
        Primitive result = null;
        for (ForeignPaintObjectHandler fpoh : all) {
            result = fpoh.forStroke(stroke);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    public static Shaped getShape(Shape shape) {
        Shaped result = null;
        for (ForeignPaintObjectHandler fpoh : all) {
            result = fpoh.forShape(shape);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    public static Primitive getPaint(Paint paint) {
        Primitive result = null;
        for (ForeignPaintObjectHandler fpoh : all) {
            result = fpoh.forPaint(paint);
            if (result != null) {
                break;
            }
        }
        return result;
    }

    protected Primitive forStroke(Stroke stroke) {
        return null;
    }

    protected Shaped forShape(Shape shape) {
        return null;
    }

    protected Primitive forPaint(Paint paint) {
        return null;
    }
}
