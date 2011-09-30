/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.fx;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.ChangeSupport;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=Effect.class)
public class Squares implements Effect {

    @Override
    public String getName() {
        return "Squares";
    }

    @Override
    public Applicator getApplicator() {
        return new A();
    }

    @Override
    public Type type() {
        return Type.COMPOSITE;
    }

    static class A implements Effect.Applicator {

        private final ChangeSupport supp = new ChangeSupport(this);
        JPanel customizer;
        private int spacing = 5;
        private int size = 50;

        @Override
        public JPanel getCustomizer() {
            if (customizer == null) {
                customizer = new JPanel(new GridBagLayout());
                customizer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                GridBagConstraints g = new GridBagConstraints();
                g.gridwidth = 1;
                g.gridx = 0;
                g.gridy = 0;
                g.fill = GridBagConstraints.HORIZONTAL;
                g.insets = new Insets(5, 5, 5, 5);

                final JLabel sz = new JLabel("Grid Square Size");
                customizer.add(sz, g);
                g.gridx = 1;
                final JSlider sizes = new JSlider(5, 200);
                g.weightx = 1;
                customizer.add(sizes, g);
                g.weightx = 0;
                g.gridx = 2;
                final JLabel spaceLabel = new JLabel("" + 5);
                customizer.add(spaceLabel, g);
                sizes.setValue(50);
                g.gridy = 1;
                g.gridx = 0;

                final JLabel sp = new JLabel("Spacing");
                customizer.add(sp, g);
                final JSlider spaces = new JSlider(1, 200);
                g.weightx = 1;
                g.gridx = 1;
                customizer.add(spaces, g);

                spaces.setValue(5);
                final JLabel sizeLabel = new JLabel("" + 5);
                g.gridx = 2;
                g.weightx = 0;
                customizer.add(sizeLabel, g);

                ChangeListener cl = new ChangeListener() {

                    @Override
                    public void stateChanged(ChangeEvent e) {
                        size = sizes.getValue();
                        spacing = spaces.getValue();
                        supp.fireChange();
                    }
                };
                sizes.addChangeListener(cl);
                spaces.addChangeListener(cl);
            }
            return customizer;
        }

        @Override
        public Composite getComposite() {
            return new C(spacing, size);
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            supp.addChangeListener(cl);
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            supp.removeChangeListener(cl);
        }

        @Override
        public boolean canPreview() {
            return true;
        }

        @Override
        public boolean canApply() {
            return true;
        }

        static class C implements Composite {

            private final int spacing;
            private final int size;

            private C(int spacing, int size) {
                this.spacing = spacing;
                this.size = size;
            }

            @Override
            public CompositeContext createContext(final ColorModel srcColorModel, final ColorModel dstColorModel, RenderingHints hints) {
                return new CompositeContext() {

                    @Override
                    public void dispose() {
                        //do nothing
                    }

                    @Override
                    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
                        for (int x = 0; x < src.getWidth() / size; x++) {
                            int offX = x * size;
                            int childX = offX + (spacing * x);
                            for (int y = 0; y < src.getHeight() / size; y++) {
                                int offY = y * size;
                                int childY = offY + (spacing * y);
                                int w = Math.min(size, src.getWidth() - offX);
                                int h = Math.min(size, src.getHeight() - offY);
                                Raster sub = src.createChild(offX, offY, w, h, childX, childY, null);
                                dstOut.setRect(sub);
                            }
                        }
                    }
                };
            }
        }
    }
}
