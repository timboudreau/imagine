package org.imagine.vector.editor.ui.tools.inspectors;

import org.imagine.editor.api.Join;
import org.imagine.editor.api.Cap;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import net.java.dev.imagine.api.vector.elements.PathText;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import org.imagine.vector.editor.ui.tools.inspectors.ShapeElementPropertiesInspector.StrokeCustomizer;
import org.netbeans.paint.api.components.EnumComboBoxModel;
import org.netbeans.paint.api.components.PopupSliderUI;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NumberModel;
import org.netbeans.paint.api.components.number.NumericConstraint;
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

        // Customizer weakly references listeners
        List<Consumer<?>> consumers = new ArrayList<>();
        pnl.putClientProperty("_cul", consumers);

        if (style.isFill()) {

            Paint fill = obj.getFill();
            // XXX need a way to listen
            Customizer<? extends Paint> cus = null;
            Component comp = null;
            if (fill instanceof Color) {
                cus = Customizers.getCustomizer(Color.class, obj.getName(), (Color) fill);
                comp = cus == null ? null : cus.getComponent();
            } else if (fill instanceof GradientPaint) {
                cus = Customizers.getCustomizer(GradientPaint.class, obj.getName(), (GradientPaint) fill);
                comp = cus == null ? null : cus.getComponent();
            } else if (fill instanceof RadialGradientPaint) {
                cus = Customizers.getCustomizer(RadialGradientPaint.class, obj.getName(), (RadialGradientPaint) fill);
                comp = cus == null ? null : cus.getComponent();
            } else if (fill instanceof LinearGradientPaint) {
                cus = Customizers.getCustomizer(LinearGradientPaint.class, obj.getName(), (LinearGradientPaint) fill);
                comp = cus == null ? null : cus.getComponent();
            }
            if (comp != null) {
                JLabel olbl = new JLabel();
                Mnemonics.setLocalizedText(olbl, Bundle.fill());
                pnl.add(olbl);
                olbl.setLabelFor(comp);
                pnl.add(comp);
                if (cus instanceof ListenableCustomizer<?>) {
                    ListenableCustomizer<? extends Paint> c = (ListenableCustomizer<? extends Paint>) cus;
                    Consumer<Paint> fillConsumer = newPaint -> {
                        obj.setFill(newPaint);
                    };
                    consumers.add(fillConsumer);
                    c.listen(fillConsumer);
                }
            }
        }
        if (style.isOutline()) {
            Paint draw = obj.getDraw();
            // XXX need a way to listen
            Customizer<? extends Paint> cus = null;
            Component comp = null;
            if (draw instanceof Color) {
                cus = Customizers.getCustomizer(Color.class, obj.getName(), (Color) draw);
                comp = cus == null ? null : cus.getComponent();
            } else if (draw instanceof GradientPaint) {
                cus = Customizers.getCustomizer(GradientPaint.class, obj.getName(), (GradientPaint) draw);
                comp = cus == null ? null : cus.getComponent();
            } else if (draw instanceof RadialGradientPaint) {
                cus = Customizers.getCustomizer(RadialGradientPaint.class, obj.getName(), (RadialGradientPaint) draw);
                comp = cus == null ? null : cus.getComponent();
            } else if (draw instanceof LinearGradientPaint) {
                cus = Customizers.getCustomizer(LinearGradientPaint.class, obj.getName(), (LinearGradientPaint) draw);
                comp = cus == null ? null : cus.getComponent();
            }
            if (comp != null) {
                JLabel olbl = new JLabel();
                Mnemonics.setLocalizedText(olbl, Bundle.draw());
                pnl.add(olbl);
                olbl.setLabelFor(comp);
                pnl.add(comp);
                if (cus instanceof ListenableCustomizer<?>) {
                    ListenableCustomizer<? extends Paint> c = (ListenableCustomizer<? extends Paint>) cus;
                    Consumer<Paint> fillConsumer = newPaint -> {
                        obj.setDraw(newPaint);
                    };
                    consumers.add(fillConsumer);
                    c.listen(fillConsumer);
                }
            }
        }
        Customizer<BasicStroke> strokeCustomizer = Customizers.getCustomizer(
                BasicStroke.class, shape.getName(), shape.stroke());

