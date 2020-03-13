/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.io;

/**
 *
 * @author Tim Boudreau
 */
public interface Writable {

    void writeTo(KeyWriter w);

    String typeId();
}
