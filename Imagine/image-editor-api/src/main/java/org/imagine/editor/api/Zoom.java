/*
 *                 Sun Public License Notice
 *
 * The contents of this file are subject to the Sun Public License
 * Version 1.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://www.sun.com/
 *
 * The Original Code is NetBeans. The Initial Developer of the Original
 * Code is Sun Microsystems, Inc. Portions Copyright 1997-2005 Sun
 * Microsystems, Inc. All Rights Reserved.
 */
package org.imagine.editor.api;

import java.awt.BasicStroke;
import java.awt.geom.AffineTransform;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Timothy Boudreau
 */
public interface Zoom {

    float getZoom();

    void setZoom(float val);

    void addChangeListener(ChangeListener cl);

    void removeChangeListener(ChangeListener cl);

    default double scale(double val) {
        return val * getZoom();
    }

    default float scale(float val) {
        return val * getZoom();
    }

    default double inverseScale(double val) {
        return val * (1D / getZoom());
    }

    default float inverseScale(float val) {
        return val * (1F / getZoom());
    }

    default AffineTransform getZoomTransform() {
        float zoom = getZoom();
        return AffineTransform.getScaleInstance(zoom, zoom);
    }

    default AffineTransform getInverseTransform() {
        float zoom = getZoom();
        return AffineTransform.getScaleInstance(1D / zoom, 1D / zoom);
    }

    default BasicStroke getStroke(double val) {
        return new BasicStroke((float) inverseScale(val));
    }

    default BasicStroke getStroke() {
        float zoom = getZoom();
        if (zoom == 1) {
            return new BasicStroke(1);
        }
        return getStroke(1);
    }
}
