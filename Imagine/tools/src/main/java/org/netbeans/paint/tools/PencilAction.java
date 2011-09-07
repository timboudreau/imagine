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
id = "org.netbeans.paint.tools.PencilAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/pencil.png",
displayName = "#CTL_Pencil Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Brushes" //, position = -300
    )
})
@Messages("CTL_PencilAction=Pencil")
public final class PencilAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new PencilTool();
    
    public PencilAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
