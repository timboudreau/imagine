package org.imagine.help.api.annotations;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.SOURCE;
import java.lang.annotation.Target;

/**
 *
 * @author Tim Boudreau
 */
@Retention(SOURCE)
@Target({TYPE, PACKAGE})
public @interface Topic {

    Loc[] value();

    @interface Loc {

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
         * The text of the topic for this locale.
         *
         * @return The text
         */
        String value();
    }
}
