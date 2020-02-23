/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.awt.key;

import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import static java.lang.Float.intBitsToFloat;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.io.PaintKeyReader;
import org.imagine.awt.io.PaintKeyWriter;
import org.imagine.awt.util.Hasher;
import org.imagine.awt.util.IdPathBuilder;

/**
 *
 * @author Tim Boudreau
 */
public class ManagedTexturePaintKey extends PaintKey<TexturePaint> {

    public static final String PREFIX = "managed-";
    public static final String ID_BASE = PREFIX + TexturePaintKey.ID_BASE;
    private final int x, y, w, h;
    private final byte[] hash;

    public ManagedTexturePaintKey(int x, int y, int w, int h, byte[] hash) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.hash = hash;
    }

    public TexturePaintKey toTexturePaintKey() {
        return new TexturePaintKey(toPaint());
    }

    @Override
    public String toString() {
        return id();
    }

    public static ManagedTexturePaintKey read(PaintKeyReader reader) throws IOException {
        int x = reader.readInt();
        int y = reader.readInt();
        int w = reader.readInt();
        int h = reader.readInt();
        byte[] hash = reader.readByteArray();
        return new ManagedTexturePaintKey(x, y, w, h, hash);
    }

    @Override
    protected Class<TexturePaint> type() {
        return TexturePaint.class;
    }

    @Override
    protected TexturePaint toPaint() {
        return Accessor.texturePaintForManaged(this);
    }

    private BufferedImage getImage() {
        return toPaint().getImage();
    }

    public Rectangle2D anchorRect() {
        return new Rectangle2D.Float(intBitsToFloat(x), intBitsToFloat(y),
                intBitsToFloat(w), intBitsToFloat(h));
    }

    @Override
    protected int computeHashCode() {
        BufferedImage img = getImage();
        return new Hasher().add(hash).add(img.getWidth())
                .add(img.getHeight())
                .add(x).add(y).add(w).add(h)
                .hashCode();
    }

    @Override
    public String idBase() {
        return ID_BASE;
    }

    @Override
    protected void buildId(IdPathBuilder bldr) {
        ByteBuffer buf = ByteBuffer.wrap(hash);
        while (buf.remaining() > 0) {
            bldr.add(buf.getInt());
        }
        bldr.add(x).add(y).add(w).add(h);
    }

    @Override
    public void writeTo(PaintKeyWriter writer) throws IOException {
        writer.writeInt(x)
                .writeInt(y)
                .writeInt(w)
                .writeInt(h)
                .writeByteArray(hash);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (!(obj instanceof ManagedTexturePaintKey)) {
            return false;
        }
        final ManagedTexturePaintKey other = (ManagedTexturePaintKey) obj;
        if (this.x != other.x) {
            return false;
        } else if (this.y != other.y) {
            return false;
        } else if (this.w != other.w) {
            return false;
        } else if (this.h != other.h) {
            return false;
        }
        return Arrays.equals(this.hash, other.hash);
    }

    @Override
    public PaintKeyKind kind() {
        return StandardPaintKeyKinds.TEXTURE;
    }

}
