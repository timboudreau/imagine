package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.netbeans.paint.api.components.EnumComboBoxModel;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "painting=&Painting",
    "fill=Fi&ll",
    "draw=&Outline"
})
@ServiceProvider(service = InspectorFactory.class, position = 20000)
public class ShapeElementPropertiesInspector extends InspectorFactory<ShapeElement> {

    public ShapeElementPropertiesInspector() {
        super(ShapeElement.class);
    }

    @Override
    public Component get(ShapeElement obj, Lookup lookup, int item, int of) {
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        ShapesCollection coll = lookup.lookup(ShapesCollection.class);

        SharedLayoutPanel sub = new SharedLayoutPanel();
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, Bundle.painting());
        PaintingStyle style = obj.getPaintingStyle();
        JComboBox<PaintingStyle> styleBox = EnumComboBoxModel.newComboBox(style);
        styleBox.addActionListener(ae -> {
            PaintingStyle nue = (PaintingStyle) styleBox.getSelectedItem();
            if (nue != obj.getPaintingStyle()) {
                coll.edit("Change painting style", obj, () -> {
                    obj.setPaintingStyle(nue);
                });
            }
        });
        lbl.setLabelFor(styleBox);
        sub.add(lbl);
        sub.add(styleBox);
        pnl.add(sub);

        if (style.isFill()) {

            Paint fill = obj.getFill();
            // XXX need a way to listen
            Customizer<? extends Paint> cus = null;
            Component comp = null;
            if (fill instanceof Color) {
                cus = Customizers.getCustomizer(Color.class, obj.toString(), (Color) fill);
                comp = cus == null ? null : cus.getComponent();
            } else if (fill instanceof GradientPaint) {
                cus = Customizers.getCustomizer(GradientPaint.class, obj.toString(), (GradientPaint) fill);
                comp = cus == null ? null : cus.getComponent();
            } else if (fill instanceof RadialGradientPaint) {
                cus = Customizers.getCustomizer(RadialGradientPaint.class, obj.toString(), (RadialGradientPaint) fill);
                comp = cus == null ? null  : cus.getComponent();
            } else if (fill instanceof LinearGradientPaint) {
                cus = Customizers.getCustomizer(LinearGradientPaint.class, obj.toString(), (LinearGradientPaint) fill);
                comp = cus == null ? null  : cus.getComponent();
            }
            if (comp != null) {
                JLabel olbl = new JLabel();
                Mnemonics.setLocalizedText(olbl, Bundle.fill());
                pnl.add(olbl);
                olbl.setLabelFor(comp);
                pnl.add(comp);
            }
        }
        if (style.isOutline()) {
            Paint draw = obj.getFill();
            // XXX need a way to listen
            Customizer<? extends Paint> cus = null;
            Component comp = null;
            if (draw instanceof Color) {
                cus = Customizers.getCustomizer(Color.class, obj.toString(), (Color) draw);
                comp = cus == null ? null  : cus.getComponent();
            } else if (draw instanceof GradientPaint) {
                cus = Customizers.getCustomizer(GradientPaint.class, obj.toString(), (GradientPaint) draw);
                comp = cus == null ? null  : cus.getComponent();
            } else if (draw instanceof RadialGradientPaint) {
                cus = Customizers.getCustomizer(RadialGradientPaint.class, obj.toString(), (RadialGradientPaint) draw);
                comp = cus == null ? null  : cus.getComponent();
            } else if (draw instanceof LinearGradientPaint) {
                cus = Customizers.getCustomizer(LinearGradientPaint.class, obj.toString(), (LinearGradientPaint) draw);
                comp = cus == null ? null  : cus.getComponent();
            }
            if (comp != null) {
                JLabel olbl = new JLabel();
                Mnemonics.setLocalizedText(olbl, Bundle.draw());
                pnl.add(olbl);
                olbl.setLabelFor(comp);
                pnl.add(comp);
            }
        }
        return pnl;
    }
}
