package org.netbeans.paint.api.cursor;

import java.awt.BasicStroke;
import static java.awt.BasicStroke.CAP_BUTT;
import static java.awt.BasicStroke.CAP_ROUND;
import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_MITER;
import static java.awt.BasicStroke.JOIN_ROUND;
import java.awt.Color;
import static java.awt.Color.BLUE;
import static java.awt.Color.RED;
import static java.awt.Color.WHITE;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import static java.lang.Math.min;
import org.imagine.geometry.Arrow;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EnhRectangle2D;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.Rhombus;
import org.imagine.geometry.Triangle2D;
import static org.netbeans.paint.api.cursor.CursorUtils.applyRenderingHints;

/**
 *
 * @author Tim Boudreau
 */
final class CursorShapes {

    void drawCloseShape(Graphics2D g, CursorProperties props) {
//        double rad = (props.centerX() - ((props.width() / 12) + (props.width() / 16)));
        double rad = (props.centerX() - ((props.width() / 12) + (props.width() / 12)));

        Circle circ = new Circle(props.centerX(), props.centerY(), rad);
        Area a = new Area(new Rectangle(0, 0, props.centerX(), props.centerY()));
        Area a2 = new Area(new Rectangle(0, 0, props.width(), props.height()));
        a2.subtract(a);
        g.setClip(a2);
        g.setColor(props.shadow());
        g.setStroke(props.shadowStroke());
        g.draw(circ);

        g.setColor(props.primary());
        g.setStroke(props.mainStroke());
        g.draw(circ);
        g.setClip(null);
        BasicStroke mstroke = new BasicStroke(props.mainStroke().getLineWidth(), CAP_SQUARE, JOIN_MITER, 17F);
        BasicStroke lstroke = new BasicStroke(props.shadowStroke().getLineWidth(), CAP_SQUARE, JOIN_MITER, 17F);
        Triangle2D[] tris = new Triangle2D[2];
        double off = -(props.width() / 16);
        double ooff = props.width() / 32;
        Runnable heads = () -> {
            double headLength = props.width() / 10;
            circ.positionOf(0, (x, y) -> {
                Circle.positionOf(135, x + off, y, headLength, (x1, y1) -> {
                    Circle.positionOf(45, x + off, y, headLength, (x2, y2) -> {
                        g.draw(tris[0] = new Triangle2D(x + off, y + ooff, x1 - off, y1 + ooff, x2 - off, y2 + ooff));
                    });
                });
            });
            circ.positionOf(270, (x, y) -> {
                Circle.positionOf(135, x, y - off, headLength, (x1, y1) -> {
                    Circle.positionOf(-135, x, y - off, headLength, (x2, y2) -> {
                        g.draw(tris[1] = new Triangle2D(x + ooff, y + off, x1 + ooff, y1 + off, x2 + ooff, y2 + off));
                    });
                });
            });
        };
        g.setClip(0, 0, props.width(), props.height());
        g.setColor(props.shadow());
        g.setStroke(lstroke);
        heads.run();
        int lo = props.width() / 12;
        g.drawLine(lo, lo, props.centerX() / 2, props.centerY() / 2);
        g.setColor(props.primary());
        g.setStroke(mstroke);
        heads.run();
        g.drawLine(lo, lo, props.centerX() / 2, props.centerY() / 2);
        g.fill(tris[0]);
        g.fill(tris[1]);

        Rectangle2D.Double r = new Rectangle2D.Double();
        r.x = tris[0].bx() + (off / 2) + (mstroke.getLineWidth() / 1.5);
        r.y = tris[0].centerY() - (mstroke.getLineWidth() / 2);
        r.width = mstroke.getLineWidth();
        r.height = mstroke.getLineWidth() + (mstroke.getLineWidth() * 0.125);
        g.fill(r);

        r.x = tris[1].ax() - (mstroke.getLineWidth() / 2);
        r.y = tris[1].by() - (off / 2) + (mstroke.getLineWidth() / 1.5);
        r.y -= mstroke.getLineWidth();
        r.height = mstroke.getLineWidth();
        r.width = mstroke.getLineWidth() + (mstroke.getLineWidth() * 0.125);
        g.fill(r);
    }

