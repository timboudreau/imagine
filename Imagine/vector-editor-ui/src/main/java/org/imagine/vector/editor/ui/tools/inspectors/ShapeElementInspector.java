package org.imagine.vector.editor.ui.tools.inspectors;

import java.awt.Component;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.java.dev.imagine.api.vector.design.ShapeNames;
import com.mastfrog.geometry.util.PooledTransform;
import org.imagine.inspectors.spi.InspectorFactory;
import org.imagine.vector.editor.ui.spi.ShapeElement;
import org.netbeans.paint.api.components.VerticalFlowLayout;
import org.openide.util.Lookup;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "triangle=Triangle",
    "oval=Oval",
    "rectangle=Rectangle",
    "path=Path",
    "roundRect=Round Rectangle",
    "shape=Shape",
    "string=Text"
})
@ServiceProvider(service = InspectorFactory.class, position = 0)
public class ShapeElementInspector extends InspectorFactory<ShapeElement> {

    public ShapeElementInspector() {
        super(ShapeElement.class);
    }

    @Override
    public Component get(ShapeElement obj, Lookup lookup, int item, int of) {
        JLabel lbl = new JLabel();
        lbl.setText(ShapeNames.nameOf(obj.item()));
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD));
        String info = ShapeNames.infoString(obj.item());
        if (info.isEmpty()) {
            return lbl;
        }
        JPanel result = new JPanel(new VerticalFlowLayout());
        result.add(lbl);
        JLabel details = new JLabel(info);
        result.add(details);
        PooledTransform.withScaleInstance(0.9, 0.9, xf -> {
            details.setFont(details.getFont().deriveFont(xf));
        });
//        details.setFont(details.getFont().deriveFont(AffineTransform.getScaleInstance(0.9, 0.9)));
        return result;
    }
}
