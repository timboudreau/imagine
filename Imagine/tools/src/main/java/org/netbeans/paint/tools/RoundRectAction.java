/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

/**
 *
 * @author eppleton
 */
import net.dev.java.imagine.spi.tools.Tool;
import net.java.dev.imagine.api.image.Layer;
import org.netbeans.paint.tools.actions.GenericToolAction;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools",
id = "org.netbeans.paint.tools.RoundRectAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/roundrect.png",
displayName = "#CTL_RoundRect Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Shapes" //, position = -300
    )
})
@Messages("CTL_RoundRectAction=RoundRect")
public final class RoundRectAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new RoundRectTool();
    
    public RoundRectAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
