package net.java.dev.imagine.api.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Fail fast if you forgot to flip the buffer.
 *
 * @author Tim Boudreau
 */
final class DebugWrapperChannel<C extends SeekableByteChannel & WritableByteChannel> implements SeekableByteChannel, WritableByteChannel {

    private final C delegate;

    public DebugWrapperChannel(C delegate) {
        this.delegate = delegate;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        return delegate.read(dst);
    }

    private String simpleClassName(String cn) {
        String[] parts = cn.split("\\.");
        if (parts.length == 1) {
            return parts[0];
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < parts.length - 1; i++) {
            if (Character.isUpperCase(parts[i].charAt(0))) {
                if (result.length() > 0) {
                    result.insert(0, ".");
                }
                result.insert(0, parts[i]);
            } else {
                break;
            }
        }
        return result.toString();
    }

    private String stackInfo() {
        StackTraceElement[] el = new Exception().fillInStackTrace().getStackTrace();
        if (el == null || el.length == 0) {
            return "<no-debug-info>";
        }
        int foundCount = 0;
        String pkgName = DebugWrapperChannel.class.getPackage().getName();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < el.length; i++) {
            StackTraceElement ste = el[i];
            String cn = ste.getClassName();
            if (cn == null || cn.contains(pkgName)) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(simpleClassName(cn));
            sb.append('.').append(ste.getMethodName());
            sb.append(':').append(ste.getLineNumber());

            if (++foundCount > 2) {
                break;
            }
        }
        return sb.toString();
    }

    @Override
    public int write(ByteBuffer src) throws IOException {
        if (src.position() != 0) {
            throw new IOException("Writing an unflipped buffer");
        }
        // Finest is lazy, so if Level.FINEST is not available,
        // we will not actually unwind the stack
        SaveSupport.finest(() -> "Write " + src.remaining() + " bytes to "
                + "channel @ " + stackInfo());
        return delegate.write(src);
    }

    @Override
    public long position() throws IOException {
        return delegate.position();
    }

    @Override
    public SeekableByteChannel position(long newPosition) throws IOException {
        delegate.position(newPosition);
        return this;
    }

    @Override
    public long size() throws IOException {
        return delegate.size();
    }

    @Override
    public SeekableByteChannel truncate(long size) throws IOException {
        delegate.truncate(size);
        return this;
    }

    @Override
    public boolean isOpen() {
        return delegate.isOpen();
    }

    @Override
    public void close() throws IOException {
        SaveSupport.finest(() -> "Close write channel " + delegate);
        delegate.close();
    }

}
