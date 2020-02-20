/*
 * CharacterIteratorWrapper.java
 *
 * Created on September 27, 2006, 7:19 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import java.awt.Graphics2D;
import java.io.Serializable;
import java.text.AttributedCharacterIterator;
import net.java.dev.imagine.api.vector.Primitive;

/**
 *
 * @author Tim Boudreau
 */
public final class CharacterIteratorWrapper implements Primitive {

    private static final long serialVersionUID = 203_942L;
    public final AttributedCharacterIterator it;
    public final double x;
    public final double y;

    public CharacterIteratorWrapper(AttributedCharacterIterator it, double x, double y) {
        assert it instanceof Serializable;
        this.it = it;
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return "CharacterIteratorWrapper: " + x
                + ", " + y + ": " + it;
    }

    @Override
    public void paint(Graphics2D g) {
        g.drawString(it, (int) x, (int) y);
    }

    @Override
    public Primitive copy() {
        return new CharacterIteratorWrapper(it, x, y);
    }
}
