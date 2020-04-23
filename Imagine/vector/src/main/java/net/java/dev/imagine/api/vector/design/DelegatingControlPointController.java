/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.vector.design;

import java.util.HashSet;
import java.util.Set;
import net.java.dev.imagine.api.vector.util.Size;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public class DelegatingControlPointController implements ControlPointController {

    private Set<ControlPointController> delegates = new WeakSet<>();
    private static final Size DEFAULT_SIZE = new Size(7, 7);

    public void listen(ControlPointController ctrllr) {
        delegates.add(ctrllr);
    }

    protected ControlPoint transform(ControlPoint cp) {
        return cp;
    }

    @Override
    public void changed(ControlPoint pt) {
        if (!delegates.isEmpty()) {
            ControlPoint xf = transform(pt);
            for (ControlPointController c : new HashSet<>(delegates)) {
                c.changed(xf);
            }
        }
    }

    @Override
    public Size getControlPointSize() {
        Size sz = null;
        for (ControlPointController cp : delegates) {
            sz = cp.getControlPointSize();
            if (!sz.isEmpty()) {
                break;
            }
        }
        return sz == null ? DEFAULT_SIZE : sz;
    }

    @Override
    public void deleted(ControlPoint pt) {
        if (!delegates.isEmpty()) {
            ControlPoint xf = transform(pt);
            for (ControlPointController cp : delegates) {
                cp.deleted(xf);
            }
        }
    }
}
