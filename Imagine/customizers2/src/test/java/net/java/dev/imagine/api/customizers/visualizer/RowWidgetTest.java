package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.FontMetrics;
import java.awt.Font;
import java.util.Random;
import net.java.dev.imagine.api.customizers.visualizer.popup.PopupScene;
import java.awt.Rectangle;
import java.util.Arrays;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.util.Collection;
import net.java.dev.imagine.api.properties.Explicit;
import org.netbeans.api.visual.widget.Scene;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Ellipse2D;
import javax.swing.JFrame;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tim
 */
public class RowWidgetTest {
    
    public RowWidgetTest() {
        assertTrue(true);
    }

    @Test
    public void testSomeMethod() throws InterruptedException {
//        Scene x = new SS();
//        JFrame jf = new JFrame();
//        jf.setDefaultCloseOperation(3);
//        jf.setContentPane(x.createView());
//        jf.pack();
//        jf.setSize(500, 500);
//        jf.setVisible(true);
//        jf.setBackground(Color.DARK_GRAY);
//        Thread.sleep(62000);
    }
    
    static class S extends Scene implements Renderer<Thing>, Explicit<Thing>, SelectionHandler<Thing> {
        final PopupScene ps;
        S() {
            addChild(new RowWidget(this, this, this, this, 5));
            ps = new PopupScene(this, this, this);
            getActions().addAction(ps.createAction()); 
            setBackground(Color.DARK_GRAY);
        }

        @Override
        public Dimension getSize(Thing t) {
            return new Dimension(40, 40);
//            return t.shape.getBounds().getSize();
        }

        @Override
        public void paint(Graphics2D g, Thing t, int x, int y) {
            Rectangle b = t.shape.getBounds();
            g.translate(-b.x, -b.y);
            g.translate(x, y);
            t.paint(g);
            g.translate(-x, -y);
            g.translate(b.x, b.y);
        }
        
        private final Thing[] tgs = makeThings(26);

        @Override
        public Collection<Thing> getValues() {
            return Arrays.asList(tgs);
        }

        @Override
        public void itemSelected(Thing item) {
            System.out.println("SELECTED: " + item);
            ps.setDescription(item == null ? "" : item.toString());
        }
    }
    
    static class SS extends Scene implements Renderer<Font>, Explicit<Font>, SelectionHandler<Font> {
        final PopupScene ps;
        SS() {
            addChild(new RowWidget(this, this, this, this, 5));
            ps = new PopupScene(this, this, this);
            getActions().addAction(ps.createAction()); 
            setBackground(Color.DARK_GRAY);
        }
        
        String txt = " Abcdef ";

        @Override
        public Dimension getSize(Font t) {
            Graphics2D g = getGraphics();
            if (g == null) {
                g = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(1, 1).createGraphics();
            }
            t = t.deriveFont(18F);
            FontMetrics fm = g.getFontMetrics(t);
            Dimension result = fm.getStringBounds(txt, g).getBounds().getSize();
            result.height = fm.getHeight();
            return result;
        }

        @Override
        public void paint(Graphics2D g, Font t, int x, int y) {
            g.setColor(Color.BLACK);
            t = t.deriveFont(18F);
            FontMetrics fm = g.getFontMetrics(t);
            g.setFont(t);
            y+= fm.getMaxAscent();
            g.drawString(txt, x, y);
        }
        

        @Override
        public Collection<Font> getValues() {
            return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
        }

        @Override
        public void itemSelected(Font item) {
            System.out.println("SELECTED: " + item);
            ps.setDescription(item == null ? "" : item.toString());
        }
    }    
    
    static Thing[] makeThings(int count) {
        int size = 20;
        Thing[] result = new Thing[count];
        Random r = new Random(System.currentTimeMillis());
        for (int i = 0; i < count; i++) {
            Color c = new Color(r.nextInt(255), r.nextInt(255), r.nextInt(255));
            Shape s;
            int x = r.nextInt(size - 5);
            int w = 30 - x;
            int y = r.nextInt(size - 5);
            int h = 30 - y;
            if (r.nextBoolean()) {
                s = new Ellipse2D.Double(x, y, w, h);
            } else {
                s = new Rectangle(x, y, w, h);
            }
            result[i] = new Thing(c, s);
        }
        return result;
    }
    
    static final Thing[] things = new Thing[] {
        new Thing(Color.BLUE, new Rectangle(2, 2, 15, 15)),
        new Thing(Color.YELLOW, new Rectangle(5, 5, 5, 5)),
        new Thing(Color.PINK, new Ellipse2D.Double(2, 2, 26, 26)),
        new Thing(Color.CYAN, new Ellipse2D.Double(10, 10, 10, 10)),
        new Thing(Color.ORANGE, new Rectangle(2, 2, 15, 15)),
        new Thing(Color.GREEN, new Rectangle(5, 5, 15, 15)),
        new Thing(Color.BLACK, new Rectangle(5, 15, 15, 15)),
        new Thing(Color.RED, new Rectangle(15, 5, 15, 25)),
        new Thing(Color.BLUE, new Ellipse2D.Double(2, 2, 26, 26)),
        new Thing(Color.DARK_GRAY, new Ellipse2D.Double(10, 10, 10, 10)),
        new Thing(Color.MAGENTA, new Ellipse2D.Double(5, 5, 25, 25)),
        new Thing(Color.YELLOW, new Ellipse2D.Double(15, 15, 10, 15)),
    };
    
    static class Thing {
        private final Color color;
        private final Shape shape;

        public Thing(Color color, Shape shape) {
            this.color = color;
            this.shape = shape;
        }
        
        public void paint(Graphics2D g) {
            g.setPaint(color);
            g.fill(shape);
            g.setPaint(Color.BLACK);
            g.draw(shape);
        }
        
        public String toString() {
            return shape + " " + color;
        }
    }
}
