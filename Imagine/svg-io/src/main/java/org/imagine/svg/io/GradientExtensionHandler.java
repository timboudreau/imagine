/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.svg.io;

import static org.apache.batik.util.SVGConstants.*;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.apache.batik.svggen.DefaultExtensionHandler;
import org.apache.batik.svggen.SVGColor;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGPaintDescriptor;
import org.w3c.dom.Element;

/**
 * Extension of Batik's {@link DefaultExtensionHandler} which handles different
 * kinds of Paint objects
 *
 * I wonder why this is not part of the svggen library.
 *
 * @author Martin Steiger
 */
public class GradientExtensionHandler extends DefaultExtensionHandler {

    @Override
    public SVGPaintDescriptor handlePaint(Paint paint, SVGGeneratorContext genCtx) {
        // Handle LinearGradientPaint
        if (paint instanceof LinearGradientPaint) {
            return getLgpDescriptor((LinearGradientPaint) paint, genCtx);
        }

        // Handle RadialGradientPaint
        if (paint instanceof RadialGradientPaint) {
            return getRgpDescriptor((RadialGradientPaint) paint, genCtx);
        }

        return super.handlePaint(paint, genCtx);
    }

    private SVGPaintDescriptor getRgpDescriptor(RadialGradientPaint gradient, SVGGeneratorContext genCtx) {
        Element gradElem = genCtx.getDOMFactory().createElementNS(SVG_NAMESPACE_URI, SVG_RADIAL_GRADIENT_TAG);

        // Create and set unique XML id
        String id = genCtx.getIDGenerator().generateID("gradient");
        gradElem.setAttribute(SVG_ID_ATTRIBUTE, id);

        // Set x,y pairs
        Point2D centerPt = gradient.getCenterPoint();
        gradElem.setAttribute("cx", String.valueOf(centerPt.getX()));
        gradElem.setAttribute("cy", String.valueOf(centerPt.getY()));

        Point2D focusPt = gradient.getFocusPoint();
        gradElem.setAttribute("fx", String.valueOf(focusPt.getX()));
        gradElem.setAttribute("fy", String.valueOf(focusPt.getY()));

        gradElem.setAttribute("r", String.valueOf(gradient.getRadius()));

        addMgpAttributes(gradElem, genCtx, gradient);

        return new SVGPaintDescriptor("url(#" + id + ")", SVG_OPAQUE_VALUE, gradElem);
    }

    private SVGPaintDescriptor getLgpDescriptor(LinearGradientPaint gradient, SVGGeneratorContext genCtx) {
        Element gradElem = genCtx.getDOMFactory().createElementNS(SVG_NAMESPACE_URI, SVG_LINEAR_GRADIENT_TAG);

        // Create and set unique XML id
        String id = genCtx.getIDGenerator().generateID("gradient");
        gradElem.setAttribute(SVG_ID_ATTRIBUTE, id);

        // Set x,y pairs
        Point2D startPt = gradient.getStartPoint();
        gradElem.setAttribute("x1", String.valueOf(startPt.getX()));
        gradElem.setAttribute("y1", String.valueOf(startPt.getY()));

        Point2D endPt = gradient.getEndPoint();
        gradElem.setAttribute("x2", String.valueOf(endPt.getX()));
        gradElem.setAttribute("y2", String.valueOf(endPt.getY()));

        addMgpAttributes(gradElem, genCtx, gradient);

        SVGPaintDescriptor result = new SVGPaintDescriptor("url(#" + id + ")", SVG_OPAQUE_VALUE, gradElem);
        return result;
    }

    private void addMgpAttributes(Element gradElem, SVGGeneratorContext genCtx, MultipleGradientPaint gradient) {
        gradElem.setAttribute(SVG_GRADIENT_UNITS_ATTRIBUTE, SVG_USER_SPACE_ON_USE_VALUE);

        // Set cycle method
        switch (gradient.getCycleMethod()) {
            case REFLECT:
                gradElem.setAttribute(SVG_SPREAD_METHOD_ATTRIBUTE, SVG_REFLECT_VALUE);
                break;
            case REPEAT:
                gradElem.setAttribute(SVG_SPREAD_METHOD_ATTRIBUTE, SVG_REPEAT_VALUE);
                break;
            case NO_CYCLE:
                gradElem.setAttribute(SVG_SPREAD_METHOD_ATTRIBUTE, SVG_PAD_VALUE);	// this is the default
                break;
        }

        // Set color space
        switch (gradient.getColorSpace()) {
            case LINEAR_RGB:
                gradElem.setAttribute(SVG_COLOR_INTERPOLATION_ATTRIBUTE, SVG_LINEAR_RGB_VALUE);
                break;

            case SRGB:
                gradElem.setAttribute(SVG_COLOR_INTERPOLATION_ATTRIBUTE, SVG_SRGB_VALUE);
                break;
        }

        // Set transform matrix if not identity
        AffineTransform tf = gradient.getTransform();
        if (!tf.isIdentity()) {
            String matrix = "matrix("
                    + tf.getScaleX() + " " + tf.getShearY() + " " + tf.getShearX() + " "
                    + tf.getScaleY() + " " + tf.getTranslateX() + " " + tf.getTranslateY() + ")";
            gradElem.setAttribute(SVG_GRADIENT_TRANSFORM_ATTRIBUTE, matrix);
            gradElem.setAttribute(SVG_TRANSFORM_ATTRIBUTE, matrix);
        }

        // Convert gradient stops
        Color[] colors = gradient.getColors();
        float[] fracs = gradient.getFractions();

        for (int i = 0; i < colors.length; i++) {
            Element stop = genCtx.getDOMFactory().createElementNS(SVG_NAMESPACE_URI, SVG_STOP_TAG);
            SVGPaintDescriptor pd = SVGColor.toSVG(colors[i], genCtx);

            stop.setAttribute(SVG_OFFSET_ATTRIBUTE, (int) (fracs[i] * 100.0f) + "%");
            stop.setAttribute(SVG_STOP_COLOR_ATTRIBUTE, pd.getPaintValue());

            int alpha = colors[i].getAlpha();
            if (alpha != 255) {
                stop.setAttribute(SVG_STOP_OPACITY_ATTRIBUTE, pd.getOpacityValue());
            }

            gradElem.appendChild(stop);
        }
    }
}
