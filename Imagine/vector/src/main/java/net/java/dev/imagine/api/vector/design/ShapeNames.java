package net.java.dev.imagine.api.vector.design;

import com.mastfrog.util.strings.Escaper;
import com.mastfrog.util.strings.Strings;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import javafx.scene.shape.Polyline;
import net.java.dev.imagine.api.vector.Primitive;
import net.java.dev.imagine.api.vector.elements.Arc;
import net.java.dev.imagine.api.vector.elements.CircleWrapper;
import net.java.dev.imagine.api.vector.elements.Clear;
import net.java.dev.imagine.api.vector.elements.ImageWrapper;
import net.java.dev.imagine.api.vector.elements.Line;
import net.java.dev.imagine.api.vector.elements.Oval;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import net.java.dev.imagine.api.vector.elements.PathText;
import net.java.dev.imagine.api.vector.elements.PolygonWrapper;
import net.java.dev.imagine.api.vector.elements.RhombusWrapper;
import net.java.dev.imagine.api.vector.elements.RoundRect;
import net.java.dev.imagine.api.vector.elements.Text;
import net.java.dev.imagine.api.vector.elements.TriangleWrapper;
import org.imagine.geometry.Triangle2D;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
@Messages({
    "circle=Circle",
    "rect=Rectangle",
    "roundRect=Round Rectangle",
    "text=Text",
    "image=Embedded Image",
    "line=Line",
    "polygon=Polygon",
    "polyline=Polyline",
    "oval=Oval",
    "string=String",
    "clear=Clear",
    "arc=Arc",
    "triangle=Triangle",
    "path=Path",
    "pathText=Path Text",
    "rhombus=Rhombus",
    "unknown=Unknown",
    "nil=Null",
    "# {0} - x",
    "# {1} - y",
    "# {2} - width",
    "# {3} - height",
    "# {4} - startAngle",
    "# {5} - arcAngle",
    "arcInfo={4}\u00B0 {0},{1} {2}x{3} {5}\u00B0",
    "# {0} - x",
    "# {1} - y",
    "# {2} - radius",
    "circleInfo={0},{1} r{2}",
    "# {0} - x",
    "# {1} - y",
    "# {2} - width",
    "# {3} - height",
    "rectangularInfo={0},{1} {2}x{3}",
    "# {0} - x1",
    "# {1} - y1",
    "# {2} - x2",
    "# {3} - y2",
    "# {4} - length",
    "lineInfo={0},{1} - {2},{3}: {4}",
    "# {0} - pointCount",
    "pathInfo={0} points",
    "# {0} - text",
    "# {1} - shape",
    "pathTextInfo=\"{0}\" for {1}",
    "# {0} - pointCount",
    "polygonInfo=Polygon with {0} points",
    "# {0} - radX",
    "# {1} - radY",
    "# {2} - rot",
    "# {3} - x",
    "# {4} - y",
    "rhombusInfo={0} x {1} rot {2}\u00B0 @ {3}, {4}"
})
public class ShapeNames {

    private static final DecimalFormat FMT
            = new DecimalFormat("######0.##");

