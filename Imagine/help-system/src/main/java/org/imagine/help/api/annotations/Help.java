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
@Target({TYPE, METHOD, FIELD, CONSTRUCTOR, ANNOTATION_TYPE, LOCAL_VARIABLE})
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
     * List of related help topics. These can be:
     * <ul>
     * <li>Simple, unqualified id-strings if they are defined the same package
     * as this topic (because it would be a compile-time error for them not to
     * be unique)</li>
     * <li>Simple, unqualified id-strings if they are defined within the same
     * project as this one <i>and the ID is unique within all IDs within the
     * project), or</li>
     * <li>The ID is a fully qualified, dot-delimited class name <i>and</i>
     * id</li>
     * <li>Not the same ID as this annotation is defining</li>
     * </ul>
     * So, if we are defining help items Foo and Bar, in package
     * <code>com.example</code>, the <code>&#064;Help</code> annotation for
     * <code>Foo</code> can reference  <code>Bar</code> as <code>Bar</code> (as
     * long as, say, com.example.whatever does not also contain a help item
     * named <code>Bar</code>), or can reference it fully qualified as
     * <code>com.example.Bar</code>.
     *
     * @return An array of strings for related help items
     */
    String[] related() default {};

    /**
     * If true, make the generated HelpItem enum class public so it can be used
     * by classes outside of the package where it is defined. If any
     * <code>&#064;Help</code> annotation in the same package sets this to true,
     * the generated class will be a public class. In general this is only
     * useful for components that recur in a lot of places, but which cannot
     * practically be subclassed.
     *
     * @return True if the generated class should be public
     */
    boolean makePublic() default false;

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
         * item when searching by keyword.
         *
         * @return A list of index terms
         */
        String[] keywords() default {};
    }
}
