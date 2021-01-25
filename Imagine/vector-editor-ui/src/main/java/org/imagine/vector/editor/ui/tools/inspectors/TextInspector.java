/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import net.java.dev.imagine.api.vector.Textual;
import net.java.dev.imagine.api.vector.elements.Text;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.paint.api.components.FontComboBoxModel;
import com.mastfrog.swing.slider.PopupSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import com.mastfrog.swing.layout.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NamedNumberEditor;
import org.netbeans.paint.api.components.number.NumberModel;
import org.netbeans.paint.api.components.number.NumericConstraint;
import org.netbeans.paint.api.components.number.StandardNumericConstraints;
import static org.netbeans.paint.api.components.number.StandardNumericConstraints.DOUBLE;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "text=Te&xt",
    "font=&Font",
    "fontSize=Font Si&ze"
})
@ServiceProvider(service = InspectorFactory.class)
public class TextInspector extends InspectorFactory<Text> {

    public TextInspector() {
        super(Text.class);
    }

    @Override
    public Component get(Text obj, Lookup lookup, int item, int of) {
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        ShapesCollection coll = lookup.lookup(ShapesCollection.class);

        JPanel pnl = new JPanel(new VerticalFlowLayout());
        NumericConstraint con = DOUBLE.withMaximum(10000D).withMinimum(-2000D);

        pnl.add(new NamedNumberEditor(Bundle.x(), con, obj::x, shape.wrap(obj::setX)));
        pnl.add(new NamedNumberEditor(Bundle.y(), con, obj::y, shape.wrap(obj::setY)));

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
        area.addFocusListener(new TextAreaListener(area, obj, coll, shape));
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

    static class TextAreaListener implements FocusListener {

        private final JTextArea area;
        private final Textual obj;
        private final ShapesCollection coll;
        private final ShapeElement shape;

        public TextAreaListener(JTextArea area, Textual obj, ShapesCollection coll, ShapeElement shape) {
            this.area = area;
            this.obj = obj;
            this.coll = coll;
            this.shape = shape;
        }

        @Override
        public void focusGained(FocusEvent e) {
            area.selectAll();
        }

        @Override
        public void focusLost(FocusEvent e) {
            String txt = area.getText().trim();
            if (!txt.isEmpty() && !obj.getText().equals(txt)) {
                coll.edit("Update text", shape, () -> {
                    obj.setText(txt);
                    shape.changed();
                });
            }
        }
    }

}
