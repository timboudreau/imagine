package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Component;
import javax.swing.JButton;
import javax.swing.JPanel;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.imagine.vector.editor.ui.spi.ShapesCollection;
import static org.netbeans.paint.api.components.number.StandardNumericConstraints.DOUBLE;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.netbeans.paint.api.components.number.NamedNumberEditor;
import org.netbeans.paint.api.components.number.NumericConstraint;
import org.openide.awt.Mnemonics;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "ax=X 1",
    "bx=X 2",
    "cx=X 3",
    "ay=Y 1",
    "by=Y 2",
    "cy=Y 3",
    "tesselate=&Tesselate"
})
@ServiceProvider(service = InspectorFactory.class)
public class TriangleInspector extends InspectorFactory<TriangleWrapper> {

    public TriangleInspector() {
        super(TriangleWrapper.class);
    }

    @Override
    public Component get(TriangleWrapper obj, Lookup lookup, int item, int of) {
        ShapesCollection coll = lookup.lookup(ShapesCollection.class);
        ShapeElement shape = lookup.lookup(ShapeElement.class);
        JPanel pnl = new JPanel(new VerticalFlowLayout());
        NumericConstraint con = DOUBLE.withMaximum(10000D).withMinimum(-2000D);
        pnl.add(new NamedNumberEditor(Bundle.ax(), con, obj::ax, shape.wrap(obj::setAx)));
        pnl.add(new NamedNumberEditor(Bundle.ay(), con, obj::ay, shape.wrap(obj::setAy)));
        pnl.add(new NamedNumberEditor(Bundle.bx(), con, obj::bx, shape.wrap(obj::setBx)));
        pnl.add(new NamedNumberEditor(Bundle.by(), con, obj::by, shape.wrap(obj::setBy)));
        pnl.add(new NamedNumberEditor(Bundle.cx(), con, obj::cx, shape.wrap(obj::setCx)));
        pnl.add(new NamedNumberEditor(Bundle.cy(), con, obj::cy, shape.wrap(obj::setCy)));
        JButton tesselate = new JButton();
        Mnemonics.setLocalizedText(tesselate, Bundle.tesselate());
        tesselate.addActionListener(ae -> {
            TriangleWrapper[] nue = obj.tesselate();
            for (int i = 0; i < nue.length; i++) {
                coll.add(nue[i], shape.fill(), shape.outline(), shape.stroke(), shape.isDraw(), shape.isFill());
            }
            shape.changed();
        });
        pnl.add(tesselate);

        return pnl;
    }
}
