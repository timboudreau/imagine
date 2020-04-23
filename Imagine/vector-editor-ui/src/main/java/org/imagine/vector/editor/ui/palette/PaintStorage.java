package org.imagine.vector.editor.ui.palette;

import java.io.IOException;
import java.nio.channels.FileChannel;
import static java.nio.charset.StandardCharsets.US_ASCII;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
class PaintStorage extends PaletteStorage<PaintKey<?>> {

    private static final Logger LOG = Logger.getLogger(PaintStorage.class.getName());

    static {
        LOG.setLevel(Level.ALL);
    }

    public PaintStorage() {
        super(PaintKey.class);
    }

    private String hashId(PaintKey<?> key) {
        // We cannot use the key's id as a single file name or we will get
        // file name too long exceptions, but we want a name that will be
        // consistent if the same thing is added twice.
        try {
            byte[] hash = MessageDigest.getInstance("SHA-1")
                    .digest(key.id().getBytes(US_ASCII));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (int i = 0; i < hash.length; i++) {
                String s = Integer.toString(hash[i] & 0xFF, 36);
                if (s.length() == 1) {
                    sb.append('0');
                }
                sb.append(s);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new AssertionError(ex);
        }
    }

    @Override
    protected String synthesizeNamePrefix(PaintKey<?> key) {
        return hashId(key);
    }

    @Override
    protected void saveImpl(String name, PaintKey obj) throws IOException {
        LOG.log(Level.FINE, "Save {0} to name {1}", new Object[]{name, obj});
        try (final FileChannel channel = writeChannel(name)) {
            PaintKeyIO.write(channel, obj);
        } catch (IOException | RuntimeException | Error ex) {
            LOG.log(Level.SEVERE, "Exception saving paint key '" + name + "' for " + obj, ex);
            throw ex;
        }
    }

    @Override
    protected PaintKey loadImpl(String name) throws IOException {
        FileChannel channel = readChannel(name);
        LOG.log(Level.FINE, "Load PaintKey {0} from {1}", new Object[]{name, channel});
        if (channel == null) {
            LOG.log(Level.SEVERE, "No PaintKey read channel for {0}", name);
            return null;
        }
        try {
            PaintKey<?> key = PaintKeyIO.read(channel);
            return key;
        } catch (IOException | RuntimeException | Error ex) {
            LOG.log(Level.SEVERE, "Exception loading paint key '" + name, ex);
            throw ex;
        } finally {
            channel.close();
        }
    }
}
