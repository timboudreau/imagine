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
id = "org.netbeans.paint.tools.ClearAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/clear.png",
displayName = "#CTL_Clear Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Tools" //, position = -300
    )
})
@Messages("CTL_ClearAction=Clear")
public final class ClearAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new ClearTool();
    
    public ClearAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
