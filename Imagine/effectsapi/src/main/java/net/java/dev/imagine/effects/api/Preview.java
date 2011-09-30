package net.java.dev.imagine.effects.api;

import java.awt.image.BufferedImage;

/**
 *
 * @author Tim Boudreau
 */
public final class Preview<ImageSourceType, ParamType, OutputType> {

    private final PreviewFactory<ImageSourceType> factory;
    private final ImageSourceType src;
    private final Effect<ParamType, OutputType> effect;

    private Preview(PreviewFactory<ImageSourceType> factory, ImageSourceType src, Effect<ParamType, OutputType> effect) {
        this.factory = factory;
        this.src = src;
        this.effect = effect;
    }

    public static <ImageSourceType, T, R> Preview create(PreviewFactory<ImageSourceType> factory, ImageSourceType src, Effect<T, R> effect) {
        return new Preview(factory, src, effect);
    }

    public BufferedImage createPreview(ParamType param) {
        return factory.createPreview(src, effect);
    }
}
