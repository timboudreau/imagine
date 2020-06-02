package org.imagine.markdown.uiapi;

/**
 * Used to check Markdown for errors.
 *
 * @author Tim Boudreau
 */
public interface ErrorConsumer {

    void onError(ProblemKind kind, int startOffset, int endOffset, String offending, String msg);

    public enum ProblemKind {
        SYNTAX_ERROR,
        PARSER_ERROR,
        ATTEMPT_FULL_CONTEXT,
        AMBIGUITY,
        CONTEXT_SENSITIVITY;

        public boolean isFatal() {
            switch (this) {
                case SYNTAX_ERROR:
                    return true;
                case PARSER_ERROR:
                    return true;
                default:
                    return false;
            }
        }
    }
}
