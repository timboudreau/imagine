/*
 * AbstractCustomizer.java
 *
 * Created on September 29, 2006, 5:44 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.toolcustomizers;

import java.awt.EventQueue;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.openide.util.ChangeSupport;

/**
 * Convenience base class for customizer implementations.
 *
 * @author Tim Boudreau
 */
public abstract class AbstractCustomizer <T extends Object> implements Customizer <T> {
    private final String name;
    public AbstractCustomizer(String name) {
        this.name = name;
    }
    
    public final JComponent getComponent() {
        JPanel result = new SharedLayoutPanel();
        for (JComponent comp : getComponents()) {
            result.add (comp);
        }
        return result;
    }

    public final String getName() {
        return name;
    }

    public T get() {
        if (c != null) {
            return getValue();
        }
        return null;
    }

    protected abstract T getValue();

    private JComponent[] c;
    public final JComponent[] getComponents() {
        if (c == null) {
            c = createComponents();
        }
        return c;
    }

    private final ChangeSupport supp = new ChangeSupport(this);
    public void addChangeListener(ChangeListener l) {
        assert EventQueue.isDispatchThread();
        supp.addChangeListener(l);
    }

    protected void change() {
        supp.fireChange();
        saveValue (get());
    }

    public void removeChangeListener(ChangeListener l) {
        assert EventQueue.isDispatchThread();
        supp.removeChangeListener(l);
    }

    protected abstract JComponent[] createComponents();
    protected abstract void saveValue (T value);
}
