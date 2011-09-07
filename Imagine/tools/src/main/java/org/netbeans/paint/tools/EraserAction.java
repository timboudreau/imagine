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
id = "org.netbeans.paint.tools.EraserAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/eraser.png",
displayName = "#CTL_Eraser Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Brushes" //, position = -300
    )
})
@Messages("CTL_EraserAction=Eraser")
public final class EraserAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new EraserTool();
    
    public EraserAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
