/*
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.paint.api.components;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SliderUI;

/**
 *
 *
 * @author Timothy Boudreau
 */
public class SimpleSliderUI extends SliderUI implements ChangeListener {

    private static final SimpleSliderUI INSTANCE = new SimpleSliderUI();

    private JSlider popupOwner;
    private Point triggerPoint;

    public SimpleSliderUI() {

    }

    private int valueOffsetAtDragStart;

    public SimpleSliderUI(JSlider popupOwner, Point triggerPoint) {
        this.popupOwner = popupOwner;
        this.triggerPoint = new Point(triggerPoint);
        valueOffsetAtDragStart = popupOwner.getValue() - popupOwner.getMinimum();
    }

    public static ComponentUI createUI(JComponent b) {
        return new SimpleSliderUI();
    }

    private int getPreferredUISpan(boolean vertical, JSlider c) {
        if (popupOwner != null && !c.isDisplayable() && popupOwner.isDisplayable()) {
            c = popupOwner;
        }
        GraphicsConfiguration config = c.getGraphicsConfiguration();
        if (config == null) {
            config = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();
        }
        FontMetrics fm = c.getFontMetrics(c.getFont());
        if (popupOwner != null) {
            Rectangle screen = config.getBounds();
            int full = vertical ? screen.height : screen.width;
            int half = full / 2;
            int quarter = full / 4;
            int span = c.getMaximum() - c.getMinimum();
            if (span < 100) {
                quarter /= 2;
            }
            // A half-line height presents a good mouse target for dragging
            int idealHeight = span * (fm.getHeight() / 2);
            if (idealHeight > quarter) {
                idealHeight = half;
            }
            return idealHeight;
        } else {
            return fm.stringWidth("A") * 40;
        }
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        JSlider js = (JSlider) c;
        int edgeGap = edgeGap(js);
        int or = js.getOrientation();
        if (charWidth == -1) {
            computeCharWidth(null, c.getFont());
        }
        int maxChars;
        StringConverter sc = (StringConverter) c.getClientProperty("converter");
        if (sc != null) {
            maxChars = sc.maxChars();
        } else {
            maxChars = Math.max((js.getMinimum() + "").length(), (js.getMaximum() + "").toString().length());
        }
        maxChars += 3;

        int pxSpan = getPreferredUISpan(or == JSlider.VERTICAL, js);

        Dimension result = new Dimension(or == JSlider.VERTICAL ? maxChars * charWidth
                : pxSpan,
                or == JSlider.VERTICAL ? pxSpan
                        : 2 * charWidth); //XXX should be height

        Insets ins = c.getInsets();
        result.width += ins.left + ins.right;
        result.height += ins.top + ins.bottom;
        if (or == JSlider.VERTICAL) {
            result.width += edgeGap * 2;
        } else {
            result.height += edgeGap * 2;
        }
        return result;
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D gr = (Graphics2D) g;
        gr.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        gr.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        JSlider sl = (JSlider) c;
        if (sl.getOrientation() == JSlider.VERTICAL) {
            g.translate(-8, 0);
            paintTrack(g, sl);
            paintThumb(g, sl);
            paintCaption(g, sl);
            g.translate(8, 0);
        } else {
            g.translate(0, -7);
            paintTrackH(g, sl);
            paintThumbH(g, sl);
            g.translate(0, 7);
            paintCaptionH(g, sl);
        }
    }

    @Override
    public void installUI(JComponent c) {
        c.setBorder(BorderFactory.createRaisedBevelBorder());
        if (popupOwner != null) {
            c.setBackground(popupOwner.getBackground());
            c.setForeground(popupOwner.getForeground());
            c.setFont(popupOwner.getFont());
        } else {
            Font f = UIManager.getFont("controlFont");
            if (f != null) {
                c.setFont(f.deriveFont(AffineTransform.getScaleInstance(0.9875, 0.9875)));
            }
            c.setBackground(UIManager.getColor("text"));
            c.setForeground(UIManager.getColor("textText"));
        }
        ((JSlider) c).addChangeListener(this);
        if (popupOwner != null) {
            c.setFocusable(false);
        } else {
            if (ml == null) {
                ml = new ML();
            }
            c.addMouseListener(ml);
            c.addMouseMotionListener(ml);
        }
    }

    private static ML ml;

    static class ML extends MouseAdapter {

        private MouseEvent armEvent;

