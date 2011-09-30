package net.java.dev.imagine.effects.spi;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import org.openide.util.Lookup;
import org.openide.util.Parameters;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
class BufferedImageImageSource implements ImageSource {

    private final Lookup.Provider provider;
    public static int DEFAULT_BUFFERED_IMAGE_TYPE =
            Utilities.isMac() ? BufferedImage.TYPE_INT_ARGB_PRE
            : BufferedImage.TYPE_INT_ARGB; //XXX move GraphicsUtils somewhere else and it

    private BufferedImageImageSource(Lookup.Provider provider) {
        this.provider = provider;
        assert provider.getLookup().lookup(BufferedImage.class) != null;
    }

    public static ImageSource create(Lookup.Provider p) {
        return p.getLookup().lookup(BufferedImage.class) != null
                ? new BufferedImageImageSource(p) : null;
    }

    @Override
    public BufferedImage getRawImage() {
        return provider.getLookup().lookup(BufferedImage.class);
    }

    @Override
    public BufferedImage createImageCopy(Dimension scaleToSize) {
        Parameters.notNull("size", scaleToSize);
        assert scaleToSize.width > 0 && scaleToSize.height > 0;
        BufferedImage orig = getRawImage();
        BufferedImage nue = new BufferedImage(scaleToSize.width,
                scaleToSize.height, DEFAULT_BUFFERED_IMAGE_TYPE);
        double w = scaleToSize.width;
        double h = scaleToSize.height;
        double xfactor = w / (double) orig.getWidth();
        double yfactor = h / (double) orig.getHeight();
        AffineTransform xform = AffineTransform.getScaleInstance(xfactor, yfactor);
        Graphics2D g = nue.createGraphics();
        g.drawRenderedImage(orig, xform);
        g.dispose();
        return nue;
    }
}
