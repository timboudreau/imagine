/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.actions;

import com.mastfrog.function.throwing.io.IOBiConsumer;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import net.java.dev.imagine.ui.common.RecentFiles;
import net.java.dev.imagine.ui.common.RecentFiles.Category;
import org.openide.awt.DynamicMenuContent;
import org.openide.util.actions.Presenter;

/**
 *
 * @author Tim Boudreau
 */
public class RecentFilesAction extends AbstractAction implements Presenter.Menu, DynamicMenuContent {

    public RecentFilesAction() {
        super(RecentFiles.name());
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        throw new UnsupportedOperationException("Should not be invoked.");
    }

    @Override
    public JMenuItem getMenuPresenter() {
        return RecentFiles.getDefault().createMenu((category, path) -> {
            OpenFileAction.openFile(new File[]{path.toFile()});
        });
    }

    @Override
    public JComponent[] getMenuPresenters() {
        JMenu result = new JMenu(RecentFiles.name());
        RecentFiles recent = RecentFiles.getDefault();
        IOBiConsumer<Category, Path> opener = (category, path) -> {
            OpenFileAction.openFile(new File[]{path.toFile()});
        };
        result.add(recent.createSubmenu(RecentFiles.Category.IMAGINE_NATIVE, opener));
        result.add(recent.createSubmenu(RecentFiles.Category.IMAGE, opener));
        return new JComponent[]{result};
    }

    @Override
    public JComponent[] synchMenuPresenters(JComponent[] jcs) {
        return getMenuPresenters();
    }

}
