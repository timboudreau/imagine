package org.netbeans.paint.toolconfigurationui;

import java.io.Serializable;
import java.util.Collection;
import java.util.MissingResourceException;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import org.imagine.editor.api.ContextLog;
import org.netbeans.paint.api.components.FlexEmptyBorder;
import com.mastfrog.swing.layout.OneComponentLayout;
import org.netbeans.paint.api.components.SharedLayoutRootPanel;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 * Top component which displays the customizer, if any, for the currently
 * selected tool.
 */
@TopComponent.Description(preferredID = "customizers",
        //iconBase="SET/PATH/TO/ICON/HERE",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS)
@ActionID(category = "Window", id = "org.netbeans.paint.toolconfigurationui.CustomizationTopComponent")
@ActionReference(path = "Menu/Window", position = 10)
@TopComponent.OpenActionRegistration(displayName = "#CTL_CustomizationAction",
        preferredID = "customizers")
public final class CustomizationTopComponent extends TopComponent implements LookupListener {

    private static CustomizationTopComponent instance;
    private final SharedLayoutRootPanel customizerContainer = new SharedLayoutRootPanel(0.875);
    private final JScrollPane customizerPane = new JScrollPane(customizerContainer);
    private final JLabel noCustomizer
            = new JLabel(NbBundle.getMessage(
                    CustomizationTopComponent.class, "LBL_No_Customizer")); //NOI18N
    private final JLabel empty
            = new JLabel(NbBundle.getMessage(
                    CustomizationTopComponent.class, "LBL_Empty")); //NOI18N

    private CustomizationTopComponent() {
        setLayout(new OneComponentLayout());
        add(customizerPane);
        customizerContainer.setBorder(new FlexEmptyBorder(0.5F, 0.25F));
        noCustomizer.setEnabled(false);
        noCustomizer.setHorizontalTextPosition(SwingConstants.CENTER);
        empty.setEnabled(false);
        empty.setHorizontalTextPosition(SwingConstants.CENTER);

        setName(NbBundle.getMessage(CustomizationTopComponent.class,
                "CTL_CustomizationTopComponent"));
        setDisplayName(getName());
        setToolTipText(NbBundle.getMessage(CustomizationTopComponent.class,
                "HINT_CustomizationTopComponent"));

//        customizerPane.setPreferredSize(new Dimension(20, 20));
        customizerPane.setBorder(BorderFactory.createEmptyBorder());
        customizerPane.setViewportBorder(BorderFactory.createEmptyBorder());
        setInnerComponent(null, true);
//        int val = Utilities.getOperatingSystem() == Utilities.OS_MAC ? 12 : 5;
//        setBorder(BorderFactory.createEmptyBorder(val, val, val, val));
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

    @Override
    public void open() {
        Mode mode = WindowManager.getDefault().findMode("customizers"); //NOI18N
        if (mode != null) {
            mode.dockInto(this);
        }
        super.open();
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
        Mutex.EVENT.readAccess(() -> {
            if (e == null && !isDisplayable()) {
                clog.log("CustComp set tool null for init"); //NOI18N
                setTool(null);
            } else {
                Collection<? extends Tool> c = res.allInstances();
                if (!c.isEmpty()) {
                    Tool t = c.iterator().next();
                    clog.log("CustComp set tool " + t); //NOI18N
                    setTool(t);
                } else {
                    clog.log("CustComp set tool null for empty lkp res"); //NOI18N
                    setTool(null);
                }
            }
        });
    }

    final ContextLog clog = ContextLog.get("selection");

    private Lookup.Result<CustomizerProvider> toolLookupResult;

    private void attachToToolsLookup(Tool tool) {
        if (toolLookupResult != null) {
            toolLookupResult.removeLookupListener(cpListener);
            toolLookupResult = null;
        }
        if (tool != null) {
            clog.log("CTC attach to lookup of " + tool.getName()); //NOI18N
            toolLookupResult = tool.getLookup().lookupResult(CustomizerProvider.class);
            toolLookupResult.addLookupListener(cpListener);
            cpListener.resultChanged(new LookupEvent(toolLookupResult));
        } else {
            clog.log("CTC no tool lookup to attach to"); //NOI18N
            cpListener.resultChanged(null);
        }
    }

    private final CPListener cpListener = new CPListener();

    final class CPListener implements LookupListener {

        @Override
        @SuppressWarnings("unchecked") //NOI18N
        public void resultChanged(LookupEvent le) {
            Mutex.EVENT.readAccess(() -> {
                if (le == null) {
                    setCustomizerProvider(null, true);
                } else {
                    Lookup.Result<CustomizerProvider> cpRes
                            = (Lookup.Result<CustomizerProvider>) le.getSource();
                    Collection<? extends CustomizerProvider> coll = cpRes.allInstances();
                    if (!coll.isEmpty()) {
                        setCustomizerProvider(coll.iterator().next(), false);
                    }
                }
            });
        }
    }

    private void setTool(Tool tool) {
        attachToToolsLookup(tool);
        setDisplayName(tool == null ? NbBundle.getMessage(CustomizationTopComponent.class,
                "CTL_CustomizationTopComponent") //NOI18N
                : NbBundle.getMessage(CustomizationTopComponent.class,
                        "FMT_CustomizationTopComponent", tool.getName())); //NOI18N
    }

    private void setCustomizerProvider(CustomizerProvider provider, boolean noTool) throws MissingResourceException {
        JComponent comp = null;
        if (provider != null) {
            Customizer c = provider.getCustomizer();
            if (c != null) {
                comp = c.getComponent();
            }
        }
        setInnerComponent(comp, noTool);
    }

    private void setInnerComponent(JComponent comp, boolean noTool) {
        if (comp == null) {
            comp = noTool ? empty : noCustomizer;
        }
        if (customizerContainer.getComponentCount() > 0 && customizerContainer.getComponent(0) == comp) {
            return;
        }
        customizerPane.invalidate();
        customizerContainer.removeAll();
        customizerContainer.add(comp);
        customizerContainer.invalidate();
        customizerContainer.revalidate();
        customizerContainer.repaint();
    }

    final static class ResolvableHelper implements Serializable {

        private static final long serialVersionUID = 12309L;

        public Object readResolve() {
            return CustomizationTopComponent.getDefault();
        }
    }
}
