/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.effects.spi;

import java.awt.Composite;
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
@ServiceProvider(service=PreviewFactory.class)
public class CompositePreviewFactory extends PreviewFactory<ImageSource> {

    public CompositePreviewFactory() {
        super(ImageSource.class);
    }

    @Override
    public <T, R> boolean canCreatePreview(Effect<T, R> effect) {
        return effect.outputType().isAssignableFrom(Composite.class);
    }

    @Override
    public <T, R> BufferedImage createPreview(ImageSource imageSource, Effect<T, R> effect, T param, Dimension size) {
        R output = effect.create(param);
        Composite composite = (Composite) output;
        BufferedImage bi = imageSource.createImageCopy(size);
        BufferedImage nue = new BufferedImage(size.width, size.height, bi.getType());
        Graphics2D g = nue.createGraphics();
        g.setComposite(composite);
        g.drawRenderedImage(bi, AffineTransform.getTranslateInstance(0, 0));
        g.dispose();
        return nue;
    }
    
}
