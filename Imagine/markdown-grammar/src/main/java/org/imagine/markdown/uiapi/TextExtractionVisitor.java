package org.imagine.markdown.uiapi;

import org.imagine.markdown.grammar.MarkdownParser;
import org.imagine.markdown.grammar.MarkdownParserBaseVisitor;

/**
 *
 * @author Tim Boudreau
 */
class TextExtractionVisitor extends MarkdownParserBaseVisitor<StringBuilder> {

    private final StringBuilder content = new StringBuilder(1024);

    @Override
    protected StringBuilder defaultResult() {
        return content;
    }

    private void appendSpace() {
        if (content.length() > 0) {
            char last = content.charAt(content.length() - 1);
            if (!Character.isWhitespace(last)) {
                content.append(' ');
            }
        }
    }

    private void appendNewline() {
        if (content.length() > 0) {
            char last = content.charAt(content.length() - 1);
            if (!Character.isWhitespace(last)) {
                content.append('\n');
            }
        }
    }

    @Override
    public StringBuilder visitUnorderedList(MarkdownParser.UnorderedListContext ctx) {
        appendNewline();
        return super.visitUnorderedList(ctx);
    }

    @Override
    public StringBuilder visitUnorderedListItemHead(MarkdownParser.UnorderedListItemHeadContext ctx) {
        // ignore the *
        return null;
    }

    @Override
    public StringBuilder visitUnorderedListItem(MarkdownParser.UnorderedListItemContext ctx) {
        appendNewline();
        return super.visitUnorderedListItem(ctx);
    }

    @Override
    public StringBuilder visitText(MarkdownParser.TextContext ctx) {
        appendSpace();
        content.append(ctx.getText());
        return super.visitText(ctx);
    }

    @Override
    public StringBuilder visitParagraphs(MarkdownParser.ParagraphsContext ctx) {
        appendNewline();
        return super.visitParagraphs(ctx);
    }

    @Override
    public StringBuilder visitBlockquote(MarkdownParser.BlockquoteContext ctx) {
        appendNewline();
        return super.visitBlockquote(ctx);
    }

    @Override
    public StringBuilder visitHeading(MarkdownParser.HeadingContext ctx) {
        appendNewline();
        return super.visitHeading(ctx);
    }

    @Override
    public StringBuilder visitDocument(MarkdownParser.DocumentContext ctx) {
        content.setLength(0);
        return super.visitDocument(ctx);
    }
}
