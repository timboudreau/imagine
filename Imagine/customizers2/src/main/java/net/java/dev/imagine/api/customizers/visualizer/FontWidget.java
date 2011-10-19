package net.java.dev.imagine.api.customizers.visualizer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.properties.Explicit;
import net.java.dev.imagine.api.customizers.visualizer.popup.PopupScene;
import net.java.dev.imagine.api.properties.Property;
import org.netbeans.api.visual.widget.LabelWidget;
import org.netbeans.api.visual.widget.Widget;
import org.openide.util.WeakListeners;

/**
 *
 * @author Tim Boudreau
 */
public class FontWidget extends Widget {

    private final Property<Font> prop;
    private final LabelWidget name;
    
    /*
    public FontWidget(ColumnDataScene scene, final ToolProperty<Font> prop) {
        super (scene);
        this.prop = prop;
        name = new LabelWidget(scene, prop.name().name());
        final JComboBox box = new JComboBox();
        box.setModel(new FontComboBoxModel());
        box.setRenderer(new FontCellRenderer());
        box.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                Font f = (Font) box.getSelectedItem();
                prop.set(f);
            }
        });
        addChild(name);
        addChild(new ComponentWidget(scene, box));
        setLayout(scene.getColumns().createLayout());
    }
    */
    Fs fs = new Fs();
    private final FW fw;

    public FontWidget(ColumnDataScene scene, Property<Font> prop) {
        super(scene);
        this.prop = prop;
        name = new LabelWidget(scene, prop.getDisplayName());
        addChild(name);
        addChild(fw = new FW());
        PopupScene s = new PopupScene(fs, fs, fs, 2);
        getActions().addAction(s.createAction());
        setLayout(scene.getColumns().createLayout());
        prop.addChangeListener(WeakListeners.change(cl, prop));
    }

    private Font font() {
        Font f = prop.get();
        f = f.deriveFont(14F);
        return f;
    }
    private final CL cl = new CL();

    class CL implements ChangeListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            fw.revalidate();
            fw.repaint();
            getScene().validate();
        }
    }
    static List<Font> allFonts;

    public static String getText(Font f) {
        return f.getFamily();
    }

    class Fs implements Renderer<Font>, Explicit<Font>, SelectionHandler<Font> {

        @Override
        public Dimension getSize(Font t) {
            Graphics2D g = getGraphics();
            FontMetrics fm = g.getFontMetrics(t.deriveFont(14F));
            Dimension d = fm.getStringBounds(getText(t), g).getBounds().getSize();
            d.width += 13;
            d.height += 13;
            return d;
        }

        @Override
        public void paint(Graphics2D g, Font t, int x, int y) {
            int ix = allFonts.indexOf(t);
            boolean bg = ix % 2 == 1;

            Rectangle r = getBounds();
            g.setFont(t.deriveFont(14F));
            FontMetrics fm = g.getFontMetrics();
            int origY = y;
            int origX = x;
            y += fm.getMaxAscent();
            x += 6;
            int txtH = (int) fm.getStringBounds(getText(t), g).getHeight();
            if (txtH < r.height) {
                y += (r.height - txtH) / 2;
            }
            if (bg) {
                g.setColor(new Color(237, 237, 255));
                g.fillRect(origX, origY, fm.stringWidth(getText(t)) + 13, y + fm.getHeight());
            }
            g.setColor(getForeground());
            g.drawString(getText(t), x, y + 6);

            g.setColor(Color.LIGHT_GRAY);
//            if (origX != 0) {
            g.drawLine(origX, origY, origX, y + fm.getHeight());
//            }
//            g.drawLine(x, origY, x + fm.stringWidth(getText(t)), origY);
        }

        @Override
        public Collection<Font> getValues() {
            if (allFonts != null) {
                return allFonts;
            }
//            return Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts());
            List<Font> result = allFonts = new ArrayList<Font>();
            for (String fam : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                result.add(new Font(fam, Font.PLAIN, 14));
            }
            return result;
        }

        @Override
        public void itemSelected(Font item) {
            System.out.println("item selected " + item);
            //XXX preserve old value
            if (item != null) {
                prop.set(item);
            }
        }
    }

    private class FW extends Widget {

        FW() {
            super(FontWidget.this.getScene());
        }

        @Override
        protected Rectangle calculateClientArea() {
            Graphics2D g = getGraphics();
            FontMetrics fm = g.getFontMetrics(font());
            setToolTipText(prop.get().getFamily());
            return fm.getStringBounds(getText(font()), g).getBounds();
        }

        @Override
        protected void paintWidget() {
            Graphics2D g = getGraphics();
            g.setFont(font());
            FontMetrics fm = g.getFontMetrics();
            Rectangle b = getPreferredBounds();
            g.setColor(FontWidget.this.getForeground());
            g.drawString(getText(font()), b.x, b.y + fm.getMaxAscent());
        }
    }
}
