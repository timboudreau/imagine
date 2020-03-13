package org.imagine.vector.editor.ui.tools.widget;

import java.util.function.Consumer;
import net.java.dev.imagine.api.vector.design.ControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeControlPoint;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.MutableProxyLookup;
import org.imagine.vector.editor.ui.tools.widget.actions.NextPrevProvider;
import org.netbeans.api.visual.action.WidgetAction;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.Lookup;

/**
 *
 * @author Tim Boudreau
 */
public class NextPrevProviderImpl implements NextPrevProvider {

    final MutableProxyLookup lkp;
    private final HetroObjectLayerWidget owner;
    private final Consumer<Widget> focusApplier;

    NextPrevProviderImpl(MutableProxyLookup lkp, HetroObjectLayerWidget owner,
            Consumer<Widget> focusApplier) {
        this.lkp = lkp;
        this.owner = owner;
        this.focusApplier = focusApplier;
    }

    @Override
    public WidgetAction.State selectNext(Widget following, SelectOperation op) {
        ShapeElement element = following.getLookup().lookup(ShapeElement.class);
        ShapesCollection shapes = following.getLookup().lookup(ShapesCollection.class);
        ShapeControlPoint point = following.getLookup().lookup(ShapeControlPoint.class);
        if (point != null) {
            Widget next = nextFromControlPoint(element, point, shapes);
            if (next != null && next != following) {
                select(next, op, following);
                return WidgetAction.State.CONSUMED;
            }
        } else {
            Widget next = nextFromShape(element, shapes);
            if (next != null && next != following) {
                select(next, op, following);
                return WidgetAction.State.CONSUMED;
            }
        }
        return WidgetAction.State.REJECTED;
    }

    @Override
    public WidgetAction.State selectPrev(Widget preceding, SelectOperation op) {
        ShapeElement element = preceding.getLookup().lookup(ShapeElement.class);
        ShapesCollection shapes = preceding.getLookup().lookup(ShapesCollection.class);
        ShapeControlPoint point = preceding.getLookup().lookup(ShapeControlPoint.class);
        if (point != null) {
            Widget prev = prevFromControlPoint(element, point);
            if (prev != null && prev != preceding) {
                select(prev, op, preceding);
                return WidgetAction.State.CONSUMED;
            }
        } else {
            Widget prev = prevFromShape(element, shapes);
            if (prev != null && prev != preceding) {
                select(prev, op, preceding);
                return WidgetAction.State.CONSUMED;
            }
        }
        return WidgetAction.State.REJECTED;
    }

    private void select(Widget target, SelectOperation op, Widget old) {
        Lookup wl = target.getLookup();
        switch (op) {
            case ADD_TO_SELECTION:
                lkp.addLookup(wl);
                break;
            case REMOVE_FROM_SELECTION:
                lkp.removeLookup(wl);
                break;
            case REPLACE_SELECTION:
                lkp.lookups(wl);
                break;
            default:
                throw new AssertionError(op);
        }
        focusApplier.accept(target);
        old.revalidate(true);
        old.getScene().validate();
    }

    // Selection order:
    // The shape, control-point-1 thru n, next shape
    private Widget nextFromShape(ShapeElement el, ShapesCollection coll) {
        ShapeControlPoint[] pts = el.controlPoints(0, NextPrevProviderImpl::doNothing);
        if (pts.length > 0) {
            for (int i = 0; i < pts.length; i++) {
                Widget next = owner.find(pts[i]);
                if (next != null) {
                    return next;
                }
            }
        }
        int ix = coll.indexOf(el);
        if (ix >= 0) {
            if (ix == coll.size() - 1) {
                ix = 0;
            } else {
                ix = ix + 1;
            }
            ShapeElement next = coll.get(ix);
            return owner.find(next);
        }
        return null;
    }

    private Widget nextFromControlPoint(ShapeElement el,
            ShapeControlPoint pt, ShapesCollection coll) {
        ShapeControlPoint[] all = pt.family();
        if (pt.index() == all.length - 1) {
            int ix = coll.indexOf(el);
            if (ix >= 0) {
                int nextIx;
                if (ix == coll.size() - 1) {
                    nextIx = 0;
                } else {
                    nextIx = ix + 1;
                }
                ShapeElement nextShape = coll.get(nextIx);
                return owner.find(nextShape);
            }
        } else {
            for (int i = pt.index() + 1; i < all.length; i++) {
                Widget w = owner.find(all[i]);
                if (w != null) {
                    return w;
                }
            }
        }
        return null;
    }

    private Widget prevFromShape(ShapeElement el, ShapesCollection coll) {
        int ix = coll.indexOf(el);
        if (ix >= 0) {
            int prevIx;
            if (ix == 0) {
                prevIx = coll.size() - 1;
            } else {
                prevIx = ix - 1;
            }
            ShapeElement prev = coll.get(prevIx);
            ShapeControlPoint[] pts = prev.controlPoints(0, NextPrevProviderImpl::doNothing);
            if (pts.length == 0) {
                return owner.find(prev);
            }
            for (int i = pts.length - 1; i >= 0; i--) {
                Widget w = owner.find(pts[i]);
                if (w != null) { // scene could be out of sync
                    return w;
                }
            }
            return owner.find(prev);
        }
        return null;
    }

    private Widget prevFromControlPoint(ShapeElement el,
            ShapeControlPoint pt) {
        if (pt.index() == 0) {
            return owner.find(pt.owner());
        } else {
            return owner.find(pt.family()[pt.index() - 1]);
        }
    }

    static void doNothing(ControlPoint cp) {

    }

}
