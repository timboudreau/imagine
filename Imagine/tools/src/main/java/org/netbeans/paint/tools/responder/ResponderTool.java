package org.netbeans.paint.tools.responder;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import static net.java.dev.imagine.api.toolcustomizers.Constants.FILL;
import static net.java.dev.imagine.api.toolcustomizers.Constants.STROKE;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.tools.fills.FillCustomizer;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqPointDouble;
import org.netbeans.paint.api.cursor.Cursors;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * Base class for modal drawing tools, where each mouse gesture may change the
 * thing that handles the next gesture.
 *
 * @author Tim Boudreau
 */
public abstract class ResponderTool extends ToolImplementation<Surface> implements CustomizerProvider,
        Supplier<PathUIProperties> {

    protected static final FillCustomizer paintC = FillCustomizer.getDefault();
    protected static final FillCustomizer outlineC = FillCustomizer.getOutline();

    protected static final Customizer<PaintingStyle> fillC
            = Customizers.getCustomizer(PaintingStyle.class, FILL);
    protected static final Customizer<BasicStroke> strokeC
            = Customizers.getCustomizer(BasicStroke.class, STROKE, null);

    private final ResponderRepainter part = new ResponderRepainter();
    private Responder currentHandler = Responder.NO_OP;
    private boolean active;
    private Repainter repainter;
    private ToolUIContext ctx;
    private Lookup.Provider attachedTo;
    private final Rectangle lastRepaintBounds = new Rectangle();
    private final ResponderInputListener inputListener = new ResponderInputListener(this);
    private PathUIProperties colors;

    protected ResponderTool(Surface obj) {
        super(obj);
    }

    protected ToolUIContext ctx() {
        return ctx == null ? ToolUIContext.DUMMY_INSTANCE : ctx;
    }

    public PathUIProperties get() {
        if (colors == null) {
            return new PathUIProperties(this::ctx);
        }
        return colors;
    }

    @Override
    public Customizer<?> getCustomizer() {
        return new AggregateCustomizer<Void>(getClass().getSimpleName(), fillC, paintC, outlineC, strokeC);
    }

    @Override
    public void createLookupContents(Set<? super Object> addTo) {
        addTo.add(this);
        addTo.add(inputListener);
        addTo.add(part);
    }

    @Override
    public final void attach(Lookup.Provider layer) {
        // do nothing, obsolete method that won't be called
    }

    protected Lookup currentLookup() {
        if (attachedTo != null) {
            return attachedTo.getLookup();
        }
        return Lookup.EMPTY;
    }

    @Override
    public final void attach(Lookup.Provider on, ToolUIContext ctx) {
        assert ctx != null : "Null ToolUIContext";
        attachedTo = on;
        this.ctx = ctx;
        lastRepaintBounds.setFrame(0, 0, 0, 0);
        colors = new PathUIProperties(this::ctx);
        onAttach();
        active = true;
        currentHandler = firstResponder();
    }

    @Override
    public final void detach() {
        Repainter rep = this.repainter;
        reset();
        colors = null;
        pendingCursor = null;
        currentHandler = Responder.NO_OP;
        active = false;
        repainter = null;
        ctx = null;
        onDetach();
        if (rep != null) {
            // Ensure we clear any bounds we painted into
            rep.requestRepaint(lastRepaintBounds);
        }
    }

    /**
     * Override to do initialization on attach. The default implementation does
     * nothing.
     */
    protected void onAttach() {

    }

    /**
     * Override to clear any variables that might result in memory leaks between
     * uses of this tool. The default implementation does nothing.
     */
    protected void onDetach() {
        // do nothing
    }

    /**
     * Clear any state associated with this tool, restoring it to the state it
     * should be in when the user initiates use of it the next time. Called
     * before <code>onDetach()</code> while the internal state is still whatever
     * it was at the end of processing the last input event.
     */
    protected abstract void reset();

    /**
     * Create the responder that handles the user's initial interaction with
     * this tool after setting it as the active tool or after completing an
     * earlier operation with this tool.
     *
     * @return The first responder
     */
    protected abstract Responder firstResponder();

    /**
     * Perform the paint that will be saved into the image being edited, using
     * whatever state has been gathered during prior input events.
     *
     * @param g The graphics to paint into
     * @return The bounding rectangle of whatever was painted
     */
    protected abstract Rectangle paintCommit(Graphics2D g);

    /**
     * Perform a paint of the edits-in-progress at this point in time.
     *
     * @param g A graphics to paint into
     * @param layerBounds The bounds of the layer or image being painted into
     * @return The bounding rectangle of whatever was painted
     */
    protected abstract Rectangle paintLive(Graphics2D g, Rectangle layerBounds);

    private Rectangle paint(Graphics2D g, Rectangle layerBounds, boolean isCommit) {
        Rectangle result = null;
        if (isCommit) {
            result = paintCommit(g);
        } else {
            result = paintLive(g, layerBounds);
            if (currentHandler instanceof PaintingResponder) {
                Rectangle r = ((PaintingResponder) currentHandler).paint(g, layerBounds);
                if (result != null) {
                    result.add(r);
                    r = result;
                }
                result = r;
            }
        }
        if (result != null) {
            lastRepaintBounds.setFrame(result);
        } else {
            lastRepaintBounds.width = lastRepaintBounds.height = 0;
        }
        return result;
    }

    /**
     * Get the current zoom factor, for scaling strokes and hit regions.
     *
     * @return A zoom factor that coordinates should be multiplied by
     */
    protected final float zoomFactor() {
        Zoom zoom = ctx == null ? Utilities.actionsGlobalContext().lookup(Zoom.class)
                : ctx.zoom();
        if (zoom != null) {
            zoom.getZoom();
        }
        return 1;
    }

    /**
     * Call this method when the user has indicated that the drawing is complete
     * and ready to be stored to the backing picture; a call to
     * <code>paintCommit()</code> will arrive subsequently (but not necessarily
     * synchronously, so maintain state).
     */
    protected final void commit() {
        if (repainter != null) {
            repainter.requestCommit();
        }
    }

    /**
     * Request a repaint of the entire image editor the user is using. Where
     * possible, prefer to repaint just the bounding rectangle that has been
     * modified and any cleared areas that were painted in the preceding paint.
     */
    protected final void repaint() {
        if (repainter != null) {
            repainter.requestRepaint();
        }
    }

    protected final void repaint(Shape shape) {
        if (repainter != null) {
            repainter.requestRepaint(shape);
        }
    }

    protected final void repaint(Shape shape, double strokeWidth) {
        if (repainter != null) {
            repainter.requestRepaint(shape, strokeWidth);
        }
    }

    protected final void repaint(Shape shape, BasicStroke stroke) {
        if (repainter != null) {
            repainter.requestRepaint(shape, stroke.getLineWidth());
        }
    }

    private static final Circle scratchCirc = new Circle();

    protected final void repaintLine(Line2D line) {
        if (repainter != null) {
            PathUIProperties props = get();
            float strokeWidth = props.lineStroke().getLineWidth();
            repaintLine(line, strokeWidth);
        }
    }

    protected final void repaintLine(Line2D line, float strokeWidth) {
        if (repainter != null) {
            Rectangle2D r = line.getBounds2D();
            repainter.requestRepaint(r.getBounds());
            double w2 = strokeWidth * 2;
            r.setFrame(r.getX() - strokeWidth, r.getY() - strokeWidth,
                    r.getWidth() - w2, r.getHeight() + w2);
        }
    }

    protected final void repaintPoint(double radius, Point2D point, float strokeWidth) {
        if (repainter != null && point != null) {
            scratchCirc.setRadius(radius);
            scratchCirc.setCenter(point);
            Rectangle2D r = scratchCirc.getBounds2D();
            double w2 = strokeWidth * 2;
            r.setFrame(r.getX() - strokeWidth, r.getY() - strokeWidth,
                    r.getWidth() - w2, r.getHeight() + w2);
            repainter.requestRepaint(r.getBounds());
        }
    }

    protected final void repaintPoint(Point2D point, double radius) {
        if (repainter != null && point != null) {
            PathUIProperties props = get();
            float strokeWidth = props.lineStroke().getLineWidth();
            repaintPoint(radius, point, strokeWidth);
        }
    }

    protected final void repaintPoint(Point2D point) {
        if (repainter != null && point != null) {
            repaintPoint(point, get().pointRadius() + 2);
        }
    }

    protected final void repaintPoint(double x, double y) {
        if (repainter != null) {
            repaintPoint(new EqPointDouble(x, y), get().pointRadius() + 2);
        }
    }

    protected final Cursors cursors() {
        return ImageEditorBackground.getDefault().style().isBright()
                ? Cursors.forBrightBackgrounds() : Cursors.forDarkBackgrounds();
    }

    private Cursor pendingCursor;

    protected final void setCursor(Cursor cursor) {
        if (repainter != null) {
            repainter.setCursor(cursor);
        } else {
            pendingCursor = cursor;
        }
    }

    protected final void repaint(Rectangle2D rect) {
        repaint(rect.getBounds());
    }

    /**
     * Request a repaint of a region of the editor.
     *
     * @param bounds
     */
    protected final void repaint(Rectangle bounds) {
        if (bounds != null && bounds.isEmpty()) {
            return;
        }
        if (repainter != null) {
            if (bounds == null) {
                repainter.requestRepaint();
            } else {
                repainter.requestRepaint(bounds);
                if (!lastRepaintBounds.equals(obj)) {
                    repainter.requestRepaint(lastRepaintBounds);
                }
                lastRepaintBounds.setBounds(bounds);
            }
        }
    }

    /**
     * Called when the repainter that can redraw the on-screen UI is attached.
     *
     * @param repainter A repainter
     */
    protected void onAttachRepainter(Repainter repainter) {
        // do nothing
    }

    private final class ResponderRepainter implements PaintParticipant {

        @Override
        public void attachRepainter(Repainter repainter) {
            ResponderTool.this.repainter = repainter;
            if (pendingCursor != null) {
                repainter.setCursor(pendingCursor);
            }
            onAttachRepainter(repainter);
        }

        @Override
        public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
            if (active) {
                Rectangle r = ResponderTool.this.paint(g2d, layerBounds, commit);
//                repaint(r);
            }
        }
    }

    private final Rectangle updateScratch = new Rectangle();

    protected boolean updateHandler(Responder nue) {
        if (nue != currentHandler) {
            if (isPrintStackTrace(currentHandler.getClass(), nue.getClass())) {
                new Exception(currentHandler + " --> " + nue).printStackTrace();
            } else if (logTransitions) {
                System.out.println(currentHandler + "\t--> " + nue);
            }
            updateScratch.setFrame(0, 0, 0, 0);
            Responder old = currentHandler;
            currentHandler = nue;
            nue._activate(old, updateScratch);
            old._resign(updateScratch);
            if (pendingCursor != null) {
                setCursor(pendingCursor);
                pendingCursor = null;
            }
            if (!updateScratch.isEmpty()) {
                repaint(updateScratch);
            }
            repaint();
            return true;
        }
        return false;
    }

    final Responder currentHandler() {
        return currentHandler;
    }

    private boolean logTransitions;

    protected final void logTransitions() {
        logTransitions = true;
    }

    protected final void logTransitions(boolean val) {
        logTransitions = val;
    }

    private Set<XKey> stackTransitionKeys = new HashSet<>();

    private boolean isPrintStackTrace(Class<? extends Responder> from, Class<? extends Responder> to) {
        return stackTransitionKeys == null ? false : stackTransitionKeys.contains(new XKey(from, to));
    }

    protected final void printStackTraceOnTransitions(Class<? extends Responder> from, Class<? extends Responder> to) {
        if (stackTransitionKeys == null) {
            stackTransitionKeys = new HashSet<>();
        }
        stackTransitionKeys.add(new XKey(from, to));
    }

    private static final class XKey {

        private final int idHash1;
        private final int idHash2;

        XKey(Class<? extends Responder> a, Class<? extends Responder> b) {
            idHash1 = a.hashCode();
            idHash2 = b.hashCode();
        }

        public boolean equals(Object o) {
            if (o == this) {
                return true;
            } else if (o == null) {
                return false;
            } else if (o instanceof XKey) {
                return ((XKey) o).idHash1 == idHash1
                        && ((XKey) o).idHash2 == idHash2;
            }
            return false;
        }

        public int hashCode() {
            return idHash1 + (437 * idHash2);
        }
    }
}
