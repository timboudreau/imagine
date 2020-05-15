package net.dev.java.imagine.spi.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generic annotation to allow modules to inject brush tips, path creators,
 * fills and other things which some tools allow to be plugged in.
 *
 * @author Tim Boudreau
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface ToolElement {

    static final String DEFAULT_ICON = "net/dev/java/imagine/api/tool/unknown.png";
    static final String DEFAULT_DISPLAY_NAME = "_unnamed";

    String folder();

    Class<?> value() default ToolElement.class;

    String icon() default DEFAULT_ICON;

    String name() default DEFAULT_DISPLAY_NAME;

    int position() default Integer.MAX_VALUE;
}
