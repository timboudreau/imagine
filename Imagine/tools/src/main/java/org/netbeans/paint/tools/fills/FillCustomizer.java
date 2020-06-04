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
import org.imagine.help.api.annotations.Help;
import org.netbeans.paint.api.components.OneComponentLayout;
import org.netbeans.paint.api.components.explorer.FolderPanel;
import org.netbeans.paint.tools.spi.Fill;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
@Help(id = "FillPattern", makePublic = true, related = {"net.java.dev.imagine.toolcustomizers.RadialGradient"},
        content = {
            @Help.HelpText(
                    language = "en", country = "US", value = "# Fill Patterns\n\nThere are several "
                    + "types of _fill patterns_ to choose from:\n\n"
                    + " * Colors - Simple colors you can choose with the color chooser "
                    + "(hint: to add transparency to a color, _right-drag_ on the color chooser)\n"
                    + " * Gradients - A simple, continuous gradient from one color to another "
                    + "with defined start and end positions, optionally repeating\n"
                    + " * Radial Gradients - Gradients between multiple color _stops_ forming "
                    + "concentric circles within a radius you define, with initial and focus "
                    + "points enabling the gradient pattern to be asymmetrical, as if it were "
                    + "painted on a wall you were facing from an angle.  The stops are defined "
                    + "as percentages of the distance between the center point and radius.\n"
                    + " * Linear Gradients - Like Radial Gradients, these allow you to have "
                    + "a define a continuous gradient between many colors, but in this case, "
                    + "in a linear, striped pattern between start- and end-points\n"
                    + " * Pattern Fills - A repeating pattern created by applying an image "
                    + "a-la a rubber stamp, across the surface of a shape\n\n"
                    + "-----------\n"
                    + "Gradient fills can all be set in repeating, reflecting or non-repeating"
                    + "modes.\n\n"
                    + "Each of these has a customizer which allows you to design and save the fills "
                    + "you design for later reuse; they can also be saved to the *Fills Palette*"
                    + "for reuse across pictures or projects.")})
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
