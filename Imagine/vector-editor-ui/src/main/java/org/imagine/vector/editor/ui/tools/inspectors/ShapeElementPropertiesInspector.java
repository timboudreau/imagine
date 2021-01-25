package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.ListenableCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import net.java.dev.imagine.api.vector.elements.PathText;
import org.imagine.editor.api.PaintingStyle;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import com.mastfrog.swing.EnumComboBoxModel;
import org.netbeans.paint.api.components.SharedLayoutPanel;
import com.mastfrog.swing.layout.VerticalFlowLayout;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "# {0} - shapeName",
    "editStroke=Edit Stroke on {0}",
    "painting=&Painting",
    "fill=Fi&ll",
    "draw=&Outline",
    "# {0} - shapeName",
    "opChangePaintingStyle=Change Painting Style on {0}",
    "# {0} - shapeName",
    "opChangeFill=Change Fill on {0}",
    "# {0} - shapeName",
    "opChangeOutline=Change Outline on {0}"
})
@ServiceProvider(service = InspectorFactory.class, position = 20000)
public class ShapeElementPropertiesInspector extends InspectorFactory<ShapeElement> {

    public ShapeElementPropertiesInspector() {
        super(ShapeElement.class);
    }

    @Override
    public Component get(ShapeElement obj, Lookup lookup, int item, int of) {
        JPanel pnl = new JPanel(new VerticalFlowLayout());
//        ShapeElement shape = lookup.lookup(ShapeElement.class);
        ShapeElement shape = obj;
        ShapesCollection coll = lookup.lookup(ShapesCollection.class);

        SharedLayoutPanel sub = new SharedLayoutPanel();
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, Bundle.painting());
        PaintingStyle style = obj.getPaintingStyle();
        JComboBox<PaintingStyle> styleBox = EnumComboBoxModel.newComboBox(style);
        styleBox.addActionListener(ae -> {
            PaintingStyle nue = (PaintingStyle) styleBox.getSelectedItem();
            if (nue != obj.getPaintingStyle()) {
                coll.edit(Bundle.opChangePaintingStyle(obj.getName()), obj, () -> {
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
                        coll.edit(Bundle.opChangeFill(shape.getName()), obj, abortable -> {
                            if (newPaint != shape.getFill()) {
                                obj.setFill(newPaint);
                            } else {
                                abortable.abort();
                            }
                        });
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
                        coll.edit(Bundle.opChangeOutline(shape.getName()), obj, abortable -> {
                            if (newPaint != obj.getDraw()) {
                                obj.setDraw(newPaint);
                            } else {
                                abortable.abort();
                            }
                        });
                    };
                    consumers.add(fillConsumer);
                    c.listen(fillConsumer);
                }
            }
        }
        Customizer<BasicStroke> strokeCustomizer = Customizers.getCustomizer(
                BasicStroke.class, shape.getName(), shape.stroke());

        pnl.add(strokeCustomizer.getComponent());
        if (strokeCustomizer instanceof ListenableCustomizer<?>) {
            ListenableCustomizer<BasicStroke> lc
                    = (ListenableCustomizer<BasicStroke>) strokeCustomizer;
            lc.listen(stroke -> {
                coll.edit(Bundle.editStroke(shape.getName()), shape, (abortable) -> {
                    if (!Objects.equals(stroke, shape.stroke())) {
                        shape.setStroke(stroke);
                    } else {
                        abortable.abort();
                    }
                });
            });
        }
        if (shape.item() instanceof PathText) {
            pnl.add(new PathTextInspector().get(((PathText) shape.item()), lookup, item, of));
        }
        return pnl;
    }
}
