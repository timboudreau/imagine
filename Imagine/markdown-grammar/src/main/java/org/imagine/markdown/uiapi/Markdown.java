package org.imagine.markdown.uiapi;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.imagine.markdown.grammar.MarkdownLexer;
import org.imagine.markdown.grammar.MarkdownParser;

/**
 * Can render markdown to a Graphics2D.
 *
 * @author Tim Boudreau
 */
public final class Markdown {

    private final Supplier<CharStream> streamSupplier;
    private Set<Consumer<Link>> linkListeners = new HashSet<>();

    public Markdown(String text) {
        this.streamSupplier = () -> CharStreams.fromString(text);
    }

    public Markdown(CharStream stream) {
        this.streamSupplier = () -> stream;
    }

    public Markdown(Supplier<CharStream> supplier) {
        this.streamSupplier = supplier;
    }

    public Markdown addLinkListener(Consumer<Link> listener) {
        linkListeners.add(listener);
        return this;
    }

    public Markdown removeLinkListener(Consumer<Link> listener) {
        linkListeners.remove(listener);
        return this;
    }

    public Markdown checkErrors(ErrorConsumer c) {
        new ErrorChecker(streamSupplier).checkForErrors(c);
        return this;
    }

    public String probableHeadingLine() {
        String result = extractPlainText().trim();
        int ix = result.indexOf('\n');
        if (ix < 0) {
            return result;
        }
        return result.substring(0, ix);
    }

    private MarkdownParser parser() {
        MarkdownLexer lexer = new MarkdownLexer(streamSupplier.get());
        lexer.removeErrorListeners();
        CommonTokenStream cts = new CommonTokenStream(lexer);
        MarkdownParser parser = new MarkdownParser(cts);
        parser.removeErrorListeners();
        return parser;
    }

    public Rectangle2D.Float render(MarkdownRenderingContext ctx, MarkdownUIProperties props, Rectangle bounds) {
        RenderVisitor rv = new RenderVisitor(ctx, props, new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height));
        parser().document().accept(rv);
        sendLinks(rv);
        System.out.println("USED " + rv.usedBounds());
        return rv.usedBounds();
    }

    public Rectangle2D.Float render(Graphics2D g, MarkdownUIProperties props, Rectangle bounds) {
        RenderVisitor rv = new RenderVisitor(MarkdownRenderingContext.renderContext(g),
                props,
                new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height));
        parser().document().accept(rv);
        sendLinks(rv);
        return rv.usedBounds();
    }

    private void sendLinks(RenderVisitor rv) {
        if (!linkListeners.isEmpty()) {
            for (Link link : rv.links()) {
                for (Consumer<Link> listener : linkListeners) {
                    listener.accept(link);
                }
            }
        }
    }

    public String extractPlainText() {
        TextExtractionVisitor tev = new TextExtractionVisitor();
        return parser().document().accept(tev).toString();
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Markdown md = new Markdown(TEST_BLOCKQUOTE_SIMPLE);
        MarkdownComponent c = new MarkdownComponent(md, false);
        c.setLinkListener(link -> {
            JOptionPane.showMessageDialog(c, link, "Link Clicked", JOptionPane.INFORMATION_MESSAGE);
        });
        c.setUIProperties(c.getUIProperties().withFont(new Font("Times New Roman", Font.PLAIN, 26)));
        jf.setContentPane(c);
        jf.pack();
        jf.setVisible(true);
        jf.pack();

        System.out.println("AD:\n" + c.getAccessibleContext().getAccessibleDescription());
    }

    private static final String TEST_SIMPLE_WITH_MARKUP = "This is a simple paragraph that has"
            + " *bold text* and some _italic text_ and so forth.\n\n";

    private static final String TEST_SIMPLE_WITH_NESTED_MARKUP = "This is a simple paragraph that has"
            + " *bold text* and some _italic and *italic bold* text_ and so forth.\n\n";

    private static final String TEST_SIMPLE_WITH_MORE_NESTED_MARKUP = "This is a simple paragraph that has"
            + " *bold text* and some _italic and *italic ~~strikethrough `code`~~ bold* text_ and so forth.\n\n";

    private static final String TEST_ONE = "# Heading One\n\n"
            + "Words and *bold words* can _have italics_ and stuff"
            + " including nested *bold _italic `code ~~strikethrough~~`_* \n\n"
            + "## Heading two\n"
            + "> Let's try a blockquote and see what happens. Will it work?\n"
            + "It should be indented a bit and maybe have a bar next to it.\n\n"
            + "Another paragraph ~~has strikethrough~~ and ends *the doc* abruptly."
            + "\n\n### Heading three\n"
            + "And there's some stuff here."
            + "\n\n#### Heading four\n"
            + "\n\n##### Heading five\n"
            + "\n\nOr perhaps we'll have a `code section` and a list:"
            + "\n"
            + " * Item one\n"
            + "   * Item one sub one\n"
            + "   * Item one sub two\n"
            + " * Item two\n"
            + " * Item three\n"
            + " * Item four is it\n"
            + "\nAnd this should be a different paragraph, but that's a bug."
            + " But we [have links](http://timboudreau.com) and that's cool.\n\n\n"
            + "Woogle *the _hoogle booble_ and* plood\n\n\n";

    private static final String TEST_BLOCKQUOTE = "# Heading Before Blockquote\n\n"
            + "> This is a blockquote. Let's see how much stuff ends up in it.\n\n"
            + "And a regular paragraph here.\n\n"
            + " * And then a list"
            + " * Or something like that";

    private static final String TEST_BLOCKQUOTE_SIMPLE = "# A heading! Head head!\nPara para para!!\n\n"
            + "Let's para some more\n with random\n line breaks that should not affect formatting.\n"
            + "> This is a blockquote. Let's see `how much stuff` ends up in it. It might "
            + "be very long and wrap, so we need to be really sure that works too.\n\n"
            + "And a regular paragraph here.\n\n"
            + " * And then a list\n"
            + " * Or something like that\n"
            + "   * With a nested item or two\n"
            + "   * Maybe even we could have some fascinating, compled, lengthy, really super lengthy so that they have to wrap\n"
            + "      * Doubly nested items!\n"
            + "      * With [links](http://timboudreau.com), even!\n"
            + "      * Do commas work after [links that could have text long enough to wrap and should still have correct bounds even in that case so let's try that](http://timboudreau.com), you think!\n"
            + "          * Or even more doubly nested items!\n\n"
            + "            * Heck, let's keep going with the menu items\n\n"
            + " * And go back to the top!\n\n"
            + "Follow that with a horizontal rule. This line really shouldn't be part of the list, "
            + "but it may render as such.  Fix it, or are multi-paragraph lists good?\n\n"
            + " * Okay, lets' see if multiple bullets work\n\nAnd some text after.  Hmm?\n\n"
            + "----------\n\n"
            + "And _a final_ paragraph with some ~~strikethrough~~.\n\n"
            + "That's *all* folks!";
}
