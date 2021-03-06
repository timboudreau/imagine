package org.imagine.vector.editor.ui.tools.widget.snap;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import net.java.dev.imagine.api.vector.Adjustable;
import org.imagine.editor.api.Zoom;
import org.imagine.editor.api.snap.OnSnap;
import org.imagine.editor.api.snap.SnapKind;
import org.imagine.editor.api.snap.SnapCoordinate;
import com.mastfrog.geometry.util.GeometryStrings;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.ShapeEntry;
import org.imagine.vector.editor.ui.ShapeSnapPointEntry;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;

/**
 *
 * @author Tim Boudreau
 */
public class SnapPainter implements OnSnap<ShapeSnapPointEntry> {

    private final RepaintHandle handle;
    private final Map<SnapKind, OneTypePainter> painters
            = new EnumMap<>(SnapKind.class);
    private final Supplier<Rectangle> bounds;
    private final Lookup.Result<ShapeElement> selection;
    private ShapeElement selected;
    private OneTypePainter lastX;
    private OneTypePainter lastY;
    private final Supplier<Zoom> zoomSupplier;

    public SnapPainter(RepaintHandle handle, Supplier<Rectangle> bounds, Lookup selection, Supplier<Zoom> zoomSupplier) {
        this.handle = handle;
        this.bounds = bounds;
        this.selection = selection.lookupResult(ShapeElement.class);
        this.selection.addLookupListener(ll);
        this.selection.allInstances();
        this.zoomSupplier = zoomSupplier;
        painters.put(SnapKind.GRID, new GridPainter());
        painters.put(SnapKind.ANGLE, new AnglePainter());
        painters.put(SnapKind.CORNER, new CornerPainter());
        painters.put(SnapKind.POSITION, new PositionPainter(bounds));
        painters.put(SnapKind.DISTANCE, new DistancePainter(bounds));
        painters.put(SnapKind.EXTENT, new ExtentPainter());
        painters.put(SnapKind.LENGTH, new LengthPainter());
    }

    private final LookupListener ll = new LookupListener() {
        @Override
        public void resultChanged(LookupEvent le) {
            Collection<? extends ShapeElement> coll = selection.allInstances();
            selected = coll.isEmpty() ? null : coll.iterator().next();
        }
    };

    private boolean isPointOnSelection(SnapCoordinate<ShapeSnapPointEntry> point) {
        ShapeSnapPointEntry ssp = point.value();
        if (ssp == null) {
            return false;
        }
        ShapeEntry en = ssp.entry;
        return Objects.equals(selected, en);
    }

    private OneTypePainter find(SnapCoordinate<ShapeSnapPointEntry> e) {
        assert e != null;
        return painters.get(e.kind());
    }

    public void paint(Graphics2D g, Zoom zoom) {
        if (lastX != null && lastY != null && lastY == lastX) {
            lastX.paint(g, zoom, selected);
        } else {
            if (lastX != null) {
                lastX.paint(g, zoom, selected);
            }
            if (lastY != null) {
                lastY.paint(g, zoom, selected);
            }
        }
    }

    private boolean isVirtual(SnapCoordinate<ShapeSnapPointEntry> p) {
        if (p.value() == null || p.value().entry == null) {
            return false;
        }
        Adjustable adj = p.value().entry.item().as(Adjustable.class);
        if (adj == null) {
            return false;
        }
        return adj.isVirtualControlPoint(p.value().controlPoint1);
    }

