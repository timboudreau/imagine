package org.netbeans.paint.tools;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.dev.java.imagine.spi.tools.Customizer;
import net.dev.java.imagine.spi.tools.CustomizerProvider;
import org.netbeans.paint.api.components.explorer.SelectAndCustomizePanel;
import org.netbeans.paint.tools.fills.AddFillPanel;
import org.netbeans.paint.tools.spi.Brush;
import org.netbeans.paint.tools.spi.MouseDrivenTool;

public final class BrushTool extends MouseDrivenTool implements CustomizerProvider, Customizer {

    public BrushTool() {
        super("org/netbeans/paint/tools/resources/brush.png", //NOI18N
              org.openide.util.NbBundle.getMessage(BrushTool.class, 
              "NAME_BrushTool")); //NOI18N
    }

    @Override
    public JComponent createCustomizer() {
        return getComponent();
    }
    
    private Brush getBrush() {
        return (Brush) sel.getSelection();
    }

    protected void dragged(java.awt.Point p, int modifiers) {
        if (!isActive()) {
            return;
        }
        Brush brush = getBrush();

        if (brush != null) {
            brush.paint(getLayer().getSurface().getGraphics(), p);
        }
    }

    @Override
    public Customizer getCustomizer() {
        return this;
    }
    
    SelectAndCustomizePanel sel = new SelectAndCustomizePanel ("brushes", true); //NOI18N
    public JComponent getComponent() {
        JPanel pnl = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.EAST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 1;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;
        pnl.add (sel, gbc);
        gbc.gridy ++;
        pnl.add (new AddFillPanel(), gbc);
        return pnl;
    }
}