    void drawShortArrow(Graphics2D g, CursorProperties props) {
        Area bds = new Area(new Rectangle(0, 0, props.width(), props.height()));
        Area a = new Area(new Triangle2D(props.width(), props.height(),
                props.width(), 0,
                0, props.height()));
        bds.subtract(a);
        g.setClip(bds);
        drawAngle45(g, props);
        g.setColor(props.shadow());
        EqLine line = EqLine.forAngleAndLength(props.centerX(), props.centerX(), -45,
                props.mainStroke().getLineWidth() / 2);
        g.draw(line);
        g.setClip(null);
//        drawAngle45(g, props);
//        g.setBackground(new Color(255, 255, 255, 0));
//        g.clearRect(props.centerX(), props.centerY(), (props.width() - props.centerX()) + 2, (props.height() - props.centerY()) + 2);
    }

    void drawArrowTilde(Graphics2D g, CursorProperties props) {
//        Area bds = new Area(new Rectangle(0, 0, props.width(), props.height()));
//        Area a = new Area(new Triangle2D(props.width(), props.height(),
//                props.width(), 0,
//                0, props.height()));
//        bds.subtract(a);
//        g.setClip(bds);
//        drawAngle45(g, props);
//        g.setClip(null);
        drawShortArrow(g, props);

        g.translate(-((props.width() / 16) - props.width() / 16), -((props.height() / 4) + (props.height() / 8) + props.height() / 32));

        int in = props.width() / 16;
        int in2 = in * 2;
        int halfY = ((props.height() - props.centerY()) / 2) - in;
        int halfX = ((props.width() - props.centerX()) / 2) - in;

        int xsub = props.width() / 16;

        Path2D.Double path = new Path2D.Double();
        double ax = props.centerX() + in;
        double ay = props.centerY() + halfY + in / 2;
        path.moveTo(ax + xsub + xsub, ay);

        double cx = props.width() - in;
        double cy = props.centerY() + halfY + in / 2;

        double qx = props.centerX() + halfX + in;
        double qy = props.centerY() + in;

        path.quadTo(qx + (xsub / 2), qy, cx, cy);
        double len = cx - (ax + xsub * 3);
        double qy2 = ay + (in * 2);
        path.quadTo(qx + (len + (xsub * 2) - (xsub / 2)), qy2, cx + len, ay);

        g.translate(-props.centerX() + (props.width() / 16), props.centerY() - (props.width() / 16));

        g.setStroke(props.shadowStroke());
        g.setColor(props.shadow());
        g.draw(path);
        g.setColor(props.primary());
        g.setStroke(props.mainStroke());
        g.draw(path);
    }

    void drawArrowPlus(Graphics2D g, CursorProperties props) {
        drawShortArrow(g, props);
        g.translate(-((props.width() / 16) - props.width() / 16), -((props.height() / 4) + (props.height() / 8) + props.height() / 32));
        int in = props.width() / 12;
        int in2 = in * 2;
        int halfY = ((props.height() - props.centerY()) / 2) - in;
        int halfX = ((props.width() - props.centerX()) / 2) - in;
        int off = props.width() / 24;
        g.setStroke(props.shadowStroke());
        g.setColor(props.shadow());
        g.drawLine(props.centerX() + in + off, props.centerY() + halfY + in / 2, props.width() - (in + off), props.centerY() + halfY + in / 2);
        g.drawLine(props.centerX() + halfX + in, props.centerY() + in, props.centerX() + halfX + in, props.height() - in2);
        g.setColor(props.primary());
        g.setStroke(props.mainStroke());
        g.drawLine(props.centerX() + in + off, props.centerY() + halfY + in / 2, props.width() - (in + off), props.centerY() + halfY + in / 2);
        g.drawLine(props.centerX() + halfX + in, props.centerY() + in, props.centerX() + halfX + in, props.height() - in2);
    }

