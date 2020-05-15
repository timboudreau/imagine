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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Rectangle;
import static java.awt.RenderingHints.KEY_ANTIALIASING;
import static java.awt.RenderingHints.KEY_TEXT_ANTIALIASING;
import static java.awt.RenderingHints.VALUE_ANTIALIAS_ON;
import static java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoundedRangeModel;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.Popup;
import javax.swing.PopupFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.SliderUI;

/**
 *
 * An implementation of SliderUI that draws an integer with a down arrow, and
 * pops up an actual JSlider.
 *
 * @author Timothy Boudreau
 */
public final class PopupSliderUI extends SliderUI implements PropertyChangeListener,
        ChangeListener,
        MouseListener,
        KeyListener,
        FocusListener,
        MouseWheelListener,
        MouseMotionListener {

    private Icon downArrow;
    private Icon downArrowLit;

    public PopupSliderUI() {
        try {
            InputStream downArrowStream = PopupSliderUI.class.getResourceAsStream("downarrow.png");
            BufferedImage downArrowImage = ImageIO.read(downArrowStream);
            downArrow = new ImageIcon(downArrowImage);

            InputStream downArrowLitStream = PopupSliderUI.class.getResourceAsStream("downarrowlit.png");
            BufferedImage downArrowLitImage = ImageIO.read(downArrowLitStream);
            downArrowLit = new ImageIcon(downArrowLitImage);
        } catch (Exception e) {
            throw new Error(e);
        }
    }
    Border raised = BorderFactory.createBevelBorder(BevelBorder.RAISED);
    private static PopupSliderUI instance = null;

    public static SliderUI createUI(JComponent b) {
        if (instance == null) {
            // Stateless UI, there never more than one
            instance = new PopupSliderUI();
        }
        return instance;
    }

    public static void attach(JSlider slider) {
        slider.setUI(createUI(slider));
    }

    @Override
    public void installUI(JComponent c) {
        JSlider js = (JSlider) c;

        c.addMouseListener(this);
        c.addMouseWheelListener(this);
        c.addKeyListener(this);
        js.addChangeListener(this);
        js.setOrientation(JSlider.VERTICAL);
        c.setFocusable(true);
        c.addPropertyChangeListener("model", this);
        c.setCursor(Cursors.forComponent(c).triangleLeft());
        Font f = UIManager.getFont("controlFont");
        if (f == null) {
            f = UIManager.getFont("Label.font");
        }
        if (f == null) {
            f = new JLabel().getFont();
        }
        c.setFont(f.deriveFont(AffineTransform.getScaleInstance(0.875, 0.875)));
        c.addFocusListener(this);
        initBorder(c);
        js.setForeground(UIManager.getColor("controlText"));
    }

    private void initBorder(JComponent c) {
//        Insets ins = raised.getBorderInsets(c);
//        c.setBorder(BorderFactory.createEmptyBorder(ins.top, ins.left,
//                ins.bottom, ins.right));
    }

    @Override
    public void uninstallUI(JComponent c) {
        c.removeMouseListener(this);
        c.removeMouseWheelListener(this);
        c.removeKeyListener(this);
        ((JSlider) c).removeChangeListener(this);
        c.setBorder(null);
        c.removePropertyChangeListener(this);
        c.removeFocusListener(this);
    }
    private static final int FOCUS_GAP = 2;

    @Override
    public int getBaseline(JComponent c, int width, int height) {
        float fontSize = c.getFont().getSize2D();
        Integer bl = (Integer) c.getClientProperty("slBase-" + fontSize);
        if (bl != null) {
            return bl;
        }
        FontMetrics fm = c.getFontMetrics(c.getFont());
        int txtY = c.getInsets().top + fm.getAscent() + 4;
        c.putClientProperty("slBase-" + fontSize, txtY);
        return txtY;
    }

    @Override
    public void paint(Graphics g, JComponent c) {
        Graphics2D gr = (Graphics2D) g;
        gr.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON);
        gr.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_LCD_HRGB);

        Rectangle clip = new Rectangle(0, 0, c.getWidth(), c.getHeight());
        clip = clip.intersection(g.getClipBounds());
        g.setClip(clip);
        JSlider js = (JSlider) c;
        String val;
        StringConverter conv = (StringConverter) js.getClientProperty(StringConverter.CLIENT_PROP_CONVERTER);
        if (conv != null) {
            val = conv.valueToString(js);
        } else {
            val = Integer.toString(js.getValue());
        }

        g.setFont(js.getFont());
        FontMetrics fm = g.getFontMetrics();
        Insets ins = js.getInsets();

        // Leave room for the focus rectangle
        ins.left += FOCUS_GAP;
        ins.right += FOCUS_GAP;
        ins.top += FOCUS_GAP;
        ins.bottom += FOCUS_GAP;
        int h = fm.getHeight();
        int w = fm.stringWidth(val);
        int txtY = fm.getMaxAscent() + ins.top;
