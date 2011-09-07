/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;


import net.dev.java.imagine.spi.tools.Tool;
import net.java.dev.imagine.api.image.Layer;
import org.netbeans.paint.tools.actions.GenericToolAction;
import org.openide.awt.ActionRegistration;
import org.openide.awt.ActionReference;
import org.openide.awt.ActionReferences;
import org.openide.awt.ActionID;
import org.openide.util.NbBundle.Messages;

@ActionID(category = "Tools",
id = "org.netbeans.paint.tools.BrushAction")
@ActionRegistration(iconBase = "org/netbeans/paint/tools/resources/brush.png",
displayName = "#CTL_BrushAction")
@ActionReferences({
    @ActionReference(path = "Toolbars/Brushes", position = 300)
})
@Messages("CTL_BrushAction=Brush")
public final class BrushAction extends GenericToolAction {

    final static Tool TOOL_INSTANCE = new BrushTool();

    public BrushAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }

}
