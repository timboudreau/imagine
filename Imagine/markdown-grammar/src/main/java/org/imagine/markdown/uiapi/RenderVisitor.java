package org.imagine.markdown.uiapi;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.imagine.markdown.grammar.MarkdownParser;
import org.imagine.markdown.grammar.MarkdownParserBaseVisitor;

/**
 *
 * @author Tim Boudreau
 */
final class RenderVisitor extends MarkdownParserBaseVisitor<Void> {

    private final MarkdownRenderingContext renderer;
    private int strikeCount;
    private final MarkdownUIProperties props;
    private final Rectangle2D.Float bounds;
    private float minX = Float.MAX_VALUE;
    private float minY = Float.MAX_VALUE;
    private float maxX = Float.MIN_VALUE;
    private float maxY = Float.MIN_VALUE;
    private float x;
    private float y;
    private float baselineAdjust;
    private boolean inNestedContent;
    private RectangleCollection currentLinkBounds;
    private boolean pristine;

    RenderVisitor(MarkdownRenderingContext ctx, MarkdownUIProperties props, Rectangle2D.Float bounds) {
        this.renderer = ctx;
        this.props = props;
        this.bounds = bounds;
    }

    Rectangle2D.Float usedBounds() {
        if (minX == Float.MIN_VALUE || minY == Float.MIN_VALUE) {
            return new Rectangle2D.Float();
        }
        Rectangle2D.Float result = new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
        return result;
    }

    private boolean withGraphics(Consumer<Graphics2D> c) {
        return renderer.withGraphics(c);
    }

    @Override
    public Void visitDocument(MarkdownParser.DocumentContext ctx) {
        renderer.setFont(props.getFont());
        renderer.withGraphics(g -> {
            g.setFont(props.getFont());
            g.setColor(props.textColor());
        });
        Void result = super.visitDocument(ctx);
        paintHorizontalRules();
        return result;
    }

    private void debugPaintRules() {
        withFont(props.getFont().deriveFont(AffineTransform.getScaleInstance(0.75, 0.75)), () -> {
            withGraphics(g -> {
                g.setColor(Color.BLUE);
                FontMetrics fm = g.getFontMetrics();
                for (int i = 0; i < usedBounds().height + 50; i += 50) {
                    g.drawLine(0, i, 10, i);
                    String s = Integer.toString(i);
                    float txtY = i - (fm.getAscent());
                    g.drawString(s, 12, txtY);
                }
            });
            return null;
        });
    }

    @Override
    public Void visitParagraphs(MarkdownParser.ParagraphsContext ctx) {
        if (!inNestedContent) {
            if (toLineStart()) {
                //                    y += g.getFontMetrics().getHeight() / 2F;
            }
        }
        return super.visitParagraphs(ctx);
    }

    @Override
    public Void visitParaBreak(MarkdownParser.ParaBreakContext ctx) {
        if (!inNestedContent) {
            toLineStart();
            y += renderer.getFontMetrics().getHeight() / 2F;
        }
        return super.visitParaBreak(ctx);
    }

    private List<Line2D.Float> postponedHorizontalRules = new ArrayList<>(3);

    void paintHorizontalRules() {
        withGraphics(g -> {
            LineMetrics lm = renderer.getLineMetrics("---");
            double lineWidth = lm.getStrikethroughThickness();
            float right = maxX;
            BasicStroke stroke = new BasicStroke((float) lineWidth);
            g.setStroke(stroke);
            double shadowOffset = lineWidth / 2D;
            Color oldColor = g.getColor();
            Color highlight = props.horizontalRuleHighlightColor();
            Color shadow = props.horizontalRuleShadowColor();
            for (Line2D.Float line : postponedHorizontalRules) {
                line.x2 = right;
                line.x1 += shadowOffset;
                g.translate(-shadowOffset, -shadowOffset);
                g.setColor(shadow);
                g.draw(line);
                g.translate(lineWidth, lineWidth);
                g.setColor(highlight);
                g.draw(line);
                g.translate(-shadowOffset, -shadowOffset);
            }
            g.setColor(oldColor);
        });
    }

