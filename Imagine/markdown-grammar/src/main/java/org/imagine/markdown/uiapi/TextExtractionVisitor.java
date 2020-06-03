package org.imagine.markdown.uiapi;

import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.RuleNode;
import org.imagine.markdown.grammar.MarkdownParser;
import org.imagine.markdown.grammar.MarkdownParserBaseVisitor;

/**
 *
 * @author Tim Boudreau
 */
class TextExtractionVisitor extends MarkdownParserBaseVisitor<StringBuilder> {

    private final StringBuilder content = new StringBuilder(1024);
    private final boolean stopAfterFirstLine;

    TextExtractionVisitor(boolean stopAfterFirstLine) {
        this.stopAfterFirstLine = stopAfterFirstLine;
    }

    @Override
    public StringBuilder visitChildren(RuleNode node) {
        if (stopAfterFirstLine && content.length() > 0) {
            return content;
        }
        return super.visitChildren(node);
    }

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
    public StringBuilder visitHorizontalRule(MarkdownParser.HorizontalRuleContext ctx) {
        appendNewline();
        return super.visitHorizontalRule(ctx);
    }

    @Override
    public StringBuilder visitHeadingContent(MarkdownParser.HeadingContentContext ctx) {
        appendSpace();
        content.append(ctx.getText());
        return super.visitHeadingContent(ctx);
    }

    @Override
    public StringBuilder visitParaBreak(MarkdownParser.ParaBreakContext ctx) {
        appendNewline();
        return super.visitParaBreak(ctx);
    }

    @Override
    public StringBuilder visitErrorNode(ErrorNode node) {
        content.append(node.getText());
        return super.visitErrorNode(node);
    }

    @Override
    public StringBuilder visitWhitespace(MarkdownParser.WhitespaceContext ctx) {
        String txt = ctx.getText().trim();
        if (!txt.isEmpty()) {
            // Currently a few lexer mode switches wind up prepending a
            // character to what is otherwise a whitespace element
            if (Character.isWhitespace(ctx.getText().charAt(0))) {
                appendSpace();
            }
            content.append(txt);
        }
        return super.visitWhitespace(ctx);
    }

    @Override
    public StringBuilder visitDocument(MarkdownParser.DocumentContext ctx) {
        content.setLength(0);
        return super.visitDocument(ctx);
    }
}
