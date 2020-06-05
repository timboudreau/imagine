package org.imagine.markdown.uiapi;

import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
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
    private final Set<Consumer<RegionOfInterest>> linkListeners = new HashSet<>();

    public Markdown(String text) {
        this.streamSupplier = () -> CharStreams.fromString(text);
    }

    public Markdown(CharStream stream) {
        this.streamSupplier = () -> stream;
    }

    public Markdown(Supplier<CharStream> supplier) {
        this.streamSupplier = supplier;
    }

    public Markdown addLinkListener(Consumer<RegionOfInterest> listener) {
        linkListeners.add(listener);
        return this;
    }

    public Markdown removeLinkListener(Consumer<RegionOfInterest> listener) {
        linkListeners.remove(listener);
        return this;
    }

    public Markdown checkErrors(ErrorConsumer c) {
        new ErrorChecker(streamSupplier).checkForErrors(c);
        return this;
    }

    public String rawMarkup() {
        return ErrorChecker.readCharStream(streamSupplier.get());
    }

    public String probableHeadingLine() {
        TextExtractionVisitor tev = new TextExtractionVisitor(true);
        return parser().document().accept(tev).toString().trim();
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
        return render(ctx, props, bounds, EmbeddedImageLoader.EMPTY);
    }

    public Rectangle2D.Float render(MarkdownRenderingContext ctx, MarkdownUIProperties props, Rectangle bounds, EmbeddedImageLoader ldr) {
        return EmbeddedImageCache.open(this, cache -> {
            RenderVisitor rv = new RenderVisitor(ctx, props, new Rectangle2D.Float(bounds.x, bounds.y, bounds.width, bounds.height), ldr, cache);
            parser().document().accept(rv);
            sendLinks(rv);
            return rv.usedBounds();
        });
    }

    private void sendLinks(RenderVisitor rv) {
        if (!linkListeners.isEmpty()) {
            for (RegionOfInterest link : rv.links()) {
                for (Consumer<RegionOfInterest> listener : linkListeners) {
                    listener.accept(link);
                }
            }
        }
    }

    public String extractPlainText() {
        TextExtractionVisitor tev = new TextExtractionVisitor(false);
        return parser().document().accept(tev).toString().trim();
    }

    public static void main(String[] args) {
        JFrame jf = new JFrame();
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        Markdown md = new Markdown(THE_README_DRAFT);
        MarkdownComponent c = new MarkdownComponent(md, false);
        c.setMargin(18);
        c.setLinkListener(link -> {
            JOptionPane.showMessageDialog(c, link, "Link Clicked", JOptionPane.INFORMATION_MESSAGE);
        });
        c.setUIProperties(c.getUIProperties().withFont(new Font("Times New Roman", Font.PLAIN, 26)).paintingCaptions()
                .withMaximumInlineImageDimension(300));
        jf.setContentPane(new JScrollPane(c));
        jf.pack();
        jf.setVisible(true);
        jf.pack();
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

    private static final String RADIAL_GRADIENT = "# Radial Gradient Customizer\n\nA radial gradient is a "
            + "sequence of multiple colors which is (optionally) repeated in concentric circles radianting out "
            + "from a central point, with a defined radius and focus point.\n\nThe customizer shows point-selector "
            + "control which mirrors the aspect ratio of the picture being edited.  Press and drag with the mouse "
            + "to draw the initial point andfocus point (creating an effect as if you were viewing the colored "
            + "circles at an angle).\n\nRadial gradients can be used to produce complex, interesting fill patters "
            + "simply and in a way that is well supported by SVG rendering engines.\n\n## Adding Points\n\nDouble "
            + "click in the gradient designer to add a new color-stop, or drag existing stops to move them; each has "
            + "a color-chooser below it which can be used to change the color.\n\nInternally, a radial gradient is defined "
            + "by a collection of colors and frationalvalues between zero and one - percentages of the spread between "
            + "the start and end  points of the gradient at which colors change.\n\nThe *Adjust Colors* button allows "
            + "you to change the palette of all colors in the gradient at once, adjusting all of their hue, saturation "
            + "or brightness at once.";

    private static final String PATH_TOOL = "# Path Tool\n\nClick to create the first point of the shape.  "
            + "For subsequent points, holding down *CTRL* will create a _quadratic_ curve, and *SHIFT* will "
            + "create a _cubic_ curve.  \n\nNow an image ![embedded 1](lres:/testit-2.png) or so.\n\n"
            + "And let's see if parenthesized things (which might be ignored if we messed up the rules) and "
            + "things in brackets [which also could get mangled] are working okay, plus it gives us a chance "
            + "to ![embedded 1](lres:/testit.png) embed another image, which would be nice.\n\n"
            + "And here is a web image ![CGit](https://timboudreau.com/cgit.png) - how you like them apples?";

    private static final String TEST_ORDERED_LIST = "# Some Ordered Lists\n\nThis is an ordered list:\n\n"
            + " 1. Turn off the alarm clock\n"
            + " 2. Read email on phone in bed\n"
            + " 3. Do morning ablutions:\n"
            + "    1. Toilet\n"
            + "    2. Wash face\n"
            + "    3. Shave\n"
            + "      4. Get out razor\n"
            + "      5. Wet it\n"
            + "      6. Don't use shaving cream\n"
            + "      7. Scrape face\n"
            + "        7. Use stiptic\n"
            + "        7. Apply bandage\n"
            + "    4. Shower\n"
            + " 5. Go downstairs\n"
            + " 6. Make coffee\n"
            + " 7. Mindlessly watch news while coffee works\n\n"
            + "And that's how we do it, eh?\n\n";

    private static final String TEST_UNORDERED_LIST = "# An Unordered List\n\nThis is an unordered list\n\n"
            + " * Thing One\n"
            + " * Thing Two\n"
            + "   * Nested Thing One\n"
            + "   * Nested Thing Two\n"
            + "     * Nested Nested Thing One\n"
            + "     * Nested Nested Thing Two\n"
            + "   * Thing Three\n"
            + "   * Thing Four\n"
            + "     * Another Nested Thing One\n"
            + " * Thing Last\n";

    private static final String TEST_PREFORMATTED = "This will be some preformatted text.\n\n"
            + "```And here we go.\n\n"
            + "    private <T> T inEmbeddedImage(Supplier<T> supp) {\n"
            + "        boolean old = inEmbeddedImage;\n"
            + "        inEmbeddedImage = true;\n"
            + "        try {\n"
            + "            return supp.get();\n"
            + "        } finally {\n"
            + "            inEmbeddedImage = old;\n"
            + "        }\n"
            + "    }"
            + "This should render exactly.```\n\n"
            + "And now back to our regularly scheduled programming.";

    private static final String THE_README_DRAFT = "# Markdown Grammar\n"
            + "\n"
            + "This is a simple Antlr grammar and supporting classes for parsing Markdown, which is used\n"
            + "to generate help files from annotations.  It does not pretend or even attempt to support the\n"
            + "full Markdown syntax, just those features that are needed for creating help files for this\n"
            + "project, as they become needed.\n"
            + "\n"
            + "Having done a review of available Markdown Antlr grammars available, everything\n"
            + "I found was either, slow, awful or both.  So, when in doubt, make the wheel _rounder_.\n"
            + "\n"
            + "While it's not the end-all and be-all of Markdown parsers, it is sufficient for its\n"
            + "current uses, and perhaps the bones of what could become a really _good_ Markdown parser.\n"
            + "\n"
            + "Currently supported:\n"
            + "\n"
            + " * Paragraphs\n"
            + " * Unordered Lists, with nesting\n"
            + " * Ordered Lists, with nesting\n"
            + " * Boldface / strong (asterisk)\n"
            + " * Strikethrough\n"
            + " * Italic / emphasis (underscore)\n"
            + " * Links\n"
            + " * Embedded Images\n"
            + "\n"
            + "# UI Support\n"
            + "\n"
            + "The project include UI components for parsing and rendering markdown content "
            + "_directly to the screen_ with no intervening heavyweight browser or other technologies - \n"
            + "just parse and give painting instructions to a `Graphics2D`.  There's a bit of flexibility\n"
            + "around colors and fonts via `MarkdownUIProperties`.  `MarkdownComponent` encapsulates all\n"
            + "of this as a simple Swing component.  Under the hood, computing preferred sizes means\n"
            + "rendering a `Markdown` instance once passing a `PrerenderContext` which doesn't really\n"
            + "write to a `Graphics2D`, just computes the bounds it wants to paint, constrained by a\n"
            + "rectangle you provide, or the `GraphicsEnvironment`'s screen bounds.\n"
            + "\n"
            + "# Lexer and Grammar Design\n"
            + "\n"
            + "Markdown, as with many formats for human, not computer languages, is hard for \n"
            + "machines to parse, and subject to ambiguity.\n"
            + "\n"
            + "The grammar here takes advantage of Antlr's *lexical modes* - these are typically used for\n"
            + "what the Terence Parr's book calls _island grammars_ - nested languages within a language.\n"
            + "\n"
            + "Here, we are using them a little bit differently.  The `mode`, `pushMode` and `popMode` lexer\n"
            + "rule directives allow one to switch out what set of rules the lexer is using to recognize\n"
            + "text. In general, the things that Markdown does involve one of two patterns: `$DELIMITER some lines of text`\n"
            + "(such as a heading or list item) or `$DELIMITER some text $DELIMITER` (such as boldface).\n"
            + "\n"
            + "Lexers don't do nesting, they just tokenize.  Parsers do do nesting.\n"
            + "\n"
            + "For the second category, bi-delimited stuff, we can handle that easily in the parser - just\n"
            + "have a token for the delimiter and a rule that nests anything between those delimiters.\n"
            + "\n"
            + "For the first category, that's where lexical modes come in handy.\n"
            + "\n"
            + "So, in our grammar, almost _nothing_ is handled in the default mode.  Any lexer rule in another\n"
            + "mode that hands control back to the default mode must do so at the end of a line of text, so the\n"
            + "default mode _only_ deals with the first characters on a line, and only to decide what mode\n"
            + "to enter - it just hands off control to the appropriate mode - a leading `POUND WHITESPACE`\n"
            + "means go into heading mode, a leading `WHITESPACE ASTRISK WHITESPACE` means go into _LIST_\n"
            + "list mode, a leading `WHITESPACE+ DIGIT DOT WHITESPACE` means go into _ORDERED_LIST_ mode,\n"
            + "and other text means go into _PARAGRAPH_ mode (where almost all text is processed).\n"
            + "\n"
            + "The `more` directive for lexer rules is the key here:  You can parse some text in lexer rule\n"
            + "_A_, realize you need to go into a different mode, and _hand the text you already parsed into\n"
            + "the first rule in that mode_. Now, the only caveat to this is that you can get some surprises - \n"
            + "like characters in your whitespace tokens, because you passed those characters into a mode,\n"
            + "and it just blindly prepended them to the whitespace that was the first thing recognized.  So\n"
            + "that's something to be aware of when making modifications.\n"
            + "\n"
            + "But modes mean we can have unique named lexer rules that say \"this is the beginning of a list\"\n"
            + "which the parser grammar can recognize as the start of a rule.\n"
            + "\n"
            + "So a lot of this involves flipping between modes, often using the `more` keyword to pass in everything\n"
            + "that got recognized.  This allows us to have token rules that are essentially identical, but have\n"
            + "different names which the parser can use to identify what parser rule should be activated.\n"
            + "\n"
            + "For a simple example:\n"
            + "\n"
            + "```\n"
            + "OpenBulletList\n"
            + "    : INLINE_WHITESPACE+ ASTERISK -> more, pushMode ( LIST );\n"
            + "```\n"
            + "\n"
            + "in the default mode, tells the lexer it is seeing the opening of a bullet list.  In fact, this\n"
            + "rule will _only ever be used if the document starts with a bullet list_.\n"
            + "\n"
            + "In the `PARAGRAPH` mode (which does the textual heavy lifting for everything except headings and\n"
            + "preformatted text), is a very simiilar rule - and since most things land in paragraph mode after\n"
            + "processing some sort of prefix, this will almost always be the rule that initiates a list:\n"
            + "\n"
            + "```\n"
            + "ParaTransitionToBulletListItem\n"
            + "    : ( ParaDoubleNewline | NEWLINE ) INLINE_WHITESPACE+ ASTERISK -> more,\n"
            + "        mode ( LIST );\n"
            + "```\n"
            + "\n"
            + "Using `more` allows the leading spaces-asterisk sequence get re-processed in list mode as two\n"
            + "tokens:\n"
            + "\n"
            + "```\n"
            + "ListPrologue\n"
            + "    : ( DOUBLE_NEWLINE? INLINE_WHITESPACE )\n"
            + "    | ( ParaDoubleNewline | NEWLINE )? INLINE_WHITESPACE+ ASTERISK\n"
            + "        INLINE_WHITESPACE;\n"
            + "\n"
            + "ListItem\n"
            + "    : ( LETTER | DIGIT ) -> more, pushMode ( PARAGRAPH );\n"
            + "```\n"
            + "\n"
            + "and the parser recognizes the `ListPrologue` token as introducing another list item.  So the \n"
            + "entirety of the parser rule for lists is:\n"
            + "\n"
            + "```\n"
            + "unorderedList\n"
            + "    : head=unorderedListItemHead unorderedListItem+;\n"
            + "\n"
            + "unorderedListItemHead\n"
            + "    : head=NestedListItemHead? ListPrologue;\n"
            + "\n"
            + "unorderedListItem\n"
            + "    : head=unorderedListItemHead? paragraph;\n"
            + "```\n"
            + "";
}
