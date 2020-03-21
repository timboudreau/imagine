/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.elements;

import net.java.dev.imagine.api.vector.Versioned;

/**
 *
 * @author Tim Boudreau
 */
abstract class AbstractVersioned implements Versioned {

    private transient int rev;

    protected AbstractVersioned() {

    }

    protected AbstractVersioned(AbstractVersioned prev) {
        this.rev = prev.rev;
    }

    public int rev() {
        return rev;
    }

    void change() {
        rev++;
    }

    void setRev(int rev) {
        this.rev = rev;
    }

    Runnable versionSnapshot() {
        int oldRev = rev;
        return () -> {
            rev = oldRev;
        };
    }
}
