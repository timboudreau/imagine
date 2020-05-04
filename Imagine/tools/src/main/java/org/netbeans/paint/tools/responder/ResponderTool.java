package org.netbeans.paint.tools.responder;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.Set;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.dev.java.imagine.spi.tool.ToolImplementation;
import net.dev.java.imagine.spi.tool.ToolUIContext;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import static net.java.dev.imagine.api.toolcustomizers.Constants.FILL;
import static net.java.dev.imagine.api.toolcustomizers.Constants.FOREGROUND;
import static net.java.dev.imagine.api.toolcustomizers.Constants.STROKE;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.editor.api.Zoom;
import org.netbeans.paint.tools.fills.FillCustomizer;
import org.imagine.editor.api.PaintingStyle;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * Base class for modal drawing tools, where each mouse gesture may change the
 * thing that handles the next gesture.
 *
 * @author Tim Boudreau
 */
public abstract class ResponderTool extends ToolImplementation<Surface> implements CustomizerProvider {

    protected static final FillCustomizer paintC = FillCustomizer.getDefault();
    protected static final Customizer<Color> outlineC = Customizers.getCustomizer(Color.class, FOREGROUND);
    protected static final Customizer<PaintingStyle> fillC = Customizers.getCustomizer(PaintingStyle.class, FILL);
    protected static final Customizer<BasicStroke> strokeC = Customizers.getCustomizer(BasicStroke.class, STROKE, null);

    private final ResponderRepainter part = new ResponderRepainter();
    private Responder currentHandler = Responder.NO_OP;
    private boolean active;
    private Repainter repainter;
    private ToolUIContext ctx;
    private final Rectangle lastRepaintBounds = new Rectangle();
    private final ResponderInputListener inputListener = new ResponderInputListener(this);

    protected ResponderTool(Surface obj) {
        super(obj);
    }

    protected ToolUIContext ctx() {
        return ctx == null ? ToolUIContext.DUMMY_INSTANCE : ctx;
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
    public final void attach(Lookup.Provider on, ToolUIContext ctx) {
        assert ctx != null : "Null ToolUIContext";
        this.ctx = ctx;
        lastRepaintBounds.setFrame(0, 0, 0, 0);
        onAttach();
        active = true;
        currentHandler = firstResponder();
    }

    @Override
    public final void detach() {
        reset();
        currentHandler = Responder.NO_OP;
        active = false;
        Repainter rep = this.repainter;
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
        if (isCommit) {
            return paintCommit(g);
        } else {
            Rectangle result = paintLive(g, layerBounds);
            if (currentHandler instanceof PaintingResponder) {
                Rectangle r = ((PaintingResponder) currentHandler).paint(g, layerBounds);
                if (result != null) {
                    result.add(r);
                    r = result;
                }
                result = r;
            }
            if (result != null) {
                lastRepaintBounds.setFrame(result);
            } else {
                lastRepaintBounds.width = 0;
                lastRepaintBounds.height = 0;
            }
            return result;
        }
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
        repaint(null);
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

    protected final void setCursor(Cursor cursor) {
        if (repainter != null) {
            repainter.setCursor(cursor);
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
            onAttachRepainter(repainter);
        }

        @Override
        public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
            if (active) {
                Rectangle r = ResponderTool.this.paint(g2d, layerBounds, commit);
                repaint(r);
            }
        }
    }

    private final Rectangle updateScratch = new Rectangle();

    protected boolean updateHandler(Responder nue) {
        if (nue != currentHandler) {
            updateScratch.setFrame(0, 0, 0, 0);
            currentHandler._resign(updateScratch);
            currentHandler = nue;
            nue._activate(updateScratch);
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
}
