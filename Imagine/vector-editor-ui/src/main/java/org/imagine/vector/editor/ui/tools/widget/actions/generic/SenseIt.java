/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.function.BooleanSupplier;

/**
 * Proxy that tells you if a predicate on a Sense has matched.
 *
 * @author Tim Boudreau
 */
interface SenseIt extends BooleanSupplier {

    void listen(Runnable r);

    void unlisten(Runnable r);

}
