package org.imagine.helpimpl;

import java.awt.BorderLayout;
import org.imagine.help.api.HelpItem;
import org.netbeans.api.settings.ConvertAsProperties;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
@ConvertAsProperties(
        dtd = "-//org.imagine.help.system.impl//Help//EN",
        autostore = false
)
@TopComponent.Description(
        preferredID = "help",
        //        iconBase = "org/imagine/inspectors/i.svg",
        persistenceType = TopComponent.PERSISTENCE_ALWAYS
)
@TopComponent.Registration(mode = "inspectors", openAtStartup = false)
@ActionID(category = "Window", id = "help")
@ActionReference(path = "Menu/Window" /*, position = 333 */)
@TopComponent.OpenActionRegistration(
        displayName = "#CTL_HelpAction",
        preferredID = "help"
)
@Messages({
    "help=Help",
    "CTL_HelpAction=Help",
    "HINT_HelpTopComponent=Shows Help topics and search UI",})
public final class HelpTopComponent extends TopComponent {

    private transient HelpWindowComponent helpWindowComponent;
    private static HelpTopComponent INSTANCE;
    private boolean selectSearch;

    public HelpTopComponent() {
        setLayout(new BorderLayout());
        setDisplayName(Bundle.help());
    }

    @Override
    protected void componentShowing() {
        if (helpWindowComponent == null) {
            helpWindowComponent = new HelpWindowComponent();
            add(helpWindowComponent, BorderLayout.CENTER);
        }
    }

    @Override
    protected String preferredID() {
        return "help";
    }
    
    private void doOpen(HelpItem item) {
        if (helpWindowComponent == null) {
            helpWindowComponent = new HelpWindowComponent();
            add(helpWindowComponent, BorderLayout.CENTER);
        }
        helpWindowComponent.open(item);
    }

    public static void open(HelpItem item) {
        HelpTopComponent tc = (HelpTopComponent) WindowManager.getDefault().findTopComponent("help");
        if (!tc.isOpened()) {
            tc.open();
        }
        tc.doOpen(item);
        tc.requestVisible();
    }

    public static HelpTopComponent getInstance() {
        return INSTANCE == null ? INSTANCE = new HelpTopComponent()
                : INSTANCE;
    }

    void writeProperties(java.util.Properties p) {
        p.setProperty("version", "1.0");
        if (helpWindowComponent != null) {
            p.setProperty("searchTabSelected", "" + helpWindowComponent.isSearchSelected());
        }
    }

    void readProperties(java.util.Properties p) {
        String version = p.getProperty("version");
        if ("1.0".equals(version)) {
            selectSearch = "true".equals(p.getProperty("searchTabSelected", "false"));
            if (helpWindowComponent != null) {
                helpWindowComponent.setSearchSelected(selectSearch);
            }
        }
    }
}
