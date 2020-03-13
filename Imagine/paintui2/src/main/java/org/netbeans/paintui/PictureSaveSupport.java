/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paintui;

import java.io.IOException;
import static java.lang.System.identityHashCode;
import java.nio.ByteBuffer;
import net.java.dev.imagine.api.io.SaveSupport;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = SaveSupport.class, position = 10)
public class PictureSaveSupport extends SaveSupport<BackgroundStyle, PictureScene.PI> {

    static final byte IO_REV = 1;

    public PictureSaveSupport() {
        super(PictureScene.PI.class, PictureLoadSupport.LOADER_ID, PictureLoadSupport.EXTENSION);
    }

    @Override
    public String displayName() {
        return Bundle.IGV_DISPLAY_NAME();
    }

    @Override
    protected <C extends java.nio.channels.WritableByteChannel & java.nio.channels.SeekableByteChannel> BackgroundStyle saveCustomData(PictureScene.PI picture, C channel) throws IOException {
        long pos = channel.position();
        int cdSize = Integer.BYTES + 1;
        fine(() -> "Save of custom data at " + pos + " bytes " + cdSize + " by " + this);
        ByteBuffer buf = ByteBuffer.allocate(cdSize);
        buf.put(IO_REV);
        BackgroundStyle style = picture.backgroundStyle();
        buf.putInt(style.ordinal());
        buf.flip();
        channel.write(buf);
        return style;
    }

    @Override
    public String toString() {
        return "PictureSaveSupport(rev=" + IO_REV
                + " instance " + Long.toString(identityHashCode(this),
                        36) + ")";
    }
}