    @Override
    public boolean onSnap(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y) {
        // Queue up clearing the wrong rectangles
        if (lastX != null) {
            lastX.requestRepaint(handle);
        }
        if (lastY != null) {
            lastY.requestRepaint(handle);
        }
        if (x == null && y == null) {
            setBothPainters(null);
            return false;
        }
        if ((x != null && Math.abs(x.coordinate()) > 10000)
                || (y != null && Math.abs(y.coordinate()) > 10000)) {
            // XXX some kind of wraparound to extreme values happening in
            // corner snap computation
            System.err.println("Given insane snap coordinates: "
                    + GeometryStrings.toString(x == null ? -1 : x.coordinate(),
                            y == null ? -1 : y.coordinate())
                    + " - aborting snap."
            );
            setBothPainters(null);
            return false;
        }
        try {
//            setBothPainters(null);
            if (x != null) {
                if (isPointOnSelection(x)) {
                    if (!isVirtual(x)) {
                        x = null;
                    }
                }
            }
            if (y != null) {
                if (isPointOnSelection(y)) {
                    if (!isVirtual(y)) {
                        y = null;
                    }
                }
            }
            if (x == null && y == null) {
                setBothPainters(null);
                return false;
            } else if (x != null && y != null) {
                if (y.kind() == x.kind()) {
                    return snapBoth(x, y);
                } else {
                    // logical, not bitwise or intentional -
                    // we want both snap painters initialized
                    return snapX(x) | snapY(y);
                }
            } else if (x != null) {
                setYPainter(null);
                return snapX(x);
            } else if (y != null) {
                setXPainter(null);
                return snapY(y);
            }
            return false;
        } finally {
            if (lastX != null) {
                lastX.requestRepaint(handle);
            }
            if (lastY != null & lastY != lastX) {
                lastY.requestRepaint(handle);
            }
        }
    }

    private boolean snapX(SnapCoordinate<ShapeSnapPointEntry> x) {
        OneTypePainter p = find(x);
        if (p != null) {
            if (lastY != null) {
                lastY.requestRepaint(handle);
            }
            if (lastX != null) {
                lastX.requestRepaint(handle);
            }
            boolean result = p.onSnap(x, null, zoomSupplier.get(), selected);
            if (result) {
                setXPainter(p);
            }
            return result;
        }
        return false;
    }

    private boolean snapY(SnapCoordinate<ShapeSnapPointEntry> y) {
        OneTypePainter p = find(y);
        if (p != null) {
            if (lastY != null) {
                lastY.requestRepaint(handle);
            }
            if (lastX != null) {
                lastX.requestRepaint(handle);
            }
            boolean result = p.onSnap(null, y, zoomSupplier.get(), selected);
            if (result) {
                setYPainter(p);
            }
            return result;
        }
        return false;
    }

    private boolean snapBoth(SnapCoordinate<ShapeSnapPointEntry> x, SnapCoordinate<ShapeSnapPointEntry> y) {
        assert x != null;
        assert y != null;
        assert x.kind() == y.kind();
        OneTypePainter p = find(x);
        if (p != null) {
            if (lastX != null) {
                lastX.requestRepaint(handle);
            }
            if (lastY != null && lastX != lastY) {
                lastY.requestRepaint(handle);
            }
            if (p != lastX && p != lastY) {
                p.requestRepaint(handle);
            }
            boolean result = p.onSnap(x, y, zoomSupplier.get(), selected);
            if (result) {
                setBothPainters(p);
            }
            return result;
        }
        return false;
    }

    private void setXPainter(OneTypePainter p) {
        if (lastX != null) {
            lastX.requestRepaint(handle);
            lastX.resign();
        }
        lastX = p;
        if (lastX != null) {
            lastX.activate();
        }
        if (p != null) {
            setYPainter(null);
        }
    }

    private void setYPainter(OneTypePainter p) {
        if (lastY != null) {
            lastY.requestRepaint(handle);
            lastY.resign();
        }
        lastY = p;
        if (lastY != null) {
            lastY.activate();
        }
        if (p != null) {
            setXPainter(null);
        }
    }

    private void setBothPainters(OneTypePainter p) {
        if (lastX != null) {
            lastX.requestRepaint(handle);
            lastX.resign();
            if (lastY != null && lastY != lastX) {
                lastY.requestRepaint(handle);
                lastY = null;
            }
            lastY = null;
        }
        if (lastY != null && lastY != lastX) {
            lastY.requestRepaint(handle);
            lastY.resign();
        }
        lastX = p;
        lastY = p;
        if (p != null) {
            p.activate();
            p.requestRepaint(handle);
        }
    }
}
