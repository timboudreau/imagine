package net.java.dev.imagine.effects.spi;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 *
 * @author Tim Boudreau
 */
public interface ImageSource {

    public BufferedImage getRawImage();

    public BufferedImage createImageCopy(Dimension size);
}
