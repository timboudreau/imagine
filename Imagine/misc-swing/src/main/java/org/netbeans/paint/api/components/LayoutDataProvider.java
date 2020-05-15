/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.Container;

/**
 * Interface that can be implemented on an ancestor component, to cause all
 * descendant components that use LDPLayout to align by column.
 *
 * @author Tim Boudreau
 */
public interface LayoutDataProvider {

    int getColumnPosition(int col);

    boolean isExpanded();

    void doSetExpanded(boolean val);

    default boolean is(Container container) {
        return container == this;
    }

    default int getIndent() {
        return 0;
    }
}
