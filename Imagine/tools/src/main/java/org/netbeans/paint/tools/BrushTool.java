package org.netbeans.paint.tools;

import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Rectangle;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.image.Surface;
import org.netbeans.paint.api.components.explorer.SelectAndCustomizePanel;
import org.netbeans.paint.tools.fills.AddFillPanel;
import org.netbeans.paint.tools.spi.Brush;
import org.netbeans.paint.tools.spi.MouseDrivenTool;
import org.openide.util.NbBundle;

@ToolDef(name = "Brush", iconPath = "org/netbeans/paint/tools/resources/brush.png")
@Tool(Surface.class)
public final class BrushTool extends MouseDrivenTool implements CustomizerProvider, Customizer, PaintParticipant {

    public BrushTool(Surface surface) {
        super(surface);
    }

    @Override
    public JComponent createCustomizer() {
        return getComponent();
    }

    private Brush getBrush() {
        return (Brush) sel.getSelection();
    }

    @Override
    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
        if (commit && inCommit) {
            try {
                doPaint(g2d, lastPoint, lastModifiers);
            } finally {
                inCommit = false;
            }
        }
    }

    boolean inCommit;
    private Point lastPoint;
    private int lastModifiers;

    protected void dragged(java.awt.Point p, int modifiers) {
        if (!isActive()) {
            return;
        }
        if (!inCommit) {
            inCommit = true;
            lastPoint = p;
            lastModifiers = modifiers;
            repainter.requestCommit();
//            return;
        }
        doPaint(surface.getGraphics(), p, modifiers);
    }

    private void doPaint(Graphics2D g, Point p, int modifiers) {
        Brush brush = getBrush();

        if (brush != null) {
            Rectangle painted = brush.paint(surface.getGraphics(), p, modifiers);
            repainter.requestRepaint(painted);
        }
    }

    @Override
    public Customizer getCustomizer() {
        return this;
    }

    SelectAndCustomizePanel sel = new SelectAndCustomizePanel("brushes", true); //NOI18N

    public JComponent getComponent() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl.add(sel, gbc);
        gbc.gridy++;
        pnl.add(new AddFillPanel(), gbc);
        return pnl;
    }

    public String getName() {
        return NbBundle.getMessage(BrushTool.class, "Brush");
    }

    public String toString() {
        return getName();
    }

    @Override
    public Object get() {
        return null;
    }
}
