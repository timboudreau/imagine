package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Component;
import javax.swing.JPanel;
import net.java.dev.imagine.api.vector.elements.Oval;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.Shapes;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import static org.netbeans.paint.api.components.number.StandardNumericConstraints.DOUBLE;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NamedNumberEditor;
import org.netbeans.paint.api.components.number.NumericConstraint;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = InspectorFactory.class)
public class OvalInspector extends InspectorFactory<Oval> {

    public OvalInspector() {
        super(Oval.class);
    }

    @Override
    public Component get(Oval obj, Lookup lookup, int item, int of) {
        ShapesCollection shapes = lookup.lookup(Shapes.class);
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        if (shape == null || shapes == null) {
            return pnl;
        }
        NumericConstraint con = DOUBLE.withMaximum(10000D).withMinimum(-2000D);
        pnl.add(new NamedNumberEditor(Bundle.x(), con, obj::x, shapes.wrapInEdit("X", shape, obj::setX)));
        pnl.add(new NamedNumberEditor(Bundle.y(), con, obj::y, shapes.wrapInEdit("Y", shape, obj::setY)));
        pnl.add(new NamedNumberEditor(Bundle.width(), con, obj::width, shapes.wrapInEdit("Width", shape, obj::setWidth)));
        pnl.add(new NamedNumberEditor(Bundle.height(), con, obj::height, shapes.wrapInEdit("Height", shape, obj::setHeight)));
        return pnl;
    }
}
