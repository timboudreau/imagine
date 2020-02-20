package net.java.dev.imagine.effects.spi;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import net.java.dev.imagine.effects.api.Effect;
import net.java.dev.imagine.effects.api.PreviewFactory;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service=PreviewFactory.class, position = 100)
public class BufferedImageOpPreviewFactory extends PreviewFactory<ImageSource> {

    public BufferedImageOpPreviewFactory() {
        super(ImageSource.class);
    }

    @Override
    public <T, R> boolean canCreatePreview(Effect<T, R> effect) {
        return effect.outputType().isAssignableFrom(BufferedImageOp.class);
    }

    @Override
    public <T, R> BufferedImage createPreview(ImageSource imageSource, Effect<T, R> effect, T param, Dimension size) {
        R output = effect.create(param);
        BufferedImageOp op = (BufferedImageOp) output;
        BufferedImage bi = imageSource.createImageCopy(size);
        BufferedImage nue = new BufferedImage(size.width, size.height, bi.getType());
        return op.filter(bi, nue);
    }
    
}
