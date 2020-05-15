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
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import javax.swing.event.ChangeListener;
import org.imagine.geometry.util.PooledTransform;

/**
 *
 * @author Timothy Boudreau
 */
public interface Zoom {

    double getZoom();

    default float getZoomAsFloat() {
        return (float) getZoom();
    }

    void setZoom(double val);

    void addChangeListener(ChangeListener cl);

    void removeChangeListener(ChangeListener cl);

    default String stringValue() {
        return ZoomPrivate.zoomToString(getZoom());
    }

    static String stringValue(double zoom) {
        return ZoomPrivate.zoomToString(zoom);
    }

    default void zoomIn() {
        setZoom((float) ZoomPrivate.nextZoom(getZoom()));
    }

    default void zoomOut() {
        setZoom((float) ZoomPrivate.prevZoom(getZoom()));
    }

    default void zoomOneToOne() {
        setZoom(1);
    }

    default boolean canZoomIn() {
        return ZoomPrivate.hasNextZoom(getZoom());
    }

    default boolean canZoomOut() {
        return ZoomPrivate.hasPreviousZoom(getZoom());
    }

    default double nearestFixedZoomTo(double val) {
        return ZoomPrivate.nearestZoom(val);
    }

    static void visitDefaultFixedZooms(DoubleConsumer dc) {
        ZoomPrivate.fixedZoomLevels(dc);
    }

    default double scale(double val) {
        return val * getZoom();
    }

    default float scale(float val) {
        return (float) (val * getZoom());
    }

    default double inverseScale(double val) {
        return val * (1D / getZoom());
    }

    default float inverseScale(float val) {
        return (float) (val * (1D / getZoom()));
    }

    default AffineTransform getZoomTransform() {
        double zoom = getZoom();
        return AffineTransform.getScaleInstance(zoom, zoom);
    }

    default AffineTransform getInverseTransform() {
        double factor = 1D / getZoom();
        return AffineTransform.getScaleInstance(factor, factor);
    }

    default BasicStroke getStroke(double val) {
        return new BasicStroke((float) inverseScale(val));
    }

    default void withZoomTransform(Consumer<AffineTransform> c) {
        double zoom = getZoom();
        PooledTransform.withScaleInstance(zoom, zoom, c);
    }

    default void withInverseTransform(Consumer<AffineTransform> c) {
        double z = 1D / getZoom();
        PooledTransform.withScaleInstance(z, z, c);
    }

    default BasicStroke getStroke() {
        double zoom = getZoom();
        if (zoom == 1) {
            return new BasicStroke(1);
        }
        return getStroke(1);
    }

    default boolean isOneToOne() {
        return getZoom() == 1D;
    }

    default BasicStroke inverseScaleStroke(BasicStroke stroke) {
        return ZoomPrivate.inverseScaledStroke(this, stroke);
    }

    public static Zoom ONE_TO_ONE = new Zoom() {
        private final AffineTransform xform = new AffineTransform();

        @Override
        public double getZoom() {
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
        public void setZoom(double val) {
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
