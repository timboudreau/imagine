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
id = "org.netbeans.paint.tools.DrawAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/draw.png",
displayName = "#CTL_Draw Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Brushes" //, position = -300
    )
})
@Messages("CTL_DrawAction=Draw")
public final class DrawAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new DrawTool();
    
    public DrawAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
