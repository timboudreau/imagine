/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.toolbar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import org.imagine.editor.api.grid.SnapSettings;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.DynamicMenuContent;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Tim Boudreau
 */
@Messages({"SnapEdit=Edit &Snap",
    "EditSnapDialog=Edit Snap Settings"
})
public class SnapEditorAction extends AbstractAction implements Presenter.Toolbar, DynamicMenuContent {

    public SnapEditorAction() {
        super(Bundle.SnapEdit());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SnapSettings settings = SnapSettings.getGlobal();
        SnapEditor ed = new SnapEditor(settings.copy());
        ed.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        NotifyDescriptor no = new NotifyDescriptor(ed, Bundle.EditSnapDialog(),
                NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.PLAIN_MESSAGE,
                new Object[]{NotifyDescriptor.OK_OPTION, NotifyDescriptor.CANCEL_OPTION},
                NotifyDescriptor.OK_OPTION);

        if (NotifyDescriptor.OK_OPTION.equals(DialogDisplayer.getDefault().notify(no))) {
            SnapSettings.getGlobal().copyFrom(ed.getSettings());
        }
    }

    @Override
    public Component getToolbarPresenter() {
        return new SnapEditor();
    }

    @Override
    public JComponent[] getMenuPresenters() {
        SnapSettings settings = SnapSettings.getGlobal();
        List<JComponent> items = new ArrayList<>();
        JMenuItem base = new JMenuItem(this);
        Mnemonics.setLocalizedText(base, Bundle.SnapEdit());
        items.add(base);

        items.add(new JSeparator());
        SnapEditor.populateMenuOrPopup(true, settings, items::add);
        return items.toArray(new JComponent[items.size()]);
    }

    @Override
    public JComponent[] synchMenuPresenters(JComponent[] jcs) {
        return getMenuPresenters();
    }

}
