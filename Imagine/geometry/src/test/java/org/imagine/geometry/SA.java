/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.geometry;

/**
 *
 * @author Tim Boudreau
 */
public class SA<S extends Sector> extends AssertionFacade<S> {

    public SA(S ca) {
        super(ca);
    }

    public SA assertExtent(double val) {
        return assertExtent(null, val);
    }

    public SA assertExtent(String msg, double val) {
        assertSaneDegrees(ca.extent(), msg);
        assertDouble(val, ca.extent(), msg("Wrong extent", msg));
        return this;
    }

    public SA assertStartingAngle(double val) {
        return assertStartingAngle(null, val);
    }

    public SA assertStartingAngle(String msg, double val) {
        assertSaneDegrees(ca.start(), msg);
        assertDouble(val, ca.start(), msg("Wrong starting angle in '" + ca + "' " + ca.start() + " expected " + val, msg));
        return this;
    }

    public SA assertMinAngle(double val) {
        return assertMinAngle(null, val);
    }

    public SA assertMinAngle(String msg, double val) {
        assertSaneDegrees(ca.minDegrees(), msg);
        assertDouble(val, ca.minDegrees(), msg("Wrong min angle in '" + ca + "'", msg));
        return this;
    }

    public SA assertMaxAngle(double val) {
        return assertMaxAngle(null, val);
    }

    public SA assertMaxAngle(String msg, double val) {
        assertSaneDegrees(ca.maxDegrees(), msg);
        assertDouble(val, ca.maxDegrees(), msg("Wrong max angle in '" + ca + "'", msg));
        return this;
    }

}
