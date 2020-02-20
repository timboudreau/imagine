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

import java.util.Collection;
import net.dev.java.imagine.api.selection.Selection;
import net.java.dev.imagine.ui.actions.spi.Selectable;
import org.netbeans.paint.api.actions.GenericContextSensitiveAction;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 *
 * @author Timothy Boudreau
 */
public class InvertSelectionAction extends GenericContextSensitiveAction<Selectable> {

    public InvertSelectionAction() {
        super(Utilities.actionsGlobalContext(), Selectable.class);
        putValue(NAME, NbBundle.getMessage(InvertSelectionAction.class, "ACT_InvertSelection")); //NOI18N
    }

    public InvertSelectionAction(Lookup lkp) {
        super(lkp, Selectable.class);
        putValue(NAME, NbBundle.getMessage(InvertSelectionAction.class, "ACT_InvertSelection")); //NOI18N
    }

    @Override
    protected <T> boolean checkEnabled(Collection<? extends T> coll, Class<T> clazz) {
        if (clazz != Selectable.class) {
            return true;
        }
        boolean result = super.checkEnabled(coll, clazz);
        if (result) {
            Collection<? extends Selection> sel = Utilities.actionsGlobalContext().lookupAll(Selection.class);
            result &= !sel.isEmpty();
            if (result) {
                for (T s : coll) {
                    if (!((Selectable)s).canInvertSelection()) {
                        result = false;
                    }
                }
            }
        }
        result &= Utilities.actionsGlobalContext().lookup(Selectable.class) != null;
        return result;
    }

    @Override
    protected void performAction(Selectable t) {
        t.invertSelection();
    }
}
