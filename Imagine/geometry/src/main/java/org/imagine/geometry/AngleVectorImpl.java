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
final class AngleVectorImpl implements AngleVector {

    private final CornerAngle corner;
    private final double lengthA;
    private final double lengthB;

    AngleVectorImpl(CornerAngle corner, double lengthA, double lengthB) {
        this.corner = corner;
        this.lengthA = lengthA;
        this.lengthB = lengthB;
    }

    @Override
    public double trailingLineLength() {
        return lengthA;
    }

    @Override
    public double leadingLineLength() {
        return lengthB;
    }

    @Override
    public CornerAngle corner() {
        return corner;
    }
}
