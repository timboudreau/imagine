/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.effects.spi;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import net.java.dev.imagine.effects.api.Effect;
import net.java.dev.imagine.effects.api.PreviewFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = PreviewFactory.class, position = 200)
public class AffineTransformPreviewFactory extends PreviewFactory<ImageSource> {

    public AffineTransformPreviewFactory() {
        super(ImageSource.class);
    }

    @Override
    public <T, R> boolean canCreatePreview(Effect<T, R> effect) {
        return effect.outputType().isAssignableFrom(AffineTransform.class);
    }

    @Override
    public <T, R> BufferedImage createPreview(ImageSource imageSource, Effect<T, R> effect, T param, Dimension size) {
        R output = effect.create(param);
        AffineTransform transform = (AffineTransform) output;
        BufferedImage bi = imageSource.createImageCopy(size);
        BufferedImage nue = new BufferedImage(size.width, size.height, bi.getType());
        Graphics2D g = nue.createGraphics();
        g.setTransform(transform);
        g.drawRenderedImage(bi, AffineTransform.getTranslateInstance(0, 0));
        g.dispose();
        return nue;
    }

}
