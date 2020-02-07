/*
 * Attribute.java
 *
 * Created on November 2, 2006, 4:45 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.vector;

/**
 *
 * @author Tim Boudreau
 */
public interface Attribute <T extends Object> extends Primitive {
    public T get();

    @Override
    public Attribute<T> copy();
}
