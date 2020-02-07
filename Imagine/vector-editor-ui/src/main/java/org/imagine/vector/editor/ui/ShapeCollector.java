/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.java.dev.imagine.api.vector.Attribute;
import net.java.dev.imagine.api.vector.Fillable;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.Shaped;
import net.java.dev.imagine.api.vector.Vector;
import net.java.dev.imagine.api.vector.Volume;
import net.java.dev.imagine.api.vector.elements.Clear;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.StringWrapper;
import net.java.dev.imagine.api.vector.graphics.AffineTransformWrapper;
import net.java.dev.imagine.api.vector.graphics.Background;
import net.java.dev.imagine.api.vector.graphics.BasicStrokeWrapper;
import net.java.dev.imagine.api.vector.graphics.ColorWrapper;
import net.java.dev.imagine.api.vector.graphics.FontWrapper;
import net.java.dev.imagine.api.vector.graphics.PaintWrapper;
import net.java.dev.imagine.api.vector.painting.VectorRepaintHandle;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.netbeans.paint.api.util.Holder;

/**
 *
 * @author Tim Boudreau
 */
public class ShapeCollector implements Consumer<Primitive>, VectorRepaintHandle {

    private final Holder<Background> clear = Holder.of(new Background(Color.BLACK));
    private final Holder<PaintWrapper> bg = Holder.create();
    private final Holder<PaintWrapper> fg = Holder.create();
    private final Holder<FontWrapper> font = Holder.of(new FontWrapper(new Font("SansSerif", Font.PLAIN, 12)));
    private final Holder<BasicStrokeWrapper> stroke = Holder.create();
    private final Holder<AffineTransformWrapper> xform = Holder.create();

    private PaintWrapper lastPaint;
    private Primitive lastTarget;

    @Override
    public void accept(Primitive t) {
        t.as(Attribute.class, attr -> {
            System.out.println("  attr " + attr);
            t.as(Background.class, bg -> {
                clear.set(bg);
            }).as(ColorWrapper.class, cw -> {
                lastPaint = cw;
            }).as(PaintWrapper.class, pw -> {
                lastPaint = pw;
            }).as(BasicStrokeWrapper.class, stk -> {
                stroke.set(stk);
            }).as(AffineTransformWrapper.class, atw -> {
                xform.set(atw);
            }).as(FontWrapper.class, fnt -> {
                font.set(fnt);
            });
        });
        t.as(Fillable.class, fill -> {
            boolean f = fill.isFill();
            if (lastPaint != null) {
                if (f) {
                    bg.set(lastPaint);
                    lastPaint = null;
                } else {
                    fg.set(lastPaint);
                    lastPaint = null;
                }
            }
            if (f) {
                outputFill(t);
            } else {
                outputDraw(t);
            }
        }, () -> {
            t.as(Vector.class, v -> {
                if (lastPaint != null) {
                    fg.set(lastPaint);
                    lastPaint = null;
                }
                outputDraw(t);
            }, () -> {
                t.as(StringWrapper.class, sw -> {
                    outputString(sw);
                }, () -> {
                    t.as(Volume.class, vol -> {
                        outputFill(vol);
                    });
                });
            });
        });
    }

    void replay(Shapes shapes) {
        System.out.println("\n\n----------------------");
        entries.forEach((e) -> e.addTo(shapes));
        System.out.println("\n-------------------------");
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    public Rectangle getBounds() {
        return bounds;
    }

    private Rectangle bounds = new Rectangle();
    @Override
    public void repaintArea(int x, int y, int w, int h) {
        bounds.add(new Rectangle(x, y, w, h));
    }

    @Override
    public void drawn(Primitive shape) {
        accept(shape);
    }

    @Override
    public void draw(Primitive shape) {
        throw new UnsupportedOperationException("Huh?");
    }

    private static class Entry {

        private final Shaped target;

        private final Holder<Background> clear;
        private final Holder<PaintWrapper> bg;
        private final Holder<PaintWrapper> fg;
        private final Holder<FontWrapper> font;
        private final Holder<BasicStrokeWrapper> stroke;
        private final Holder<AffineTransformWrapper> xform;
        private boolean draw;
        private boolean fill;

        public Entry(Shaped target, Holder<Background> clear,
                Holder<PaintWrapper> bg, Holder<PaintWrapper> fg,
                Holder<FontWrapper> font, Holder<BasicStrokeWrapper> stroke,
                Holder<AffineTransformWrapper> xform, boolean draw, boolean fill) {
            this.target = target;
            this.clear = clear.copy();
            this.bg = bg.copy();
            this.fg = fg.copy();
            this.font = font.copy();
            this.stroke = stroke.copy();
            this.xform = xform.copy();
            this.draw = draw;
            this.fill = fill;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder(target.getClass().getSimpleName())
                    .append('(');
            if (fill) {
                sb.append("filled ");
            }
            bg.ifSet(p -> {
                sb.append("fillPaint ").append(p).append(' ');
            });
            if (draw) {
                sb.append("drawn ");
            }
            fg.ifSet(p -> {
                sb.append("drawPaint ").append(p).append(' ');
            });
            stroke.ifSet(s -> {
                sb.append("stroke ").append(s).append(' ');
            });
            xform.ifSet(x -> {
                sb.append("xform ").append(x).append(' ');
            });
            if (target instanceof Clear) {
                clear.ifSet(c -> {
                    sb.append("clear ").append(c);
                });
            }
            return sb.toString();
        }

        void addTo(Shapes s) {
            System.out.println(" * " + this);
            if (target instanceof Clear) {
                s.add(target, clear.isSet() ? clear.get().toPaint() : null,
                        null, null, false, true);
            } else {
                s.add(target, bg.isSet() ? bg.get().toPaint() : null,
                        fg.isSet() ? fg.get().toPaint() : null,
                        stroke.isSet() ? stroke.get().toStroke() : null,
                        draw, fill);
            }
        }
    }

    private void addFillToLastTarget() {
        Entry last = entries.get(entries.size() - 1);
        last.bg.setFrom(bg);
        last.fill = true;
    }

    private void addDrawToLastTarget() {
        Entry last = entries.get(entries.size() - 1);
        last.fg.setFrom(fg);
        last.stroke.setFrom(stroke);
        last.draw = true;
    }

    private List<Entry> entries = new ArrayList<>();

    void outputFill(Primitive target) {
        if (target.equals(lastTarget)) {
            addFillToLastTarget();
        } else {
            Entry e = new Entry((Shaped) target, clear, bg, fg, font, stroke,
                    xform, false, true);
            entries.add(e);
            this.lastTarget = target;
        }
    }

    void outputDraw(Primitive target) {
        if (target.equals(lastTarget)) {
            addDrawToLastTarget();
        } else {
            Entry e = new Entry((Shaped) target, clear, bg, fg, font, stroke,
                    xform, false, true);
            entries.add(e);
            this.lastTarget = target;
        }
    }

    void outputString(StringWrapper sw) {
        Font f = font.get().get();
        GraphicsUtils.newBufferedImage(1, 1, g -> {
            FontRenderContext frc = g.getFontRenderContext();
            GlyphVector gv = f.createGlyphVector(frc, sw.getText());
            PathIteratorWrapper iw = new PathIteratorWrapper(gv, (float) sw.x,
                    (float) sw.y);
            outputFill(iw);
        });
    }

}
