package org.imagine.editor.api.snap;

import com.mastfrog.function.DoubleBiConsumer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import static java.lang.Math.acos;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import org.imagine.geometry.Angle;
import org.imagine.geometry.Circle;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPoint;
import org.imagine.geometry.EqPointDouble;
import org.imagine.geometry.LineVector;
import org.imagine.geometry.Quadrant;
import org.imagine.geometry.util.GeometryStrings;

/**
 *
 * @author Tim Boudreau
 */
public class TestIt {

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        PointComp comp = new PointComp();

        comp.setPreferredSize(new Dimension(500, 500));

        jf.setLayout(new BorderLayout());
        jf.add(comp, BorderLayout.CENTER);

        JSlider slider = new JSlider(-360, 360);
        slider.setBackground(Color.DARK_GRAY);
        slider.setForeground(Color.LIGHT_GRAY);
        slider.setValue(10);
        jf.add(slider, BorderLayout.SOUTH);
        slider.addChangeListener(ce -> {
            comp.setExtent(slider.getValue());
        });

        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setVisible(true);
    }

    static class PointComp extends JComponent {

//        EqPointDouble apex = new EqPointDouble(500, 500);
//        EqPointDouble trailing = new EqPointDouble(420, 400);
//        EqPointDouble leading = new EqPointDouble(410, 600);
        EqPointDouble apex = new EqPointDouble(500, 500);
        EqPointDouble trailing = new EqPointDouble(400, 400);
        EqPointDouble leading = new EqPointDouble(600, 400);
        int ext = 10;
        private final Circle pcirc = new Circle(0, 0, 9);
        boolean showIt = true;

        double lastX;
        double lastY;

        PointComp() {
            PointTool pt = new PointTool(this);
            addMouseListener(pt);
            addMouseMotionListener(pt);
            InputMap im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "tog");
            am.put("tog", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showIt = !showIt;
                    repaint();
                }
            });
        }

        void setExtent(int val) {
            this.ext = val;
            repaint();
        }

        private LineVector vector() {
            if (trailing != null && apex != null && leading != null) {
                return LineVector.of(trailing, apex, leading);
            }
            return null;
        }

        @Override
        protected void paintComponent(Graphics g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            paintComponent((Graphics2D) g);
        }

        private Circle setFromPoints() {
            return setFromPoints(trailing.x, trailing.y, apex.x, apex.y, leading.x, leading.y);
        }

        private Circle setFromPoints(double x1, double y1, double x2, double y2, double x3, double y3) {
            double xDiff1to2 = x1 - x2;
            double xDiff1to3 = x1 - x3;

            double yDiff1to2 = y1 - y2;
            double yDiff1to3 = y1 - y3;

            double yDiff3to1 = y3 - y1;
            double yDiff2to1 = y2 - y1;

            double xDiff3to1 = x3 - x1;
            double xDiff2to1 = x2 - x1;

            double x1squared = x1 * x1;
            double y1squared = y1 * y1;

            double xSquared1to3 = (x1squared
                    - Math.pow(x3, 2));

            double ySquard1to3 = (y1squared
                    - Math.pow(y3, 2));
            double xSquared2to1 = (Math.pow(x2, 2)
                    - x1squared);
            double ySquared2to1 = (Math.pow(y2, 2)
                    - Math.pow(y1, 2));
            double vx = (xSquared1to3 * xDiff1to2
                    + ySquard1to3 * xDiff1to2
                    + xSquared2to1 * xDiff1to3
                    + ySquared2to1 * xDiff1to3)
                    / (2 * (yDiff3to1 * xDiff1to2 - yDiff2to1 * xDiff1to3));
            double vy = (xSquared1to3 * yDiff1to2
                    + ySquard1to3 * yDiff1to2
                    + xSquared2to1 * yDiff1to3
                    + ySquared2to1 * yDiff1to3)
                    / (2 * (xDiff3to1 * yDiff1to2 - xDiff2to1 * yDiff1to3));

            double c = -x1squared - y1squared
                    - 2 * vy * x1 - 2 * vx * y1;

            // eqn of circle be x^2 + y^2 + 2*g*x + 2*f*y + c = 0
            // where centre is (h = -g, k = -f) and radius r
            // as r^2 = h^2 + k^2 - c
            double cx = -vy;
            double cy = -vx;
            double rSquared = cx * cx + cy * cy - c;

            // r is the radius
            double r = Math.sqrt(rSquared);
            return new Circle(cx, cy, r);
        }

        private void paintCircle(Graphics2D g) {
            if (trailing != null && apex != null && leading != null) {
                Circle c = setFromPoints();
                g.setColor(Color.GRAY);
                g.draw(c);
            }
        }

        protected void paintComponent(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            paintCircle(g);

            LineVector vect = vector();
            if (vect != null) {
                // Draw the two circles around our apex
                g.setColor(Color.DARK_GRAY.brighter());
                pcirc.setCenter(vect.apexX(), vect.apexY());
                pcirc.setRadius(vect.minLength());
                // Draw a label with the radius for each circle
                String rtxt = "r" + GeometryStrings.toShortString(pcirc.radius());
                EqLine tanLine = pcirc.tangent(0);
                FontMetrics fm = g.getFontMetrics();
                tanLine.shiftPerpendicular(-fm.getHeight());
                EqPoint tanMid = tanLine.midPoint().toFloat();
                float sw = fm.stringWidth(rtxt);
                g.setColor(Color.DARK_GRAY.brighter().brighter());
                g.drawString(rtxt, tanMid.x - (sw / 2f), tanMid.y);

                g.setColor(Color.DARK_GRAY.brighter());
                g.draw(pcirc);
                pcirc.setRadius(vect.maxLength());
                g.draw(pcirc);
                rtxt = "r" + GeometryStrings.toShortString(pcirc.radius());
                tanLine = pcirc.tangent(0);
                tanLine.shiftPerpendicular(-fm.getHeight());
                tanMid = tanLine.midPoint().toFloat();
                sw = fm.stringWidth(rtxt);
                g.setColor(Color.DARK_GRAY.brighter().brighter());
                g.drawString(rtxt, tanMid.x - (sw / 2F), tanMid.y);

                g.setColor(Color.GRAY);
                EqLine tl = vect.trailingLine();
                EqLine ll = vect.leadingLine();
                g.setColor(new Color(160, 160, 255, 200));
                g.draw(tl);
                g.setColor(new Color(255, 255, 60, 200));
                g.draw(ll);
                g.setColor(Color.LIGHT_GRAY);
                paintLength(tl, vect.corner().trailingAngle(), g, false);
                paintLength(ll, vect.corner().leadingAngle(), g, true);

//                PieWedge wedg = new PieWedge();
//                wedg.setFrom(vect);
//                g.setColor(Color.BLUE);
//                g.draw(wedg);
                if (showIt) {
                    paintComputedExtent(g, vect);
//                    if (getVisibleRect().contains(lastX, lastY)) {
                    paintBestExtent(g, vect, lastX, lastY);
//                    }
                }
            }
            pcirc.setRadius(9);
            g.setColor(Color.GRAY);
            String extString = "Extent " + ext;
            FontMetrics fm = g.getFontMetrics();
            int w = fm.stringWidth(extString);
            int h = fm.getMaxAscent() + 5;
            int l = 5;
            g.drawString(extString, l, h);

            if (trailing != null) {
                pcirc.setCenter(trailing);
                g.setColor(new Color(255, 255, 255, 128));
                g.fill(pcirc);
                g.setColor(Color.WHITE);
                g.draw(pcirc);
            }
            if (apex != null) {
                pcirc.setCenter(apex);
                g.setColor(new Color(40, 200, 40, 128));
                g.fill(pcirc);
                g.setColor(new Color(40, 200, 40));
                g.draw(pcirc);
            }

            if (leading != null) {
                pcirc.setCenter(leading);
                g.setColor(new Color(100, 100, 200, 128));
                g.fill(pcirc);
                g.setColor(new Color(100, 100, 200));
                g.draw(pcirc);
            }

        }

        private void paintLength(EqLine ln, double ang, Graphics2D g, boolean lead) {
            g = ((Graphics2D) g.create());
            FontMetrics fm = g.getFontMetrics();
            float h = fm.getMaxAscent() + fm.getMaxDescent() * 1.75F;
            ln.shiftPerpendicular(h);
            String txt
                    = (lead ? "lead " : "trail ")
                    + GeometryStrings.toShortString(ln.length())
                    + "px " + GeometryStrings.toDegreesStringShort(ang);
            EqPoint mid = ln.midPoint().toFloat();
            double rotTxt = Angle.normalize(ln.angle() - 90);
            Quadrant q = Quadrant.forAngle(rotTxt);

            switch (q) {
                case SOUTHWEST:
                case SOUTHEAST:
                    rotTxt = Angle.opposite(rotTxt);
            }
            AffineTransform xform = AffineTransform.getRotateInstance(
                    Math.toRadians(rotTxt), mid.x, mid.y);
            g.transform(xform);
            if (lead) {
                g.setColor(new Color(255, 255, 60, 200));
            } else {
                g.setColor(new Color(160, 160, 255, 200));
            }
            float textW = g.getFontMetrics().stringWidth(txt);
            g.drawString(txt, mid.x - (textW / 2), mid.y);
            g.dispose();
        }

        double solveSide(double a, double b, double C) {
            C = toRadians(C);
            if (C > 0.001) {
                return Math.sqrt(a * a + b * b - 2 * a * b * Math.cos(C));
            } else {
                // https://www.nayuki.io/page/numerically-stable-law-of-cosines
                return Math.sqrt((a - b) * (a - b) + a * b * C * C * (1 - C * C / 12));
            }
        }

        double solveAngle(double a, double b, double c) {
            double temp = (a * a + b * b - c * c) / (2 * a * b);
            if (-1 <= temp && temp <= 0.9999999) {
                return toDegrees(acos(temp));
            } else if (temp <= 1) {
                // https://www.nayuki.io/page/numerically-stable-law-of-cosines
                return toDegrees(sqrt((c * c - (a - b) * (a - b)) / (a * b)));
            } else {
                return Double.MIN_VALUE;
            }
        }

        private void intersectionPoints(EqPointDouble p1, EqPointDouble p2, double rad, DoubleBiConsumer cn) {
            Circle c1 = new Circle(p1, rad);
            Circle c2 = new Circle(p2, rad);
            double D = c1.distanceToCenter(c2.centerX(), c2.centerY());

            double a = p1.y;
            double b = p1.x;

            double c = p2.y;
            double d = p2.x;

            double r0 = rad;
            double r1 = rad;

            double theta = 0.25 * ((D + r0 + r1) * (D - r0 + r1) * (-D + r0 + r1));

            double x1 = ((a + c) / 2) + (((c - a) * ((r0 * r0) - (r1 * r1))) / (2 * Math.pow(D, 2)))
                    + (2 * ((a - c) / Math.pow(D, 2)) * theta);

            double y1 = ((b + d) / 2) + (((b - d) * ((r0 * r0) - (r1 * r1))) / (2 * Math.pow(D, 2)))
                    + (2 * ((b - d) / Math.pow(D, 2)) * theta);

            cn.accept(x1, y1);

//            double ang1 = Math.acos((Math.pow(d, 2D) + Math.pow(rad, 2D) - Math.pow(rad, 2) / (2D * d * rad)));
//            double ang2 = Math.acos((Math.pow(d, 2D) + Math.pow(rad, 2D) - Math.pow(rad, 2) / (2D * d * rad)));
//            c1.positionOf(ang1, cn);
//            c1.positionOf(ang2, cn);
//            c2.positionOf(ang1, cn);
//            c2.positionOf(ang2, cn);
        }


        public void paintBestExtent(Graphics2D g, LineVector vect, double lx, double ly) {
            LineVector lv2 = LineVector.of(vect.trailingX(),
                    vect.trailingY(), lx, ly, vect.leadingX(), vect.leadingY());

            g.setColor(new Color(120, 0, 180));
            g.draw(lv2.trailingLine());
            g.setColor(new Color(0, 0, 180));
            g.draw(lv2.leadingLine());
            g.setColor(Color.WHITE);

            float tx = (float) lx;
            float ty = (float) (ly + 20);
            String s = GeometryStrings.toDegreesStringShort(lv2.corner().extent());
            tx -= g.getFontMetrics().stringWidth(s) / 2;
            ty += g.getFontMetrics().getHeight();
            g.drawString(s,
                    tx, ty);

            ty += g.getFontMetrics().getHeight();
            s = GeometryStrings.toDegreesStringShort(vect.corner().extent());
            tx = (float) lx;
            tx -= g.getFontMetrics().stringWidth(s) / 2;
            g.setColor(Color.LIGHT_GRAY);
            g.drawString(s,
                    tx, ty);

            extCirc.nearestPoint(lx, ly, (x, y) -> {
                pcirc.setCenter(x, y);
                pcirc.setRadius(7);
                g.setColor(Color.ORANGE);
                g.fill(pcirc);
                g.draw(pcirc);
            });

        }

        Circle extCirc = new Circle();

        private void paintComputedExtent(Graphics2D g, LineVector vect) {
            extCirc = vect.extentCircle(ext);
            g.setColor(Color.YELLOW);
            g.draw(extCirc);
        }
    }

    static class PointTool extends MouseAdapter {

        private final PointComp comp;
        private int state = -1;

        public PointTool(PointComp comp) {
            this.comp = comp;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            handleClick(e.getPoint());
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            comp.lastX = e.getX();
            comp.lastY = e.getY();
            comp.repaint();
        }

        @Override
        public void mouseExited(MouseEvent e) {
            comp.lastX = -2;
            comp.lastY = -2;
        }

        private void handleClick(Point p) {
            state = ++state;
            if (state > 2) {
                state = 0;
            }
            switch (state) {
                case 0:
                    comp.trailing = EqPointDouble.of(p);
                    comp.apex = null;
                    comp.leading = null;
                    comp.showIt = true;
                    break;
                case 1:
                    comp.apex = EqPointDouble.of(p);
                    break;
                case 2:
                    comp.leading = EqPointDouble.of(p);
                    break;
                default:
                    throw new AssertionError(state);
            }
            comp.repaint();
        }

    }
}
