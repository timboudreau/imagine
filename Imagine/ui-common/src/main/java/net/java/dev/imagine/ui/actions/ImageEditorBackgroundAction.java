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
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.editor.api.EditorBackground;
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
            EditorBackground bg = (EditorBackground) ((JComboBox<?>) e.getSource())
                    .getSelectedItem();
            ImageEditorBackground.getDefault().setStyle(bg);
        } else {
            JRadioButtonMenuItem item = (JRadioButtonMenuItem) e.getSource();
            EditorBackground bg = (EditorBackground) item.getClientProperty(PROP_BG);
            ImageEditorBackground.getDefault().setStyle(bg);
        }
    }

    @Override
    public JMenuItem getMenuPresenter() {
        JMenu menu = new JMenu((String) getValue(NAME));
        ButtonGroup grp = new ButtonGroup();
        ImageEditorBackground ieb = ImageEditorBackground.getDefault();
        EditorBackground[] backgrounds = ieb.allBackgrounds();
        EditorBackground selected = ImageEditorBackground.getDefault().style();
        for (EditorBackground bg : backgrounds) {
            JRadioButtonMenuItem item = new JRadioButtonMenuItem(bg.toString());
            item.setSelected(bg.equals(selected));
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
        JComboBox<? extends EditorBackground> box
                = ImageEditorBackground.getDefault().createSelectorComboBox();
        box.setFocusable(false); // use the menu item for keyboard access
        pnl.add(box);
        box.addActionListener(this);
        lbl.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        lbl.setFont(lbl.getFont().deriveFont(AffineTransform.getScaleInstance(0.8, 0.8)));
        return pnl;
    }
}
