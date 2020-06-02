package org.imagine.help.implspi;

import java.util.Locale;
import org.imagine.help.api.HelpItem;
import org.openide.util.Lookup;

/**
 * HelpLoader allows modules that use HelpId not to depend directly on the
 * markdown parser, and for it to be replaced without requiring code
 * modifications (though possibly requiring recompilation).
 *
 * @author Tim Boudreau
 */
public interface HelpLoader {

    public static HelpLoader getDefault() {
        return Lookup.getDefault().lookup(HelpLoader.class);
    }

    /**
     * Get the help contents as an instance of some type.  Most implementations
     * will support one specific type.  This method assumes that the help markup
     * is available directly from the help item, and the job of this method is
     * to return a parsed representation of it (the default implementation
     * passes <code>Markdown.class</code> and supports nothing else.
     *
     * @param <T> The type requested
     * @param type The type requested
     * @param helpId The help id (if a generated enum, it's <code>name()</code>)
     * @param fromText The raw markup
     * @return A parsed representation of the markup or null if unsupported
     */
    <T> T load(Class<T> type, String helpId, String fromText);

    /**
     * Load markup and parse it according to an agreed upon convention (such as
     * looking up a file named <code>$ID-$LANGUAGE-$COUNTRY.md</code> next to
     * the class <code>relativeTo</code>.
     *
     * @param <T> The type requested for a parsed representation of the markup
     * @param type The type requested for a parsed representation of the markup
     * @param helpId The help id
     * @param locale The locale
     * @param relativeTo The class to look for a resource relative to
     * @return Parsed markup or null if no such resource exists
     */
    <T> T load(Class<T> type, String helpId, Locale locale, Class<?> relativeTo);

    /**
     * Get the heading text - typically the first line of the help item.
     *
     * @param item The help item
     * @param locale The locale help is sought for (if a specific country is not
     * present but a more general language-only locale is, that will be returned).
     * @return The heading text
     */
    String heading(HelpItem item, Locale locale);

    /**
     * Get a parsed representation of the markup converted to plain, searchable
     * text.
     *
     * @param item The help item
     * @param locale The locale
     * @return A string or null
     */
    String fullText(HelpItem item, Locale locale);
}
