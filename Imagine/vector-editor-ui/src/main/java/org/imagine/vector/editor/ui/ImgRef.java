/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui;

import java.lang.ref.WeakReference;
import org.netbeans.paint.misc.image.ByteNIOBufferedImage;
import org.openide.util.Utilities;

/**
 *
 * @author Tim Boudreau
 */
class ImgRef extends WeakReference<ByteNIOBufferedImage> implements Runnable {

    private final Runnable disposer;

    public ImgRef(Runnable disposer, ByteNIOBufferedImage t) {
        super(t, Utilities.activeReferenceQueue());
        this.disposer = disposer;
    }

    @Override
    public void run() {
        disposer.run();
    }

}
