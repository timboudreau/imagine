package org.imagine.markdown.uiapi;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import static org.imagine.markdown.uiapi.ClassRelativeImageLoader.toToolkitFriendlyImage;

/**
 *
 * @author Tim Boudreau
 */
class UrlImageLoader implements EmbeddedImageLoader {

    @Override
    public BufferedImage load(String url) {
        try {
            URL u = new URL(url);
            try (InputStream in = u.openStream()) {
                if (in == null) {
                    Logger.getLogger(ClassRelativeImageLoader.class.getName()).log(Level.WARNING,
                            "No such resource {0}", url);
                    return null;
                }
                BufferedImage img = ImageIO.read(in);
                return toToolkitFriendlyImage(img);
            } catch (IOException ex) {
                Logger.getLogger(ClassRelativeImageLoader.class.getName()).log(Level.INFO, null, ex);
            }
            return null;
        } catch (MalformedURLException ex) {
            Logger.getLogger(UrlImageLoader.class.getName()).log(Level.SEVERE, "Bad embedded help image url " + url, ex);
            return null;
        }
    }

    @Override
    public boolean canLoad(String url) {
        return url.startsWith("file:/") || url.startsWith("nbres:")
                || url.startsWith("nbresloc:") || url.startsWith("http://")
                || url.startsWith("https://");
    }

}
