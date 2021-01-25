package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Component;
import javax.swing.JPanel;
import net.java.dev.imagine.api.vector.elements.Rectangle;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import static org.netbeans.paint.api.components.number.StandardNumericConstraints.DOUBLE;
import com.mastfrog.swing.layout.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NamedNumberEditor;
import org.netbeans.paint.api.components.number.NumericConstraint;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "x=X",
    "y=Y",
    "width=Width",
    "height=Height"
})
@ServiceProvider(service = InspectorFactory.class)
public class RectangleInspector extends InspectorFactory<Rectangle> {

    public RectangleInspector() {
        super(Rectangle.class);
    }

    @Override
    public Component get(Rectangle obj, Lookup lookup, int item, int of) {
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        NumericConstraint con = DOUBLE.withMaximum(10000D).withMinimum(-2000D);
        pnl.add(new NamedNumberEditor(Bundle.x(), con, obj::x, shape.wrap(obj::setX)));
        pnl.add(new NamedNumberEditor(Bundle.y(), con, obj::y, shape.wrap(obj::setY)));
        pnl.add(new NamedNumberEditor(Bundle.width(), con, obj::width, shape.wrap(obj::setWidth)));
        pnl.add(new NamedNumberEditor(Bundle.height(), con, obj::height, shape.wrap(obj::setHeight)));
        return pnl;
    }
}