    @Override
    public Void visitHorizontalRule(MarkdownParser.HorizontalRuleContext ctx) {
        toLineStart();

        FontMetrics fm = renderer.getFontMetrics();
        float fullLineHeight = fm.getHeight() + fm.getDescent() + fm.getLeading();
        float offY = fullLineHeight / 2F;
        Line2D.Float rule = new Line2D.Float(x, y + offY, x + 1, y + offY);
        postponedHorizontalRules.add(rule);

        y += fullLineHeight;
        addToBounds(x, y);
        return super.visitHorizontalRule(ctx);
    }

    @Override
    public Void visitBlockquote(MarkdownParser.BlockquoteContext ctx) {
        return outsideRoot(() -> {
            FontMetrics fm = renderer.getFontMetrics();
            y += fm.getHeight() / 2F;
            float origX = bounds.x;
            float ind = fm.charWidth('0') * 3F;
            try {
                bounds.x += ind;
                if (x > bounds.x - ind) {
                    toLineStart();
                } else {
                    x = bounds.x;
                }
                float oldY = y;
                Void result = super.visitBlockquote(ctx);
                float newY = maxY;
                return withColor(props.blockquoteSidebarColor(), () -> {
                    Rectangle2D.Float r = new Rectangle2D.Float();
                    float w = ind / 4;
//                    r.setFrame(origX, oldY, w, newY - oldY);
                    r.setFrame(origX, oldY, w, lastTextBottom - oldY);
                    renderer.withGraphics(g -> {
                        g.fill(r);
                        g.draw(r);
                    });
//                    addToBounds(r);
                    toLineStart();
                    return result;
                });
            } finally {
                bounds.x = origX;
            }
        });
    }

    private <T> T outsideRoot(Supplier<T> s) {
        boolean old = inNestedContent;
        inNestedContent = true;
        try {
            return s.get();
        } finally {
            inNestedContent = old;
        }
    }

    @Override
    public Void visitHeading(MarkdownParser.HeadingContext ctx) {
        return outsideRoot(() -> {
            boolean top = y == bounds.y;
            if (!top) {
                y += renderer.getFontMetrics().getHeight() / 2F;
            }
            Token hhc = ctx.head;
            TerminalNode content = ctx.headingContent().HeadingContent();
            if (hhc != null && content != null) {
                if (!top || x != bounds.x) {
                    toLineStart();
                }
                Font f = renderer.getFont();
                int hl = getHeadingLevel(hhc.getText());
                double scale = 1 + (hl * 0.25);
                f = f.deriveFont(AffineTransform.getScaleInstance(scale, scale));
                return withFont(f, () -> {
                    drawWords(ctx.body.getText().trim());
                    Void result = super.visitHeading(ctx);
                    toLineStart();
                    return result;
                });
            }
            return super.visitHeading(ctx);
        });
    }

    private <T> T setListItemIndent(float ind, Supplier<T> supp) {
        if (listItemIndent != null) {
            listItemIndent.setIndent(ind);
            return supp.get();
        } else {
            return supp.get();
        }
    }

    private <T> T usingListItemIndent(Supplier<T> supp) {
        if (listItemIndent != null) {
            return listItemIndent.withIndent(supp);
        }
        return supp.get();
    }
    private ListItemIndent listItemIndent;

    class ListItemIndent {

        private float indent;
        private boolean reentry;
        Rectangle2D.Float origBounds = new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height);

        void setIndent(float indent) {
            this.indent = indent;
        }

        <T> T withIndent(Supplier<T> run) {
            if (reentry) {
                return run.get();
            }
            reentry = true;
            try {
                float oldBx = bounds.x;
                float oldW = bounds.width;
                bounds.x += indent;
                bounds.width -= indent;
                if (x < bounds.x) {
                    x = bounds.x;
                }
                //                x -= indent;
                try {
                    return run.get();
                } finally {
                    bounds.x = oldBx;
                    bounds.width = oldW;
                }
            } finally {
                reentry = false;
            }
        }

