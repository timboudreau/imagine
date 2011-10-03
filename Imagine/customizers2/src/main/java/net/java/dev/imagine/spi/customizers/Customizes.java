package net.java.dev.imagine.spi.customizers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Tim Boudreau
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Customizes {
    Class<?> value();
}
