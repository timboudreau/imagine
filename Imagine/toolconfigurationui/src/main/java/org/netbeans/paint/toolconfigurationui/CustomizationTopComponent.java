package org.netbeans.paint.toolconfigurationui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.Serializable;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import org.netbeans.paint.api.components.DefaultSharedLayoutData;
import org.netbeans.paint.api.components.FontManagingPanelUI;
import org.netbeans.paint.api.components.LayoutDataProvider;
import org.netbeans.paint.api.components.SharedLayoutData;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.TopComponent;

/**
 * Top component which displays the customizer, if any, for the currently
 * selected tool.
 */
@TopComponent.Description(preferredID = "customizers",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
//@TopComponent.Registration(mode = "customizers", openAtStartup = true)
@ActionID(category = "Window", id = "org.netbeans.paint.toolconfigurationui.CustomizationTopComponent")
@ActionReference(path = "Menu/Window", position = 10)
@TopComponent.OpenActionRegistration(displayName = "#CTL_CustomizationAction",
        preferredID = "customizers")
public final class CustomizationTopComponent extends TopComponent implements LookupListener, SharedLayoutData {

    private static CustomizationTopComponent instance;
    private final JScrollPane jScrollPane1 = new JScrollPane();

    private CustomizationTopComponent() {
        setLayout(new BorderLayout());
        JPanel fontAdjustingParent = new JPanel(new BorderLayout());
        fontAdjustingParent.setUI(new FontManagingPanelUI());
        add(fontAdjustingParent, BorderLayout.CENTER);
        fontAdjustingParent.add(jScrollPane1, BorderLayout.CENTER);

        setName(NbBundle.getMessage(CustomizationTopComponent.class,
                "CTL_CustomizationTopComponent"));
        setToolTipText(NbBundle.getMessage(CustomizationTopComponent.class,
                "HINT_CustomizationTopComponent"));

        jScrollPane1.setPreferredSize(new Dimension(20, 20));
        jScrollPane1.setBorder(BorderFactory.createEmptyBorder());
        jScrollPane1.setViewportBorder(BorderFactory.createEmptyBorder());
        setInnerComponent(null, true);
        int val = Utilities.getOperatingSystem() == Utilities.OS_MAC ? 12 : 5;
        setBorder(BorderFactory.createEmptyBorder(val, val, val, val));
    }

    private Lookup.Result<Tool> res = null;

    @Override
    public void addNotify() {
        super.addNotify();
        res = Utilities.actionsGlobalContext().lookupResult(Tool.class);
        res.addLookupListener(this);
        resultChanged(null);
    }

    @Override
    public void removeNotify() {
        res.removeLookupListener(this);
        super.removeNotify();
        resultChanged(null);
    }

    /**
     * Gets default instance. Don't use directly, it reserved for '.settings'
     * file only, i.e. deserialization routines, otherwise you can get
     * non-deserialized instance.
     */
    public static synchronized CustomizationTopComponent getDefault() {
        if (instance == null) {
            instance = new CustomizationTopComponent();
        }
        return instance;
    }

    @Override
    public int getPersistenceType() {
        return TopComponent.PERSISTENCE_ALWAYS;
    }

    @Override
    public void componentOpened() {
        // TODO add custom code on component opening
    }

    @Override
    public void componentClosed() {
        // TODO add custom code on component closing
    }

    /**
     * replaces this in object stream
     */
    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    @Override
    protected String preferredID() {
        return "customizers";
    }

    @Override
    public void resultChanged(LookupEvent e) {
        if (e == null && !isDisplayable()) {
            setTool(null);
        } else {
            Collection c = res.allInstances();
            if (c.size() > 0) {
                setTool((Tool) c.iterator().next());
            } else {
                setTool(null);
            }
        }
    }

    private void setTool(Tool tool) {
        CustomizerProvider provider = tool == null ? null
                : tool.getLookup().lookup(CustomizerProvider.class);
        JComponent comp = null;
        if (provider != null) {
            Customizer c = provider.getCustomizer();
            if (c != null) {
                comp = c.getComponent();
            }
        }
        setInnerComponent(comp, tool == null);
        setDisplayName(tool == null ? NbBundle.getMessage(CustomizationTopComponent.class,
                "CTL_CustomizationTopComponent")
                : NbBundle.getMessage(CustomizationTopComponent.class,
                        "FMT_CustomizationTopComponent", tool.getName()));
    }

    private void setInnerComponent(JComponent comp, boolean noTool) {
        if (comp == null) {
            comp = new JLabel(NbBundle.getMessage(
                    CustomizationTopComponent.class, noTool ? "LBL_Empty" : "LBL_No_Customizer"));
            comp.setEnabled(false);
            ((JLabel) comp).setHorizontalTextPosition(SwingConstants.CENTER);
        }
//        comp.setBackground(Color.WHITE); //XXX
        jScrollPane1.setViewportView(comp);
    }

    final static class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 12309L;

        public Object readResolve() {
            return CustomizationTopComponent.getDefault();
        }
    }

    private final SharedLayoutData data = new DefaultSharedLayoutData();

    @Override
    public int xPosForColumn(int column) {
        return data.xPosForColumn(column);
    }

    @Override
    public void register(LayoutDataProvider p) {
        data.register(p);
    }

    @Override
    public void unregister(LayoutDataProvider p) {
        data.unregister(p);
    }

    @Override
    public void expanded(LayoutDataProvider p, boolean state) {
        data.expanded(p, state);
    }
}
