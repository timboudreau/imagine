package net.dev.java.imagine.api.tool;

import java.awt.Toolkit;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.annotations.common.NonNull;
import org.openide.util.Lookup;
import org.openide.util.Parameters;
import org.openide.util.WeakSet;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 * Encapsulates the concept of a "selected tool".  Called by actions which
 * can be used to set the selected tool.
 * <p/>
 * An application should subclass this and implement onChange() to make the
 * selected tool available wherever it is needed.
 *
 * @author Tim Boudreau
 */
public class SelectedTool {

    private final Set<Observer> observers = Collections.synchronizedSet(new WeakSet<>());
    private final InstanceContent content = new InstanceContent();
    private final Lookup lookup = new AbstractLookup(content);
    private Tool selected;

    /**
     * Add an observer which will be notified if the selected tool changes.
     * Observers are weakly referenced;  if you want your observer to be notified,
     * you will need to hold a reference to it.
     * @param observer An observer
     */
    public final void addObserver(@NonNull Observer observer) {
        Parameters.notNull("observer", observer);
        observers.add(observer);
    }
    
    public final void removeObserver(@NonNull Observer observer) {
        for (Iterator<Observer> it = observers.iterator(); it.hasNext();) {
            if (it.next() == observer) {
                it.remove();
            }
        }
    }
    
    public Lookup getLookup() {
        return lookup;
    }

    /**
     * Determine if the currently selected tool has this name
     * @param toolName
     * @return 
     */
    public final boolean isToolSelected(String toolName) {
        return selected != null && toolName.equals(selected.getName());
    }

    /**
     * Set the selected tool
     * @param selectedTool The selected tool
     */
    public final void setSelectedTool(Tool selectedTool) {
        //XXX use an accessor and hide this method?
//        assert EventQueue.isDispatchThread() : "Not on EQ thread";
        if (selected != null && selected.equals(selectedTool) || (selected == null && selectedTool == null)) {
            return;
        }
        Tool old = selected;
        if (onChange(old, selectedTool)) {
            if (old != null) {
                content.remove(old);
            }
            selected = selectedTool;
            if (selectedTool != null) {
                content.add(selectedTool);
            }
            for (Observer o : observers) {
                if (o != null) {
                    try {
                        o.selectedToolChanged(old, selected);
                    } catch (Exception e) {
                        Logger.getLogger(SelectedTool.class.getName()).log(Level.SEVERE, "Exception notifying " + o, e);
                    }
                }
            }
        } else {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Called when the selected tool changes.  Subclasses may veto a tool
     * change by overriding this method and returning false.
     * 
     * @param old The old tool or null
     * @param nue The new tool or null
     * @return True if the selected tool should indeed be changed
     */
    protected boolean onChange(Tool old, Tool nue) {
        assert old != nue;
        return true;
    }
    private static volatile SelectedTool instance;

    public static SelectedTool getDefault() {
        if (instance == null) {
            synchronized (SelectedTool.class) {
                if (instance == null) {
                    instance = Lookup.getDefault().lookup(SelectedTool.class);
                }
                if (instance == null) {
                    instance = new SelectedTool();
            Logger.getLogger(SelectedTool.class.getName()).log(Level.WARNING,
                    "No instance of SelectedTool in the default lookup;  using "
                    + "default implementation (which probably won't make the "
                    + "selected tool available everywhere you want it)");
                }
            }
        }
        return instance;
    }

    /**
     * Observes changes in the selected tool
     */
    public interface Observer {
        /**
         * Called when the selected tool changes
         * @param old The old tool or null
         * @param nue The new tool or null, but (old == null) != (nue == null)
         */
        public void selectedToolChanged(Tool old, Tool nue);
    }
}
