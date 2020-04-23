/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import net.java.dev.imagine.ui.common.ImageEditorBackground;
import org.imagine.utils.java2d.CheckerboardBackground;
import org.netbeans.paint.api.components.EnumComboBoxModel;
import org.openide.util.NbBundle;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Tim Boudreau
 */
public class ImageEditorBackgroundAction extends AbstractAction implements Presenter.Menu, Presenter.Toolbar, ActionListener {

    private static final String PROP_BG = "bg";

    public ImageEditorBackgroundAction() {
        putValue(NAME, NbBundle.getMessage(ImageEditorBackgroundAction.class, 
                "IMAGE_EDITOR_BACKGROUND")); //NOI18N
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() instanceof JComboBox<?>) {
            CheckerboardBackground bg = (CheckerboardBackground) ((JComboBox<?>) e.getSource())
                    .getSelectedItem();
            ImageEditorBackground.getDefault().setStyle(bg);
        } else {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
            CheckerboardBackground bg = (CheckerboardBackground) item.getClientProperty(PROP_BG);
            ImageEditorBackground.getDefault().setStyle(bg);
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenu menu = new JMenu((String) getValue(NAME));
        ButtonGroup grp = new ButtonGroup();
        CheckerboardBackground selected = ImageEditorBackground.getDefault().style();
        for (CheckerboardBackground bg : CheckerboardBackground.values()) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(bg.toString());
            item.setSelected(selected == bg);
            grp.add(item);
            item.putClientProperty(PROP_BG, bg);
            menu.add(item);
            item.addActionListener(this);
        }
        return menu;
    }

    @Override
    public Component getToolbarPresenter() {
        JPanel pnl = new JPanel();
        JLabel lbl = new JLabel(NbBundle.getMessage(ImageEditorBackgroundAction.class,
                "IMAGE_EDITOR_BACKGROUND"));
        pnl.add(lbl);
        JComboBox<CheckerboardBackground> box
                = EnumComboBoxModel.newComboBox(ImageEditorBackground.getDefault().style());
        box.setFocusable(false); // use the menu item for keyboard access
        pnl.add(box);
        box.addActionListener(this);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        lbl.setFont(lbl.getFont().deriveFont(AffineTransform.getScaleInstance(0.8, 0.8)));
        return pnl;
    }
}
