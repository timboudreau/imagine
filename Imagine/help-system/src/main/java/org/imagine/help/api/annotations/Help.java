package org.imagine.help.api.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Defines a help item; the annotation processor in help-annotation-processors
 * will use this annotation to generate an enum named HelpItems for all such
 * annotations in the package where this annotation appears.
 *
 * @author Tim Boudreau
 */
@Retention(SOURCE)
@Target({TYPE, METHOD, FIELD})
public @interface Help {

    /**
     * Get a unique identifier for this help item.
     *
     * @return An identifier
     */
    String id();

    /**
     * Get the help text, one for each supported language.
     *
     * @return An array of help content
     */
    HelpText[] content();

    /**
     * Optionally, omit this item from the help index / table of contents - this
     * may be useful for some popup hints.
     *
     * @return If true, this hint will not be indexed
     */
    boolean noIndex() default false;

    /**
     * Localized contents of a help item specifying its language and text.
     */
    public @interface HelpText {

        /**
         * Get the ISO 639 language code (as used in {@link java.util.Locale}
         * for the language of this help text.
         *
         * @return A language
         */
        String language() default "en";

        /**
         * Get the ISO 639 country code (as used in {@link java.util.Locale} for
         * the language of this help text.
         *
         * @return A country
         */
        String country() default "US";

        /**
         * Get the localized text of this help item, or a file name prefix ( the
         * annotation processor will look for
         * <code>$PREFIX-language-country.md</code>) relative to the class it
         * appears on - any whitespace in the value here - if it is not a single
         * word - will cause it to be interpreted as help text rather than a
         * file name).
         *
         * @return The localized text
         */
        String value();

        /**
         * Get an optional topic used in any help index for this topic.
         *
         * @return A topic
         */
        String topic() default "";

        /**
         * Get any terms that should be included in any help index for this help
         * item.
         *
         * @return A list of index terms
         */
        String[] keywords() default {};
    }
}
