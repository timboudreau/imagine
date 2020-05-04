package org.imagine.svg.io;

import com.mastfrog.function.state.Obj;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.dev.imagine.api.image.Picture;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.bridge.DocumentLoader;
import org.apache.batik.bridge.GVTBuilder;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.UserAgentAdapter;
import org.apache.batik.ext.awt.RenderingHintsKeyExt;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.util.XMLResourceDescriptor;
import org.imagine.utils.painting.RepaintHandle;
import org.imagine.vector.editor.ui.spi.VectorLayerFactory;
import org.openide.util.Exceptions;
import org.openide.util.Parameters;
import org.w3c.dom.Document;

/**
 *
 * @author Tim Boudreau
 */
public class SvgLoader {

    private static final Logger LOG = Logger.getLogger(SvgLoader.class.getName());
    /* A limit of 8192 pixels on each side means the resulting maximum image buffer would take 268
    megabytes. It's also twice twice as long as the longest side of a 4K display. This is small
    enough to avoid an OutOfMemoryError but large enough to cater for most SVG loading scenarios.
    Photoshop had 10000 pixels as a maximum limit for many years. */
    private static final int MAX_DIMENSION_PIXELS = 8192;
    // XML document factories are expensive to initialize, so do it once per thread only.
    private static final ThreadLocal<SAXSVGDocumentFactory> DOCUMENT_FACTORY
            = new ThreadLocal<SAXSVGDocumentFactory>() {
        @Override
        protected SAXSVGDocumentFactory initialValue() {
            return new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
        }
    };

    private final Path path;

    public SvgLoader(Path path) {
        this.path = path;
    }

    private Dimension max(Dimension a, Dimension b) {
        return new Dimension(Math.max(a.width, b.width), Math.max(a.height, b.height));
    }

    Picture load(BiFunction<Dimension, BiConsumer<RepaintHandle, Function<List<LayerImplementation>, Picture>>, Void> func, VectorLayerFactory vlf, String name) throws MalformedURLException, IOException {
        Dimension docSize = new Dimension();
        GraphicsNode gn = loadGraphicsNode(path.toUri().toURL(), null, docSize);
        Rectangle2D r = gn.getBounds();
        Dimension d = r.getBounds().getBounds().getSize();
        Obj<Picture> result = Obj.create();
        System.out.println("        SVG LOAD " + path + " with size " + d);
        func.apply(max(d, docSize), (handle, pictureGenerator) -> {
            System.out.println("Got callback with " + handle + " and " + pictureGenerator);
            List<LayerImplementation> layers = new ArrayList<>(3);
            try {
                load(gn, () -> {
                    LayerImplementation<?> layer = vlf.createLayer(name + " "
                            + (layers.size() + 1), handle, d);
                    layers.add(layer);
                    return layer;
                });
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
                return;
            }
            System.out.println("Loaded with " + layers.size() + " layers");
            if (!layers.isEmpty()) {
                Picture pic = pictureGenerator.apply(layers);
                System.out.println("GOT picture" + pic);
                result.set(pic);
            } else {
                System.out.println("Empty layer list, no picture");
            }
        });
        return result.get();
    }

    public void load(GraphicsNode gn, Supplier<LayerImplementation<?>> supp) throws IOException {
        gn.setVisible(true);
        gn.setRenderingHint(RenderingHintsKeyExt.KEY_AVOID_TILE_PAINTING, RenderingHintsKeyExt.VALUE_AVOID_TILE_PAINTING_ON);
        gn.setRenderingHint(RenderingHintsKeyExt.KEY_TRANSCODING, RenderingHintsKeyExt.VALUE_TRANSCODING_VECTOR);
        LayerImplementation<?> layer = supp.get();
        SurfaceImplementation surf = layer.getSurface();
        surf.beginUndoableOperation("Load " + path.getFileName());
        System.out.println("      create layer " + layer + " with surface " + surf);
        // XXX carve this up into groups which each get a layer
        Graphics2D g = surf.getGraphics();
        System.out.println("         Paint " + gn + " into " + g);
        gn.primitivePaint(g);
        surf.endUndoableOperation();
        // Don't dispose - GraphicsNode does it
    }

    private static GraphicsNode loadGraphicsNode(URL url, Dimension toSize, Dimension docsize)
            throws IOException {
        Parameters.notNull("url", url);
        final GraphicsNode graphicsNode;
        final Dimension2D documentSize;
        final Document doc;
        try (InputStream is = url.openStream()) {
            // See http://batik.2283329.n4.nabble.com/rendering-directly-to-java-awt-Graphics2D-td3716202.html
            SAXSVGDocumentFactory factory = DOCUMENT_FACTORY.get();
            /* Don't provide an URI here; we shouldn't commit to supporting relative links from
            loaded SVG documents. */
            doc = factory.createDocument(null, is);
            UserAgent userAgent = new UserAgentAdapter();
            DocumentLoader loader = new DocumentLoader(userAgent);
            BridgeContext bctx = new BridgeContext(userAgent, loader);
            try {
                bctx.setDynamicState(BridgeContext.STATIC);
                graphicsNode = new GVTBuilder().build(bctx, doc);
                documentSize = bctx.getDocumentSize();
                docsize.setSize(documentSize);
            } finally {
                bctx.dispose();
            }
        } catch (RuntimeException e) {
            /* Rethrow the many different exceptions that can occur when parsing invalid SVG files;
            DOMException, BridgeException etc. */
            throw new IOException("Error parsing SVG file", e);
        }
        if (toSize != null) {
            int width = (int) Math.ceil(documentSize.getWidth());
            int height = (int) Math.ceil(documentSize.getHeight());
            final int widthLimited = Math.min(MAX_DIMENSION_PIXELS, width);
            final int heightLimited = Math.min(MAX_DIMENSION_PIXELS, height);
            if (width != widthLimited || height != heightLimited) {
                LOG.log(Level.WARNING,
                        "SVG image {0} too large (dimensions were {1}x{2}, each side can be at most {3}px)",
                        new Object[]{url, width, height, MAX_DIMENSION_PIXELS});
            } else if (width <= 1 && height <= 1) {
                LOG.log(Level.WARNING,
                        "SVG image {0} did not specify a width/height, or is incorrectly sized", url);
            }
            toSize.width = widthLimited;
            toSize.height = heightLimited;
        }
        return graphicsNode;
    }
}
