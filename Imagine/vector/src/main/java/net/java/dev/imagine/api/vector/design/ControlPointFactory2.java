/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.design;

import java.util.Map;
import java.util.WeakHashMap;
import net.java.dev.imagine.api.vector.Adjustable;
import net.java.dev.imagine.api.vector.Versioned;

/**
 *
 * @author Tim Boudreau
 */
public class ControlPointFactory2 {

    private final Map<Versioned, ControlPointSharedData> cache = new WeakHashMap<>(300);

    private final ControlPointFactory oldFactory = new ControlPointFactory();

    private final DelegatingControlPointController ctrllr = new DelegatingControlPointController();

    public void listen(ControlPointController ctrllr) {
        this.ctrllr.listen(ctrllr);
    }

    @SuppressWarnings("element-type-mismatch")
    public ControlPoint[] getControlPoints(Adjustable p, ControlPointController c) {
        ctrllr.listen(c);
        if (p instanceof Versioned) {
            Versioned v = (Versioned) p;
            ControlPointSharedData<?> data = cache.get(v);
            if (data == null) {
                data = new ControlPointSharedData(p, ctrllr);
                cache.put(v, data);
            }
            return data.getControlPoints();
        }
        return oldFactory.getControlPoints(p, ctrllr);
    }
}
