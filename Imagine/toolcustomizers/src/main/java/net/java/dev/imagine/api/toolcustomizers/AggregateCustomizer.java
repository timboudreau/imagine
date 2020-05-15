/*
 * AggregateCustomizer.java
 *
 * Created on September 30, 2006, 2:51 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.toolcustomizers;

import javax.swing.JComponent;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import org.netbeans.paint.api.components.VerticalFlowLayout;

/**
 *
 * @author Tim Boudreau
 */
public class AggregateCustomizer <T extends Object> implements Customizer<T> {
    protected final Customizer[] merge;
    private final String name;
    /** Creates a new instance of AggregateCustomizer */
    public AggregateCustomizer(String name, Customizer... merge) {
        this.merge = merge;
        this.name = name;
    }

    public JComponent getComponent() {
        JPanel jp = new JPanel(new VerticalFlowLayout());
        for (Customizer c : merge) {
            jp.add(c.getComponent());
        }
        return jp;
    }

    public final String getName() {
        return name;
    }

    public T get() {
        return null;
    }
}
