package net.java.dev.imagine.ui.components;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import com.mastfrog.swing.EnumComboBoxModel;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.SharedLayoutRootPanel;
import org.netbeans.paint.api.components.dialog.DialogBuilder;
import org.netbeans.paint.api.components.dialog.DialogController;
import org.netbeans.paint.api.editing.LayerFactory;
import org.openide.awt.Mnemonics;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
@Messages(
        {
            "newImage=New Picture",
            "imageSize=Image Si&ze",
            "instructions=Choose a size and initial layer type.",
            "initialLayer=Initial &Layer",
            "background=&Background",
            "titleNewImage=Picture Properties"
        }
)
public class NewPicturePanel extends SharedLayoutRootPanel {

    private static final String KEY = "new-image";

    private final SizePanel sizePanel;
    private final JComboBox<LayerFactory> layerType;
    private final JComboBox<BackgroundStyle> backgroundStyle;
    private final ChangeSupport supp = new ChangeSupport(this);
    private final L l = new L();

    public NewPicturePanel() {
        sizePanel = new SizePanel(KEY);
//        JLabel titleLabel = new JLabel();
//        Mnemonics.setLocalizedText(titleLabel, Bundle.newImage());
//        Font f = titleLabel.getFont().deriveFont(Font.BOLD).deriveFont(AffineTransform.getScaleInstance(1.25, 1.25));
//        titleLabel.setFont(f);
//        titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("controlShadow")));
//        add(titleLabel);
//        titleLabel.setLabelFor(sizePanel);

        JLabel instructionsLabel = new JLabel();
        Mnemonics.setLocalizedText(instructionsLabel, Bundle.instructions());
        add(instructionsLabel);

        Preferences prefs = prefs();
        String lastLayer = prefs.get(KEY + "layer", null);

        DefaultComboBoxModel<LayerFactory> layerTypeModel = new DefaultComboBoxModel<>();
        for (LayerFactory lf : Lookup.getDefault().lookupAll(LayerFactory.class)) {
            layerTypeModel.addElement(lf);
            if (lf.getName().equals(lastLayer)) {
                layerTypeModel.setSelectedItem(lf);
            }
        }
        layerType = new JComboBox<>(layerTypeModel);
        layerType.setRenderer(new LayerRen());

        String lastBackground = prefs.get(KEY + "background", BackgroundStyle.TRANSPARENT.name());
        BackgroundStyle bs;
        try {
            bs = BackgroundStyle.valueOf(lastBackground);
        } catch (Exception ex) {
            // If the enum constants have changed across versions
            bs = BackgroundStyle.TRANSPARENT;
        }
        backgroundStyle = EnumComboBoxModel.newComboBox(bs);

        JLabel backgroundLabel = new JLabel();
        Mnemonics.setLocalizedText(backgroundLabel, Bundle.background());
        backgroundLabel.setLabelFor(backgroundStyle);

        JLabel layerTypeLabel = new JLabel();
        Mnemonics.setLocalizedText(layerTypeLabel, Bundle.initialLayer());
        add(sizePanel);

        add(new SharedLayoutPanel(backgroundLabel, backgroundStyle));
        add(new SharedLayoutPanel(layerTypeLabel, layerType));
    }

    public static NewPictureParameters showDialog() {
        NewPictureParameters result = DialogBuilder.forName("newPictureDialog")
                .okCancel()
                .setTitle(Bundle.titleNewImage())
                .ownedBy(WindowManager.getDefault().getMainWindow())
                .forComponent(NewPicturePanel::new, (pnl, meaning) -> {
                    switch (meaning) {
                        case AFFIRM:
                            if(pnl.isValidValues()) {
                                pnl.save();
                                return true;
                            } else {
                                return false;
                            }
                    }
                    return true;
                })
                .onShowOrHideDialog((boolean hide, NewPicturePanel pnl, String key, DialogController ctrllr, JDialog dlg) -> {
                    if (hide) {
                        CL cl = (CL) pnl.getClientProperty("cl");
                        if (cl != null) {
                            pnl.removeChangeListener(cl);
                        }
                    } else {
                        CL cl = new CL(ctrllr);
                        pnl.putClientProperty("cl", cl);
                        pnl.addChangeListener(cl);
                        cl.stateChanged(new ChangeEvent(pnl));
                    }
                })
                .openDialog(NewPicturePanel::get);
        return result;
    }

    private void save() {
        sizePanel.save();
        Preferences prefs = prefs();
        prefs.put(KEY + "layer", ((LayerFactory)layerType.getSelectedItem()).getName());
        prefs.put(KEY + "background", ((BackgroundStyle) backgroundStyle.getSelectedItem()).name());
    }

    static class CL implements ChangeListener {

        private final DialogController ctrllr;

        public CL(DialogController ctrllr) {
            this.ctrllr = ctrllr;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            NewPicturePanel npp = (NewPicturePanel) e.getSource();
            ctrllr.setValidity(npp.isValidValues());
        }

    }

    public void addNotify() {
        super.addNotify();
        sizePanel.addChangeListener(l);
        backgroundStyle.addItemListener(l);
        layerType.addItemListener(l);
    }

    public void removeNotify() {
        layerType.removeItemListener(l);
        backgroundStyle.removeItemListener(l);
        sizePanel.removeChangeListener(l);
        super.removeNotify();
    }

    private static Preferences prefs() {
        return NbPreferences.forModule(NewPicturePanel.class);
    }

    public boolean isValidValues() {
        return sizePanel.isValidValue() && backgroundStyle.getSelectedItem() != null
                && layerType.getSelectedItem() != null;
    }

    public void addChangeListener(ChangeListener listener) {
        supp.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
    }

    public NewPictureParameters get() {
        if (!isValidValues()) {
            return null;
        }
        return new NewPictureParameters(sizePanel.getDimension(),
                (LayerFactory) layerType.getSelectedItem(), (BackgroundStyle) backgroundStyle.getSelectedItem());
    }

    public static class NewPictureParameters {

        public final Dimension size;
        public final LayerFactory layerFactory;
        public final BackgroundStyle style;

        public NewPictureParameters(Dimension size, LayerFactory layerFactory, BackgroundStyle style) {
            this.size = size;
            this.layerFactory = layerFactory;
            this.style = style;
        }
    }

    class L implements ChangeListener, ItemListener {

        @Override
        public void stateChanged(ChangeEvent e) {
            supp.fireChange();
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            supp.fireChange();
        }
    }

    static class LayerRen implements ListCellRenderer<LayerFactory> {

        private final DefaultListCellRenderer realRenderer = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList<? extends LayerFactory> list, LayerFactory value, int index, boolean isSelected, boolean cellHasFocus) {
            String name = value.getDisplayName();
            return realRenderer.getListCellRendererComponent(list, name, index, isSelected, cellHasFocus);
        }
    }
}
