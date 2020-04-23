/*
 * TextTool.java
 *
 * Created on September 28, 2006, 10:18 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.java.dev.imagine.api.image.Layer;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Constants;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.tools.fills.FillCustomizer;
import org.netbeans.paint.tools.spi.Fill;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */
@ToolDef(name = "Text", iconPath = "org/netbeans/paint/tools/resources/text.svg")
@Tool(Surface.class)
public class TextTool implements KeyListener, MouseListener, MouseMotionListener, PaintParticipant, CustomizerProvider, Attachable {

    private final Surface surface;
    final Customizer<Fill> fillC;
    final Customizer<String> textC;
    final Customizer<Font> fontC;

    public TextTool(Surface surface) {
        this.surface = surface;
        fillC = FillCustomizer.getDefault();
        textC = Customizers.getCustomizer(String.class, Constants.TEXT);
        assert textC != null : "Null text customizer";
        fontC = Customizers.getCustomizer(Font.class, Constants.FONT);
        assert fontC != null : "Null font family customizer";
    }

    @Override
    public String toString() {
        return NbBundle.getMessage(TextTool.class, "Text"); //NOI18N
    }

    public String getInstructions() {
        return NbBundle.getMessage(TextTool.class, "Click_to_position_text"); //NOI18N
    }

    public boolean canAttach(Layer layer) {
        return layer.getLookup().lookup(Surface.class) != null;
    }

    public void keyTyped(KeyEvent e) {
        if (armed && repainter != null) {
//            txt.append (e.getKeyChar());
            repainter.requestRepaint(null);
        }
    }

    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
//            if (txt.length() > 0) {
//                txt.deleteCharAt(txt.length() - 1);
//                if (armed) {
//                    repainter.requestRepaint(null);
//                }
//                e.consume();
//            }
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    public void mouseClicked(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {
        setLoc(e.getPoint());
    }

    public void mouseReleased(MouseEvent e) {
        if (armed) {
            commit(e.getPoint());
        }
    }

    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
        committing = commit;
        paint(g2d);
        committing = false;
    }

    boolean committing = false;

    private void commit(Point p) {
        setLoc(p);
        repainter.requestCommit();
    }

    boolean armed;

    public void mouseEntered(MouseEvent e) {
        armed = true;
        Point p = e.getPoint();
        setLoc(p);
        repainter.requestRepaint(null);
    }

    public void mouseExited(MouseEvent e) {
        armed = false;
        setLoc(null);
        repainter.requestRepaint(null);
    }

    public void paint(Graphics2D g2d) {
        assert textC != null : "Null text customizer";
        String text = textC.get();
        if (text.length() == 0 || "".equals(text.trim())) {
            return;
        }
        Composite comp = null;
        if (!committing) {
            comp = g2d.getComposite();
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5F));
        }
        g2d.setFont(getFont());
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (loc == null) {
            return;
        }
        g2d.setPaint(fillC.get().getPaint());
        g2d.drawString(text, loc.x, loc.y);
        if (!committing) {
            g2d.setComposite(comp);
        }
    }

    public void mouseDragged(MouseEvent e) {
        armed = true;
        setLoc(e.getPoint());
    }

    public void mouseMoved(MouseEvent e) {
        armed = true;
        setLoc(e.getPoint());
    }

    private Point loc = null;

    private void setLoc(Point p) {
        loc = p;
        repainter.requestRepaint(null);
    }

    public String getName() {
        return toString();
    }

    public void detach() {
        if (repainter != null) {
            repainter.requestRepaint(null);
            repainter = null;
        }
        armed = false;
        loc = null;
    }

    public Lookup getLookup() {
        return Lookups.singleton(this);
    }

    private Repainter repainter;

    public void attachRepainter(PaintParticipant.Repainter repainter) {
        this.repainter = repainter;
    }

    @Override
    public Customizer getCustomizer() {
        return new AggregateCustomizer("textTool", textC, fontC, fillC); //NOI18N
    }

    private Font getFont() {
        return fontC.get();
    }

    @Override
    public void attach(Provider on) {
        //do nothing
    }
}
