package org.imagine.markdown.uiapi;

import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import javax.swing.JComponent;

/**
 *
 * @author Tim Boudreau
 */
public interface MarkdownUIProperties {

    Color linkColor();

    Color textColor();

    Color blockquoteSidebarColor();

    Color horizontalRuleHighlightColor();

    Color horizontalRuleShadowColor();

    Shape bulletShape(int depth, float x1, float y1, float size);

    default Font fixedWidthFont() {
        Font f = getFont();
        return new Font("Courier New", f.getStyle(), f.getSize());
    }

    default boolean isBulletShapeFilled(int depth) {
        return depth < 3;
    }

    default boolean paintImageCaptions() {
        return false;
    }

    default int maximumInlineImageDimension() {
        return -1;
    }

    Font getFont();

    static MarkdownUIProperties forComponent(JComponent comp) {
        return new ComponentMarkdownUIProperties(comp);
    }

    default MarkdownUIProperties withFont(Font f) {
        assert f != null : "null font";
        return new MarkdownUIProperties() {
            @Override
            public Color linkColor() {
                return MarkdownUIProperties.this.linkColor();
            }

            @Override
            public Color textColor() {
                return MarkdownUIProperties.this.textColor();
            }

            @Override
            public Color blockquoteSidebarColor() {
                return MarkdownUIProperties.this.blockquoteSidebarColor();
            }

            @Override
            public Color horizontalRuleHighlightColor() {
                return MarkdownUIProperties.this.horizontalRuleHighlightColor();
            }

            @Override
            public Color horizontalRuleShadowColor() {
                return MarkdownUIProperties.this.horizontalRuleShadowColor();
            }

            @Override
            public Font getFont() {
                return f;
            }

            public boolean paintImageCaptions() {
                return MarkdownUIProperties.this.paintImageCaptions();
            }

            @Override
            public Shape bulletShape(int depth, float x1, float y1, float size) {
                return MarkdownUIProperties.this.bulletShape(depth, x1, y1, size);
            }

            @Override
            public int maximumInlineImageDimension() {
                return MarkdownUIProperties.this.maximumInlineImageDimension();
            }
        };
    }

    default MarkdownUIProperties paintingCaptions() {
        return paintingCaptions(true);
    }

    default MarkdownUIProperties paintingCaptions(boolean captions) {
        return new MarkdownUIProperties() {
            @Override
            public Color linkColor() {
                return MarkdownUIProperties.this.linkColor();
            }

            @Override
            public Color textColor() {
                return MarkdownUIProperties.this.textColor();
            }

            @Override
            public Color blockquoteSidebarColor() {
                return MarkdownUIProperties.this.blockquoteSidebarColor();
            }

            @Override
            public Color horizontalRuleHighlightColor() {
                return MarkdownUIProperties.this.horizontalRuleHighlightColor();
            }

            @Override
            public Color horizontalRuleShadowColor() {
                return MarkdownUIProperties.this.horizontalRuleShadowColor();
            }

            @Override
            public Font getFont() {
                return MarkdownUIProperties.this.getFont();
            }

            @Override
            public MarkdownUIProperties paintingCaptions(boolean captions) {
                return MarkdownUIProperties.this.paintingCaptions(captions); //To change body of generated methods, choose Tools | Templates.
            }

            public boolean paintImageCaptions() {
                return captions;
            }

            @Override
            public Shape bulletShape(int depth, float x1, float y1, float size) {
                return MarkdownUIProperties.this.bulletShape(depth, x1, y1, size);
            }

            @Override
            public int maximumInlineImageDimension() {
                return MarkdownUIProperties.this.maximumInlineImageDimension();
            }

        };
    }

    default MarkdownUIProperties withMaximumInlineImageDimension(int dim) {
        return new MarkdownUIProperties() {
            @Override
            public Color linkColor() {
                return MarkdownUIProperties.this.linkColor();
            }

            @Override
            public int maximumInlineImageDimension() {
                return dim;
            }

            @Override
            public Color textColor() {
                return MarkdownUIProperties.this.textColor();
            }

            @Override
            public Color blockquoteSidebarColor() {
                return MarkdownUIProperties.this.blockquoteSidebarColor();
            }

            @Override
            public Color horizontalRuleHighlightColor() {
                return MarkdownUIProperties.this.horizontalRuleHighlightColor();
            }

            @Override
            public Color horizontalRuleShadowColor() {
                return MarkdownUIProperties.this.horizontalRuleShadowColor();
            }

            @Override
            public Font getFont() {
                return MarkdownUIProperties.this.getFont();
            }

            @Override
            public MarkdownUIProperties paintingCaptions(boolean captions) {
                return MarkdownUIProperties.this.paintingCaptions(captions);
            }

            public boolean paintImageCaptions() {
                return MarkdownUIProperties.this.paintImageCaptions();
            }

            @Override
            public Shape bulletShape(int depth, float x1, float y1, float size) {
                return MarkdownUIProperties.this.bulletShape(depth, x1, y1, size);
            }
        };
    }

}
