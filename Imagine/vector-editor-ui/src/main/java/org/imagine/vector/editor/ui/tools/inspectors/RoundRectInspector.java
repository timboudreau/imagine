package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Component;
import javax.swing.JPanel;
import net.java.dev.imagine.api.vector.elements.RoundRect;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import static org.netbeans.paint.api.components.number.StandardNumericConstraints.DOUBLE;
import org.netbeans.paint.api.components.VerticalFlowLayout;
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
    "arcWidth=Arc Width",
    "arcHeight=Arc Height"
})
@ServiceProvider(service = InspectorFactory.class)
public class RoundRectInspector extends InspectorFactory<RoundRect> {

    public RoundRectInspector() {
        super(RoundRect.class);
    }

    @Override
    public Component get(RoundRect obj, Lookup lookup, int item, int of) {
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        NumericConstraint con = DOUBLE.withMaximum(10000D).withMinimum(-2000D);
        pnl.add(new NamedNumberEditor(Bundle.x(), con, obj::x, shape.wrap(obj::setX)));
        pnl.add(new NamedNumberEditor(Bundle.y(), con, obj::y, shape.wrap(obj::setY)));
        pnl.add(new NamedNumberEditor(Bundle.width(), con, obj::width, shape.wrap(obj::setWidth)));
        pnl.add(new NamedNumberEditor(Bundle.height(), con, obj::height, shape.wrap(obj::setHeight)));
        pnl.add(new NamedNumberEditor(Bundle.arcWidth(), con, obj::getArcWidth, shape.wrap(obj::setArcWidth)));
        pnl.add(new NamedNumberEditor(Bundle.arcHeight(), con, obj::getArcHeight, shape.wrap(obj::setArcHeight)));
        return pnl;
    }
}
