/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.fills;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import org.netbeans.paint.api.components.OneComponentLayout;
import org.netbeans.paint.api.components.explorer.FolderPanel;
import org.netbeans.paint.tools.spi.Fill;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
public class FillCustomizer implements Customizer<Fill> {

    private FolderPanel<Fill> fp;

    private final String name;

    public FillCustomizer(String name) {
        this.name = name;
        fp = FolderPanel.create("fills", Fill.class, name, false);
    }

    public FillCustomizer() {
        this.name = null;
        fp = FolderPanel.create("fills", Fill.class);
    }

    public String getName() {
        return name == null
                ? NbBundle.getMessage(FillCustomizer.class, "FILL_CUSTOMIZER")
                : name;
    }

    public Fill get() {
        Fill fill = fp.getSelection();
        if (fill == null) {
            return new Fill() {
                @Override
                public Paint getPaint() {
                    return Color.BLUE;
                }

                @Override
                public Component getCustomizer() {
                    JLabel result = new JLabel("No customizer");
                    result.setHorizontalTextPosition(SwingConstants.CENTER);
                    result.setVerticalTextPosition(SwingConstants.CENTER);
                    JPanel pnl = new JPanel(new OneComponentLayout());
                    pnl.add(result);
                    return result;
                }
            };
        }
        return fp.getSelection();
    }

    private final Set<ChangeListener> listeners = new HashSet<ChangeListener>();

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    private static FillCustomizer INSTANCE;
    private static FillCustomizer DRAW_INSTANCE;

    public static FillCustomizer getOutline() {
        if (DRAW_INSTANCE == null) {
            DRAW_INSTANCE = new FillCustomizer(
                    NbBundle.getMessage(FillCustomizer.class, "CUST_DRAW"));
        }
        return DRAW_INSTANCE;
    }

    public static FillCustomizer getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new FillCustomizer();
        }
        return INSTANCE;
    }

    public JComponent getComponent() {
        return fp;
    }
}
