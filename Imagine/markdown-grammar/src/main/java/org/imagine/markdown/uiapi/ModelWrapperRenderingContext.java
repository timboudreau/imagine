/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.uiapi;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.LineMetrics;
import java.awt.geom.Rectangle2D;
import java.util.function.Consumer;
import org.imagine.markdown.uiapi.graphics.CapturingGraphics;
import org.imagine.markdown.uiapi.graphics.MarkdownDetailsModel;
import org.imagine.markdown.uiapi.graphics.MarkdownRenderingModel;
import org.imagine.markdown.uiapi.graphics.StringDetailsSupplier;

/**
 *
 * @author Tim Boudreau
 */
public class ModelWrapperRenderingContext implements MarkdownRenderingContext {

    private final MarkdownRenderingContext delegate;
    private CapturingGraphics cg;
    private StringDetailsSupplier details;

    public ModelWrapperRenderingContext(MarkdownRenderingContext delegate) {
        this.delegate = delegate;
    }

    public MarkdownRenderingModel createModel(Rectangle2D.Float renderedBounds) {
        return cg.toModel(renderedBounds);
    }

    public MarkdownDetailsModel toDetailsModel(Rectangle2D.Float renderedBounds) {
        return cg.toDetailsModel(renderedBounds);
    }

    public ModelWrapperRenderingContext setDetailsSupplier(StringDetailsSupplier supp) {
        this.details = supp;
        return this;
    }

    @Override
    public boolean withGraphics(Consumer<Graphics2D> c) {
        if (cg != null) {
            c.accept(cg);
        } else {
            if (!delegate.isPaintingContext()) {
                cg = details == null ? new CapturingGraphics() : new CapturingGraphics(details);
            } else {
                delegate.withGraphics(g -> {
                    cg = new CapturingGraphics(g, details);
                });
            }
            c.accept(cg);
        }
        return true;
    }

    @Override
    public void setFont(Font f) {
        delegate.setFont(f);
        if (cg != null) {
            cg.setFont(f);
        }
    }

    @Override
    public Font getFont() {
        return delegate.getFont();
    }

    @Override
    public FontMetrics getFontMetrics() {
        return delegate.getFontMetrics();
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return delegate.getFontMetrics(font);
    }

    @Override
    public LineMetrics getLineMetrics(String s) {
        return delegate.getLineMetrics(s);
    }

    @Override
    public boolean isPaintingContext() {
        return true;
    }
}
