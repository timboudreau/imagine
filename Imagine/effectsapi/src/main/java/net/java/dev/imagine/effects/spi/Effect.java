package net.java.dev.imagine.effects.spi;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *
 * @author Tim Boudreau
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Effect {

    public static final String DEFAULT_NAME = "_name";

    /**
     * The output type of the effect - such as BufferedImageOp or Composite for
     * image-based effects.
     * @return 
     */
    Class<?> value();

    /**
     * The type of parameter the effect takes, such 
     * @return 
     */
    Class<?> parameter() default Void.class;

    String name() default DEFAULT_NAME;

    boolean canPreview() default true;

    int position() default -1;
}
