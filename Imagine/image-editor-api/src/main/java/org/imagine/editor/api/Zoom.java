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
        double factor = 1D / getZoom();
        return AffineTransform.getScaleInstance(factor, factor);
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

    default boolean isOneToOne() {
        return getZoom() == 1F;
    }

    public static Zoom ONE_TO_ONE = new Zoom() {
        private final AffineTransform xform = new AffineTransform();

        @Override
        public float getZoom() {
            return 1;
        }

        @Override
        public AffineTransform getZoomTransform() {
            xform.setToIdentity();
            return xform;
        }

        @Override
        public AffineTransform getInverseTransform() {
            xform.setToIdentity();
            return xform;
        }

        @Override
        public void setZoom(float val) {
            // do nothing
        }

        @Override
        public void addChangeListener(ChangeListener cl) {
            // do nothing
        }

        @Override
        public void removeChangeListener(ChangeListener cl) {
            // do nothing
        }
    };
}
