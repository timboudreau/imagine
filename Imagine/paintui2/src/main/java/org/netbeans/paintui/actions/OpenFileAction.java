/*
 *                 Sun Public License Notice
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
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import org.netbeans.paint.api.components.FileChooserUtils;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.netbeans.paintui.PaintTopComponent;
import org.openide.ErrorManager;
import org.openide.filesystems.FileChooserBuilder;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Timothy Boudreau
 */
public class OpenFileAction extends AbstractAction {
    
    private static final String ICON_BASE = 
	    "org/netbeans/paintui/resources/openFile24.png"; //NOI18N
    
    public OpenFileAction() {
	putValue (Action.NAME, NbBundle.getMessage (OpenFileAction.class,
		"ACT_Open")); //NOI18N
	Icon ic = new ImageIcon (ImageUtilities.loadImage(ICON_BASE));
	putValue (Action.SMALL_ICON, ic);
    }

    public void actionPerformed(ActionEvent e) {
        File[] f = new FileChooserBuilder(OpenFileAction.class).addFileFilter(new FF())
                .setFilesOnly(true).setTitle(NbBundle.getMessage(OpenFileAction.class,
		"TTL_OpenDlg")).setFileHiding(false).showMultiOpenDialog();
        if (f != null && f.length > 0) {
	    TopComponent last = null;
	    for (int i = 0; i < f.length; i++) {
                 try {
                     BufferedImage bi = ImageIO.read(f[i]);

                     // Loaded PNGs are slow to render;  unpack the data.
                     if (bi.getType() != GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE) {
                         BufferedImage nue = new BufferedImage(bi.getWidth(),
                                                               bi.getHeight(),
                                                               GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
                         Graphics2D g2d = (Graphics2D) nue.createGraphics();

                         g2d.drawRenderedImage(bi,
                                               AffineTransform.getTranslateInstance(0,
                                                                                    0));
                         g2d.dispose();
                         bi = nue;
                     }
                     last = new PaintTopComponent(bi, f[i]);
                     last.setDisplayName(f[i].getName());
                     last.open();
                 }
                 catch (IOException ioe) {
                     ErrorManager.getDefault().notify(ErrorManager.USER, ioe);
                 }
             }
	    if (last != null) {
		last.requestActive();
	    }
	}
    }
    
    private static class FF extends FileFilter {
	final Set <String> fmts;
	FF() {
	    fmts = new HashSet<String>();
            for (String fmt : ImageIO.getReaderFormatNames()) {
                fmts.add(fmt.toLowerCase());
            }
	}
	
	public boolean accept(File f) {
	    int ix = f.getName().lastIndexOf('.'); //NOI18N
	    if (ix != -1 && ix != f.getName().length() - 1) {
		String s = f.getName().substring(ix+1);
		return (fmts.contains (s.toLowerCase()) ||
			fmts.contains(s.toUpperCase())) && f.isFile();
	    }
	    return f.isDirectory();
	}

	public String getDescription() {
	    return NbBundle.getMessage (FF.class, "LBL_ImageFileFormats"); //NOI18N
	}
    }
    
}
