/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class TestThings {

    private static final String BASIC = "These are *some words*, which shall be given up for you."
            + "\n\n"
            + "This is another sentence. How do you like it?"
            + "\nJust a single line break should be joined!"
            + "\n\n\n";

    @Test
    public void testSomething() {
        if (true) {
            return;
        }
        testBoth(BASIC);
    }

    private void testBoth(String text) {
        System.out.println("\n*************************");
        System.out.println(text);
        System.out.println("--------------------------------");
        testTokens(text, (ix, tok) -> {
            System.out.println(ix + ". " + NlTest.VOCABULARY.getDisplayName(tok.getType()) + ": '" + escape(tok.getText()) + "'");
        });
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
        NlTest lex = lexer(text, a -> al[0] = a);
        int ix = 0;
        int oldMode = lex._mode;
        for (Token tok = lex.nextToken(); tok.getType() != -1; tok = lex.nextToken(), ix++) {
            int newMode = lex._mode;
            c.accept(ix, tok);
            if (oldMode != newMode) {
                String msg = modeName(oldMode) + " -> " + modeName(newMode) + " @ " + tok.getLine() + ":" + tok.getCharPositionInLine();
                System.out.println(" * " + msg + "\n");
            }
            oldMode = newMode;
            al[0].rethrow();
        }
    }

    private void testParse(String text) {
        NlTest lex = lexer(text);
        CTS cts = new CTS(lex);
        Nl parser = new Nl(cts);
        V v = new V(text);
        parser.document().accept(v);
    }

    private NlTest lexer(String text) {
        return lexer(text, null);
    }

    private NlTest lexer(String text, Consumer<AL> c) {
        CodePointCharStream stream = CharStreams.fromString(text, "source");
        NlTest lexer = new NlTest(stream);
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

        public V(String text) {
            this.text = text;
        }

        @Override
        public Void visitErrorNode(ErrorNode node) {
            Token tok = node.getSymbol();
            String msg = indicateError(text, tok.getLine(), tok.getCharPositionInLine(),
                    "Error node '" + node.getText() + " at source interval "
                    + node.getSourceInterval().a + ":" + node.getSourceInterval().b
                    + " " + node.toStringTree());
            fail(msg);
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
        private final NlTest lex;
        private final List<AssertionError> errors = new ArrayList<>();

        public AL(String text, NlTest lex) {
            this.text = text;
            this.lex = lex;
        }

        String currentMode() {
            int md = lex._mode;
            return md < 0 ? "<none>" : NlTest.modeNames[md];
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
        public void reportAmbiguity(Parser parser, DFA dfa, int i, int i1, boolean bln, BitSet bitset, ATNConfigSet atncs) {
            System.out.println("ambiguity at " + i + ":" + i1);
        }

        @Override
        public void reportAttemptingFullContext(Parser parser, DFA dfa, int i, int i1, BitSet bitset, ATNConfigSet atncs) {
            System.out.println("fullCtx at " + i + ":" + i1);
        }

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atncs) {
            System.out.println("ctxSens at " + i + ":" + i1);
        }
    }

    static String modeName(int mode) {
        return mode < 0 ? "<none>" : NlTest.modeNames[mode];
    }

    static class CTS extends CommonTokenStream {

        public CTS(NlTest tokenSource) {
            super(tokenSource);
        }

        NlTest lex() {
            return (NlTest) getTokenSource();
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
