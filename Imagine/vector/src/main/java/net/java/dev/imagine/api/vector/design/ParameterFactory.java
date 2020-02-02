/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package net.java.dev.imagine.api.vector.design;

import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.elements.Arc;
import net.java.dev.imagine.api.vector.elements.RoundRect;

/**
 *
 * @author Tim Boudreau
 */
public class ParameterFactory {


    public static Parameter[] parameters(Primitive prim) {
        if (prim instanceof Arc) {
            Arc arc = (Arc) prim;
            return new Parameter[] {
                Parameter.from("Start Angle", arc::getStartAngle, arc::setStartAngle),
                Parameter.from("Arc Angle", arc::getArcAngle, arc::setArcAngle)
            };
        } else if (prim instanceof RoundRect) {
            RoundRect rr = (RoundRect) prim;
            return new Parameter[] {
                Parameter.from("Start Angle", rr::getArcWidth, rr::setArcWidth),
                Parameter.from("Arc Angle", rr::getArcHeight, rr::setArcHeight)
            };
        }
        return new Parameter[0];
    }
}
