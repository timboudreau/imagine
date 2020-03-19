package org.netbeans.paint.api.components.points;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JComponent;
import org.imagine.geometry.Circle;
import org.imagine.geometry.Triangle2D;

/**
 *
 * @author Tim Boudreau
 */
class TestPointPainter extends JComponent {

    Rectangle2D.Double bds = new Rectangle2D.Double(0, 0, 100, 100);
    private final Point2D.Double point = new Point2D.Double(0, 0);
    private final Point2D.Double otherPoint = new Point2D.Double(-100, -100);

    {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON3) {
                    setOtherPoint(e.getPoint());
                } else if (e.getButton() == MouseEvent.BUTTON1) {
                    setPoint(e.getPoint());
                }
            }
        });
    }

    void setOtherPoint(Point2D point) {
        Point2D.Double old = new Point2D.Double(point.getX(), point.getY());
        otherPoint.setLocation(point);
        firePropertyChange("otherPoint", old, point);
        repaint();
    }

    void setPoint(Point2D p) {
        Point2D.Double old = new Point2D.Double(point.getX(), point.getY());
        point.setLocation(p);
        firePropertyChange("point", old, p);
        repaint();
    }

    @Override
    public void reshape(int x, int y, int w, int h) {
        bds.setRect(0, 0, w, h);
        super.reshape(x, y, w, h);
        System.out.println("RECT " + x + "," + y + "," + w + "," + h);
        firePropertyChange("bds", null, bds);
    }

    public Dimension getPreferredSize() {
        return bds.getBounds().getSize();
    }

    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public void paint(Graphics gr) {
        Graphics2D g = (Graphics2D) gr;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.setColor(Color.BLACK);
        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        Circle circ = new Circle(point.getX(), point.getY(), 13);
        g.setColor(new Color(180, 180, 255));
        g.fill(circ);
        g.setColor(Color.BLUE);
        g.draw(circ);

        Triangle2D tri = Triangle2D.isoceles(otherPoint, 13);
        g.setColor(Color.BLACK);
        g.fill(tri);
        g.setColor(Color.RED);
        g.draw(tri);

        Line2D.Double ln = new Line2D.Double(point, otherPoint);
        g.draw(ln);
    }

}