    void drawDottedRect(Graphics2D g, CursorProperties props) {
        int off = props.width() / 8;
        Rectangle rect = new Rectangle(off, off, props.width() - (off * 2), props.height() - (off * 2));
        float ws = props.mainStroke().getLineWidth() * 1.5F;
        int sz = props.width() - (off * 2);
        float[] flts = new float[]{sz / 4F};
        float shift = (sz / 8);
        BasicStroke shadowStroke = new BasicStroke(ws, CAP_ROUND, JOIN_ROUND, 1F, flts, shift);
        g.setStroke(shadowStroke);
        g.setColor(props.shadow());
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
        float wl = props.mainStroke().getLineWidth();
        g.setColor(props.primary());
        BasicStroke lineStroke = new BasicStroke(wl, CAP_BUTT, JOIN_ROUND, 1F, flts, shift);
        g.setStroke(lineStroke);
        g.drawRect(rect.x, rect.y, rect.width, rect.height);
    }

    static void clipped(Graphics2D g, Shape clip, Runnable r) {
        Shape old = g.getClip();
        g.setClip(clip);
        try {
            r.run();
        } finally {
            g.setClip(old);
        }
    }

    static void clipped(Graphics2D g, Shape clip, Stroke stroke, Runnable r) {
        if (true) {
            r.run();
        }
        Shape old = g.getClip();
        g.setClip(stroke.createStrokedShape(clip));
        try {
            r.run();
        } finally {
            g.setClip(old);
        }
    }

    void drawRotateMany(Graphics2D g, CursorProperties props) {
        drawRotate(g, props);
        double off = (props.width() / 9);
        double rad = props.width() / 10;
        Circle circ = new Circle(props.centerX() - off, props.centerY() - off, rad);
        Path2D.Double p = new Path2D.Double();
        clipped(g, circ, props.shadowStroke(), () -> {
            p.append(props.shadowStroke().createStrokedShape(circ), false);
            fillLine(g, props, circ);
            paintLine(g, props, circ);
        });
        circ.setCenter(props.centerX() + off, props.centerY() + off);
        clipped(g, circ, props.shadowStroke(), () -> {
            p.append(props.shadowStroke().createStrokedShape(circ), false);
            fillLine(g, props, circ);
            paintLine(g, props, circ);
        });
        circ.setCenter(props.centerX() - off, props.centerY() - off);
        clipped(g, circ, props.shadowStroke(), () -> {
            p.append(props.mainStroke().createStrokedShape(circ), false);
            paintLine(g, props, circ);
        });
        clear(g, p, props);
    }

    static void clear(Graphics2D g, Shape invert, CursorProperties props) {
        Area a = new Area(new Rectangle(0, 0, props.width(), props.height()));
        a.subtract(new Area(invert));
        g.setClip(a);
        g.setBackground(new Color(255, 255, 255, 0));
//        g.setColor(Color.ORANGE);

//        for (int x = 0; x < props.width(); x++) {
//            for (int y = 0; y < props.width(); y++) {
//                if (a.contains(x, y)) {
//                    g.clearRect(x, y, 1, 1);
//                }
//            }
//        }
//        g.fillRect(0, 0, props.width(), props.height());
    }

    void drawRotate(Graphics2D g, CursorProperties props) {
        g.transform(AffineTransform.getQuadrantRotateInstance(2, props.centerX(), props.centerY()));
//        g.rotate(Math.toRadians(270), props.centerX(), props.centerY());
        double space = props.width() / 8;
        double rad = (props.width() - (props.width() / 3)) / 2;
        Circle circ = new Circle(props.centerX(), props.centerY(), rad);
        Area a = new Area(new Rectangle(0, 0, props.width(), props.height()));
        a.subtract(new Area(new Rectangle2D.Double(props.centerX(), props.centerY(), props.width() / 2, props.height() / 2)));
        double headAngle = 22.5;
        double dist = space;// + props.width() / 16;
        double arrOff = 0.5;
        double arrHeadDist = -2;
        circ.positionOf(90, (x, y) -> {
            double ht = props.height() / 6;
            Arrow arrow = new Arrow(ht, false, true, x, y + arrOff, x, y + ht + dist);
            arrow.headAngleB = headAngle;
            arrow.setHeadOffset(arrHeadDist);
            fillLine(g, props, arrow);
        });

        Shape old = g.getClip();
        g.setClip(a);
        fillLine(g, props, circ);
        paintLine(g, props, circ);

        g.setClip(old);
        circ.positionOf(90, (x, y) -> {
            double ht = props.height() / 6;
            Arrow arrow = new Arrow(ht, false, true, x, y + arrOff, x, y + ht + dist);
            arrow.headAngleB = headAngle;
            arrow.setHeadOffset(arrHeadDist);
            paintLine(g, props, arrow);
//            double w = (props.width() / 16);
//            Rectangle2D.Double rr = new Rectangle2D.Double(arrow.x2 - (w / 2), arrow.y2 + (w / 4), w, w);
//            g.fill(rr);
        });
        circ.positionOf(180, (x, y) -> {
            double h = props.shadowStroke().getLineWidth();
            EqLine ln = new EqLine(x, y - (h / 3.25), x, y + (h / 3.25));
            g.setColor(props.shadow());
            g.setStroke(props.mainStroke());
            g.draw(ln);
        });

    }

