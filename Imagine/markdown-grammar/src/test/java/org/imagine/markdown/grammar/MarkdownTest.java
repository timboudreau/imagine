package org.imagine.markdown.grammar;

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
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.RuleNode;
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
            + " * And then a list"
            + " * Or something like that";

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

    @Test
    public void testSomeMethod() {
//        testBoth(TEST_MUCH);
//        testBoth(TEST_SIMPLE_WITH_NESTED_MARKUP);
//        testBoth(TEST_SIMPLE_WITH_NESTED_MARKUP);
//        testBoth(TEST_NESTED_MARKUP);
//        testBoth(TEST_BLOCKQUOTE);
        testBoth(TEST_BLOCKQUOTE_SIMPLE);
//        testBoth(TEST_ONE);
//        testBoth(TEST_LINK_AFTER_TEXT);
//        testBoth(TEST_LINK);
//        testBoth(TEST_LIST);
//        testBoth(TEST_BLOCKQUOTE);
    }

    private void testBoth(String text) {
        System.out.println("\n*************************");
        System.out.println(text);
        System.out.println("--------------------------------");
        testTokens(text, (ix, tok) -> {
            System.out.println(ix + ". " + MarkdownLexer.VOCABULARY.getDisplayName(tok.getType()) + ": '" + escape(tok.getText()) + "'");
        });
        System.out.println("\n------------- PARSE ---------------\n");
        testParse(text);
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
                System.out.println(" --- " + msg + "\n");
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
        parser.addErrorListener(als[0]);
        V v = new V(text);
        parser.document().accept(v);
        v.rethrow();
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
            System.out.println(msg);
            errors.add(new AssertionError(msg));
            return super.visitErrorNode(node);
        }

        @Override
        public Void visitChildren(RuleNode node) {
            char[] c = new char[depth * 2];
            Arrays.fill(c, ' ');
            System.out.println(new String(c) + node.getClass().getSimpleName() + "\t'" + escape(node.getText()) + "'");
            depth++;
            super.visitChildren(node);
            depth--;
            return null;
        }

    }

    private static String indicateError(String text, int line, int pos, String msg) {
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
            StringBuilder sb = new StringBuilder("Syntax Error in mode ").append(currentMode()).append('\n');
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
                alternatives.append(MarkdownParser.ruleNames[bit]);
            }
            System.out.println("ambiguity at " + startIndex + ":" + stopIndex + " '" + escape(text.substring(startIndex, stopIndex + 1))
                    + "' alternatives " + alternatives + " - exact? " + exact);

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

        @Override
        public void reportAttemptingFullContext(Parser parser, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlternatives, ATNConfigSet configs) {
            StringBuilder alternatives = new StringBuilder();
            for (int bit = conflictingAlternatives.nextSetBit(0); bit >= 0; bit = conflictingAlternatives.nextSetBit(bit + 1)) {
                if (alternatives.length() > 0) {
                    alternatives.append(", ");
                }
                alternatives.append(MarkdownParser.ruleNames[bit]);
            }
            System.out.println("\nAttemptFullCtx with " + alternatives + " at " + startIndex + ":"
                    + stopIndex + " '" + escape(text.substring(startIndex, stopIndex + 1)) + "'\n"
                    + contextLine(startIndex));
        }

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
            System.out.println("\nContext Sens at " + startIndex + ":" + stopIndex + " prediction "
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
                System.out.println(" * " + msg);
            }
        }
    }
}
