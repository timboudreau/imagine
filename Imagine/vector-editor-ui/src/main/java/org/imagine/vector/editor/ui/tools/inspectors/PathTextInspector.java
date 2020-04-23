/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Component;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import net.java.dev.imagine.api.vector.elements.PathText;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.paint.api.components.FontComboBoxModel;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NumberModel;
import org.netbeans.paint.api.components.number.NumericConstraint;
import org.netbeans.paint.api.components.number.StandardNumericConstraints;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = InspectorFactory.class, position = -1000)
public class PathTextInspector extends InspectorFactory<PathText> {

    public PathTextInspector() {
        super(PathText.class);
    }

    @Override
    public Component get(PathText obj, Lookup lookup, int item, int of) {
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        ShapesCollection coll = lookup.lookup(ShapesCollection.class);

        SharedLayoutPanel sub = new SharedLayoutPanel();
        JLabel txt = new JLabel();
        Mnemonics.setLocalizedText(txt, Bundle.text());
        JTextArea area = new JTextArea();
        area.setColumns(40);
        area.setRows(5);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        JScrollPane areaScroll = new JScrollPane();
        areaScroll.setViewportBorder(BorderFactory.createEmptyBorder());
        txt.setLabelFor(area);
        sub.add(txt);
        sub.add(area);
        area.addFocusListener(new TextInspector.TextAreaListener(area, obj, coll, shape));
        pnl.add(sub);

        sub = new SharedLayoutPanel();
        JLabel fontLabel = new JLabel();
        Mnemonics.setLocalizedText(fontLabel, Bundle.font());

        sub.add(fontLabel);
        JComboBox<Font> fonts = FontComboBoxModel.newFontComboBox();
        fontLabel.setLabelFor(fonts);
        sub.add(fonts);
        fonts.addActionListener(ae -> {
            Font font = (Font) fonts.getSelectedItem();
            String family = font.getFamily();
            if (!family.equals(obj.getFontName())) {
                coll.edit("Set Font", shape, () -> {
                    obj.setFontName(family);
                });
            }
        });
        pnl.add(sub);

        sub = new SharedLayoutPanel();
        JLabel fontSizeLabel = new JLabel();
        Mnemonics.setLocalizedText(fontSizeLabel, Bundle.fontSize());
        NumericConstraint fontConstraints = StandardNumericConstraints.FLOAT_NON_NEGATIVE
                .withMaximum(300F).withStep(0.5F).withMinimum(4F);
        NumberModel num = NumberModel.ofFloat(fontConstraints, obj::fontSize, (float val) -> {
            coll.edit("Change Font Size", shape, () -> {
                obj.setFontSize(val);
            });
        });
        JSlider slider = new JSlider(num.toBoundedRangeModel());
        PopupSliderUI.attach(slider);
        fontSizeLabel.setLabelFor(slider);
        sub.add(fontSizeLabel);
        sub.add(slider);
        pnl.add(sub);
        pnl.add(new JLabel(""));

        return pnl;
        
    }
}
