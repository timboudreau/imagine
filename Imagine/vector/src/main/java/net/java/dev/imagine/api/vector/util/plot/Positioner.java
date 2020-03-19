/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.util.plot;

import org.imagine.geometry.EqLine;

/**
 *
 * @author Tim Boudreau
 */
public interface Positioner {

    public double position(double x, double y, EqLine tangent);

}