        @Override
        public void mousePressed(MouseEvent e) {
            armEvent = e;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            armEvent = null;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (armEvent != null) {
                if (armEvent.getSource() == e.getSource() && e.getWhen() - armEvent.getWhen() < 300) {
                    mouseDragged(e);
                }
                armEvent = null;
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            armEvent = null;
            JSlider slider = (JSlider) e.getSource();
            if (slider.getUI() instanceof SimpleSliderUI) {
                SimpleSliderUI ui = (SimpleSliderUI) slider.getUI();
                boolean horiz = slider.getOrientation() == JSlider.HORIZONTAL;
                ui.dragTo(horiz ? e.getX() : e.getY(), slider);
            }
        }

    }

    @Override
    public int getBaseline(JComponent c, int width, int height) {
        JSlider sl = (JSlider) c;
        boolean horiz = sl.getOrientation() == JSlider.HORIZONTAL;
        if (horiz) {
            return height - c.getInsets().top;
        }
        return 0;
    }

    @Override
    public void uninstallUI(JComponent c) {
        if (ml != null) {
            c.removeMouseListener(ml);
            c.removeMouseMotionListener(ml);
        }
        c.setBorder(null);
        ((JSlider) c).removeChangeListener(this);
    }

    private void computeCharWidth(Graphics g, Font f) {
        boolean created = g == null;
        if (created) {
            BufferedImage im = GraphicsEnvironment
                    .getLocalGraphicsEnvironment()
                    .getDefaultScreenDevice()
                    .getDefaultConfiguration().createCompatibleImage(1, 1);
            g = im.createGraphics();
        }
        FontMetrics fm = g.getFontMetrics(f);
        charWidth = fm.charWidth('0');
        if (created) {
            g.dispose();
        }
    }

    private int edgeGap(JSlider slider) {
        return slider.getFontMetrics(slider.getFont()).stringWidth("O");
    }

    private int thumbSize(JSlider slider) {
        return slider.getFontMetrics(slider.getFont()).stringWidth("O");
    }

    private void paintTrackH(Graphics g, JSlider sl) {
        int edgeGap = edgeGap(sl);
        Insets ins = sl.getInsets();
        int center = (sl.getHeight() - (ins.top + ins.bottom)) / 2;
        int end = sl.getWidth() - (edgeGap + ins.left + ins.right);

        g.setColor(UIManager.getColor("textText"));
        g.drawLine(edgeGap + ins.left, center, end, center);
        g.drawLine(edgeGap + ins.left, center + edgeGap, edgeGap + ins.left,
                center);
        g.drawLine(end, center + edgeGap, end, center);
    }

    private void paintThumbH(Graphics g, JSlider sl) {
        int thumbSize = thumbSize(sl);
        int pos = getThumbPositionH(sl);
        int center = (sl.getHeight() / 2) - 2;
        int[] yp = new int[]{center + 1,
            center + 1 + thumbSize,
            center + 1 + thumbSize};
        int[] xp = new int[]{pos, pos - thumbSize, pos + thumbSize};

        g.fillPolygon(xp, yp, 3);
    }

    private int getThumbPositionH(JSlider sl) {
        int edgeGap = edgeGap(sl);
        int val = sl.getValue();
        Insets ins = sl.getInsets();
        float range = sl.getWidth()
                - ((edgeGap * 2) + ((ins.left + ins.right + ins.left)));
        float scale = sl.getMaximum() - sl.getMinimum();
        float factor = range / scale;
        float normVal = val - sl.getMinimum();

        return (int) (edgeGap + (normVal * factor)) + ins.left;
    }

    private void paintCaptionH(Graphics g, JSlider sl) {
        Font f = sl.getFont();
        int thumbSize = thumbSize(sl);

        g.setFont(f.deriveFont(AffineTransform.getScaleInstance(0.975, 0.975)));
        int center = sl.getHeight() / 2;
        String s = valueToString(sl);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        int h = fm.getMaxAscent();
        int y = (center + thumbSize + (h / 2)) - 2;
        int x = getThumbPositionH(sl) - (w / 2);
        g.setColor(sl.getForeground());
        g.drawString(s, x, y);
    }

    public void setStringConverter(StringConverter converter) {
        this.converter = converter;
    }

    private StringConverter converter = null;

    String valueToString(JSlider sl) {
        return converter == null ? Integer.toString(sl.getValue()) : converter.valueToString(sl);
    }

