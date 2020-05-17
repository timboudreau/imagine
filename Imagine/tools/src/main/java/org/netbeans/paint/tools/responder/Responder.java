/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.responder;

import com.mastfrog.util.strings.Strings;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Same idea as Apple's Cocoa responder tool - a responder which handles input
 * events, and the call to handle an input event can return a different
 * responder that handles the next such input event - this allows us to have
 * different editors for sequences of start point, control point, end point,
 * etc.
 */
public abstract class Responder {

    public static final Responder NO_OP = new Responder() {
    };
    private boolean active;

    protected final boolean isActive() {
        return active;
    }

    void _activate(Responder prev, Rectangle addTo) {
        active = true;
        onTakeoverFrom(prev);
        activate(addTo);
    }

    void onTakeoverFrom(Responder prev) {

    }

    void _resign(Rectangle addTo) {
        active = false;
        resign(addTo);
    }

    /**
     * Called when this Responder has become the one responsible for handling
     * input events.
     *
     * @param addTo A rectangle to add to if a repaint should be performed as a
     * consequence of activation
     */
    protected void activate(Rectangle addTo) {
    }

    /**
     * Called when another Responder has taken over handling input events.
     *
     * @param addTo A rectangle to alter if a repaint should be performed as a
     * consequence of resignation
     */
    protected void resign(Rectangle addTo) {
        // do nothing
    }

    protected void onAnyMouseEvent(double x, double y, MouseEvent e) {
        // do nothing
    }

    protected void onBeforeHandleInputEvent(InputEvent evt) {

    }

    /**
     * Override to handle mouse clicks
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onClick(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse presses
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onPress(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse releases.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onRelease(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse drags.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onDrag(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse motion events.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onMove(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse enter events.
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onEnter(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle mouse exit events
     *
     * @param x The x coordinate in sub-pixel units
     * @param y The y coordinate in sub-pixel units
     * @param e The originating mouse event
     * @return The next responder or this
     */
    protected Responder onExit(double x, double y, MouseEvent e) {
        return this;
    }

    /**
     * Override to handle key-typed events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onTyped(KeyEvent e) {
        return this;
    }

    /**
     * Override to handle key-pressed events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onKeyPress(KeyEvent e) {
        return this;
    }

    /**
     * Override to handle key-released events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onKeyRelease(KeyEvent e) {
        return this;
    }

    /**
     * Override to handle mouse-wheel events.
     *
     * @param e The event
     * @return The next responder or this
     */
    protected Responder onWheel(MouseWheelEvent e) {
        return this;
    }

    protected Responder defer(Responder next) {
        return new DeferredResponder(next);
    }

    public String toString() {
        return getClass().getSimpleName();
    }

    public Responder or(Responder other) {
        return new OrResponder(other, this);
    }

    private static final class DeferredResponder extends Responder {

        private final Responder delegate;

        public DeferredResponder(Responder delegate) {
            this.delegate = delegate;
        }

        @Override
        protected Responder defer(Responder next) {
            return delegate;
        }

        @Override
        protected Responder onWheel(MouseWheelEvent e) {
            return delegate;
        }

        @Override
        protected Responder onKeyRelease(KeyEvent e) {
            return delegate;
        }

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            return delegate;
        }

        @Override
        protected Responder onTyped(KeyEvent e) {
            return delegate;
        }

        @Override
        protected Responder onExit(double x, double y, MouseEvent e) {
            return delegate;
        }

        @Override
        protected Responder onEnter(double x, double y, MouseEvent e) {
            return delegate;
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            return delegate;
        }

        @Override
        protected Responder onDrag(double x, double y, MouseEvent e) {
            return delegate;
        }

        @Override
        protected Responder onRelease(double x, double y, MouseEvent e) {
            return delegate;
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            return delegate;
        }

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            return delegate;
        }
    }

    static class OrResponder extends Responder implements PaintingResponder {

        private final List<Responder> delegates = new ArrayList<>();

        OrResponder(Responder a, Responder b) {
            delegates.add(a);
            delegates.add(b);
        }

        @Override
        public Rectangle paint(Graphics2D g, Rectangle bounds) {
            Rectangle result = null;
            // process mouse events in forward order, paint backwards
            for (int i = 0; i < delegates.size() - 1; i--) {
                Responder del = delegates.get(i);
                if (del instanceof PaintingResponder) {
                    PaintingResponder resp = (PaintingResponder) del;
                    Rectangle res = resp.paint(g, bounds);
                    if (res != null && !res.isEmpty()) {
                        if (result == null) {
                            result = res;
                        } else {
                            result.add(res);
                        }
                    }
                }
            }
            return result == null ? new Rectangle() : result;
        }

        public Responder or(Responder resp) {
            if (delegates.contains(resp)) {
                return this;
            }
            delegates.add(resp);
            return this;
        }

        @Override
        public String toString() {
            return Strings.join(',', delegates);
        }

        @Override
        protected Responder onWheel(MouseWheelEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onWheel(e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onKeyRelease(KeyEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onKeyRelease(e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onKeyPress(KeyEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onKeyPress(e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onTyped(KeyEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onTyped(e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onExit(double x, double y, MouseEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onExit(x, y, e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onEnter(double x, double y, MouseEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onEnter(x, y, e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onMove(double x, double y, MouseEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onMove(x, y, e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onDrag(double x, double y, MouseEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onDrag(x, y, e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onRelease(double x, double y, MouseEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onRelease(x, y, e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected Responder onPress(double x, double y, MouseEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onPress(x, y, e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;

        }

        @Override
        protected Responder onClick(double x, double y, MouseEvent e) {
            Responder result = this;
            for (Responder del : delegates) {
                Responder maybeRes = del.onClick(x, y, e);
                if (maybeRes != del) {
                    result = maybeRes;
                    break;
                }
            }
            return result;
        }

        @Override
        protected void onBeforeHandleInputEvent(InputEvent evt) {
            for (Responder r : delegates) {
                r.onBeforeHandleInputEvent(evt);
            }
        }

        @Override
        protected void onAnyMouseEvent(double x, double y, MouseEvent e) {
            for (Responder r : delegates) {
                r.onAnyMouseEvent(x, y, e);
            }
        }

        @Override
        protected void resign(Rectangle addTo) {
            for (Responder r : delegates) {
                r.resign(addTo);
            }
        }

        @Override
        protected void activate(Rectangle addTo) {
            for (Responder r : delegates) {
                r.activate(addTo);
            }
        }

        @Override
        void onTakeoverFrom(Responder prev) {
            if (prev instanceof HoverPointResponder) {
                for (Responder r : delegates) {
                    if (r instanceof HoverPointResponder) {
                        ((HoverPointResponder) prev).copyHoverPointTo(r);
                    }
                }
            }
        }
    }
}
