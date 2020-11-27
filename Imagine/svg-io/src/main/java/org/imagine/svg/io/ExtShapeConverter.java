package org.imagine.svg.io;

import java.awt.Shape;
import java.awt.geom.Point2D;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGShape;
import com.mastfrog.geometry.Circle;
import com.mastfrog.geometry.Polygon2D;
import com.mastfrog.geometry.Rhombus;
import com.mastfrog.geometry.Triangle2D;
import org.w3c.dom.Element;

/**
 *
 * @author Tim Boudreau
 */
class ExtShapeConverter extends SVGShape {

    public ExtShapeConverter(SVGGeneratorContext generatorContext) {
        super(generatorContext);
    }

    @Override
    public Element toSVG(Shape shape) {
        if (shape instanceof Circle) {
            return handleCircle((Circle) shape);
        } else if (shape instanceof Polygon2D) {
            return handlePolygon((Polygon2D) shape);
        } else if (shape instanceof Triangle2D) {
            return handleTriangle((Triangle2D) shape);
        } else if (shape instanceof Rhombus) {
            return handleRhombus((Rhombus) shape);
        }
        return super.toSVG(shape);
    }

    private Element handleCircle(Circle circle) {
        Element cir
                = generatorContext.getDOMFactory().createElementNS(SVG_NAMESPACE_URI,
                        SVG_CIRCLE_TAG);

        cir.setAttributeNS(null, SVG_CX_ATTRIBUTE, doubleString(circle.centerX()));
        cir.setAttributeNS(null, SVG_CY_ATTRIBUTE, doubleString(circle.centerY()));
        // SVG radius attribute is "radius" which is wrong
        cir.setAttributeNS(null, "r", doubleString(circle.radius()));

        return cir;
    }

    private Element handlePolygon(Polygon2D polygon) {
        Element svgPolygon
                = generatorContext.getDOMFactory().createElementNS(SVG_NAMESPACE_URI,
                        SVG_POLYGON_TAG);
        StringBuilder points = new StringBuilder(polygon.pointCount() * 8);

        double[] pts = polygon.pointsArray();
        for (int i = 0; i < pts.length; i += 2) {
            appendPoint(points, pts[i], pts[i + 1]);
        }
        svgPolygon.setAttributeNS(null,
                SVG_POINTS_ATTRIBUTE,
                points.toString());

        return svgPolygon;
    }

    private Element handleTriangle(Triangle2D tri) {
        Element svgPolygon
                = generatorContext.getDOMFactory().createElementNS(SVG_NAMESPACE_URI,
                        SVG_POLYGON_TAG);
        StringBuilder points = new StringBuilder(28);
        appendPoint(points, tri.ax(), tri.ay());
        appendPoint(points, tri.bx(), tri.by());
        appendPoint(points, tri.cx(), tri.cy());
        svgPolygon.setAttributeNS(null,
                SVG_POINTS_ATTRIBUTE,
                points.toString());

        return svgPolygon;
    }

    private Element handleRhombus(Rhombus rhom) {
        Element svgPolygon
                = generatorContext.getDOMFactory().createElementNS(SVG_NAMESPACE_URI,
                        SVG_POLYGON_TAG);
        StringBuilder points = new StringBuilder(28);
        for (int i = 0; i < rhom.pointCount(); i++) {
            Point2D pt = rhom.point(i);
            appendPoint(points, pt.getX(), pt.getY());
        }
        svgPolygon.setAttributeNS(null,
                SVG_POINTS_ATTRIBUTE,
                points.toString());

        return svgPolygon;
    }

    private void appendPoint(StringBuilder points, double x, double y) {
        points.append(doubleString(x));
        points.append(SPACE);
        points.append(doubleString(y));
        points.append(SPACE);
    }
}
