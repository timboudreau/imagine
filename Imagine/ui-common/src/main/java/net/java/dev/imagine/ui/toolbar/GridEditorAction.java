/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.toolbar;

import java.awt.Component;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import org.imagine.editor.api.grid.Grid;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.util.NbBundle.Messages;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Tim Boudreau
 */
@Messages({"GridEdit=Edit &Grid",
    "EditGridDialog=Edit Grid Settings"
})
public class GridEditorAction extends AbstractAction implements Presenter.Toolbar {

    public GridEditorAction() {
        super(Bundle.GridEdit());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        GridEditor ed = new GridEditor(Grid.getInstance().copy());
        ed.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        NotifyDescriptor no = new NotifyDescriptor(ed, Bundle.EditGridDialog(),
                NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.PLAIN_MESSAGE,
                new Object[]{NotifyDescriptor.OK_OPTION, NotifyDescriptor.CANCEL_OPTION},
                NotifyDescriptor.OK_OPTION);

        if (NotifyDescriptor.OK_OPTION.equals(DialogDisplayer.getDefault().notify(no))) {
            Grid.getInstance().updateFrom(ed.getGrid());
        }
    }

    @Override
    public Component getToolbarPresenter() {
        return new GridEditor();
    }

}
