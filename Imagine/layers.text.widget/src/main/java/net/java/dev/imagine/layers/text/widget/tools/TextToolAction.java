package net.java.dev.imagine.layers.text.widget.tools;

import net.dev.java.imagine.spi.tools.Tool;
import net.java.dev.imagine.api.image.Layer;
import org.netbeans.paint.tools.actions.GenericToolAction;
import org.openide.awt.ActionID;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionRegistration;
import org.openide.util.NbBundle.Messages;

/**
 * Temporary action
 * @author Tim Boudreau
 */
@ActionID(category = "Tools",
id = "net.java.dev.imagine.layers.text.widget.tools.TextToolAction")
@ActionRegistration(iconBase = "net/java/dev/imagine/layers/text/widget/tools/text.png",
displayName = "#CTL_TextAction")
@ActionReferences({
    @ActionReference(path = "Toolbars/Brushes", position = 310)
})
@Messages("CTL_BrushAction=Brush")
public final class TextToolAction extends GenericToolAction {

    final static Tool TOOL_INSTANCE = new TextTool();

    public TextToolAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}