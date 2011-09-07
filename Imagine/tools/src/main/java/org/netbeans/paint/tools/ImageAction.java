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
id = "org.netbeans.paint.tools.ImageAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/image.png",
displayName = "#CTL_Image Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Shapes" //, position = -300
    )
})
@Messages("CTL_ImageAction=Image")
public final class ImageAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new ImageTool();
    
    public ImageAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}