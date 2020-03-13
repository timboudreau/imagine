/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

/**
 *
 * @author Tim Boudreau
 */
class MultiSenseIt implements SenseIt {

    final SenseIt[] senses;

    MultiSenseIt(SenseIt... senses) {
        this.senses = senses;
    }

    @Override
    public void listen(Runnable r) {
        for (SenseIt it : senses) {
            it.listen(r);
        }
    }

    @Override
    public void unlisten(Runnable r) {
        for (SenseIt it : senses) {
            it.unlisten(r);
        }
    }

    @Override
    public boolean getAsBoolean() {
        boolean result = true;
        for (SenseIt it : senses) {
            if (!it.getAsBoolean()) {
                result = false;
                break;
            }
        }
        return result;
    }

}