    private void paintTrack(Graphics g, JSlider sl) {
        int edgeGap = edgeGap(sl);
        Insets ins = sl.getInsets();
        int center = (sl.getWidth() - (ins.left + ins.right)) / 2;
        int end = sl.getHeight() - (edgeGap + ins.top + ins.bottom);

        g.setColor(sl.getForeground());
        g.drawLine(center, edgeGap + ins.top, center, end);
        g.drawLine(center, edgeGap + ins.top, center + edgeGap,
                edgeGap + ins.top);
        g.drawLine(center, end, center + edgeGap, end);
    }

    private void paintThumb(Graphics g, JSlider sl) {
        int thumbSize = thumbSize(sl);
        int pos = getThumbPosition(sl);
        int center = sl.getWidth() / 2;
        int[] xp = new int[]{center + 1, center + 1 + thumbSize,
            center + 1 + thumbSize};
        int[] yp = new int[]{pos, pos - thumbSize, pos + thumbSize};

        g.fillPolygon(xp, yp, 3);
    }

    private int getThumbPosition(JSlider sl) {
        int edgeGap = edgeGap(sl);
        int val = sl.getValue();
        Insets ins = sl.getInsets();
        float range = sl.getHeight()
                - ((edgeGap * 2) + ((ins.top + ins.bottom + ins.top)));
        float scale = sl.getMaximum() - sl.getMinimum();
        float factor = range / scale;
        float normVal = val - sl.getMinimum();

        return (int) (edgeGap + (normVal * factor)) + ins.top;
    }

    int charWidth = -1;

    private void paintCaption(Graphics g, JSlider sl) {
        int thumbSize = thumbSize(sl);
        String s = valueToString(sl);
        Font f = sl.getFont();
        g.setFont(f);
        if (charWidth == -1) {
            computeCharWidth(g, f);
        }
//        g.setXORMode(Color.WHITE);
        g.setColor(UIManager.getColor("textText"));
        int x = (sl.getWidth() / 2) + thumbSize + 3;
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth(s);
        int h = fm.getMaxAscent();
        // int y = (sl.getHeight() / 2) - h / 2;
        int y = getThumbPosition(sl) + (h / 2);

        g.drawString(s, x, y);
        g.setPaintMode();
    }

    void clearDetent() {
        valueOffsetAtDragStart = -1;
    }

    void dragTo(int coord, JSlider sl) {
        boolean horiz = sl.getOrientation() == JSlider.HORIZONTAL;
        Insets ins = sl.getInsets();
        float pixelBounds = horiz ? sl.getWidth() - (ins.left + ins.right)
                : sl.getHeight() - (ins.top + ins.bottom);
        float valueRange = sl.getMaximum() - sl.getMinimum();

        float pxOffsetToClickLocation = 0;
        if (triggerPoint != null) {
            Point nue = new Point(triggerPoint);
            SwingUtilities.convertPointFromScreen(nue, sl);
            pxOffsetToClickLocation = horiz ? nue.x : nue.y;
        }
        coord -= (horiz ? ins.left : ins.top);
        float fx = coord - pxOffsetToClickLocation;
        float pct = fx / pixelBounds;

        float val = sl.getMinimum() + (valueRange * pct) + valueOffsetAtDragStart;
        if (val >= sl.getMaximum()) {
            int maxCoord = horiz ? sl.getWidth() - ins.right : sl.getHeight() - ins.bottom;
            if (coord > maxCoord && valueOffsetAtDragStart != 0) {
                if (triggerPoint != null) {
                    Point p = new Point(0, 0);
                    SwingUtilities.convertPointToScreen(p, sl);
                    triggerPoint.setLocation(p);
                }
                valueOffsetAtDragStart = 0;
            } else {
                // Ensures that once you go past the far edge of the slider, you don't
                // have to move the mouse all the way back to within its bounds to go
                // backwards
                int nue = (int) (sl.getMaximum() - val);
                valueOffsetAtDragStart = Math.max(1, valueOffsetAtDragStart + nue);
            }
        } else {
            int newValue = Math.max(sl.getMinimum(), Math.min(sl.getMaximum(), Math.round(val)));
            sl.setValue(newValue);
        }
    }

    public void stateChanged(ChangeEvent e) {
        ((JSlider) e.getSource()).repaint();
    }

    private static Map hintsMap = null;

    static final Map getHints() {
        //XXX We REALLY need to put this in a graphics utils lib
        if (hintsMap == null) {
            //Thanks to Phil Race for making this possible
            hintsMap = (Map) (Toolkit.getDefaultToolkit().getDesktopProperty("awt.font.desktophints")); //NOI18N
            if (hintsMap == null) {
                hintsMap = new HashMap();
                hintsMap.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            }
        }
        return hintsMap;
    }

}