        void close() {
            bounds.setFrame(origBounds);
        }
    }

    private <T> T withIndent(Supplier<T> supp) {
        ListItemIndent old = listItemIndent;
        ListItemIndent nue = new ListItemIndent();
        this.listItemIndent = nue;
        try {
            return supp.get();
        } finally {
            nue.close();
            this.listItemIndent = old;
        }
    }

    @Override
    public Void visitUnorderedList(MarkdownParser.UnorderedListContext ctx) {
        return outsideRoot(() -> {
            return withIndent(() -> {
                // The first list items head is the indication to the parser that
                // we are starting a list, so it belongs to the list itself, not
                // to the list item.  Weird but necessary.
                setupListIndentFromHead(ctx.head);
                return super.visitUnorderedList(ctx);
            });
        });
    }

    private int currIndentLevel;

    private void setupListIndentFromHead(MarkdownParser.UnorderedListItemHeadContext head) {
        if (head != null) {
            int leadingSpaces = countLeadingSpaces(head.getText());
            currIndentLevel = Math.max(1, leadingSpaces / 2);
            int indentPosition = currIndentLevel * 2;
            int indentBy = Math.max(1, indentPosition);
            float indentPx = indentPixels(indentBy);
            setListItemIndent(indentPx, () -> {
                return null;
            });
        }
    }

    @Override
    public Void visitUnorderedListItemHead(MarkdownParser.UnorderedListItemHeadContext ctx) {
        return usingListItemIndent(() -> {
            toLineStart();
            float outdent = indentPixels(1.5F);
            drawBullet(x - outdent);
            return null;
        });
    }

    @Override
    public Void visitUnorderedListItem(MarkdownParser.UnorderedListItemContext ctx) {
        MarkdownParser.UnorderedListItemHeadContext head = ctx.head;
        setupListIndentFromHead(ctx.head);
        return usingListItemIndent(() -> {
            toLineStart();
            return super.visitUnorderedListItem(ctx);
        });
        //            return super.visitUnorderedListItem(ctx);
    }
    private final Ellipse2D.Float ell = new Ellipse2D.Float();

    private void drawBullet(float x) {
        FontMetrics fm = renderer.getFontMetrics();
        float chw = fm.charWidth('O');
        float sz = chw * 0.625F;
        float height = fm.getHeight();
        ell.width = ell.height = sz;
        ell.x = x;
        ell.y = y + (height / 2) - (sz / 2) - 1;
        renderer.withGraphics(g -> {
            Shape shape = props.bulletShape(currIndentLevel, ell.x, ell.y, sz);
            if (props.isBulletShapeFilled(currIndentLevel)) {
                g.fill(shape);
            }
            g.setStroke(new BasicStroke(1));
            g.draw(ell);
        });
        // Compensate for the stroke
        ell.x -= 0.5;
        ell.y -= 0.5;
        ell.width += 1;
        ell.height += 1;
        addToBounds(ell.getBounds2D());
    }

    private int getHeadingLevel(String text) {
        int count = countLeadingPounds(text);
        int result = Math.max(1, 5 - count);
        return result;
    }

    private int countLeadingPounds(String text) {
        int count = 0;
        loop:
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '#':
                    count++;
                    break;
                default:
                    if (Character.isWhitespace(c)) {
                        if (count > 0) {
                            break loop;
                        }
                        continue;
                    }
                    break;
            }
        }
        return count;
    }

    private int countLeadingSpaces(String text) {
        int leadingNewlines = 0;
        for (int i = 0; i < text.length(); i++) {
            switch (text.charAt(i)) {
                case '\n':
                    leadingNewlines++;
                    continue;
                case ' ':
                case '\t':
                    // XXX
                    continue;
                default:
                    return i - leadingNewlines;
            }
        }
        return 0;
    }

    @Override
    public Void visitWhitespace(MarkdownParser.WhitespaceContext ctx) {
        drawWords(ctx.getText());
        return super.visitWhitespace(ctx);
    }

    @Override
    public Void visitText(MarkdownParser.TextContext ctx) {
        drawWords(ctx.getText());
        return super.visitText(ctx);
    }

    @Override
    public Void visitItalic(MarkdownParser.ItalicContext ctx) {
        Font f = renderer.getFont();
        return withFont(f.deriveFont(f.getStyle() | Font.ITALIC), () -> {
            return super.visitItalic(ctx);
        });
    }

    @Override
    public Void visitCode(MarkdownParser.CodeContext ctx) {
        Font f = renderer.getFont();
        Font nue = new Font("Courier New", f.getStyle(), f.getSize());
        AffineTransform xf = f.getTransform();
        if (xf != null && !xf.isIdentity()) {
            nue = nue.deriveFont(xf);
        }
        return withFont(nue, () -> {
            return super.visitCode(ctx);
        });
    }

    @Override
    public Void visitStrikethrough(MarkdownParser.StrikethroughContext ctx) {
        return withStrikethrough(() -> super.visitStrikethrough(ctx));
    }

    @Override
    public Void visitBold(MarkdownParser.BoldContext ctx) {
        Font f = renderer.getFont();
        return withFont(f.deriveFont(f.getStyle() | Font.BOLD), () -> {
            return super.visitBold(ctx);
        });
    }
    private List<LinkImpl> links = new ArrayList<>();

    @Override
    public Void visitLink(MarkdownParser.LinkContext ctx) {
        return withColor(props.linkColor(), () -> {
            currentLinkBounds = new RectangleCollection();
            Void result = super.visitLink(ctx);
            if (!currentLinkBounds.isEmpty()) {
                links.add(new LinkImpl(currentLinkBounds, ctx.href.href.getText()));
            }
            currentLinkBounds = null;
            return result;
        });
    }

    List<LinkImpl> links() {
        return new ArrayList<>(links);
    }

    private float indentPixels(float characters) {
        FontMetrics fm = renderer.getFontMetrics();
        float w = fm.charWidth('0');
        float lead = fm.getLeading();
        return (w + lead) * characters;
    }

    private float indent(int characters) {
        toLineStart();
        //            int w = g.getFontMetrics().charWidth('0');
        //            float indentPx = w * amount;
        float indentPx = indentPixels(characters);
        x += indentPx;
        return indentPx;
    }

    private void drawWords(String text) {
        StringBuilder curr = new StringBuilder(text.length());
        char last = (char) 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (curr.length() > 0 && Character.isWhitespace(c) && !Character.isWhitespace(last)) {
                drawWord(curr.toString());
                curr.setLength(0);
            }
            switch (c) {
                case ' ':
                    if (last != c && !Character.isWhitespace(last)) {
                        moveOneSpace();
                    }
                    break;
                case '\t':
                    // XXX compute tab stops
                    boolean lastWs = Character.isWhitespace(last);
                    for (int j = 0; j < (lastWs ? 3 : 4); j++) {
                        moveOneSpace();
                    }
                    break;
                case '\r':
                    // ignore
                    continue;
                case '\n':
                    if (last != c && !Character.isWhitespace(last) && x != bounds.x) {
                        //                            toLineStart();
                        moveOneSpace();
                    }
                    break;
                default:
                    curr.append(c);
                    if (i == text.length() - 1) {
                        drawWord(curr.toString());
                    }
            }
            last = c;
        }
    }

    private boolean justDrewSpace = false;
    private void moveOneSpace() {
        if (justDrewSpace) {
            return;
        }
        x += spaceSpacing();
        if (x >= bounds.getMaxX()) {
            toLineStart();
        }
        justDrewSpace = true;
    }

    private static final Line2D.Float line = new Line2D.Float();
    private float lastTextBottom = Float.MIN_VALUE;

    private void drawWord(String text) {
        pristine = false;
        float width = width(text);
        if (x + width > bounds.width) {
            toLineStart();
        }
        LineMetrics mx = renderer.getLineMetrics(text);
        float textY = y + mx.getAscent() + baselineAdjust;
        withGraphics(g -> {
            g.drawString(text, x, textY);
        });
        if (currentLinkBounds != null) {
            currentLinkBounds.add(new Rectangle2D.Float(x, y, width, mx.getHeight() + mx.getDescent()));
        }
        float oldX = x;
        if (isStrikethrough()) {
            float strikeY = textY + mx.getStrikethroughOffset();
            float strikeSize = mx.getStrikethroughThickness();
            line.setLine(x, strikeY, x + width, strikeY);
            withGraphics(g -> {
                g.setStroke(new BasicStroke(strikeSize));
                g.draw(line);
            });
            addToBounds(line.x1, line.y1);
            addToBounds(line.x2, line.y2);
        }
        x += width;
//        addToBounds(oldX, y, width, mx.getHeight() + mx.getDescent() + mx.getLeading() + baselineAdjust);
        lastTextBottom = y + mx.getHeight() + mx.getDescent() + mx.getLeading() + baselineAdjust;
        addToBounds(oldX, y, width, (textY + mx.getDescent()));
        justDrewSpace = false;
//        System.out.println("ATB " + oldX + ", " + " width  " + width + " endX " + (oldX + width)
//                + " for '" + escape(text) + "' -> " + usedBounds().width);
//        System.out.println("ATB " + oldX + ", " + y + "   " + width + " * "
//                + (mx.getHeight() + mx.getDescent() + mx.getLeading())
//                + " for '" + escape(text) + "' -> " + usedBounds().width + " x " + usedBounds().height);
    }

    private void addToBounds(float x, float y, float w, float h) {
        if (w < 0 || h < 0) {
            throw new IllegalArgumentException(x + ", " + y + " " + w + " x " + h);
        }
        minX = Math.min(minX, x);
        minY = Math.min(minY, y);
        maxX = Math.max(maxX, x + w);
        maxY = Math.max(maxY, y + h);
//        System.out.println("minX " + minX + " maxX " + maxX
//                + " for " + x + " " + w + " x "
//                + " used width now " + (maxX - minX) + " of " + bounds.width);
//        System.out.println("minX " + minX + " minY " + minY + " maxX " + maxX
//                + " maxY " + maxY + " for " + x + ", " + y + " " + w + " x "
//                + h + " used size now " + (maxX - minX) + " x " + (maxY - minY));
    }

    private void addToBounds(Rectangle2D r) {
//        addToBounds((float) r.getX(), (float) r.getY(), (float) r.getWidth(), (float) r.getHeight());
        minX = (float) Math.min(minX, r.getMinX());
        maxX = (float) Math.max(maxX, r.getMaxX());
        minY = (float) Math.min(minY, r.getMinY());
        maxY = (float) Math.max(maxY, r.getMaxY());
    }

    private void addToBounds(float x, float y) {
        minX = Math.min(minX, x);
        maxX = Math.max(maxX, x);
        minY = Math.min(minY, y);
        maxY = Math.max(maxY, y);
    }

    private float width(String s) {
        return renderer.getFontMetrics().stringWidth(s);
    }

    private float spaceSpacing() {
        FontMetrics fm = renderer.getFontMetrics();
        return fm.charWidth(' ');
    }

    private boolean toLineStart() {
        if (pristine) {
            // no leading newlines
            return false;
        }
        if (x != bounds.x) {
            x = bounds.x;
            FontMetrics fm = renderer.getFontMetrics();
            y += fm.getHeight() + fm.getDescent() + fm.getLeading();
            return true;
        }
        return false;
    }

    private boolean isStrikethrough() {
        return strikeCount > 0;
    }

    private <T> T withStrikethrough(Supplier<T> run) {
        strikeCount++;
        try {
            return run.get();
        } finally {
            strikeCount--;
        }
    }

    private <T> T withFont(Font font, Supplier<T> run) {
        Font old = renderer.getFont();
        renderer.setFont(font);
        float oldBaselineAdjust = baselineAdjust;
        FontMetrics fm = renderer.getFontMetrics();
        if (!font.getName().equals(old.getName())) {
            baselineAdjust = renderer.getFontMetrics(old).getAscent() - fm.getAscent();
        }
        try {
            return run.get();
        } finally {
            renderer.setFont(old);
            baselineAdjust = oldBaselineAdjust;
        }
    }

    private <T> T withColor(Color color, Supplier<T> run) {
        Object[] hold = new Object[1];
        boolean hasGraphics = renderer.withGraphics(g -> {
            Color old = g.getColor();
            g.setColor(color);
            try {
                hold[0] = run.get();
            } finally {
                g.setColor(old);
            }
        });
        if (hasGraphics) {
            return (T) hold[0];
        }
        return run.get();
    }

}
