/*
 * ControlPointFactory.java
 *
 * Created on October 30, 2006, 10:55 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.design;

import net.java.dev.imagine.api.vector.Adjustable;

/**
 *
 * @author Tim Boudreau
 */
public class ControlPointFactory {

    public ControlPoint[] getControlPoints(Adjustable p, ControlPointController c) {
        int count = p.getControlPointCount();
        int[] idxs = p.getVirtualControlPointIndices();
        ControlPointKind[] kinds = p.getControlPointKinds();
        ControlPoint[] result = new ControlPoint[count];
        double[] pts = new double[count * 2];
        p.getControlPoints(pts);
        int idxix = 0;
        int max = count * 2;
        for (int i = 0; i < max; i += 2) {
            boolean virtual;
            if (idxix < idxs.length) {
                virtual = idxs[idxix] == i / 2;
            } else {
                virtual = false;
            }
            if (virtual) {
                idxix++;
            }
            result[i / 2] = new ControlPointImpl(p, c, i / 2,
                    virtual, kinds[i / 2], () -> result);
        }
        return result;
    }
}
