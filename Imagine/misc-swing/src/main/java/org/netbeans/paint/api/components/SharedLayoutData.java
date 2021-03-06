package org.netbeans.paint.api.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

/**
 * An interface implemented by an ancestor component that allows nested panels
 * to all align their columns. Usage: Create a panel, set its layout to
 * LDPLayout, and add some components to be displayed in a horizontal row. Add
 * them to a component that implements, or has an ancestor that implements,
 * SharedLayoutData. The LDPLayout instance will access the SharedLayoutData
 * ancestor to find the column positions across all children that implement
 * LayoutDataProvider, even if they are not all children of the same component.
 * <p/>
 * To easily create a component that implements SharedLayoutData, just subclass
 * a JPanel or similar, implement SharedLayoutData and delegate to an instance
 * of DefaultSharedLayoutData, which handles memory management appropriately.
 *
 * @author Tim Boudreua
 */
public interface SharedLayoutData {

    /**
     * The x position for this column, as agreed up on as the max position
     * across all registered components, so columns are aligned.
     *
     * @param column The index of the column.
     * @return The X position
     */
    public int xPosForColumn(int column, Container requester);

    /**
     * Register a component that should participate in layout data
     *
     * @param p
     */
    public void register(LayoutDataProvider p);

    /**
     * Unregister a component.
     *
     * @param p
     */
    public void unregister(LayoutDataProvider p);

    /**
     * Called when the expanded method is called
     *
     * @param p
     * @param state
     */
    public void expanded(LayoutDataProvider p, boolean state);

    public int indentFor(Container requester);

    static SharedLayoutData find(Component c) {
        if (c instanceof SharedLayoutPanel) {
            SharedLayoutData result = ((SharedLayoutPanel) c).dataUnsafe();
            if (result != null) {
                return result;
            }
        } else if (c instanceof TitledPanel2) {
            SharedLayoutData result = ((TitledPanel2) c).dataUnsafe();
            if (result != null) {
                return result;
            }
        }
        SharedLayoutData data = (SharedLayoutData) SwingUtilities.getAncestorOfClass(SharedLayoutData.class, c);
        if (data != null) {
            Component comp = (Component) data;
            while (comp != null && !(comp instanceof Window) && !(comp instanceof JRootPane)) {
                SharedLayoutData nue = (SharedLayoutData) SwingUtilities.getAncestorOfClass(SharedLayoutData.class, comp);
                if (nue != null) {
                    data = nue;
                    comp = ((Component) nue).getParent();
                } else {
                    comp = comp.getParent();
                }
            }
        }
        return data;
    }
}