//        int txtX = c.getWidth()
//                - (w + ins.right + FOCUS_GAP + downArrow.getIconWidth() - 2);
        int txtX = ins.left + FOCUS_GAP + 1;

        g.setColor(js.isEnabled() ? js.getForeground()
                : UIManager.getColor("controlDkShadow"));

        g.drawString(val, txtX, txtY);
        if (c.hasFocus() && c.getBorder() != raised) {
            Color col = UIManager.getColor("controlShadow");

            g.setColor(col);
            g.drawRect(ins.left, ins.top,
                    js.getWidth() - (ins.left + ins.right),
                    js.getHeight() - (ins.top + ins.bottom));
        }
        int iconY = txtY + 1;
        int iconX = c.getWidth() - (ins.right + downArrow.getIconWidth());
        Icon ic = containsMouse(js) ? downArrowLit
                : downArrow;

        ic.paintIcon(js, g, iconX, iconY);
        g.setColor(UIManager.getColor("controlShadow"));
        g.drawLine(ins.left, iconY, txtX + w, iconY);
    }

    @Override
    public Dimension getPreferredSize(JComponent c) {
        Graphics g = c.getGraphics();
        boolean created = false;
        if (g == null) {
            g = GraphicsEnvironment.
                    getLocalGraphicsEnvironment().
                    getDefaultScreenDevice().
                    getDefaultConfiguration().createCompatibleImage(2, 2).
                    getGraphics();
            created = true;
        }
        try {
            g.setFont(c.getFont());
            JSlider js = (JSlider) c;
            int maxchars = Math.max(Integer.toString(js.getMaximum()).length(),
                    Integer.toString(js.getMinimum()).length());
            int w = g.getFontMetrics().charWidth('A') * maxchars;
            int h = g.getFontMetrics().getHeight();

            w += downArrow.getIconWidth() / 2;
            Insets ins = c.getInsets();

            w += ins.left + ins.right + (FOCUS_GAP * 2)
                    + (downArrow.getIconWidth() - 2);
            h += ins.top + ins.bottom + (FOCUS_GAP * 2) + 3;
            return new Dimension(w, h);
        } finally {
            if (created) {
                g.dispose();
            }
        }
    }

    @Override
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    @Override
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }
    Popup currPopup = null;
    JComponent popupOwner = null;
    JSlider sliderInPopup = null;

    private Popup showPopup(JSlider js, Point p) {
        StringConverter conv = (StringConverter) js.getClientProperty(StringConverter.CLIENT_PROP_CONVERTER);
        int orientation = js.getOrientation();
        SwingUtilities.convertPointToScreen(p, js);
        Rectangle screenBounds = js.getGraphicsConfiguration().getDevice().getDefaultConfiguration().getBounds();
        BoundedRangeModel model = js.getModel();
        Point eventPoint = new Point(p);
        p.y += js.getHeight() / 2;
        return showPopup(p, js, model, screenBounds, orientation, conv, eventPoint);
    }

    private Popup showPopup(Point p, JComponent owner, BoundedRangeModel model, Rectangle screenBounds, int orientation, StringConverter conv, Point eventPoint) {
        if (currPopup != null) {
            hidePopup();
        }
        popupOwner = owner;
        sliderInPopup = new JSlider();
        SimpleSliderUI ui = new SimpleSliderUI((JSlider) owner, eventPoint);
        if (conv != null) {
            ui.setStringConverter(conv);
        }
        sliderInPopup.setUI(ui);
        sliderInPopup.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
        sliderInPopup.setOrientation(orientation);
        sliderInPopup.setPaintLabels(true);
        sliderInPopup.addFocusListener(this);
        Dimension psize = sliderInPopup.getPreferredSize();
        Rectangle r = screenBounds;
        Rectangle rf = new Rectangle(screenBounds);
        Rectangle test = new Rectangle(p, psize);
        if (!r.contains(test)) {
            int offy = Math.max(0, (test.y + test.height) - (rf.y + rf.height));
            int offx = Math.max(0, (test.x + test.width) - (rf.x + rf.width));
            p.x -= offx;
            p.y -= offy;
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener(this);
        sliderInPopup.setModel(model);
        currPopup = PopupFactory.getSharedInstance().getPopup(owner, sliderInPopup,
                p.x, p.y);
        currPopup.show();
        popupOwner.addMouseMotionListener(this);
        return currPopup;
    }

    private static final DefaultBoundedRangeModel UNOWEND_MODEL
            = new DefaultBoundedRangeModel();

    private void hidePopup() {
        if (currPopup == null) {
            return;
        }
        currPopup.hide();
        sliderInPopup.removeFocusListener(this);
        // Whoever designed these method names really liked typing...
        KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener(this);
        // Do this so our slider's model won't hold a permanent
        // reference to our popup slider by listening on its model
        sliderInPopup.setModel(UNOWEND_MODEL);
        popupOwner.removeMouseMotionListener(this);
        popupOwner = null;
        sliderInPopup = null;
        currPopup = null;
    }

    public void mouseClicked(MouseEvent e) {
    }

    private SimpleSliderUI uiOf(JSlider sl) {
        return (SimpleSliderUI) sl.getUI();
    }
    private Reference containingMouse = null;

    public void mouseEntered(MouseEvent e) {
        Component js = (Component) e.getSource();

        containingMouse = new WeakReference(e.getSource());
        js.repaint();
    }

    private boolean containsMouse(JSlider slider) {
        if (containingMouse != null) {
            return containingMouse.get() == slider;
        }
        return false;
    }

    public void mouseExited(MouseEvent e) {
        Component js = (Component) e.getSource();

        containingMouse = null;
        js.repaint();
    }

    public void keyTyped(KeyEvent e) {
    }

    public void keyPressed(KeyEvent e) {
        if (!(e.getSource() instanceof JSlider)) {
            return;
        }
        JSlider js = (JSlider) e.getSource();
//        if (sliderInPopup == null) {
//            return;
//        }

        boolean horiz = sliderInPopup == null ? js.getOrientation() == JSlider.HORIZONTAL
                : sliderInPopup.getOrientation() == JSlider.HORIZONTAL;
        int key = e.getKeyCode();

        if (key == KeyEvent.VK_ENTER || key == KeyEvent.VK_SPACE) {
            if (currPopup != null) {
                hidePopup();
            } else {
                Point p = js.getLocation();
                SwingUtilities.convertPointToScreen(p, js);
                showPopup(js,
                        new Point(p.x + js.getWidth() / 2,
                                p.y + js.getHeight()));
            }
            e.consume();
        } else if (((key == KeyEvent.VK_UP && horiz)
                || (key == KeyEvent.VK_DOWN && !horiz))
                || key == KeyEvent.VK_RIGHT) {
            int val = js.getValue();

            if (val < js.getMaximum()) {
                val++;
                js.setValue(val);
            }
            e.consume();
        } else if (((key == KeyEvent.VK_DOWN && horiz)
                || (key == KeyEvent.VK_UP && !horiz)) || key == KeyEvent.VK_LEFT) {
            int val = js.getValue();

            if (val >= js.getMinimum()) {
                val--;
                js.setValue(val);
            }
            e.consume();
        } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            hidePopup();
            e.consume();
        }
    }

    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void focusGained(FocusEvent e) {
        e.getComponent().repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        e.getComponent().repaint();
    }

    @Override
    public void stateChanged(ChangeEvent e) {
        JSlider js = (JSlider) e.getSource();
        js.repaint();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getSource() instanceof JSlider) {
            switch (evt.getPropertyName()) {
                case "value":
                case "minimum":
                case "maximum":
                case "background":
                case "foreground":
                case "font":
                    ((JSlider) evt.getSource()).repaint();
            }
        } else if (evt.getSource() instanceof KeyboardFocusManager) {
            if ("activeWindow".equals(evt.getPropertyName())) {
                hidePopup();
            } else if (!"permanentFocusOwner".equals(evt.getPropertyName())
                    || evt.getNewValue() == sliderInPopup
                    || evt.getNewValue() == null) {
                if ("focusOwner".equals(evt.getPropertyName())) {
                    if (!(evt.getNewValue() instanceof JSlider)
                            && evt.getNewValue() != null) {
                        hidePopup();
                    }
                }
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        JSlider js = (JSlider) e.getSource();

        js.requestFocus();
        if (!e.isPopupTrigger() && e.getClickCount() == 1) {
            showPopup((JSlider) e.getSource(), e.getPoint());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (popupOwner != null && sliderInPopup.isShowing()) {
            boolean horiz = sliderInPopup.getOrientation() == JSlider.HORIZONTAL;
            Point p = e.getPoint();
            p = SwingUtilities.convertPoint(popupOwner, p, sliderInPopup);
            uiOf(sliderInPopup).dragTo(horiz ? p.x
                    : p.y, sliderInPopup);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (currPopup != null) {
            uiOf(sliderInPopup).clearDetent();
            hidePopup();
            e.consume();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // do nothing
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        double amt = -e.getPreciseWheelRotation();
        if (e.isShiftDown()) {
            amt *= 2;
        }
        if (e.isControlDown()) {
            amt *= 10;
        }
        JSlider slider = (JSlider) e.getSource();
        int newValue = Math.min(slider.getMaximum() - 1, Math.max(slider.getMinimum(), (int) (slider.getValue() + Math.round(amt))));
        if (newValue >= slider.getMinimum() && newValue < slider.getMaximum()) {
            slider.setValue(newValue);
        }
    }
}
