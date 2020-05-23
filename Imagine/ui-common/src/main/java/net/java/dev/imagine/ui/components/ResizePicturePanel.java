/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.ui.components;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.Preferences;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.event.ChangeListener;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.Picture;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.SharedLayoutRootPanel;
import org.netbeans.paint.api.components.number.NumberEditor;
import org.netbeans.paint.api.components.number.NumberModel;
import org.netbeans.paint.api.components.number.NumericConstraint;
import org.netbeans.paint.api.components.number.StandardNumericConstraints;
import org.openide.awt.Mnemonics;
import org.openide.util.ChangeSupport;
import org.openide.util.NbBundle;
import org.openide.util.NbPreferences;

/**
 *
 * @author Tim Boudreau
 */
public class ResizePicturePanel extends SharedLayoutRootPanel {

    private final JLabel titleLabel = new JLabel();
    private final JLabel widthLabel = new JLabel();
    private final JLabel heightLabel = new JLabel();
    private final Dimension originalDimension;
    private final Dimension currentDimension;
    private final Dimension maxLayerSize = new Dimension();
    private final JCheckBox resizeCanvas = new JCheckBox();
    private final NumberModel widthModel;
    private final NumberModel heightModel;
    private final NumberEditor widthEditor;
    private final NumberEditor heightEditor;
    private final JButton useLayerSizeButton = new JButton();
    private final AL al = new AL();
    private boolean updating;
    private final ChangeSupport supp = new ChangeSupport(this);

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public ResizePicturePanel(Picture picture) {
        originalDimension = picture.getSize();
        if (originalDimension.width == 0 || originalDimension.height == 0) {
            originalDimension.width = 640;
            originalDimension.height = 480;
        }
        currentDimension = new Dimension(originalDimension);
        NumericConstraint widthConstraint = StandardNumericConstraints.INTEGER.withMinimum(1).withMaximum(Math.max(originalDimension.width, 20_000));
        NumericConstraint heightConstraint = StandardNumericConstraints.INTEGER.withMinimum(1).withMaximum(Math.max(originalDimension.width, 20_000));
        widthModel = NumberModel.ofInt(widthConstraint, this::currentWidth, this::setCurrentWidth);
        heightModel = NumberModel.ofInt(heightConstraint, this::currentHeight, this::setCurrentHeight);
        widthEditor = new NumberEditor(widthModel);
        heightEditor = new NumberEditor(heightModel);
        Mnemonics.setLocalizedText(titleLabel, NbBundle.getMessage(ResizePicturePanel.class, "LBL_Size"));
        Mnemonics.setLocalizedText(widthLabel, NbBundle.getMessage(ResizePicturePanel.class, "LBL_Width"));
        Mnemonics.setLocalizedText(heightLabel, NbBundle.getMessage(ResizePicturePanel.class, "LBL_Height"));
        Mnemonics.setLocalizedText(resizeCanvas, NbBundle.getMessage(ResizePicturePanel.class, "LBL_ResizeCanvas"));
        Mnemonics.setLocalizedText(useLayerSizeButton, NbBundle.getMessage(ResizePicturePanel.class, "BTN_SizeCanvasToLayers"));
        heightLabel.setLabelFor(heightEditor);
        widthLabel.setLabelFor(widthEditor);
        add(new SharedLayoutPanel(titleLabel));
        titleLabel.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIManager.getColor("controlDkShaadow")));
        add(new SharedLayoutPanel(widthLabel, widthEditor));
        add(new SharedLayoutPanel(heightLabel, heightEditor));
        add(new SharedLayoutPanel(resizeCanvas, useLayerSizeButton));
        for (Layer layer : picture) {
            Rectangle r = layer.getContentBounds();
            int w = r.width + -r.x;
            int h = r.height + -r.y;
            maxLayerSize.width = Math.max(maxLayerSize.width, w);
            maxLayerSize.height = Math.max(maxLayerSize.height, h);
        }
        resizeCanvas.addActionListener(al);
        useLayerSizeButton.addActionListener(al);
        updateState();
        load();
    }

    public Dimension getDimension() {
        return new Dimension(currentDimension);
    }

    public boolean isResizeCanvasOnly() {
        return resizeCanvas.isSelected();
    }

    private void load() {
        Preferences prefs = prefs();
        resizeCanvas.setSelected(prefs.getBoolean("resizeCanvas", false));
        updateState();
    }

    private void updateState() {
        useLayerSizeButton.setEnabled((currentDimension.width != maxLayerSize.width
                || currentDimension.height != maxLayerSize.height) && resizeCanvas.isSelected());
        if (!updating) {
            supp.fireChange();
        }
    }

    private Preferences prefs() {
        return NbPreferences.forModule(ResizePicturePanel.class);
    }

    private void setCurrentWidth(int newWidth) {
        currentDimension.width = newWidth;
        updateState();
    }

    private void setCurrentHeight(int newHeight) {
        currentDimension.height = newHeight;
        updateState();
    }

    private int currentWidth() {
        return currentDimension.width;
    }

    private int currentHeight() {
        return currentDimension.height;
    }

    public void addChangeListener(ChangeListener listener) {
        supp.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        supp.removeChangeListener(listener);
    }

    class AL implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() == useLayerSizeButton) {
                updating = true;
                widthModel.setValue(maxLayerSize.width);
                heightModel.setValue(maxLayerSize.height);
                widthEditor.refresh();
                heightEditor.refresh();
                updating = false;
            }
            updateState();
        }
    }
}
