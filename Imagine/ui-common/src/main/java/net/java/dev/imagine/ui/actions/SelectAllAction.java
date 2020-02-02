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

package net.java.dev.imagine.ui.actions;

import net.java.dev.imagine.ui.actions.spi.Selectable;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Timothy Boudreau
 */
public class SelectAllAction extends GenericContextSensitiveAction <Selectable> {

    public SelectAllAction() {
	super (Utilities.actionsGlobalContext(), Selectable.class);
        putValue(NAME, NbBundle.getMessage (ClearSelectionAction.class,
            "ACT_SelectAll")); //NOI18N
    }
    
    public SelectAllAction(Lookup lkp) {
        super (lkp, Selectable.class);
        putValue(NAME, NbBundle.getMessage (ClearSelectionAction.class,
            "ACT_SelectAll")); //NOI18N
    }

    public void performAction(Selectable ptc) {
	ptc.selectAll();
    }

}
