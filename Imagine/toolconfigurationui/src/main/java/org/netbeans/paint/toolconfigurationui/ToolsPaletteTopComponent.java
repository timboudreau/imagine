package org.netbeans.paint.toolconfigurationui;

import com.mastfrog.util.strings.Strings;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.UIManager;
import net.dev.java.imagine.api.tool.Category;
import net.dev.java.imagine.api.tool.SelectedTool;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.spi.tool.Tools;
import com.mastfrog.geometry.util.PooledTransform;
import org.netbeans.paint.api.components.FlexEmptyBorder;
import org.netbeans.paint.api.components.OneComponentLayout;
import org.netbeans.paint.api.components.TilingLayout;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.ContextAwareAction;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
@TopComponent.Description(preferredID = "tools-palette",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@ActionID(category = "Window",
        id = "org.netbeans.paint.toolconfigurationui.ToolsPaletteTopComponent")
@ActionReference(path = "Menu/Window", position = 11)
@TopComponent.OpenActionRegistration(displayName = "#CTL_ToolsAction",
        preferredID = "tools-palette")
public final class ToolsPaletteTopComponent extends TopComponent {

    private static ToolsPaletteTopComponent instance;
    private final JScrollPane pane = new JScrollPane();
    private final JPanel panel = new JPanel(new TilingLayout(ToolsPaletteTopComponent::toolsIconSize,
            TilingLayout.TilingPolicy.FIXED_SIZE));

    public ToolsPaletteTopComponent() {
        setDisplayName(NbBundle.getMessage(ToolsPaletteTopComponent.class,
                "CTL_ToolsAction"));
        setLayout(new OneComponentLayout());
        add(pane);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setViewportBorder(BorderFactory.createEmptyBorder());
        pane.setViewportView(panel);
    }

    static int tiSize = 26;
    static int toolsIconSize() {
        return tiSize;
    }

    @Override
    protected String preferredID() {
        return "tools-palette";
    }

    @Override
    public void open() {
        Mode mode = WindowManager.getDefault().findMode("tools-palette");
        if (mode != null) {
            mode.dockInto(this);
        }
        super.open();
    }

    @Override
    protected void componentOpened() {
        super.componentOpened();
//        populateOrig();
        populate();
        invalidate();
    }

    private void populate() {
//        JPanel tabs = new JPanel(new VerticalFlowLayout(3));
        JPanel tabs = new JPanel(new VerticalFlowLayout(3, true));
        tabs.setBorder(new FlexEmptyBorder());

        Map<Category, JPanel> pnlForCategory = new HashMap<>();
        ButtonGroup grp = new ButtonGroup();
        Iterator<Category> it = Tools.getDefault().iterator();
        List<Category> l = new ArrayList<>();
        while (it.hasNext()) {
            l.add(it.next());
        }
        l.sort((a, b) -> {
            return -a.name().compareToIgnoreCase(b.name());
        });
        for (Category cat : l) {
            JPanel pane = pnlForCategory.get(cat);
            if (pane == null) {
                JLabel lbl = new JLabel(Strings.capitalize(cat.name()));
                pane = new JPanel(new TilingLayout(ToolsPaletteTopComponent::toolsIconSize,
                        TilingLayout.TilingPolicy.BEST_FIT));
                pane.setName(cat.toString());
                lbl.setLabelFor(pane);
                lbl.setFont(lbl.getFont().deriveFont(AffineTransform.getScaleInstance(0.875, 0.875)));
                tabs.add(lbl);
                tabs.add(pane);
            }
            for (Tool tool : Tools.getDefault().forCategory(cat)) {
                pane.add(createButton(cat, tool, grp));
            }
        }
        this.pane.setViewportView(tabs);
    }

    private JToggleButton createButton(Category cat, Tool tool, ButtonGroup grp) {
        JToggleButton b = new JToggleButton();
//        b.setUI(new BasicToggleButtonUI());
        b.setToolTipText(tool.getDisplayName());

        Icon icon = new ScaledIcon(tool.getIcon());
        tiSize = Math.max(icon.getIconHeight(), Math.max(tiSize, icon.getIconWidth()));
        b.setIcon(icon);
        b.setRolloverEnabled(true);
        b.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3),
                BorderFactory.createLineBorder(UIManager.getColor("controlDkShadow"))));
//        b.setContentAreaFilled(false);
        b.setText("");
        b.setMinimumSize(new Dimension(toolsIconSize(), toolsIconSize()));
        b.setSelected(SelectedTool.getDefault().isToolSelected(tool.getName()));
        b.addActionListener(ae -> {
            SelectedTool.getDefault().setSelectedTool(tool);
        });
        SelectedTool.Observer obs = (Tool old, Tool nue) -> {
            boolean select = nue != null && tool.getName().equals(nue.getName());
            System.out.println("observer notified " + old + " -> " + nue + " select " + select);
            b.setSelected(select);
        };
        SelectedTool.getDefault().addObserver(obs);
        b.putClientProperty("obs", obs);
        grp.add(b);
        return b;
    }

    static final class ScaledIcon implements Icon {

        private final Icon orig;

        public ScaledIcon(Icon orig) {
            this.orig = orig;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            PooledTransform.withScaleInstance(2, 2, xform -> {
                Graphics2D gx = ((Graphics2D) g.create());
                gx.translate(-x, -y);
                gx.transform(xform);
                try {
                    orig.paintIcon(c, gx, x, y);
                } finally {
                    gx.dispose();
                }
            });
        }

        @Override
        public int getIconWidth() {
            return orig.getIconWidth() * 2;
        }

        @Override
        public int getIconHeight() {
            return orig.getIconHeight() * 2;
        }

    }

    private void populateOrig() {
        List<? extends Action> actions = Utilities.actionsForPath("Menu/ToolActions");
        for (Action a : actions) {
            if (a instanceof ContextAwareAction) {
                a = ((ContextAwareAction) a).createContextAwareInstance(Utilities.actionsGlobalContext());
            }
            if (a instanceof Presenter.Toolbar) {
                Presenter.Toolbar ptb = (Presenter.Toolbar) a;
                panel.add(ptb.getToolbarPresenter());
                continue;
            }
            JButton btn = new JButton(a);
            btn.setContentAreaFilled(false);
            btn.setBorder(BorderFactory.createEmptyBorder());
            btn.setRolloverEnabled(true);
            btn.setToolTipText((String) a.getValue(NAME));
            btn.setText("");

            panel.add(btn);
        }
    }

    @Override
    protected void componentClosed() {
        super.componentClosed();
        panel.removeAll();
    }

    @Override
    public int getPersistenceType() {
        return PERSISTENCE_ALWAYS;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        Container top = getTopLevelAncestor();
        if (top != null) {
            dim.width = Math.min(top.getWidth() / 5, dim.width);
        }
        return dim;
    }

    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    final static class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 12319L;

        public Object readResolve() {
            return ToolsPaletteTopComponent.getDefault();
        }
    }

    public static synchronized ToolsPaletteTopComponent getDefault() {
        if (instance == null) {
            instance = new ToolsPaletteTopComponent();
        }
        return instance;
    }
}
