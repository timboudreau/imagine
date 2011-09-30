package net.dev.java.imagine.api.tool;

import org.openide.util.Parameters;

/**
 * A logical category for a tool
 *
 * @author Tim Boudreau
 */
public final class Category {
    private final String name;
    public static final Category DEFAULT_CATEGORY = new Category("drawing");

    Category(String name) {
        Parameters.notNull("name", name);
        this.name = name;
    }

    public String name() {
        return name;
    }

    public String toString() {
        return name();
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object o) {
        return o instanceof Category && o.toString().equals(toString());
    }
    
}
