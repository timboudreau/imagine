package net.dev.java.imagine.spi.tool;

import static java.lang.Integer.MAX_VALUE;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for registering a Tool implementation
 *
 * @author Tim Boudreau
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Tool {
    /**
     * The type which must be present in the active layer's Lookup in order
     * for this tool implementation to be usable.
     * 
     * @return 
     */
    Class<?> value();
    /**
     * The programmatic name of the tool.  Multiple classes annotated with
     * &#064;Tool with the same name may coexist, as long as all of them are
     * sensitive to different types.
     * 
     * @return A name which may be shared by other Tool implementations, and
     * may correcpond to a ToolDef name on some class.
     */
    String name() default ToolDef.DEFAULT_NAME;

    int menuPosition() default MAX_VALUE;

    int toolbarPosition() default MAX_VALUE;
}
