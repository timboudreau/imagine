package net.java.dev.imagine.ui.toolbar;

import com.mastfrog.util.strings.Strings;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.imagine.editor.api.grid.Grid;
import org.imagine.editor.api.grid.SnapSettings;
import org.imagine.editor.api.snap.SnapKind;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "snapTo=S&nap To...",
    "enableSnap=Snappin&g"
})
public final class SnapEditor extends JPanel {

    private Reference<JPopupMenu> menu;
    private final SnapSettings settings;
    private final JButton button = new JButton();
    private final JCheckBox box = new JCheckBox();
    private final JLabel enableLabel = new JLabel();
    private final AL al = new AL();

    public SnapEditor() {
        this(SnapSettings.getGlobal());
    }

    public SnapEditor(SnapSettings settings) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 0));
        this.settings = settings;
        setBackground(new Color(0, 0, 0, 0));
        setOpaque(false);
        Mnemonics.setLocalizedText(button, Bundle.snapTo());
        Mnemonics.setLocalizedText(enableLabel, Bundle.enableSnap());
        enableLabel.setLabelFor(box);
        add(enableLabel);
        add(box);
        box.setHorizontalTextPosition(SwingConstants.LEADING);
        box.setHorizontalAlignment(SwingConstants.TRAILING);
        box.setOpaque(false);
        add(button);
        Font f = box.getFont().deriveFont(AffineTransform.getScaleInstance(0.9, 0.9));
        box.setFont(f);
        button.setFont(f);
        enableLabel.setFont(f);
    }

    public SnapSettings getSettings() {
        return settings;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        attachAndRefresh();
    }

    private void attachAndRefresh() {
        button.setEnabled(settings.isEnabled());
        box.setSelected(settings.isEnabled());

        button.addMouseListener(al);
        button.addActionListener(al);
        box.addActionListener(al);
        settings.addChangeListener(al);
        updateToolTip();

        Object tb = SwingUtilities.getAncestorOfClass(JToolBar.class, this);
        button.setFocusable(tb == null);
        box.setFocusable(tb == null);
    }

    @Override
    public void removeNotify() {
        detach();
        super.removeNotify();
    }

    private void detach() {
        button.removeActionListener(al);
        button.removeMouseListener(al);
        box.removeActionListener(al);
        settings.removeChangeListener(al);
    }

    private void updateToolTip() {
        Set<SnapKind> kinds = settings.getEnabledSnapKinds();
        button.setToolTipText(Strings.join(", ", kinds));
    }

    private JPopupMenu lastMenu() {
        if (menu != null) {
            return menu.get();
        }
        return null;
    }

    private JPopupMenu newPopupMenu() {
        JPopupMenu menu = new JPopupMenu();
        populateMenu(menu);
        this.menu = new WeakReference<>(menu);
        return menu;
    }

    private void showPopup(MouseEvent evt) {
        JPopupMenu last = lastMenu();
        if (last != null && last.isShowing()) {
            return;
        }
        JPopupMenu menu = newPopupMenu();
        if (evt == null) {
            menu.show(button, 0, button.getHeight());
        } else {
            menu.show(button, evt.getX(), evt.getY());
        }
    }

    static void populateMenuOrPopup(boolean includeEnabled, SnapSettings settings, Consumer<JComponent> c) {
        boolean gridEnabled = Grid.getInstance().isEnabled();
        Set<SnapKind> possibleKinds = SnapKind.kinds(true);
        Set<SnapKind> currentKinds = settings.getEnabledSnapKinds();
        if (includeEnabled) {
            JCheckBoxMenuItem ena = new JCheckBoxMenuItem();
            Mnemonics.setLocalizedText(ena, Bundle.enableSnap());
            ena.setSelected(settings.isEnabled());
            ena.addActionListener(ae -> {
                settings.setEnabled(ena.isSelected());
            });
            c.accept(ena);
            c.accept(new JSeparator());
        }
        for (SnapKind kind : possibleKinds) {
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(kind.toString());
            item.setSelected(currentKinds.contains(kind));
            if (kind == SnapKind.GRID && !gridEnabled) {
                item.setEnabled(false);
            }
            if (!settings.isEnabled()) {
                item.setEnabled(false);
            }
            item.addActionListener(ae -> {
                if (item.isSelected()) {
                    settings.addSnapKind(kind);
                } else {
                    settings.removeSnapKind(kind);
                }
            });
            c.accept(item);
        }

    }

    private void populateMenu(JPopupMenu menu) {
        populateMenuOrPopup(false, settings, menu::add);
    }

    class AL extends MouseAdapter implements ActionListener, ChangeListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JButton) {
                showPopup(null);
            } else {
                JCheckBox box = (JCheckBox) e.getSource();
                settings.setEnabled(box.isSelected());
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            showPopup(e);
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            detach();
            attachAndRefresh();
        }

    }
}
