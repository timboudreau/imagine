/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.fx;

import java.awt.Composite;
import java.awt.Dimension;
import java.awt.image.BufferedImageOp;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.spi.effects.Effect;
import org.openide.util.NbBundle;
import org.openide.util.Parameters;

/**
 *
 * @author Tim Boudreau
 */
public abstract class AbstractOpEffect implements Effect {

    private final String name;

    protected AbstractOpEffect(String bundleKey) {
        Parameters.notNull("bundleKey", bundleKey);
        this.name = NbBundle.getMessage(getClass(), bundleKey);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Applicator getApplicator() {
        return new App();
    }

    protected BufferedImageOp getOp() {
        throw new UnsupportedOperationException("Not implemented");
    }
    
    protected BufferedImageOp getOp(Dimension d) {
        return getOp();
    }

    class App implements Effect.BufferedImageOpApplicator {

        @Override
        public BufferedImageOp getOp(Dimension size) {
            return AbstractOpEffect.this.getOp(size);
        }

        @Override
        public JPanel getCustomizer() {
            return new JPanel();
        }

        @Override
        public Composite getComposite() {
            throw new UnsupportedOperationException("Should not be called");
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            //do nothing
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            //do nothing
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
