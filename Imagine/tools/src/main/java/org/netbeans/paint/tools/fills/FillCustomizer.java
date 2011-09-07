/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.tools.fills;

import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.spi.tools.Customizer;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import org.netbeans.paint.api.components.explorer.FolderPanel;
import org.netbeans.paint.tools.spi.Fill;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public class FillCustomizer implements Customizer<Fill> {
    private FolderPanel<Fill> fp = new FolderPanel <Fill> ("fills", Fill.class);

    private FillCustomizer() {
        
    }
    
    public String getName() {
        return NbBundle.getMessage (FillCustomizer.class, "FILL_CUSTOMIZER");
    }

    public Fill get() {
        return fp.getSelection();
    }

    private final Set<ChangeListener> listeners = new HashSet<ChangeListener>();
    public void addChangeListener(ChangeListener l) {
        listeners.add (l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    private static FillCustomizer INSTANCE;
    public static FillCustomizer getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new FillCustomizer();
        }
        return INSTANCE;
    }
    
    public static Customizer combine(String name, Customizer... other) {
        Customizer[] c = new Customizer[other.length + 1];
        System.arraycopy(other, 0, c, 0, other.length);
        c[other.length] = getDefault();
        AggregateCustomizer result = new AggregateCustomizer (name, c);
        return result;
    }

    public JComponent getComponent() {
        return fp;
    }
}
