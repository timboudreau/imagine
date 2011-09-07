/*
 *
 * Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.netbeans.paintui.actions;
import java.awt.Frame;
import javax.swing.JOptionPane;
import org.netbeans.paintui.PaintTopComponent;
import org.openide.util.HelpCtx;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
public final class NewCanvasAction extends org.openide.util.actions.CallableSystemAction {

    public NewCanvasAction() {
        setIcon(new javax.swing.ImageIcon(ImageUtilities.loadImage("org/netbeans/paintui/resources/newFile.png")));
    }

    public void performAction() {
        final ImageSizePanel pnl = new ImageSizePanel();
        String ttl = NbBundle.getMessage(ResizeAction.class, "TTL_NewImage");
        //This code really should use DialogDisplayer, but is not due
        //to a bug in the window system
        int result = JOptionPane.showOptionDialog(Frame.getFrames()[0], pnl, 
                ttl, JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, 
                null, null, null);
        if (result == JOptionPane.OK_OPTION) {
            PaintTopComponent tc = new PaintTopComponent(
                    pnl.getDimension(), pnl.isTransparent());
            tc.open();
            tc.requestActive();
        }
    }

    public String getName() {
        return NbBundle.getMessage(NewCanvasAction.class, "ACT_NewImage");
    }

    public String iconResource() {
        return null;
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    protected boolean asynchronous() {
        return false;
    }
}
