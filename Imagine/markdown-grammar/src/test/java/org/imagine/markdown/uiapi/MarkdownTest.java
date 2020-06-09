package org.imagine.markdown.uiapi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CodePointCharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.atn.PredictionMode;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.imagine.markdown.grammar.MarkdownLexer;
import org.imagine.markdown.grammar.MarkdownParser;
import org.imagine.markdown.grammar.MarkdownParserBaseListener;
import org.imagine.markdown.grammar.MarkdownParserBaseVisitor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class MarkdownTest {

    private static final String TEST_ONE = "# Heading One\n\n"
            + "Words and *bold words* can _have italics_ and stuff\n\n"
            + "Another paragraph ~~has strikethrough~~ and ends the doc abruptly";

    private static final String TEST_LINK_AFTER_TEXT
            = "And [we can have links](http://example.com) too!\n\nSo `there` you go\n\n";

    private static final String TEST_LINK = "\n\nAnd [this is a link](http://example.com/link)\n\n";

    private static final String TEST_LIST = "# Heading of List\n\n"
            + " * The first item\n"
            + " * The second item\n"
            + " * The third item\n"
            + " * The fourth item\n"
            + "\n"
            + "And a paragraph of something `else` (not sure what) here\n"
            + "Can we end with a question mark?";

    private static final String TEST_BLOCKQUOTE = "# Heading Before Blockquote\n\n"
            + "> This is a blockquote. Let's see how much stuff ends up in it.\n\n"
            + "And a regular paragraph here.\n\n"
            + " * And then a list\n"
            + " * Or something like that\n";

    private static final String TEST_BLOCKQUOTE_SIMPLE = "# A heading! Head head!\nPara para para\n\n"
            + "Let's para some more\n with random\n line breaks that should not affect formatting.\n"
            + "> This is a blockquote. Let's see how much stuff ends up in it. It might "
            + "be very long and wrap, so we need to be _really sure_ that works too.\n\n"
            + "And a regular paragraph here.\n\n"
            + " * And then a list\n"
            + " * Or something like that\n"
            + "   * With a nested item or two\n"
            + "   * Maybe even we could have some fascinating, compled, lengthy\n"
            + "     * Doubly nested items!\n"
            + "     * With [links](http://timboudreau.com) even!\n"
            + "     * Do commas work after [links](http://timboudreau.com), you think!\n"
            + "     * Or even more doubly nested items!\n\n"
            + " * And go back to the top!\n\n"
            + "Follow that with a horizontal rule:\n\n"
            + "----------\n\n"
            + "And a final paragraph.";

    private static final String TEST_NESTED_MARKUP = "Hello *this is bold*"
            + " this is plain this is *bold _with nested italic_ so there*\n\n";

    private static final String TEST_JUST_PARAGRAPHS = "This is paragraph one.\n"
            + "This is paragraph two.\n"
            + "This is paraagraph three with double newline\n\n"
            + "This is paragraph four terminated with eof.";

    private static final String TEST_TEXT_WITH_PUNCTUATION = "This is some text - hyphens should be okay."
            + " And so should semicolons; that is why we defined PUNC2.  But what about this >= that?"
            + "\n\nOr some math, like 2 + 2 = 42?";

    private static final String TEST_SIMPLE_WITH_MARKUP = "This is a simple paragraph that has"
            + " *bold text* and some _italic text_ and so forth.\n\n";

    private static final String TEST_SIMPLE_WITH_NESTED_MARKUP = "This is a simple paragraph that has"
            + " *bold text* and some _italic and *italic bold* text_ and so forth.\n\n";

    private static final String TEST_SIMPLE_WITH_MORE_NESTED_MARKUP = "This is a simple paragraph that has"
            + " *bold text* and some _italic and *italic ~~strikethrough `code`~~ bold* text_ and so forth.\n\n";
    private static final String TEST_MUCH = "# Heading One\n\n"
            + "Words and *bold words* can _have italics_ and stuff\n\n"
            + "## Heading two\n"
            + "> Let's try a blockquote and see what happens. Will it work?"
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
            + "create a _cubic_curve_.  \n\nNow an image ![embedded 1](lres:/testit.png) or so";

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
    private static final String TEST_UNORDERED_LIST_2 = "# An Unordered List\n\nThis is an unordered list\n\n"
            + " * Thing 1\n"
            + " * Thing 2\n"
            + "   * Nested Thing 1\n"
            + "   * Nested Thing 2\n"
            + "     * Nested Nested Thing 1\n"
            + "     * Nested Nested Thing 2\n"
            + "   * Thing 3\n"
            + "   * Thing 4\n"
            + "     * Another Nested Thing 1\n"
            + " * Thing Last";

    private static final String TEST_LEAD_WITH_ORDERED_LIST = " 1. Hello\n 2. There\n    1. Nested One\n";
    private static final String TEST_LEAD_WITH_UNORDERED_LIST = " * Hello\n * There\n    * Nested One\n";

    private static final String TEST_IMAGE_LINK = "This is a paragraph with some content. "
            + "![and an image](http://foo.example/img.png) stuck in there.";

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

    private static final String CAPS_ITA = "So, in our grammar, almost _nothing_ is handled in the default mode.  Any lexer rule in another\n"
            + "mode that hands control back to the default mode must do so at the end of a line of text, so the\n"
            + "default mode _only_ deals with the first characters on a line, and only to decide what mode\n"
            + "to enter - it just hands off control to the appropriate mode - a leading `POUND WHITESPACE`\n"
            + "means go into heading mode, a leading `WHITESPACE ASTRISK WHITESPACE` means go into _LIST_\n"
            + "list mode, a leading `WHITESPACE+ DIGIT DOT WHITESPACE` means go into _ORDERED_LIST_ mode,\n"
            + "and other text means go into _PARAGRAPH_ mode (where almost all text is processed).\n"
            + "\n";

    private static final String THE_README_DRAFT = "# Markdown Grammar\n"
            + "\n"
            + "This is a simple Antlr grammar and supporting classes for parsing \n"
            + "[Markdown](https://daringfireball.net/projects/markdown/syntax), which is used\n"
            + "to generate help content from Java annotations.  \n"
            + "\n"
            + "This project does not pretend or even attempt to support the\n"
            + "full Markdown syntax, just those features that are needed for creating help files for this\n"
            + "project, as they become needed.\n"
            + "\n"
            + "Having done a review of available Markdown Antlr grammars available, everything\n"
            + "I found was either, slow, awful or both.  So, when in doubt, make the wheel _rounder_.\n"
            + "\n"
            + "While it's not the end-all and be-all of Markdown parsers, it is sufficient for its\n"
            + "current uses, and perhaps the bones of what could become a really _good_ Markdown parser.\n"
            + "\n"
            + "## Currently supported:\n"
            + "\n"
            + " * Paragraphs\n"
            + " * Unordered Lists, with nesting\n"
            + " * Ordered Lists, with nesting\n"
            + " * Boldface / strong (asterisk)\n"
            + " * Strikethrough\n"
            + " * Italic / emphasis (underscore)\n"
            + " * Code blocks\n"
            + " * Preformatted text\n"
            + " * Hyperlinks\n"
            + " * Embedded Images\n"
            + "\n"
            + "## Currently unsupported:\n"
            + "\n"
            + " * H1 and H2 headings using the underline style\n"
            + " * Nested blockquotes\n"
            + " * Unusual mixes of text and uncommon punctuation, particularly punctuation that has semantic meaning in Markdown used as the leading character in a word\n"
            + " * Multi-paragraph list items\n"
            + " * Tables\n"
            + " * Code blocks within list items\n"
            + " * Reference style links\n"
            + " * Link definitions\n"
            + " * Escaping (asterisks, backticks, etc.)\n"
            + "\n"
            + "Some of these would likely be trivial to implement.\n"
            + "\n"
            + "## Never to be supported:\n"
            + "\n"
            + " * Inline HTML - we're not writing a browser here\n"
            + "\n"
            + "\n"
            + "\n"
            + "# UI Support\n"
            + "\n"
            + "The project include UI components for parsing and rendering markdown content\n"
            + "_directly to the screen_, with no intervening heavyweight browser or other technologies - \n"
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
            + "\n"
            + "In general the pattern is:\n"
            + "\n"
            + " 1. Top level lexer rule matches a character pattern that begins a line and indicates a particular kind of markup\n"
            + " 2. That rule dumps the lexer into a mode specific for that type of markup (horizontal rule, ordered list, unordered list, paragraph), usually using `more` to hand the matched text into the new lexer mode\n"
            + " 3. The mode has a specific named prologue token that the parser can use for matching\n"
            + " 4. In most cases, the mode matches the prologue, then dumps the lexer into `PARAGRAPH` mode which handles text, and is the main thing involved in rendering\n"
            + " 5. For things that repeat, such as list items, `PARAGRAPH` mode has similar rules that dump the lexer back into the mode that spawned it, for repeating items\n"
            + " 6. Terminating a paragraph where no other mode should be entered dumps the parser back out into default mode\n"
            + "\n"
            + "There's a little bit of lexical predicate black magic to facilitate ordered list items and detect changes \n"
            + "in indent levels as signals to open or close a sublist.  But other than that, once you get the hang of thinking\n"
            + "about Antlr grammars this way (using lexical modes not for island grammars, but as a way to have a bunch of tokens that\n"
            + "match the same, or nearly the same thing, in ways the parser can differentiate without doing anything terribly exciting),\n"
            + "it works pretty well - and far less torturously than some other grammars I've seen.\n";

//    @Test
    public void testCapsIta() {
        testBoth(CAPS_ITA);
    }

//    @Test
    public void testReadme() {
        testBoth(THE_README_DRAFT);
    }

//    @Test
    public void testPreformatted() {
        testBoth(TEST_PREFORMATTED);
    }

//    @Test
    public void testImageLink() {
        testBoth(PATH_TOOL);
    }

//    @Test
    public void testOrderedList() {
        testBoth(TEST_ORDERED_LIST);
    }

//    @Test
    public void testUnorderedList() {
        testBoth(TEST_UNORDERED_LIST);
    }

    @Test
    public void testUnorderedList2() {
        testBoth(TEST_UNORDERED_LIST_2, true);
    }

//    @Test
    public void testLeadWithOrderedList() {
        testBoth(TEST_LEAD_WITH_ORDERED_LIST);
    }

//    @Test
    public void testLeadWithUnorderedList() {
        testBoth(TEST_LEAD_WITH_UNORDERED_LIST);
    }

//    @Test
    public void testTextExtraction() {
        Markdown md = new Markdown(RADIAL_GRADIENT);
        String hl = md.probableHeadingLine();
        assertEquals("Radial Gradient Customizer", hl);
        String extracted = md.extractPlainText();
        String expectedExtracted = "Radial Gradient Customizer\n"
                + "A radial gradient is a sequence of multiple colors which is optionally repeated in concentric circles radianting out from a central point, with a defined radius and focus point.\n"
                + "The customizer shows point-selector control which mirrors the aspect ratio of the picture being edited.  Press and drag with the mouse to draw the initial point andfocus point creating an effect as if you were viewing the colored circles at an angle\n"
                + "Radial gradients can be used to produce complex, interesting fill patters simply and in a way that is well supported by SVG rendering engines.\n"
                + "Adding Points\n"
                + "Double click in the gradient designer to add a new color-stop, or drag existing stops to move them; each has a color-chooser below it which can be used to change the color.\n"
                + "Internally, a radial gradient is defined by a collection of colors and frationalvalues between zero and one - percentages of the spread between the start and end  points of the gradient at which colors change.\n"
                + "The Adjust Colors button allows you to change the palette of all colors in the gradient at once, adjusting all of their hue, saturation or brightness at once.";
        assertEquals(expectedExtracted, extracted);
    }

//    @Test
    public void testRadialGradientWithEmbeddedImages() {
        testBoth(RADIAL_GRADIENT);
    }

//    @Test
    public void testOthers() {
        testBoth(TEST_BLOCKQUOTE_SIMPLE);
        testBoth(TEST_SIMPLE_WITH_NESTED_MARKUP);
        testBoth(TEST_NESTED_MARKUP);
        testBoth(TEST_MUCH);
        testBoth(TEST_ONE);
        testBoth(TEST_LINK_AFTER_TEXT);
        testBoth(TEST_LINK);
        testBoth(TEST_LIST);
        testBoth(TEST_BLOCKQUOTE);
        testBoth(PATH_TOOL);
    }

//    @Test
    public void testStuff() {
        String text = RADIAL_GRADIENT;
        AL[] als = new AL[1];
        MarkdownLexer lex = lexer(text, al -> als[0] = al);
        CTS cts = new CTS(lex);
        MarkdownParser parser = new MarkdownParser(cts);
        parser.removeErrorListeners();
        parser.addErrorListener(als[0]);
        parser.addParseListener(new MarkdownParserBaseListener() {
            @Override
            public void enterEveryRule(ParserRuleContext ctx) {
                log("ENTER RULE " + ctx.invokingState + " " + ctx.getClass().getSimpleName());
                super.enterEveryRule(ctx); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            public void exitDocument(MarkdownParser.DocumentContext ctx) {
                super.exitDocument(ctx); //To change body of generated methods, choose Tools | Templates.
                log("exit document");
            }

        });
        List<ParseTree> docChildren = parser.document().children;
        log("DocChildren: " + docChildren.size());
        for (ParseTree pt : docChildren) {
            log(" - " + pt.getClass().getName() + ": " + escape(pt.getText() + " kids " + pt.getChildCount()));
        }
        als[0].rethrow();
    }

    private static ThreadLocal<Boolean> log = ThreadLocal.withInitial(() -> Boolean.FALSE);

    static void log(String what) {
        if (log.get()) {
            System.out.println(what);
        }
    }

    private void testBoth(String text) {
        testBoth(text, false);
    }

    private void testBoth(String text, boolean log) {
        boolean wasLog = this.log.get();
        if (log) {
            this.log.set(true);
        }
        log("\n*************************");
        log(text);
        log("--------------------------------");
        testTokens(text, (ix, tok) -> {
            log("\nEMIT: " + ix + ". " + MarkdownLexer.VOCABULARY.getDisplayName(tok.getType()) + ": '" + escape(tok.getText()) + "'");
        });
        log("\n------------- PARSE ---------------\n");
        testParse(text);
        if (log) {
            this.log.set(wasLog);
        }
    }

    private static String escape(String txt) {
        if (txt == null) {
            return "<null>";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            switch (c) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\r':
                    sb.append("\\r");
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }

    private void testTokens(String text, BiConsumer<Integer, Token> c) {
        AL[] al = new AL[1];
        MarkdownLexer lex = lexer(text, a -> al[0] = a);
        int ix = 0;
        int oldMode = lex._mode;
        for (Token tok = lex.nextToken(); tok.getType() != -1; tok = lex.nextToken(), ix++) {
            int newMode = lex._mode;
            c.accept(ix, tok);
            if (oldMode != newMode) {
                String msg = modeName(oldMode) + " -> " + modeName(newMode) + " @ "
                        + tok.getLine() + ":" + tok.getCharPositionInLine();
                log(" --- " + msg + "\n");
            }
            oldMode = newMode;
            al[0].rethrow();
        }
    }

    private void testParse(String text) {
        AL[] als = new AL[1];
        MarkdownLexer lex = lexer(text, al -> als[0] = al);
        CTS cts = new CTS(lex);
        MarkdownParser parser = new MarkdownParser(cts);
        parser.removeErrorListeners();
//        parser.addErrorListener(new DiagnosticErrorListener());
        parser.addErrorListener(als[0]);
        parser.getInterpreter().setPredictionMode(PredictionMode.LL_EXACT_AMBIG_DETECTION);
        V v = new V(text);
//        parser.document().accept(new ListItemFixer());
        parser.document().accept(v);
        v.rethrow();
        als[0].rethrow();
    }

    private MarkdownLexer lexer(String text) {
        return lexer(text, null);
    }

    private MarkdownLexer lexer(String text, Consumer<AL> c) {
        CodePointCharStream stream = CharStreams.fromString(text, "source");
        MarkdownLexer lexer = new MarkdownLexer(stream);
        lexer.removeErrorListeners();
        AL al = new AL(text, lexer);
        lexer.addErrorListener(al);
        if (c != null) {
            c.accept(al);
        }
        return lexer;
    }

    static class V extends MarkdownParserBaseVisitor<Void> {

        private int depth;
        private final String text;

        private List<AssertionError> errors = new ArrayList<>();

        public V(String text) {
            this.text = text;
        }

        public void rethrow() {
            if (!errors.isEmpty()) {
                Iterator<AssertionError> it = errors.iterator();
                AssertionError first = it.next();
                while (it.hasNext()) {
                    first.addSuppressed(it.next());
                }
                throw first;
            }
        }

        @Override
        public Void visitErrorNode(ErrorNode node) {
            Token tok = node.getSymbol();
            String msg = indicateError(text, tok.getLine(), tok.getCharPositionInLine(),
                    "Error node '" + escape(node.getText()) + "' at source interval "
                    + node.getSourceInterval().a + ":" + node.getSourceInterval().b
                    + " " + node.toStringTree());
            log(msg);
            errors.add(new AssertionError(msg));
            return super.visitErrorNode(node);
        }

        @Override
        public Void visitChildren(RuleNode node) {
            char[] c = new char[depth * 2];
            Arrays.fill(c, ' ');
            log(new String(c) + node.getClass().getSimpleName() + "\t'" + escape(node.getText()) + "'");
            depth++;
            super.visitChildren(node);
            depth--;
            return null;
        }

    }

    static String indicateError(String text, int line, int pos, String msg) {
        StringBuilder sb = new StringBuilder("Error node:").append('\n');
        String[] lines = text.split("\n");
        for (int i = 0; i < lines.length; i++) {
            sb.append(lines[i]).append('\n');
            if (i + 1 == line) {
                char[] in = new char[pos];
                Arrays.fill(in, ' ');
                sb.append(in).append("^ ").append(msg).append('\n');
            }
        }
        return sb.toString();
    }

    static class AL implements ANTLRErrorListener {

        private final String text;
        private final MarkdownLexer lex;
        private final List<AssertionError> errors = new ArrayList<>();

        public AL(String text, MarkdownLexer lex) {
            this.text = text;
            this.lex = lex;
        }

        String currentMode() {
            int md = lex._mode;
            return md < 0 ? "<none>" : MarkdownLexer.modeNames[md];
        }

        private String indicateError(int line, int pos, String msg) {
            StringBuilder sb = new StringBuilder("Syntax Error in mode ").append(currentMode())
                    .append(" at ").append(line).append(':').append(pos).append('\n');
            String[] lines = text.split("\n");
            for (int i = 0; i < lines.length; i++) {
                sb.append(lines[i]).append('\n');
                if (i + 1 == line) {
                    char[] in = new char[pos];
                    Arrays.fill(in, ' ');
                    sb.append(in).append("^ ").append(msg).append('\n');
                }
            }
            return sb.toString();
        }

        @Override
        public void syntaxError(Recognizer<?, ?> rcgnzr, Object o, int i, int i1, String string, RecognitionException re) {
            String msg = indicateError(i, i1, o + " " + i + " / " + i1 + " " + string + " / " + re);
            AssertionError err = new AssertionError(msg);
            if (re != null) {
                err.initCause(re);
            }
            errors.add(err);
//            fail(msg);
        }

        void rethrow() {
            if (errors.isEmpty()) {
                return;
            }
            if (errors.size() == 1) {
                throw errors.get(0);
            }
            Iterator<AssertionError> it = errors.iterator();
            AssertionError first = it.next();
            while (it.hasNext()) {
                first.addSuppressed(it.next());
            }
            throw first;
        }

        @Override
        public void reportAmbiguity(Parser parser, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet conflictingAlternatives, ATNConfigSet atncs) {
            if (conflictingAlternatives == null) {
                conflictingAlternatives = atncs.getAlts();
            }
            StringBuilder alternatives = new StringBuilder();
            for (int bit = conflictingAlternatives.nextSetBit(0); bit >= 0; bit = conflictingAlternatives.nextSetBit(bit + 1)) {
                if (alternatives.length() > 0) {
                    alternatives.append(", ");
                }
//                alternatives.append(MarkdownParser.ruleNames[bit]);
                alternatives.append(bit);
            }
            String desc = getDecisionDescription(parser, dfa);
            Token firstToken = parser.getTokenStream().get(startIndex);
            Token lastToken = parser.getTokenStream().get(stopIndex);

            String tokenText;
            if (firstToken == lastToken) {
                tokenText = firstToken.getText();
            } else {
                tokenText = firstToken.getText() + "..." + lastToken.getText();
            }
            tokenText = escape(tokenText);

            log("ambiguity " + desc + " line at " + firstToken.getLine() + ":" + firstToken.getCharPositionInLine()
                    + "'" + tokenText + "'"
                    + " (" + MarkdownLexer.VOCABULARY.getSymbolicName(firstToken.getType()) + ")"
                    + "-'"
                    + " " + " (" + MarkdownLexer.VOCABULARY.getSymbolicName(lastToken.getType())
                    + ")"
                    + " in rules " + invocationStack(parser)
                    + " alternatives " + alternatives + " - exact? " + exact
                    + ":\n"
                    + context(firstToken, lastToken) + '\n');
            ;
        }

        private String context(Token from, Token to) {
            StringBuilder sb = new StringBuilder();
            int startLineStartOffset = findLineStartOffset(from.getLine());
            if (startLineStartOffset < 0) {
                throw new IllegalArgumentException("Token '" + escape(from.getText()) + "' ix "
                    + from.getTokenIndex() + " says it is from line " + from.getLine()
                    + " of " + (text.split("\n").length) + " lines but did not find line start");
            }
            int endLineStartOffset;
            if (to == from) {
                endLineStartOffset = startLineStartOffset;
            } else {
                int ln = to.getLine();
                if (ln == from.getLine()) {
                    endLineStartOffset = startLineStartOffset;
                } else {
                    endLineStartOffset = findLineStartOffset(ln);
                    if (endLineStartOffset < 0) {
                        throw new IllegalStateException("Could not find line " + ln + " in "
                            + text.split("\n").length + " lines");
                    }
                }
            }
            int endLineEndOffset = findLineEndOffset(endLineStartOffset);
            String sub = text.substring(startLineStartOffset, endLineEndOffset);
            int lineDiff = to.getLine() - from.getLine();
            int localLine = 0;
            int lineStart = 0;
            sb.append(from.getLine()).append(". ");
            for (int i = 0; i < sub.length(); i++) {
                char c = sub.charAt(i);
                int cpInLine = i - lineStart;
                if (localLine == 0 && cpInLine == from.getCharPositionInLine()) {
                    sb.append(">>");
                }
                switch (c) {
                    case '\n':
                        localLine++;
                        lineStart = i;
                        sb.append("\n");
                        sb.append(from.getLine() + localLine).append(". ");
                        break;
                    case '\t':
                        sb.append("\\t");
                        break;
                    case '\r':
                        sb.append("\\r");
                        break;
                    default:
                        sb.append(c);
                }
                if (localLine == lineDiff && cpInLine == to.getCharPositionInLine() + to.getText().length()) {
                    sb.append("<<");
                }
            }
            return sb.toString();
        }

        private int findLineEndOffset(int charOffset) {
            if (charOffset > text.length()) {
                return text.length();
            }
            for (int i = charOffset; i < text.length(); i++) {
                switch (text.charAt(i)) {
                    case '\n':
                        return i;
                }
            }
            return text.length();
        }

        private int findLineStartOffset(int line) {
            int currentLine = 1;
            if (line == currentLine) {
                return 0;
            }
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                switch (c) {
                    case '\n':
                        currentLine++;
                        if (currentLine == line) {
                            return i + 1;
                        }
                        break;
                }
            }
            return -1;
        }

        private String contextLine(int pos) {
            StringBuilder sb = new StringBuilder();
            StringBuilder ind = new StringBuilder();
            for (int i = pos; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    break;
                }
                sb.append(text.charAt(i));
                if (i == pos) {
                    ind.append('^');
                }
            }
            for (int i = pos - 1; i >= 0; i--) {
                sb.insert(0, text.charAt(i));
                ind.insert(0, ' ');
            }
            sb.append('\n').append(ind);
            return sb.toString();
        }

        protected String getDecisionDescription(Parser recognizer, DFA dfa) {
            int decision = dfa.decision;
            int ruleIndex = dfa.atnStartState.ruleIndex;

            String[] ruleNames = recognizer.getRuleNames();
            if (ruleIndex < 0 || ruleIndex >= ruleNames.length) {
                return String.valueOf(decision);
            }

            String ruleName = ruleNames[ruleIndex];
            if (ruleName == null || ruleName.isEmpty()) {
                return String.valueOf(decision);
            }

            return String.format("rule '%s' (%d)", ruleName, decision);
        }

        private final String cl(int start, int stop) {
            int lineStart = -1;
            for (int i = Math.max(0, start); i >= 0; i--) {
                if (text.charAt(i) == '\n') {
                    lineStart = i;
                    break;
                }
            }
            if (lineStart == -1) {
                lineStart = 0;
            }
            int lineStop = -1;
            for (int i = stop - 1; i < text.length(); i++) {
                if (text.charAt(i) == '\n') {
                    lineStop = i;
                    break;
                }
            }
            if (lineStop == -1) {
                lineStop = text.length();
            }
            StringBuilder sb = new StringBuilder();
            for (int i = lineStart; i < lineStop; i++) {
                if (i == start) {
                    sb.append(">>");
                }
                sb.append(text.charAt(i));
                if (i == stop) {
                    sb.append("<<");
                }
            }
            return escape(sb.toString());
        }

        private String invocationStack(Parser parser) {
            List<String> stack = parser.getRuleInvocationStack();
            StringBuilder sb = new StringBuilder();
            for (String s : stack) {
                if (sb.length() > 0) {
                    sb.append("<--");
                }
                sb.append(s);
            }
            return sb.toString();
        }

        @Override
        public void reportAttemptingFullContext(Parser parser, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlternatives, ATNConfigSet configs) {
//            StringBuilder alternatives = new StringBuilder();
//            for (int bit = conflictingAlternatives.nextSetBit(0); bit >= 0; bit = conflictingAlternatives.nextSetBit(bit + 1)) {
//                if (alternatives.length() > 0) {
//                    alternatives.append(", ");
//                }
//                alternatives.append(bit);
//            }
//            String desc = getDecisionDescription(parser, dfa);
//            log("\nAttemptFullCtx " + desc + " with " + alternatives + " at " + startIndex + ":"
//                    + stopIndex + " '" + escape(text.substring(startIndex, stopIndex + 1))
//                    + cl(startIndex, stopIndex)
//                    + " in rules " + invocationStack(parser)
//            );
        }

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
            log("\nContext Sens at " + startIndex + ":" + stopIndex + " prediction "
                    + MarkdownParser.ruleNames[prediction] + " (" + prediction + ") " + " uniqueAlt " + configs.uniqueAlt
                    + "\n" + contextLine(startIndex));
        }
    }

    static String modeName(int mode) {
        return mode < 0 ? "<none>" : MarkdownLexer.modeNames[mode];
    }

    static class CTS extends CommonTokenStream {

        public CTS(MarkdownLexer tokenSource) {
            super(tokenSource);
        }

        MarkdownLexer lex() {
            return (MarkdownLexer) getTokenSource();
        }

        @Override
        public void consume() {
            int oldMode = lex()._mode;
            super.consume();
            int newMode = lex()._mode;
            if (oldMode != newMode) {
                String msg = modeName(oldMode) + " -> " + modeName(newMode) + " @ " + index();
                log(" * " + msg);
            }
        }
    }
}