    void drawMultiMove(Graphics2D g, CursorProperties props) {
        double majorRadius = props.width() - (props.width() / 2);
        double radius = props.width() / 6;
        double qtr = props.width() / 4;
        Circle big = new Circle(props.centerX() + qtr, props.centerY() + qtr, majorRadius);

        Circle circ = new Circle(0, 0, radius);

        Circle cen = new Circle(0, 0, radius / 3);

        EqPointDouble bottomRight = new EqPointDouble(props.width() - radius / 2, props.height() - radius / 2);
        EqPointDouble bottomRightPlus = bottomRight.copy();
        bottomRightPlus.translate(props.width() / 32, props.width() / 32);

        EqLine ln = new EqLine(bottomRight, big.center());
        g.setColor(WHITE);
        double r4 = big.radius() / 2;
        EqLine ln2 = new EqLine(bottomRightPlus, bottomRightPlus);
//        g.draw(big);
        big.positionOf(315, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            Point2D oldPoint2 = ln.getP2();
            big.positionOf(135, r4, (x1, y1) -> {
                ln.setPoint2(x1, y1);
            });
//            ln.setPoint1(circ.centerX(), circ.centerY());
            fillLine(g, props, ln);
            drawCircle(g, props, circ);
            ln.setPoint2(oldPoint2);
        });

