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
id = "org.netbeans.paint.tools.TextAction")
@ActionRegistration(  iconBase = "org/netbeans/paint/tools/resources/text.png",
displayName = "#CTL_Text Tool")
@ActionReferences({
    @ActionReference(path = "Toolbars/Tools" //, position = -300
    )
})
@Messages("CTL_TextAction=Text")
public final class TextAction extends GenericToolAction {
    
    final static Tool TOOL_INSTANCE = new TextTool();
    
    public TextAction(Layer context) {
        super(context, TOOL_INSTANCE);
    }
}
