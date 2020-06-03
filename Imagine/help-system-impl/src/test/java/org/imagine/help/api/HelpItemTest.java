package org.imagine.help.api;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.imagine.help.api.search.HelpIndex;
import org.imagine.help.api.search.HelpSearchCallback;
import org.imagine.help.api.search.HelpSearchConstraint;
import org.imagine.help.impl.HelpComponentManagerTrampoline;
import org.imagine.markdown.uiapi.Markdown;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class HelpItemTest {

    @Test
    public void testLocalization() {
        Locale loc = Locale.getDefault();
        loc.getCountry();

        Markdown md = HelpItems.first.getContent(Markdown.class);
        assertNotNull(md);

        assertEquals("This is some help and its neighbors", md.extractPlainText());

        Markdown gbMd = HelpItems.first.getContent(Markdown.class, Locale.UK);
        assertNotNull(gbMd);

        assertEquals("This is some help and its neighbours", gbMd.extractPlainText());

        // We don't have a south africa specific english, so should use the US version
        Markdown fallbackMd = HelpItems.first.getContent(Markdown.class, Locale.forLanguageTag("en-ZA"));
        assertNotNull(fallbackMd);
        assertEquals(md.extractPlainText(), fallbackMd.extractPlainText());

        Markdown czech = HelpItems.first.getContent(Markdown.class, Locale.forLanguageTag("cs"));
        assertNotNull(czech);

        assertEquals("Tohle je nějaká pomoc a její sousedé", czech.extractPlainText());

        Markdown frenchShouldNotExist = HelpItems.first.getContent(Markdown.class, Locale.FRANCE);
        assertNull(frenchShouldNotExist);
    }

    @Test
    public void testHelpIndex() throws InterruptedException {
        HSC hsc = new HSC();
        HelpIndex.search(Locale.forLanguageTag("en-US"), "danger", 10, hsc, HelpSearchConstraint.KEYWORD);
        hsc.await();

        hsc = new HSC();
        HelpIndex.search(Locale.forLanguageTag("en-US"), "Need", 10, hsc, HelpSearchConstraint.TOPIC);
        hsc.await();

        hsc = new HSC();
        HelpIndex.search(Locale.forLanguageTag("en-US"), "terribly", 10, hsc);
        hsc.await();

        hsc = new HSC();
        HelpIndex.search(Locale.forLanguageTag("en-GB"), "neighbor", 10, hsc);
        hsc.await();

        hsc = new HSC();
        HelpIndex.search(Locale.forLanguageTag("en-GB"), "neighbour", 10, hsc);
        hsc.await();

        hsc = new HSC();
        HelpIndex.search(Locale.forLanguageTag("en-GB"), "neighbor", 10, hsc);
        hsc.await();
    }

    static class HSC implements HelpSearchCallback {

        private final CountDownLatch latch = new CountDownLatch(1);
        private final Set<HelpItem> items = Collections.synchronizedSet(new HashSet<>());

        @Override
        public boolean onMatch(String of, HelpItem item, String heading, String topic, float score, boolean isLast) {
            items.add(item);
            System.out.println("onMatch! " + item + " " + score + " '" + heading + "' " + topic);
            return true;
        }

        @Override
        public void onFinish() {
            System.out.println("hsc onFinish");
            latch.countDown();
        }

        @Override
        public void onStart() {
            System.out.println("hsc onStart");
        }

        public void assertItems(HelpItem... items) throws InterruptedException {
            await();
            assertEquals(new HashSet<>(Arrays.asList(items)), this.items);
        }

        void await() throws InterruptedException {
            latch.await(5, TimeUnit.SECONDS);
        }
    }

    @BeforeAll
    public static void beforeClass() {
        HelpComponentManagerTrampoline.INDEXES = () -> {
            return Arrays.asList(new HIndex(), new org.imagine.helpimpl.HIndex());
        };
        System.setProperty("help-search-threads", "2");
        System.setProperty("java.util.logging.config.file",
                ClassLoader.getSystemResource("logging.properties").getPath());
    }
}
