package org.imagine.svg.io;

import org.apache.batik.svggen.ExtensionHandler;
import org.apache.batik.svggen.ImageHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;

/**
 *
 * @author Tim Boudreau
 */
public class ExtSvgGraphics2D extends SVGGraphics2D {

    public ExtSvgGraphics2D(Document domFactory) {
        super(domFactory);
    }

    public ExtSvgGraphics2D(Document domFactory, ImageHandler imageHandler, ExtensionHandler extensionHandler, boolean textAsShapes) {
        super(domFactory, imageHandler, extensionHandler, textAsShapes);
    }

    public ExtSvgGraphics2D(SVGGeneratorContext generatorCtx, boolean textAsShapes) {
        super(generatorCtx, textAsShapes);
    }

    public ExtSvgGraphics2D(SVGGraphics2D g) {
        super(g);
    }

    @Override
    protected void setGeneratorContext(SVGGeneratorContext generatorCtx) {
        super.setGeneratorContext(generatorCtx);
        shapeConverter = new ExtShapeConverter(generatorCtx);
    }
//
//    public void add(ShapeElement el) {
//        short method = el.isFill() && el.isDraw()
//                ? DOMGroupManager.DRAW | DOMGroupManager.FILL
//                : el.isFill() ? DOMGroupManager.FILL : DOMGroupManager.DRAW;
//
//        if (el.isFill()) {
//            setPaint(el.getFillKey().toPaint());
//        }
//
//    }
}
