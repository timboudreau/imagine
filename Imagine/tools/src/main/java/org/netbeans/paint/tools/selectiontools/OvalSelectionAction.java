/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.selectiontools;

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
id = "org.netbeans.paint.tools.selectiontools.OvalSelectionAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/ovalselection.png",
displayName = "#CTL_OvalSelection Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Selection" //, position = -300
    )
})
@Messages("CTL_OvalSelectionAction=OvalSelection")
public final class OvalSelectionAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new OvalSelectionTool();
    
    public OvalSelectionAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
