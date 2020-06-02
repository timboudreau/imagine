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

    default boolean isBulletShapeFilled(int depth) {
        return true;
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

            @Override
            public Shape bulletShape(int depth, float x1, float y1, float size) {
                return MarkdownUIProperties.this.bulletShape(depth, x1, y1, size);
            }
        };
    }
}