    public static String infoString(Primitive primitive) {
        if (primitive == null) {
            return "";
        } else if (primitive instanceof Arc) {
            Arc arc = (Arc) primitive;
            return Bundle.arcInfo(FMT.format(arc.x),
                    FMT.format(arc.y), FMT.format(arc.width),
                    FMT.format(arc.height), FMT.format(arc.startAngle),
                    FMT.format(arc.arcAngle));
        } else if (primitive instanceof CircleWrapper) {
            CircleWrapper cw = (CircleWrapper) primitive;
            return Bundle.circleInfo(FMT.format(cw.centerX), FMT.format(cw.centerY), FMT.format(cw.radius));
        } else if (primitive instanceof ImageWrapper) {
            ImageWrapper img = (ImageWrapper) primitive;
            Rectangle2D.Double r = new Rectangle2D.Double();
            img.getBounds(r);
            return Bundle.rectangularInfo(FMT.format(r.getX()), FMT.format(r.getY()),
                    FMT.format(img.imageWidth()), FMT.format(img.imageHeight()));
        } else if (primitive instanceof net.java.dev.imagine.api.vector.elements.Rectangle) {
            net.java.dev.imagine.api.vector.elements.Rectangle r = (net.java.dev.imagine.api.vector.elements.Rectangle) primitive;
            return Bundle.rectangularInfo(FMT.format(r.x()), FMT.format(r.y()),
                    FMT.format(r.width()), FMT.format(r.height()));
        } else if (primitive instanceof Text) {
            return Bundle.text();
        } else if (primitive instanceof PathText) {
            PathText pt = (PathText) primitive;
            return Bundle.pathTextInfo(pt.getText(), infoString(pt.shape()));
        } else if (primitive instanceof PolygonWrapper) {
            PolygonWrapper poly = (PolygonWrapper) primitive;
            return Bundle.polygonInfo(poly.getControlPointCount());
        } else if (primitive instanceof RoundRect) {
            RoundRect r = (RoundRect) primitive;
            return Bundle.rectangularInfo(FMT.format(r.x()), FMT.format(r.y()),
                    FMT.format(r.width()), FMT.format(r.height()));
        } else if (primitive instanceof Triangle2D) {
            return Bundle.triangle();
        } else if (primitive instanceof PathIteratorWrapper) {
            PathIteratorWrapper w = (PathIteratorWrapper) primitive;
            return Bundle.pathInfo(w.size());
        } else if (primitive instanceof Line) {
            Line line = (Line) primitive;
            return Bundle.lineInfo(FMT.format(line.x1()),
                    FMT.format(line.y1()), FMT.format(line.x2()),
                    FMT.format(line.y2()), FMT.format(line.length()));
        } else if (primitive instanceof Oval) {
            Oval r = (Oval) primitive;
            return Bundle.rectangularInfo(FMT.format(r.x()), FMT.format(r.y()),
                    FMT.format(r.width()), FMT.format(r.height()));
        } else if (primitive instanceof Clear) {
            Clear r = (Clear) primitive;
            return Bundle.rectangularInfo(r.x, r.y, r.width, r.height);
        } else if (primitive instanceof RhombusWrapper) {
            RhombusWrapper w = (RhombusWrapper) primitive;
            return Bundle.rhombusInfo(w.radiusX(), w.radiusY(), w.rotation(),
                    w.centerX(), w.centerY());
        }
        return "";
    }

    public static String nameOf(Primitive primitive) {
        if (primitive == null) {
            return "";
        } else if (primitive instanceof Arc) {
            return Bundle.arc();
        } else if (primitive instanceof CircleWrapper) {
            return Bundle.circle();
        } else if (primitive instanceof ImageWrapper) {
            return Bundle.image();
        } else if (primitive instanceof net.java.dev.imagine.api.vector.elements.Rectangle) {
            return Bundle.rect();
        } else if (primitive instanceof Text) {
            return Bundle.text();
        } else if (primitive instanceof RoundRect) {
            return Bundle.roundRect();
        } else if (primitive instanceof Triangle2D || primitive instanceof TriangleWrapper) {
            return Bundle.triangle();
        } else if (primitive instanceof PathIteratorWrapper) {
            return Bundle.path();
        } else if (primitive instanceof Line) {
            return Bundle.line();
        } else if (primitive instanceof Oval) {
            return Bundle.oval();
        } else if (primitive instanceof Clear) {
            return Bundle.clear();
        } else if (primitive instanceof PathText) {
            return Bundle.pathText();
        } else if (primitive instanceof PolygonWrapper || primitive instanceof net.java.dev.imagine.api.vector.elements.Polygon) {
            return Bundle.polygon();
        } else if (primitive instanceof RhombusWrapper) {
            return Bundle.rhombus();
        } else if (primitive instanceof Polyline) {
            return Bundle.polyline();
        }
        String result = Strings.escape(primitive.getClass().getSimpleName(),
                ESC).trim();
        if (result.endsWith(" Wrapper")) {
            result = result.substring(0, result.length() - " Wrapper".length());
        }
        return result;
    }

    static Esc ESC = new Esc();

    static class Esc implements Escaper {

        @Override
        public CharSequence escape(char c) {
            if (Character.isUpperCase(c)) {
                return " " + c;
            }
            return Strings.singleChar(c);
        }
    }

    private ShapeNames() {
    }
}
