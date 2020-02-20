/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.vector;

/**
 *
 * @author Tim Boudreau
 */
public interface Copyable extends Primitive {

    @Override
    Copyable copy();
}
