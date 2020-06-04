package org.imagine.helpimpl;

import static com.mastfrog.util.collections.CollectionUtils.setOf;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.imagine.help.api.HelpItem;
import org.imagine.help.api.annotations.Help;
import org.imagine.help.api.annotations.Help.HelpText;
import org.imagine.help.impl.HelpComponentManagerTrampoline;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tim Boudreau
 */
public class RelatedTest {

    private static final Set<HelpItem> allLoadableHelpItems = new HashSet<>();

    @Test
    public void testRelatedItemsCanBeResolved() {
        List<? extends HelpItem> sliderRelated = HelpItems.Slider.related();
        assertNotNull(sliderRelated, "Null related items");
        assertFalse(sliderRelated.isEmpty(), "No related items");

        assertTrue(sliderRelated.contains(HelpItems.Tree), sliderRelated.toString());
        assertTrue(sliderRelated.contains(HelpItems.Text), sliderRelated.toString());
        assertEquals(4, sliderRelated.size(), sliderRelated.toString());

        Set<String> foundNames = new HashSet<>();
        for (HelpItem it : sliderRelated) {
            if (it == HelpItems.Slider || it == HelpItems.Tree || it == HelpItems.Text) {
                continue;
            }
            String name = it.name();
            switch (name) {
                case "InheritTopic1":
                    foundNames.add(name);
                    assertEquals("org.imagine.help.api.demo." + name, it.identifier());
                    break;
                case "first":
                    foundNames.add(name);
                    assertEquals("org.imagine.help.api." + name, it.identifier());
                    break;
                default:
                    fail("Unexpected related help item " + it + " (" + it.getClass().getName() + ")");
            }
        }
        assertEquals(setOf("InheritTopic1", "first"), foundNames);
    }

    @Test
    @Help(id = "woogle", related = "moopy.flork", content = @HelpText(value = "xfyasdf"))
    public void testAllLoadableHelpItemsAreResolvableViaHelpIndices() {
        for (HelpItem item : allLoadableHelpItems) {
            String id = item.identifier();
            HelpItem resolved = HelpItem.find(id);
            assertNotNull(resolved, "Could not resolve id " + id);
            assertSame(item, resolved, "Not same instance: " + item + " and " + resolved + " ("
                    + item.getClass().getName() + " vs. " + resolved.getClass().getName() + ")");
        }
    }

    @BeforeAll
    public static void beforeClass() throws ClassNotFoundException {
        HelpComponentManagerTrampoline.setIndices(() -> {
            return Arrays.asList(new org.imagine.help.api.HIndex(), new org.imagine.helpimpl.HIndex(),
                    new org.imagine.help.api.demo.HIndex());
        });
        System.setProperty("help-search-threads", "2");
        System.setProperty("java.util.logging.config.file",
                ClassLoader.getSystemResource("logging.properties").getPath());

        for (String nm : new String[]{"org.imagine.help.api.demo.HelpItems", "org.imagine.help.api.HelpItems", "org.imagine.helpimpl.HelpItems"}) {
            Class<?> type = Class.forName(nm);
            Object[] o = type.getEnumConstants();
            for (Object item : o) {
                assertTrue(item instanceof HelpItem);
                assertTrue(item instanceof Enum<?>);
                allLoadableHelpItems.add(HelpItem.class.cast(item));
            }
        };
    }
}
