package org.netbeans.paint.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Set;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant.Repainter;
import net.dev.java.imagine.spi.tool.ToolImplementation;
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
 * Base class for modal drawing tools, where each mouse gesture may change
 * the thing that handles the next gesture.
 *
 * @author Tim Boudreau
 */
abstract class MultiStateTool extends ToolImplementation<Surface> implements CustomizerProvider {

    protected static final FillCustomizer paintC = FillCustomizer.getDefault();
    protected static final Customizer<Color> outlineC = Customizers.getCustomizer(Color.class, FOREGROUND);
    protected static final Customizer<PaintingStyle> fillC = Customizers.getCustomizer(PaintingStyle.class, FILL);
    protected static final Customizer<Float> strokeC = Customizers.getCustomizer(Float.class, STROKE, 0.05F, 10F);

    private final Part part = new Part();
    private InputHandler currentHandler = InputHandler.NO_OP;
    private boolean active;
    private Repainter repainter;

    public MultiStateTool(Surface obj) {
        super(obj);
    }

    @Override
    public Customizer<?> getCustomizer() {
        return new AggregateCustomizer<Void>("Freehand", fillC, paintC, outlineC, strokeC);
    }

    @Override
    public void createLookupContents(Set<? super Object> addTo) {
        addTo.add(this);
        addTo.add(ml);
        addTo.add(part);
    }

    @Override
    public final void attach(Lookup.Provider on) {
        onAttach();
        active = true;
        currentHandler = createInitialInputHandler();
    }

    @Override
    public final void detach() {
        clearState();
        currentHandler = InputHandler.NO_OP;
        active = false;
        repainter = null;
        onDetach();
    }

    protected void onAttach() {

    }

    protected void onDetach() {

    }

    protected abstract void clearState();

    protected abstract InputHandler createInitialInputHandler();

    protected abstract Rectangle paintCommit(Graphics2D g);

    protected abstract Rectangle paintLive(Graphics2D g, Rectangle layerBounds);

    private Rectangle paint(Graphics2D g, Rectangle layerBounds, boolean isCommit) {
        if (isCommit) {
            return paintCommit(g);
        } else {
            Rectangle result = paintLive(g, layerBounds);
            if (currentHandler instanceof Paintable) {
                Rectangle r = ((Paintable) currentHandler).paint(g, layerBounds);
                if (result != null) {
                    result.add(r);
                    r = result;
                }
                return r;
            } else {
                return result;
            }
        }
    }

    protected static float zoomFactor() {
        Zoom zoom = Utilities.actionsGlobalContext().lookup(Zoom.class);
        if (zoom != null) {
            zoom.getZoom();
        }
        return 1;
    }

    protected final void commit() {
        if (repainter != null) {
            repainter.requestCommit();
        }
    }

    protected final void repaint() {
        if (repainter != null) {
            repainter.requestRepaint();
        }
    }

    protected final void repaint(Rectangle bounds) {
        if (repainter != null) {
            if (bounds == null) {
                repainter.requestRepaint();
            } else {
                repainter.requestRepaint(bounds);
            }
        }
    }

    private final class Part implements PaintParticipant {

        @Override
        public void attachRepainter(Repainter repainter) {
            MultiStateTool.this.repainter = repainter;
        }

        @Override
        public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
            if (active) {
                Rectangle r = MultiStateTool.this.paint(g2d, layerBounds, commit);
                repaint(r);
            }
        }
    }

    protected interface Paintable {

        Rectangle paint(Graphics2D g, Rectangle bounds);
    }

    private final Rectangle updateScratch = new Rectangle();

    protected boolean updateHandler(InputHandler nue) {
        if (nue != currentHandler) {
            updateScratch.setFrame(0, 0, 0, 0);
            currentHandler._deactivate(updateScratch);
            currentHandler = nue;
            nue._activate(updateScratch);
            if (!updateScratch.isEmpty()) {
                repaint(updateScratch);
            }
            return true;
        }
        return false;
    }

    protected static abstract class InputHandler {

        static final InputHandler NO_OP = new InputHandler() {
        };
        private boolean active;

        protected final boolean isActive() {
            return active;
        }

        private void _activate(Rectangle addTo) {
            active = true;
            activate(addTo);
        }

        private void _deactivate(Rectangle addTo) {
            active = false;
            deactivate(addTo);
        }

        protected void activate(Rectangle addTo) {
        }

        protected void deactivate(Rectangle addTo) {
        }

        protected InputHandler onClick(MouseEvent e) {
            return this;
        }

        protected InputHandler onPress(MouseEvent e) {
            return this;
        }

        protected InputHandler onRelease(MouseEvent e) {
            return this;
        }

        protected InputHandler onDrag(MouseEvent e) {
            return this;
        }

        protected InputHandler onMove(MouseEvent e) {
            return this;
        }

        protected InputHandler onEnter(MouseEvent e) {
            return this;
        }

        protected InputHandler onExit(MouseEvent e) {
            return this;
        }

        protected InputHandler onTyped(KeyEvent e) {
            return this;
        }

        protected InputHandler onKeyPress(KeyEvent e) {
            return this;
        }

        protected InputHandler onKeyRelease(KeyEvent e) {
            return this;
        }

        protected InputHandler onWheel(MouseWheelEvent e) {
            return this;
        }
    }

    private final ML ml = new ML();

    private final class ML implements MouseListener, MouseMotionListener, KeyListener, MouseWheelListener {

        @Override
        public void mouseClicked(MouseEvent e) {
            if (updateHandler(currentHandler.onClick(e))) {
                e.consume();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (updateHandler(currentHandler.onPress(e))) {
                e.consume();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (updateHandler(currentHandler.onRelease(e))) {
                e.consume();
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (updateHandler(currentHandler.onDrag(e))) {
                e.consume();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            if (updateHandler(currentHandler.onMove(e))) {
                e.consume();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            if (updateHandler(currentHandler.onEnter(e))) {
                e.consume();
            }
        }

        @Override
        public void mouseExited(MouseEvent e) {
            if (updateHandler(currentHandler.onExit(e))) {
                e.consume();
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {
            if (updateHandler(currentHandler.onTyped(e))) {
                e.consume();
            }
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (updateHandler(currentHandler.onKeyPress(e))) {
                e.consume();
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (updateHandler(currentHandler.onKeyRelease(e))) {
                e.consume();
            }
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (updateHandler(currentHandler.onWheel(e))) {
                e.consume();
            }
        }
    }

}
