/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.awt.dnd;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import javax.activation.DataSource;
import org.imagine.awt.impl.Accessor;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
final class PaintKeyDataSource implements DataSource {

    private PaintKey<?> key;
    private Class<?> type;

    public PaintKeyDataSource(PaintKey<?> key) {
        this.key = key;
        type = key.getClass();
    }

    public PaintKeyDataSource(Class<?> type) {
        this.type = type;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(PaintKeyIO.writeAsBytes(key));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                key = PaintKeyIO.read(toByteArray());
            }
        };
    }

    @Override
    public String getContentType() {
        return PaintKeyDropSupport.mimeTypeFor(type);
    }

    @Override
    public String getName() {
        for (Map.Entry<String, Class<?>> e : Accessor.allSupportedTypes().entrySet()) {
            if (e.getValue() == type) {
                return e.getKey();
            }
        }
        return type.getSimpleName();
    }
}
