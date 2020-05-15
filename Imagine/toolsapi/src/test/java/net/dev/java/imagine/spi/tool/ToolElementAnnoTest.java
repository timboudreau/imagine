package net.dev.java.imagine.spi.tool;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Tests layer generation.
 *
 * @author Tim Boudreau
 */
@ToolElement(folder = "wookies", icon = "net/java/dev/imagine/api/tool/foo.png", name = "Wookie", position = 32)
public class ToolElementAnnoTest extends ArrayList<String> implements Consumer<String> {

    @Override
    public void accept(String t) {
        add(t);
    }

}
