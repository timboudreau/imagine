package org.imagine.markdown.uiapi;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

/**
 *
 * @author Tim Boudreau
 */
public interface EmbeddedImageLoader {

    static EmbeddedImageLoader classRelative(Class<?> type) {
        return new ClassRelativeImageLoader(type).or(new UrlImageLoader());
    }

    default Dimension2D getImageSize(double x, double y, String url, Rectangle2D within, EmbeddedImageCache cache) {
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

    default boolean render(String url, Graphics2D g, double x, double y, Dimension2D size, EmbeddedImageCache cache) {
        if (size.getWidth() == 0 || size.getHeight() == 0) {
            return false;
        }
        BufferedImage img = cache.get(url, () -> load(url));
        if (img == null) {
            return false;
        }
        if (size.getWidth() == img.getWidth() && size.getHeight() == img.getHeight()) {
            g.drawRenderedImage(img, AffineTransform.getTranslateInstance(x, y));
        } else {
            double sx = size.getWidth() / img.getWidth();
            double sy = size.getHeight() / img.getHeight();
            AffineTransform xform = AffineTransform.getTranslateInstance(x, y);
            xform.concatenate(AffineTransform.getScaleInstance(sx, sy));
            g.drawRenderedImage(img, xform);
        }
        return true;
    }

    default EmbeddedImageLoader or(EmbeddedImageLoader other) {
        if (other == this) {
            return this;
        }
        return of(this, other);
    }

    BufferedImage load(String url);

    boolean canLoad(String url);

    static Dimension2D dimension(double width, double height) {
        return new DimensionImpl(width, height);
    }

    static EmbeddedImageLoader of(EmbeddedImageLoader... all) {
        return new EmbeddedImageLoader() {
            @Override
            public Dimension2D getImageSize(double x, double y, String url, Rectangle2D within, EmbeddedImageCache cache) {
                for (EmbeddedImageLoader ldr : all) {
                    if (ldr.canLoad(url)) {
                        Dimension2D dim = ldr.getImageSize(x, y, url, within, cache);
                        if (dim.getWidth() > 0 && dim.getHeight() > 0) {
                            return dim;
                        }
                    }
                }
                return DimensionImpl.EMPTY;
            }

            @Override
            public boolean render(String url, Graphics2D g, double x, double y, Dimension2D size, EmbeddedImageCache cache) {
                for (EmbeddedImageLoader ldr : all) {
                    if (ldr.canLoad(url)) {
                        if (ldr.render(url, g, x, y, size, cache)) {
                            return true;
                        }
                    }
                }
                return false;
            }

            @Override
            public boolean canLoad(String url) {
                for (EmbeddedImageLoader ldr : all) {
                    if (ldr.canLoad(url)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public BufferedImage load(String url) {
                for (EmbeddedImageLoader ldr : all) {
                    if (ldr.canLoad(url)) {
                        BufferedImage result = ldr.load(url);
                        if (result != null) {
                            return result;
                        }
                    }
                }
                return null;
            }
        };
    }

    static EmbeddedImageLoader EMPTY = new EmbeddedImageLoader() {
        @Override
        public Dimension2D getImageSize(double x, double y, String url, Rectangle2D within, EmbeddedImageCache cache) {
            return DimensionImpl.EMPTY;
        }

        @Override
        public boolean render(String url, Graphics2D g, double x, double y, Dimension2D size, EmbeddedImageCache cache) {
            return false;
        }

        @Override
        public boolean canLoad(String url) {
            return false;
        }

        @Override
        public BufferedImage load(String url) {
            return null;
        }
    };
}
