package org.netbeans.paint.api.util;

/**
 * Interface which can be implemented on an object which has mutable state,
 * to define begin and end points of undoable operations and allow that object
 * to create a snapshot of before / after state and store it somehow as 
 * undoable operations.
 *
 * @author Tim Boudreau
 */
public interface UndoSupport {
    public void beginUndoableOperation(Object owner, String name);
    public void endUndoableOperation(Object owner);
    public void cancelUndoableOperation(Object owner);
}
