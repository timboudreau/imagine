package net.java.dev.imagine.ui.toolbar;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.colorchooser.ColorChooser;
import org.imagine.editor.api.grid.Grid;
import org.imagine.editor.api.grid.GridStyle;
import org.netbeans.paint.api.components.EnumComboBoxModel;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.openide.awt.Mnemonics;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "GRID=Grid"
})
public final class GridEditor extends JPanel {

    private final Grid grid;
    private final ColorChooser gridColor;
    private final JCheckBox gridEnabled = new JCheckBox();
    private final JLabel gridLabel = new JLabel();
    private final JComboBox<GridStyle> gridStyle;
    private final JSlider slider = new JSlider(2, 200, 16);
    private final L l = new L();

    public GridEditor() {
        this(Grid.getInstance());
    }

    public GridEditor(Grid grid) {
        super(new FlowLayout(FlowLayout.LEADING, 5, 0));
        this.grid = grid;
        gridColor = new ColorChooser(grid.getColor());
        setBackground(new Color(0, 0, 0, 0));
        setOpaque(false);
        PopupSliderUI.attach(slider);
        slider.setOpaque(false);
        Mnemonics.setLocalizedText(gridLabel, Bundle.GRID());
        gridStyle = EnumComboBoxModel.newComboBox(grid.getStyle());
        Font f = gridEnabled.getFont().deriveFont(AffineTransform.getScaleInstance(0.9, 0.9));
        slider.setFont(f);
        gridLabel.setFont(f);
        gridStyle.setFont(f);
        gridColor.setFont(f);
        gridEnabled.setFont(f);
        gridLabel.setLabelFor(gridEnabled);
        add(gridLabel);
        add(gridEnabled);
        gridEnabled.setOpaque(false);
        gridEnabled.setContentAreaFilled(false);
        gridEnabled.setHorizontalTextPosition(SwingConstants.LEADING);
        gridEnabled.setHorizontalAlignment(SwingConstants.TRAILING);
        add(slider);
        add(gridColor);
        add(gridStyle);
        gridLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1 && !e.isPopupTrigger()) {
                    gridEnabled.doClick();
                }
            }
        });
        setMinimumSize(new Dimension(32, 32));
    }

    public Grid getGrid() {
        return grid;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        attachAndRefresh();
        Object tb = SwingUtilities.getAncestorOfClass(JToolBar.class, this);
        gridEnabled.setFocusable(tb == null);
        slider.setFocusable(tb == null);
//        gridColor.setFocusable(tb == null);
        gridStyle.setFocusable(tb == null);
    }

    @Override
    public void removeNotify() {
        detach();
        super.removeNotify();
    }

    private void attachAndRefresh() {
        slider.setValue(grid.size());
        gridStyle.setSelectedItem(grid.getStyle());
        gridColor.setColor(grid.getColor());
        gridEnabled.setSelected(grid.isEnabled());
        slider.addChangeListener(l);
        gridColor.addActionListener(l);
        gridEnabled.addActionListener(l);
        gridStyle.addActionListener(l);
        grid.addChangeListener(l);
    }

    private void detach() {
        slider.removeChangeListener(l);
        gridColor.removeActionListener(l);
        gridEnabled.removeActionListener(l);
        gridStyle.removeActionListener(l);
        grid.removeChangeListener(l);
    }

    class L implements ActionListener, ChangeListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JCheckBox) {
                grid.setEnabled(((JCheckBox) e.getSource()).isSelected());
            } else if (e.getSource() instanceof ColorChooser) {
                grid.setColor(((ColorChooser) e.getSource()).getColor());
            } else if (e.getSource() instanceof JComboBox<?>) {
                GridStyle style = (GridStyle) (((JComboBox<GridStyle>) e.getSource()).getSelectedItem());
                grid.setGridStyle(style);
            }
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            if (e.getSource() instanceof JSlider) {
                JSlider slider = (JSlider) e.getSource();
                grid.setSize(slider.getValue());
            } else {
                detach();
                attachAndRefresh();
            }
        }
    }

}