//        StrokeCustomizer sc = new StrokeCustomizer(shape.stroke(), newStroke -> {
//            coll.edit(Bundle.EDIT_STROKE(shape.getName()), shape, () -> {
//                shape.setStroke(newStroke);
//            });
//        });
        pnl.add(strokeCustomizer.getComponent());
        if (strokeCustomizer instanceof ListenableCustomizer<?>) {
            ListenableCustomizer<BasicStroke> lc = 
                    (ListenableCustomizer<BasicStroke>) strokeCustomizer;
            lc.listen(stroke -> {
                coll.edit(Bundle.EDIT_STROKE(shape.getName()), shape, () -> {
                    shape.setStroke(stroke);
                });
            });
        } else {
            System.out.println("Not a listenable customizer: " + strokeCustomizer);
        }
        if (shape.item() instanceof PathText) {
            pnl.add(new PathTextInspector().get(((PathText) shape.item()), lookup, item, of));
        }
        return pnl;
    }

    @Messages({"CAP=Cap", "JOIN=Join", "SIZE=Width", "EDIT_STROKE=Edit Stroke on {0}"})
    static class StrokeCustomizer extends JPanel {

        BasicStroke stroke;
        private final JLabel capLabel = new JLabel();
        private final JLabel joinLabel = new JLabel();
        private final JLabel widthLabel = new JLabel();
        private final JComboBox<Cap> capBox;
        private final JComboBox<Join> joinBox;

        StrokeCustomizer(BasicStroke stroke, Consumer<BasicStroke> updater) {
            super(new VerticalFlowLayout());
            if (stroke == null) {
                stroke = new BasicStroke(1);
            }
            this.stroke = stroke;
            capBox = EnumComboBoxModel.newComboBox(Cap.forStroke(stroke));
            joinBox = EnumComboBoxModel.newComboBox(Join.forStroke(stroke));
            Mnemonics.setLocalizedText(capLabel, Bundle.CAP());
            Mnemonics.setLocalizedText(joinLabel, Bundle.JOIN());
            Mnemonics.setLocalizedText(widthLabel, Bundle.SIZE());
            JPanel sub1 = new SharedLayoutPanel();
            sub1.add(capLabel);
            sub1.add(capBox);
            add(sub1);
            JPanel sub2 = new SharedLayoutPanel();
            sub2.add(joinLabel);
            sub2.add(joinBox);
            add(sub2);
            NumericConstraint con = DOUBLE.withMaximum(75D).withMinimum(0.1D).withStep(0.5D);
            NumberModel<Double> mdl = NumberModel.ofDouble(con, () -> {
                return this.stroke.getLineWidth();
            }, sz -> {
                this.stroke = new BasicStroke((float) sz, ((Cap) capBox.getSelectedItem()).constant(),
                        ((Join) joinBox.getSelectedItem()).constant());
                updater.accept(this.stroke);
            });
            JSlider slider = new JSlider(mdl.toBoundedRangeModel());
            PopupSliderUI.attach(slider);
            JPanel sub3 = new SharedLayoutPanel();
            sub3.add(widthLabel);
            sub3.add(slider);
            ItemListener il = e -> {
                Cap cap = (Cap) capBox.getSelectedItem();
                Join join = (Join) joinBox.getSelectedItem();
                if (cap != null) {
                    StrokeCustomizer.this.stroke = new BasicStroke(mdl.get().floatValue(), cap.constant(),
                            join.constant());
                    updater.accept(this.stroke);
                }
            };
            capBox.addItemListener(il);
            joinBox.addItemListener(il);
            add(sub3);
        }
    }


}
