package org.netbeans.paint.toolconfigurationui;

import java.awt.Container;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.Action;
import static javax.swing.Action.NAME;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JToggleButton;
import javax.swing.plaf.basic.BasicToggleButtonUI;
import net.dev.java.imagine.api.tool.Category;
import net.dev.java.imagine.api.tool.SelectedTool;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.spi.tool.Tools;
import org.netbeans.paint.api.components.OneComponentLayout;
import org.netbeans.paint.api.components.TilingLayout;
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
            TilingLayout.TilingPolicy.BEST_FIT));

    public ToolsPaletteTopComponent() {
        setDisplayName(NbBundle.getMessage(ToolsPaletteTopComponent.class,
                "CTL_ToolsAction"));
        setLayout(new OneComponentLayout());
        add(pane);
        pane.setBorder(BorderFactory.createEmptyBorder());
        pane.setViewportBorder(BorderFactory.createEmptyBorder());
        pane.setViewportView(panel);
    }

    static int toolsIconSize() {
        return 32;
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
        populateOrig();
        invalidate();
    }

    private void populate() {
        JTabbedPane tabs = new JTabbedPane();
        Map<Category, JScrollPane> pnlForCategory = new HashMap<>();
        ButtonGroup grp = new ButtonGroup();
        for (Category cat : Tools.getDefault()) {
            JScrollPane pane = pnlForCategory.get(cat);
            JPanel panel;
            if (pane == null) {
                panel = new JPanel(new TilingLayout(ToolsPaletteTopComponent::toolsIconSize,
                        TilingLayout.TilingPolicy.BEST_FIT));
                pane = new JScrollPane(panel);
                pane.setName(cat.toString());
                tabs.add(pane);
            } else {
                panel = (JPanel) pane.getViewport().getView();
            }
            for (Tool tool : Tools.getDefault().forCategory(cat)) {
                panel.add(createButton(cat, tool, grp));
            }
        }
        remove(this.pane);
        add(tabs);
    }

    private JToggleButton createButton(Category cat, Tool tool, ButtonGroup grp) {
        JToggleButton b = new JToggleButton();
        b.setUI(new BasicToggleButtonUI());
        b.setToolTipText(tool.getDisplayName());
        b.setIcon(tool.getIcon());
        b.setRolloverEnabled(true);
        b.setBorder(BorderFactory.createEmptyBorder());
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
