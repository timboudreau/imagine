package net.java.dev.imagine.effects.api;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 *
 * @author Tim Boudreau
 */
public abstract class PreviewFactory<ImageSourceType> {

    private final Class<ImageSourceType> sourceType;

    public PreviewFactory(Class<ImageSourceType> sourceType) {
        this.sourceType = sourceType;
    }

    public final Class<ImageSourceType> sourceType() {
        return sourceType;
    }

    public abstract <T, R> boolean canCreatePreview(Effect<T, R> effect);

    public abstract <T, R> BufferedImage createPreview(ImageSourceType imageSource, Effect<T, R> effect, T param, Dimension size);
}
