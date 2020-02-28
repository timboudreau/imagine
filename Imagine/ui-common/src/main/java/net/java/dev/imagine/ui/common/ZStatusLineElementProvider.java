/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.common;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.geom.AffineTransform;
import java.text.DecimalFormat;
import java.util.Collection;
import java.util.Objects;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.imagine.editor.api.Zoom;
import org.openide.awt.StatusLineElementProvider;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = StatusLineElementProvider.class, position = 10)
public class ZStatusLineElementProvider implements StatusLineElementProvider {

    private ZoomLabel label;

    @Override
    public synchronized Component getStatusLineElement() {
        if (label == null) {
            label = new ZoomLabel();
        }
        return label;
    }

    private static final class ZoomLabel extends JLabel implements LookupListener, ChangeListener {

        private Lookup.Result<Zoom> res;

        ZoomLabel() {
            setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
            setFont(getFont().deriveFont(AffineTransform.getScaleInstance(0.9, 0.9)));
        }

        @Override
        public void addNotify() {
            res = Utilities.actionsGlobalContext().lookupResult(Zoom.class);
            res.addLookupListener(this);
            super.addNotify();
            resultChanged(new LookupEvent(res));
        }

        @Override
        public void removeNotify() {
            res.removeLookupListener(this);
            res = null;
            if (currentZoom != null) {
                currentZoom.removeChangeListener(this);
                currentZoom = null;
            }
            setText("");
            super.removeNotify();
        }

        @Override
        public void resultChanged(LookupEvent le) {
            Lookup.Result<Zoom> src = (Lookup.Result<Zoom>) le.getSource();
            Collection<? extends Zoom> all = src.allInstances();
            if (all.isEmpty()) {
                attachTo(null);
            } else {
                attachTo(all.iterator().next());
            }
        }

        Zoom currentZoom;

        void attachTo(Zoom zoom) {
            if (Objects.equals(zoom, currentZoom)) {
                updateFrom(zoom);
                return;
            }
            if (currentZoom != null) {
                currentZoom.removeChangeListener(this);
            }
            currentZoom = zoom;
            if (zoom == null) {
                setText("");
            } else {
                zoom.addChangeListener(this);
                updateFrom(zoom);
            }
        }

        private static final DecimalFormat FMT = new DecimalFormat("#.00%");

        void updateFrom(Zoom zoom) {
            if (zoom == null) {
                setText("");
            } else {
                int val = (int) Math.round(zoom.scale(100));
                if (val <= 10) {
                    setText(FMT.format(zoom.scale(100)));
                } else {
                    setText(val + "%");
                }
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            Zoom z = (Zoom) e.getSource();
            updateFrom(z);
        }

        private boolean firstPaint = true;
        private int charWidth = -1;

        @Override
        public void paint(Graphics g) {
            if (firstPaint) {
                g.setFont(getFont());
                FontMetrics fm = g.getFontMetrics(getFont());
                charWidth = fm.stringWidth("0");
                firstPaint = false;
            }
            super.paint(g);
        }

        @Override
        public void setFont(Font font) {
            firstPaint = true;
            charWidth = -1;
            super.setFont(font);
        }

        @Override
        public Dimension getPreferredSize() {
            Insets ins = getInsets();
            Dimension result = super.getPreferredSize();
            int w = charWidth;
            if (w == -1) {
                w = 12;
            }
            result.width = Math.max(ins.left + ins.right + (w * 4), result.width);
            result.height = Math.max(10, result.height);
            return result;
        }
    }
}
