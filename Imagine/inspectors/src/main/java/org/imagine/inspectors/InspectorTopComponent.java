package org.imagine.inspectors;

import com.mastfrog.function.TriConsumer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import org.imagine.inspectors.spi.InspectorFactory;
import org.netbeans.api.settings.ConvertAsProperties;
import org.netbeans.paint.api.components.DefaultSharedLayoutData;
import org.netbeans.paint.api.components.FontManagingPanelUI;
import org.netbeans.paint.api.components.LayoutDataProvider;
import org.netbeans.paint.api.components.SharedLayoutData;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.windows.TopComponent;
import org.openide.util.NbBundle.Messages;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.WindowManager;

/**
 * Top component which displays something.
 */
@ConvertAsProperties(
        dtd = "-//org.imagine.inspectors//Inspector//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "inspectors",
        iconBase = "org/imagine/inspectors/gradientfill.png",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "inspectors", openAtStartup = false)
@ActionID(category = "Window", id = "inspectors")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_InspectorAction",
        preferredID = "inspectors"
)
@Messages({
    "CTL_InspectorAction=Inspectors",
    "CTL_InspectorTopComponent=Inspectors",
    "HINT_InspectorTopComponent=Shows properties of selected elements when editing vector layers",
    "LBL_NoInspectors=No Inspectors"
})
public final class InspectorTopComponent extends TopComponent implements TriConsumer<Lookup, List<? extends InspectorFactory<?>>, List<? extends InspectorFactory<?>>>, SharedLayoutData {

    private final Inspectors inspectors = new Inspectors();
    private final JLabel noInspectors = new JLabel(Bundle.LBL_NoInspectors());
    private final JScrollPane scroll = new JScrollPane();
    private static final Border EMPTY_BORDER = BorderFactory.createEmptyBorder();
    private boolean active;
    private final SharedLayoutData data = new DefaultSharedLayoutData();
    private final JPanel fontManagingPanel = new JPanel(new BorderLayout());

    public InspectorTopComponent() {
        setName(Bundle.CTL_InspectorTopComponent());
        setToolTipText(Bundle.HINT_InspectorTopComponent());
        putClientProperty(PROP_KEEP_PREFERRED_SIZE_WHEN_SLIDED_IN, true);
        setLayout(new BorderLayout());
        fontManagingPanel.setUI(new FontManagingPanelUI());
        add(fontManagingPanel, BorderLayout.CENTER);
        noInspectors.setEnabled(false);
        noInspectors.setHorizontalAlignment(SwingConstants.CENTER);
        noInspectors.setVerticalAlignment(SwingConstants.CENTER);
        noInspectors.setVerticalTextPosition(SwingConstants.CENTER);
        noInspectors.setHorizontalTextPosition(SwingConstants.CENTER);
        fontManagingPanel.add(scroll, BorderLayout.CENTER);
        scroll.setViewportView(noInspectors);
        scroll.setViewportBorder(EMPTY_BORDER);
        scroll.setBorder(EMPTY_BORDER);
        int insets = Utilities.isMac() ? 12 : 5;
        fontManagingPanel.setBorder(BorderFactory.createEmptyBorder(insets, insets, insets, insets));
    }

    @Override
    protected void componentShowing() {
        inspectors.listen(this);
    }

    @Override
    protected void componentHidden() {
        inspectors.listen(null);
    }

    @Override
    protected String preferredID() {
        return "inspectors";
    }

    @Override
    protected void componentActivated() {
        active = true;
    }

    @Override
    protected void componentDeactivated() {
        active = false;
    }

    @Override
    public void apply(Lookup lkp, List<? extends InspectorFactory<?>> removed, List<? extends InspectorFactory<?>> added) {
        if (active) {
            // Otherwise, changing focus to this component will clobber
            // its contents
            return;
        }
        boolean changed = false;
        if (added.isEmpty()) {
            changed = scroll.getViewport().getView() != noInspectors;
            if (changed) {
                scroll.setViewportView(noInspectors);
            }
        } else {
            JPanel pnl = new JPanel(new VerticalFlowLayout(5));
            scroll.setViewportView(pnl);
            for (InspectorFactory<?> f : added) {
                changed |= handleOne(lkp, f, pnl);
            }
        }
        if (changed) {
            invalidate();
            revalidate();
            repaint();
        }
    }

    private <T> boolean handleOne(Lookup lkp, InspectorFactory<T> f, Container pnl) {
        boolean any = false;
        Collection<? extends T> all = lkp.lookupAll(f.type());
        if (!all.isEmpty()) {
            int size = all.size();
            int cursor = 0;
            for (T obj : new LinkedHashSet<>(all)) {
                Component comp = f.get(obj, lkp, cursor, size);
                if (comp != null) {
                    pnl.add(comp);
                    any = true;
                }
                cursor++;
            }
        }
        return any;
    }

    @Override
    public void componentOpened() {
        // do nothing
    }

    @Override
    public void componentClosed() {
        // do nothing
    }

    @Override
    public void open() {
        // Allow for creating a cloned inspectors window for
        // other things
        if (inspectors.isGlobal()) {
            Mode mode = WindowManager.getDefault().findMode("inspectors");
            if (mode != null) {
                mode.dockInto(this);
            }
        }
        super.open();
    }

    @Override
    public int getPersistenceType() {
        int result = super.getPersistenceType();
        if (!inspectors.isGlobal()) {
            result = PERSISTENCE_NEVER;
        }
        return result;
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
    }

    public int xPosForColumn(int column) {
        return data.xPosForColumn(column);
    }

    public void register(LayoutDataProvider p) {
        data.register(p);
    }

    public void unregister(LayoutDataProvider p) {
        data.unregister(p);
    }

    public void expanded(LayoutDataProvider p, boolean state) {
        data.expanded(p, state);
    }
}
