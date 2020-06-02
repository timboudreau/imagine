package org.imagine.helpimpl;

import java.io.IOException;
import java.io.InputStream;
import static java.nio.charset.StandardCharsets.UTF_8;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.v4.runtime.CharStreams;
import org.imagine.help.api.HelpItem;
import org.imagine.help.implspi.HelpLoader;
import org.imagine.markdown.uiapi.Markdown;
import org.openide.util.lookup.ServiceProvider;

/**
 * Provides a little bit of indirection, so all users of help don't wind up
 * needing a dependency on the Markdown parser.
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = HelpLoader.class)
public class HelpLoaderImpl implements HelpLoader {

    @Override
    public String heading(HelpItem item, Locale locale) {
        Markdown md = item.getContent(Markdown.class, locale);
        return md == null ? "" : md.probableHeadingLine();
    }

    @Override
    public String fullText(HelpItem item, Locale locale) {
        Markdown md = item.getContent(Markdown.class, locale);
        return md == null ? "" : md.extractPlainText();
    }

    @Override
    public <T> T load(Class<T> type, String helpId, String fromText) {
        if (type != Markdown.class) {
            return null;
        }
        Markdown result = new Markdown(fromText);
        return type.cast(result);
    }

    @Override
    public <T> T load(Class<T> type, String helpId, Locale locale, Class<?> relativeTo) {
        if (type != Markdown.class) {
            return null;
        }
        InputStream in = findInputStream(helpId, relativeTo, locale);
        if (in != null) {
            try {
                in.close();
                in = null;
                return type.cast(new Markdown(() -> {
                    InputStream in2 = findInputStream(helpId, relativeTo, locale);
                    try {
                        return CharStreams.fromStream(in2, UTF_8);
                    } catch (IOException ex) {
                        Logger.getLogger(HelpLoaderImpl.class.getName()).log(
                                Level.INFO, "Error loading helpId " + helpId + " for " + locale
                                + " relative to " + relativeTo.getName() + " from " + in2, ex);
                        return CharStreams.fromString("Error loading help " + helpId
                                + ".  Check the log file for details.");
                    }
                }));
            } catch (IOException ex) {
                Logger.getLogger(HelpLoaderImpl.class.getName()).log(Level.INFO,
                        "Error loading helpId " + helpId + " for " + locale
                        + " relative to " + relativeTo.getName(), ex);
            }
        }
        return null;
    }

    private static InputStream findInputStream(String helpId, Class<?> relativeTo, Locale locale) {
        String lang = locale.getLanguage();
        String var = locale.getVariant();
        return returnFirstNonNull(lang, var, relativeTo, helpId);
    }

    private static InputStream returnFirstNonNull(String lang, String var, Class<?> relativeTo, String helpId) {
        for (String test : new String[]{helpId + "-" + lang + "-" + var + ".md", helpId + "-" + lang + ".md", helpId + "en-US.md", helpId + "-en.md"}) {
            InputStream in = relativeTo.getResourceAsStream(test);
            if (in != null) {
                return in;
            }
        }
        return null;
    }
}
