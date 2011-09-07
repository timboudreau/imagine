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
id = "org.netbeans.paint.tools.MoveAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/move.png",
displayName = "#CTL_Move Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Tools" //, position = -300
    )
})
@Messages("CTL_MoveAction=Move")
public final class MoveAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new MoveTool();
    
    public MoveAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
