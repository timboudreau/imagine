package org.imagine.helpimpl;

import java.awt.BorderLayout;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.TopComponent;

/**
 *
 * @author Tim Boudreau
 */
@Messages("help=Help")
public final class HelpTopComponent extends TopComponent {

    private transient HelpWindowComponent helpWindowComponent;
    private static HelpTopComponent INSTANCE;

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

    public static HelpTopComponent getInstance() {
        return INSTANCE == null ? INSTANCE = new HelpTopComponent()
                : INSTANCE;
    }
}
