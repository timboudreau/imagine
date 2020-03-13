package org.netbeans.paintui;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import net.java.dev.imagine.api.io.LoadSupport;
import net.java.dev.imagine.api.io.LoadSupport.PictureFactory;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.ui.common.BackgroundStyle;
import static org.netbeans.paintui.PictureSaveSupport.IO_REV;
import org.openide.util.NbBundle.Messages;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = LoadSupport.class)
public class PictureLoadSupport extends
        LoadSupport<PictureScene.RH, BackgroundStyle, PictureScene.PI> {

    static final int LOADER_ID = 327;
    static final String EXTENSION = "igv";

    public PictureLoadSupport() {
        this(new Supp());
    }

    private PictureLoadSupport(Supp supp) {
        super(supp, LOADER_ID, EXTENSION);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(rev="
                + IO_REV + " instance "
                + Long.toString(System.identityHashCode(this), 36)
                + ")";
    }

    @Override
    @Messages("IGV_DISPLAY_NAME=Imagine Vector Binary")
    protected String displayName() {
        return Bundle.IGV_DISPLAY_NAME();
    }

    @Override
    protected void open(PictureScene.PI pictureImpl) {
        EventQueue.invokeLater(() -> {
            PaintTopComponent tc = new PaintTopComponent(pictureImpl.scene());
            tc.open();
            tc.requestActive();
        });
    }

    static class Supp implements PictureFactory<PictureScene.RH, BackgroundStyle, PictureScene.PI> {

        @Override
        public PictureScene.PI toPicture(Dimension d, PictureScene.RH r, List<LayerImplementation> layers, BackgroundStyle customData) {
            PictureScene.PI result = r.scene().getPicture();
            for (int i = 0; i < layers.size(); i++) {
                result.add(i, layers.get(i));
            }
            return result;
        }

        @Override
        public PictureScene.RH newRepaintHandle(Dimension pictureSize, BackgroundStyle backgroundStyle) {
            PictureScene scene = new PictureScene(pictureSize, backgroundStyle, false);
            return scene.rh();
        }

        @Override
        public String toString() {
            // Have a reasonable string for logging purposes
            return PictureLoadSupport.class.getSimpleName() + ".Supp(rev="
                    + IO_REV + " instance " + Long.toString(System.identityHashCode(this), 36)
                    + ")";
        }

        @Override
        public <C extends java.nio.channels.ReadableByteChannel & java.nio.channels.SeekableByteChannel> BackgroundStyle loadCustomData(C channel) throws IOException {
            long start = channel.position();
            int cdSize = Integer.BYTES + 1;
            fine(() -> "Load of custom data at " + start + " of " + cdSize
                    + " bytes by " + this);

            ByteBuffer buf = ByteBuffer.allocate(cdSize);
            channel.read(buf);
            buf.flip();
            byte rev = buf.get();
            if (rev != IO_REV) {
                throw new IOException("Unexpected file format revision for background info " + rev + " expected " + PictureSaveSupport.IO_REV);
            }
            int ord = buf.getInt();
            if (ord < 0) {
                throw new IOException("BackgroundStyle ordinal < 0: " + ord);
            }
            BackgroundStyle[] styles = BackgroundStyle.values();
            if (ord >= styles.length) {
                throw new IOException("Invalid BackgroundStyle ordinal " + ord);
            }
            return styles[ord];
        }
    }
}
