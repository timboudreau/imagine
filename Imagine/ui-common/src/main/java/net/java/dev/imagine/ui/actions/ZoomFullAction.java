/*
 * ZoomFullAction.java
 *
 * Created on November 20, 2006, 8:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.ui.actions;
import javax.swing.ImageIcon;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.netbeans.paint.api.editor.Zoom;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Tim
 */
public class ZoomFullAction extends GenericContextSensitiveAction <Zoom> {
    
    /** Creates a new instance of ZoomFullAction */
    public ZoomFullAction() {
        super (Utilities.actionsGlobalContext(), Zoom.class);
        setIcon(new ImageIcon(
                ImageUtilities.loadImage("net/java/dev/imagine/ui/actions/zoomFull.png"))); //NOI18N
        putValue (NAME, NbBundle.getMessage(ZoomInAction.class, "ACT_ZoomFull")); //NOI18N
    }
    
    public ZoomFullAction(Lookup lkp) {
        super(lkp);
        setIcon(new ImageIcon(
                ImageUtilities.loadImage("net/java/dev/imagine/ui/actions/zoomFull.png"))); //NOI18N
    }
    
    
    protected void performAction(Zoom t) {
        
    }
    
}
