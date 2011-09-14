package net.java.dev.imagine.fx;

import com.jhlabs.image.GaussianFilter;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.image.BufferedImageOp;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=Effect.class)
public final class GaussianBlur implements Effect {

    @Override
    public String getName() {
        return NbBundle.getMessage(GaussianBlur.class, "GAUSSIAN_BLUR", GaussianBlur.class);
    }

    @Override
    public Applicator getApplicator() {
        return new A();
    }
    
    static class A implements BufferedImageOpApplicator {
        private final ChangeSupport supp = new ChangeSupport(this);
        int amt = 6;

        @Override
        public BufferedImageOp getOp(Dimension ignored) {
            return new GaussianFilter(amt);
        }

        @Override
        public JPanel getCustomizer() {
            final JSlider slider = new JSlider(1, 20);
            slider.setValue(amt);
            JLabel lbl = new JLabel(NbBundle.getMessage(A.class, "RADIUS", slider));
            final JLabel amount = new JLabel("" + amt);
            int dist = Utilities.isMac() ? 13 : 5;
            JPanel result = new JPanel(new FlowLayout(FlowLayout.LEADING, dist, dist));
            result.add(lbl);
            result.add(slider);
            result.add(amount);
            result.setBorder(BorderFactory.createEmptyBorder(dist, dist, dist, dist));
            slider.addChangeListener(new ChangeListener() {

                @Override
                public void stateChanged(ChangeEvent e) {
                    amt = slider.getValue();
                    String txt = amt + "";
                    if (txt.length() == 1) {
                        txt = " " + txt;
                    }
                    amount.setText(txt);
                    supp.fireChange();
                }
            });
            return result;
        }

        @Override
        public Composite getComposite() {
            throw new UnsupportedOperationException("Not supported.");
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
        
    }
    
}
