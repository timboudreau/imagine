package org.imagine.vector.editor.ui.tools.inspectors;

import org.netbeans.paint.api.components.number.NumberModel;
import java.awt.Component;
import javax.swing.JPanel;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import static org.netbeans.paint.api.components.number.StandardNumericConstraints.DOUBLE;
import static org.netbeans.paint.api.components.number.StandardNumericConstraints.DOUBLE_NON_NEGATIVE;
import com.mastfrog.swing.layout.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NamedNumberEditor;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "radius=&Radius",
    "centerX=Center &X",
    "centerY=Center &Y",
    "circle=Circle",
    "centerX_EditName=Center X",
    "centerY_EditName=Center Y",
    "radius_EditName=Radius",})
@ServiceProvider(service = InspectorFactory.class)
public class CircleInspector extends InspectorFactory<CircleWrapper> {

    public CircleInspector() {
        super(CircleWrapper.class);
    }

    @Override
    public Component get(CircleWrapper obj, Lookup lookup, int item, int of) {
        ShapesCollection shapes = lookup.lookup(ShapesCollection.class);
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        NumberModel centerXModel = NumberModel.ofDouble(DOUBLE.withMaximum(10000D).withMinimum(-2000D), obj::centerX, shapes.wrapInEdit(Bundle.centerX_EditName(), shape, num -> {
            obj.setCenterX(num);
            if (shape != null) {
                shape.changed();
            }
        }));
        pnl.add(new NamedNumberEditor(Bundle.centerX(), centerXModel));
        NumberModel centerYModel = NumberModel.ofDouble(DOUBLE.withMaximum(10000D).withMinimum(-2000D), obj::centerY,
                shapes.wrapInEdit(Bundle.centerY_EditName(), shape, num -> {
                    obj.setCenterY(num);
                    if (shape != null) {
                        shape.changed();
                    }
                }));
        pnl.add(new NamedNumberEditor(Bundle.centerY(), centerYModel));
        NumberModel radiusXModel = NumberModel.ofDouble(DOUBLE_NON_NEGATIVE.withMaximum(10000D), obj::radius,
                shapes.wrapInEdit(Bundle.radius_EditName(), shape, num -> {
                    obj.setRadius(num);
                    if (shape != null) {
                        shape.changed();
                    }
                }));
        pnl.add(new NamedNumberEditor(Bundle.radius(), radiusXModel));
        return pnl;
    }
}