        big.positionOf(0, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            fillLine(g, props, ln);
            drawCircle(g, props, circ);
        });

        big.positionOf(270, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            fillLine(g, props, ln);
            drawCircle(g, props, circ);
        });

        ln2.setPoint1(bottomRightPlus.x, bottomRightPlus.y - radius);
        fillLine(g, props, ln2);
        ln2.setPoint1(bottomRightPlus.x - radius, bottomRightPlus.y);
        fillLine(g, props, ln2);

        big.positionOf(315, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            Point2D oldPoint2 = ln.getP2();
            big.positionOf(135, r4, (x1, y1) -> {
                ln.setPoint2(x1, y1);
            });
            paintLine(g, props, ln);
            ln.setPoint2(oldPoint2);

            cen.setCenter(x, y);
            g.setColor(props.shadow());
            g.fill(cen);
//            g.setBackground(new Color(255,255,255,0));
//            g.clearRect(cen.addToBounds().x, cen.addToBounds().y, cen.addToBounds().width, cen.addToBounds().height);
        });

        big.positionOf(0, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            paintLine(g, props, ln);

            cen.setCenter(x, y);
            g.setColor(props.shadow());
            g.fill(cen);
//            g.setBackground(new Color(255,255,255,0));
//            g.clearRect(cen.addToBounds().x, cen.addToBounds().y, cen.addToBounds().width, cen.addToBounds().height);
        });

        big.positionOf(270, (x, y) -> {
            circ.setCenter(x, y);
            ln.setPoint1(circ.centerX(), circ.centerY());
            paintLine(g, props, ln);

            cen.setCenter(x, y);
            g.setColor(props.shadow());
            g.fill(cen);
//            g.setBackground(new Color(255,255,255,0));
//            g.clearRect(cen.addToBounds().x, cen.addToBounds().y, cen.addToBounds().width, cen.addToBounds().height);
        });

        ln2.setPoint1(bottomRightPlus.x, bottomRightPlus.y - radius);
        paintLine(g, props, ln2);
        ln2.setPoint1(bottomRightPlus.x - radius, bottomRightPlus.y);
        paintLine(g, props, ln2);
    }

    void drawCircle(Graphics2D g, CursorProperties props, Circle circ) {
        g.setStroke(props.mainStroke());
        g.setColor(props.shadow());
        g.draw(circ);
        g.setColor(props.primary());
        g.fill(circ);
    }

    void drawLine(Graphics2D g, CursorProperties props, Shape line) {
        fillLine(g, props, line);
        paintLine(g, props, line);
    }

    void paintLine(Graphics2D g, CursorProperties props1, Shape line) {
        g.setStroke(props1.mainStroke());
        g.setColor(props1.primary());
        g.draw(line);
    }

    void fillLine(Graphics2D g, CursorProperties props, Shape line) {
        Object old = g.getRenderingHint(KEY_ANTIALIASING);
//        g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF);
        g.setStroke(props.shadowStroke());
//        g.setStroke(new BasicStroke(props1.mainStroke().getLineWidth() * 1.5F));
        g.setColor(props.shadow());
        g.draw(line);
//        g.setRenderingHint(KEY_ANTIALIASING, old);
    }

    void drawAnglesCrossed(Graphics2D g, CursorProperties props) {
        AffineTransform old = g.getTransform();
        drawAngle45(g, props);
        g.transform(AffineTransform.getQuadrantRotateInstance(1, props.width() / 2, props.height() / 2));
        drawAngle45(g, props);
        g.transform(AffineTransform.getQuadrantRotateInstance(1, props.width() / 2, props.height() / 2));
        g.setStroke(props.mainStroke());
        g.setColor(props.primary());
        g.drawLine(props.cornerOffset(), props.cornerOffset(), props.width() - props.cornerOffset(), props.height() - props.cornerOffset());
        g.setBackground(new Color(255, 255, 255, 0));
        EnhRectangle2D r = new EnhRectangle2D(0, 0, props.width() / 6, props.height() / 6);
        r.setCenter(props.centerX(), props.centerY());
        Rectangle r2 = r.getBounds();
        g.clearRect(r2.x, r2.y, r2.width, r2.height);
        g.setTransform(old);
    }

    void drawAngle45(Graphics2D g, CursorProperties props) {
        g.setStroke(props.shadowStroke());
        g.setColor(props.shadow());
        int cornerOffset = props.cornerOffset();
        int w = props.width();
        int h = props.height();

        int leftTop = w / 8;
        int cxA = props.centerX() - (w / 8);
        int cxB = props.centerX() + (w / 8);
        int rightBottom = w - (w / 8);

        g.drawLine(cornerOffset, cornerOffset, w - cornerOffset, h - cornerOffset);
        g.drawLine(leftTop, leftTop, leftTop, cxA);
        g.drawLine(leftTop, leftTop, cxA, leftTop);
        g.drawLine(rightBottom, rightBottom, rightBottom, cxB);
        g.drawLine(rightBottom, rightBottom, cxB, rightBottom);
        g.setStroke(props.mainStroke());
        g.setColor(props.primary());
        g.drawLine(cornerOffset, cornerOffset, w - cornerOffset, h - cornerOffset);
        g.drawLine(leftTop, leftTop, leftTop, cxA);
        g.drawLine(leftTop, leftTop, cxA, leftTop);
        g.drawLine(rightBottom, rightBottom, rightBottom, cxB);
        g.drawLine(rightBottom, rightBottom, cxB, rightBottom);
    }

    void drawAngle0_90(Graphics2D g, CursorProperties props) {
        int w = props.width();
        int h = props.height();
        int start = w / 8;
        int end = w - start;
        g.setColor(props.shadow());
        g.setStroke(props.wideShadowStroke());

        int centerX = props.centerX();
        int centerY = props.centerY();

        g.drawLine(start + (w / 16), centerY, end - (w / 16), centerY);
        g.drawLine(start, centerY, (w / 4), (h / 2) - (h / 8));
        g.drawLine(start, centerY, (w / 4), (h / 2) + (h / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY - (w / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY + (w / 8));
        g.setStroke(props.wideMainStroke());
        g.setColor(props.primary());
        g.drawLine(start + (w / 16), centerY, end - (w / 16), centerY);
        g.drawLine(start, centerY, (w / 4), (h / 2) - (h / 8));
        g.drawLine(start, centerY, (w / 4), (h / 2) + (h / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY - (w / 8));
        g.drawLine(end, centerY, centerX + (w / 4), centerY + (w / 8));
    }

    void drawX(Graphics2D g, CursorProperties props) {
        g.setStroke(props.wideShadowStroke());
        g.setColor(props.shadow());

        int w = props.width();
        int h = props.height();
        int x1 = w / 4;
        int y1 = h / 4;
        int x2 = w - x1;
        int y2 = h - y1;

        g.drawLine(x1, y2, x2, y1);
        g.drawLine(x1, y1, x2, y2);

        g.setStroke(props.wideMainStroke());
        g.setColor(props.primary());
        g.drawLine(x1, y2, x2, y1);
        g.drawLine(x1, y1, x2, y2);
    }

    void drawRhombus(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Rhombus rhom = new Rhombus(new Rectangle(o1 * 2, o1 * 2, w - (o1 * 4), h - (o1 * 4)), 0);
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
    }

    void drawTriangle(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Triangle2D rhom = new Triangle2D(o1 * 2, o1 * 2, w - (o1 * 2), o1 * 2, props.centerX(), h - (o1 * 2));
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
    }

    void drawTriangleRight(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Triangle2D rhom = new Triangle2D(o1 * 2, o1 * 2, o1 * 2, h - (o1 * 2), w - (o1 * 2), props.centerY());
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        g.setColor(BLUE);
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
        g.setColor(RED);
        g.draw(new Line2D.Double(rhom.ax(), rhom.ay(), rhom.centerX(),
                rhom.cy()));
    }

    void drawTriangleLeft(Graphics2D g, CursorProperties props, boolean fill) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;

        Triangle2D rhom = new Triangle2D(w - (o1 * 2), o1 * 2, w - (o1 * 2), h - (o1 * 2), 0, props.centerY());
        if (fill) {
            g.setColor(props.primary());
            g.fill(rhom);
        }
        fillLine(g, props, rhom);
        paintLine(g, props, rhom);
    }

    void drawArrowsCrossed(Graphics2D g, CursorProperties props) {
        int w = props.width();
        int h = props.height();
        int o1 = w / 16;
        int e = w / 8;
        Triangle2D tri = new Triangle2D(o1, o1 + e, props.centerX() - e, props.centerY(), o1, w - (o1 + e));
        Triangle2D tri2 = new Triangle2D(w - o1, h - (o1 + e), props.centerX() + e, props.centerY(), w - o1, o1 + e);

        g.setStroke(props.wideShadowStroke());
        g.setColor(props.shadow());
        g.fill(tri);
        g.fill(tri2);
        g.setStroke(new BasicStroke(props.shadowStroke().getLineWidth() * 1.5F));
        g.drawLine(props.centerX(), o1 * 2, props.centerX(), props.height() - (o1 * 2));

        g.setStroke(props.wideMainStroke());
        g.setColor(props.primary());
        g.draw(tri);
        g.draw(tri2);
        g.setStroke(new BasicStroke(props.mainStroke().getLineWidth() * 1.5F));
        g.drawLine(props.centerX(), o1 * 2, props.centerX(), props.height() - (o1 * 2));
    }

    void drawBarbellImage(Graphics2D g, CursorProperties props) {
        props = props.attentionVariant();
        int w = props.width();
        int h = props.height();
        Rectangle rect1 = new Rectangle((w / 8) + 1, (h / 8) + 1);
        rect1.x = (w / 8);
        rect1.y = (h / 2) - 1;
//        int e = w/16;
//        int triHeight = (w/4)-(e*3);
//        int triWidth = triHeight + (e*3);
//        Triangle2D rect1 = new Triangle2D(e, props.centerY(), props.centerX() - triWidth, e + triHeight, props.centerX() - triWidth, h-(e+triHeight));

        Rectangle rect2 = new Rectangle((w / 8) + 1, (h / 8) + 1);
        rect2.x = w - (rect2.width + 1);
        rect2.y = (h / 2) - 1;
        rect1.width -= w / 16;
        rect2.width -= w / 16;
        g.setColor(props.shadow());
//        g.setStroke(new BasicStroke(props.mainStroke().getLineWidth()*1.75F));
        g.setStroke(props.wideShadowStroke());
        g.draw(rect1);
        g.fill(rect1);
        g.draw(rect2);
        g.fill(rect2);

        Line2D ln = new Line2D.Double(rect1.getCenterX(), rect1.getCenterY(),
                rect2.getCenterX(), rect2.getCenterY());
        g.draw(ln);

        g.setColor(props.primary());
        g.setStroke(props.mainStroke());

        g.draw(rect1);
        g.draw(rect2);
        g.fill(rect1);
        g.fill(rect2);
        g.draw(ln);

//        g.setBackground(new Color(255, 255, 255, 0));
//        Rectangle r = new Rectangle((int) rect1.getCenterX()-1, (int) rect1.getCenterY()-1, 3, 3);
//        g.clearRect(r.x, r.y, r.width, r.height);
    }

    void drawNoImage(Graphics2D g, CursorProperties props) {
        props = props.warningVariant();
        int w = props.width();
        int h = props.height();
        if (w % 16 != 0) {
            w = (w / 16) * 16;
        }
        if (h % 16 != 0) {
            h = (h / 16) * 16;
        }
        applyRenderingHints(g);
        Ellipse2D.Double ell = new Ellipse2D.Double(w / 8, w / 8, w - (w / 4), h - (h / 4));
        g.setStroke(props.wideShadowStroke());
        g.setColor(props.shadow());
        int cornerOffsetX = (w / 8) + 1;
        int cornerOffsetY = (h / 8) + 1;
        g.drawLine(cornerOffsetX, cornerOffsetY, w - cornerOffsetX, h - cornerOffsetY);
        g.draw(ell);
        g.setStroke(new BasicStroke(w / 8F));
        g.setColor(props.primary());
        g.draw(ell);
        g.drawLine(cornerOffsetX, cornerOffsetY, w - cornerOffsetX, h - cornerOffsetY);
    }

    void drawStarImage(Graphics2D g, CursorProperties props) {
        props = props.creationVariant();
        int w = props.width();
        int h = props.height();
        int centerFree = min(w, h) / 8;
        int edgeFree = centerFree / 2;
        int cornerFree = centerFree * 2;

        int cx = (w / 2);
        int cy = (h / 2);
        g.setColor(props.shadow());
        g.setStroke(props.wideShadowStroke());
        drawStarPattern(g, cornerFree, cx, centerFree, cy, h, w, edgeFree);
        g.setColor(props.primary());
        g.setStroke(props.wideMainStroke());
        drawStarPattern(g, cornerFree, cx, centerFree, cy, h, w, edgeFree);
    }

    void drawStarPattern(Graphics2D g,
            int cornerFree, int cx, int centerFree, int cy, int h, int w, int edgeFree) {
        g.drawLine(cornerFree, cornerFree, cx - centerFree, cy - centerFree);
        g.drawLine(cornerFree, h - cornerFree, cx - centerFree, cy + centerFree);
        g.drawLine(w - cornerFree, h - cornerFree, cx + centerFree, cy + centerFree);
        g.drawLine(w - cornerFree, cornerFree, cx + centerFree, cy - centerFree);
        // vertical
        g.drawLine(centerFree, cy, cx - (centerFree + edgeFree), cy);
        g.drawLine(w - centerFree, cy, cx + centerFree + edgeFree, cy);
        // horizontal
        g.drawLine(cx, centerFree, cx, cy - (centerFree + edgeFree));
        g.drawLine(cx, cy + centerFree + edgeFree, cx, h - centerFree);
        // and a dot in the center
        g.fillRect(cx - edgeFree, cy - edgeFree, centerFree, centerFree);
    }

}
