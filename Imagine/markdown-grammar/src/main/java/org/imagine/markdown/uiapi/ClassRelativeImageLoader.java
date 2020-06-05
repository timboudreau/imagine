package org.imagine.markdown.uiapi;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Tim Boudreau
 */
final class ClassRelativeImageLoader implements EmbeddedImageLoader {

    private final Class<?> relativeTo;
    private static final String PROTO = "lres:/";

    public ClassRelativeImageLoader(Class<?> relativeTo) {
        this.relativeTo = relativeTo;
    }

    public String toString() {
        return "Lres-loader(" + relativeTo.getName() + ")";
    }

    @Override
    public boolean canLoad(String url) {
        return url.startsWith("lres:/");
    }

    @Override
    public Dimension2D getImageSize(double x, double y, String url, Rectangle2D within, EmbeddedImageCache cache) {
        BufferedImage img = cache.get(url, () -> load(url));
        if (img == null) {
            return DimensionImpl.EMPTY;
        }
        double remainderX = within.getMaxX() - x;
        double remainderY = within.getMaxY() - y;
        if (remainderX >= img.getWidth() && remainderY >= img.getHeight()) {
            return new DimensionImpl(img);
        }
        remainderX = within.getWidth();
        remainderY = within.getHeight();
        if (remainderX >= img.getWidth() && remainderY >= img.getHeight()) {
            return new DimensionImpl(img);
        }
        double scale;
        if (img.getWidth() > remainderX) {
            scale = remainderX / img.getWidth();
        } else {
            scale = remainderY / img.getHeight();
        }
        return new DimensionImpl(img.getWidth() * scale, img.getHeight() * scale);
    }

    public BufferedImage load(String url) {
        url = url.substring(PROTO.length());
        try (InputStream in = relativeTo.getResourceAsStream(url)) {
            if (in == null) {
                Logger.getLogger(ClassRelativeImageLoader.class.getName()).log(Level.WARNING,
                        "No such resource {0} relative to {1}", new Object[]{url,
                            relativeTo.getName()});
                return null;
            }
            BufferedImage img = ImageIO.read(in);
            return toToolkitFriendlyImage(img);
        } catch (IOException ex) {
            Logger.getLogger(ClassRelativeImageLoader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    static BufferedImage loadImageFromStream(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        BufferedImage img = ImageIO.read(in);
        return toToolkitFriendlyImage(img);
    }

    private static int toolkitType = Integer.MIN_VALUE;

    static BufferedImage toToolkitFriendlyImage(BufferedImage img) {
        if (img == null) {
            return null;
        }
        if (toolkitType != Integer.MIN_VALUE && img.getType() == toolkitType) {
            return img;
        }
        BufferedImage toCache = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(img.getWidth(), img.getHeight(), Transparency.TRANSLUCENT);
        if (toolkitType == Integer.MIN_VALUE) {
            toolkitType = toCache.getType();
        }
        Graphics2D g = toCache.createGraphics();
        try {
            g.drawRenderedImage(img, null);
        } finally {
            g.dispose();
            img.flush();
        }
        return toCache;

    }
}
