/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.markdown.uiapi;

import java.util.Arrays;
import java.util.BitSet;
import java.util.function.Supplier;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.imagine.markdown.grammar.MarkdownLexer;
import org.imagine.markdown.grammar.MarkdownParser;
import org.imagine.markdown.grammar.MarkdownParserBaseVisitor;

/**
 *
 * @author Tim Boudreau
 */
class ErrorChecker {

    private final Supplier<CharStream> streamSupplier;

    ErrorChecker(Supplier<CharStream> supp) {
        this.streamSupplier = supp;
    }

    private static String readCharStream(CharStream chars) {
        StringBuilder sb = new StringBuilder();
        int curr;
        while ((curr = chars.LA(1)) != CharStream.EOF) {
            // JDK 9 and up
//            sb.append(Character.toString((int) curr));
            sb.append(Character.toString((char) curr));
            chars.consume();
        }
        return sb.toString();
    }

    private String fetchMarkdownText() {
        return readCharStream(streamSupplier.get());
    }

    void checkForErrors(ErrorConsumer errs) {
        String text = fetchMarkdownText();
        MarkdownLexer lex = new MarkdownLexer(streamSupplier.get());
        lex.removeErrorListeners();
        CommonTokenStream cts = new CommonTokenStream(lex, 0);
        MarkdownParser parser = new MarkdownParser(cts);
        parser.removeErrorListeners();
        AL al = new AL(text, lex, errs);
        lex.addErrorListener(al);
        parser.addErrorListener(al);
        ErrV errv = new ErrV(text, errs);
        parser.document().accept(errv);
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

    static class ErrV extends MarkdownParserBaseVisitor<Void> {

        private final String text;
        private final ErrorConsumer consumer;

        public ErrV(String text, ErrorConsumer consumer) {
            this.text = text;
            this.consumer = consumer;
        }

        @Override
        public Void visitErrorNode(ErrorNode node) {
            Token tok = node.getSymbol();
            String msg = indicateError(text, tok.getLine(), tok.getCharPositionInLine(),
                    "Error node '" + escape(node.getText()) + "' at source interval "
                    + node.getSourceInterval().a + ":" + node.getSourceInterval().b
                    + " " + node.toStringTree());
            consumer.onError(ErrorConsumer.ProblemKind.PARSER_ERROR, node.getSourceInterval().a, node.getSourceInterval().b, escape(tok.getText()), msg);
            return super.visitErrorNode(node);
        }

    }

    static class AL implements ANTLRErrorListener {

        private final String text;
        private final MarkdownLexer lex;
        private final ErrorConsumer consumer;

        public AL(String text, MarkdownLexer lex, ErrorConsumer consumer) {
            this.text = text;
            this.lex = lex;
            this.consumer = consumer;
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
        public void syntaxError(Recognizer<?, ?> rcgnzr, Object o, int line, int charPositionInLine, String string, RecognitionException re) {
            String msg = indicateError(line, charPositionInLine, o + " " + line + " / " + charPositionInLine + " " + string + " / " + re);
            consumer.onError(ErrorConsumer.ProblemKind.SYNTAX_ERROR, line, charPositionInLine, string, msg);
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
            consumer.onError(ErrorConsumer.ProblemKind.AMBIGUITY, startIndex, stopIndex, text, "ambiguity at " + startIndex + ":" + stopIndex + " '" + escape(text.substring(startIndex, stopIndex + 1))
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
            consumer.onError(ErrorConsumer.ProblemKind.ATTEMPT_FULL_CONTEXT, startIndex, stopIndex, text, "AttemptFullCtx with " + alternatives + " at " + startIndex + ":"
                    + stopIndex + " '" + escape(text.substring(startIndex, stopIndex + 1)) + "'\n"
                    + contextLine(startIndex));
        }

        @Override
        public void reportContextSensitivity(Parser parser, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
            consumer.onError(ErrorConsumer.ProblemKind.CONTEXT_SENSITIVITY, startIndex, stopIndex, text, "Context Sens at " + startIndex + ":" + stopIndex + " prediction "
                    + MarkdownParser.ruleNames[prediction] + " (" + prediction + ") " + " uniqueAlt " + configs.uniqueAlt
                    + "\n" + contextLine(startIndex));
        }
    }

    static String modeName(int mode) {
        return mode < 0 ? "<none>" : MarkdownLexer.modeNames[mode];
    }

    static String escape(String txt) {
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

}
