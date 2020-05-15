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
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizerSupport;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.util.ChangeSupport;

/**
 * Convenience base class for customizer implementations.
 *
 * @author Tim Boudreau
 */
public abstract class AbstractCustomizer<T> extends ListenableCustomizerSupport<T> {

    private final String name;
    private JPanel component;

    public AbstractCustomizer(String name) {
        this.name = name;
    }

    public final JComponent getComponent() {
        if (component != null) {
            return component;
        }
//        JPanel result = new SharedLayoutRootPanel();
        JPanel result = new JPanel(new VerticalFlowLayout(0, true));
        for (JComponent comp : getComponents()) {
            result.add(comp);
        }
        return component = result;
    }

    @Override
    public boolean isInUse() {
        return component != null && component.isDisplayable();
    }

    public final String getName() {
        return name;
    }

    public T get() {
//        if (c != null) {
        return getValue();
//        }
//        return null;
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
        saveValue(get());
        fire();
    }

    public void removeChangeListener(ChangeListener l) {
        assert EventQueue.isDispatchThread();
        supp.removeChangeListener(l);
    }

    protected abstract JComponent[] createComponents();

    protected abstract void saveValue(T value);
}
