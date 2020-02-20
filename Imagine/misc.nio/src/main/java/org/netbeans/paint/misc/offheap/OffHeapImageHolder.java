package org.netbeans.paint.misc.offheap;

import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.netbeans.paint.misc.image.ByteNIOBufferedImage;

/**
 *
 * @author Tim Boudreau
 */
public class OffHeapImageHolder {

    private Reference<BufferedImage> original;
    public OffHeapImageHolder(BufferedImage orig) {
        ByteNIOBufferedImage img = new ByteNIOBufferedImage(orig);
        original = new WeakReference<>(orig);
    }
}
