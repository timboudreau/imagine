package org.imagine.io;

import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

/**
 * It is common to store the hash code of objects as a checksum to compare with
 * deserialized instances. This annotation indicates that technique cannot be
 * used for a particular type (for example, objects that embed large images,
 * where extracting the raster array and hashing it on every call to hashCode()
 * would be prohibitively expensive).
 *
 * @author Tim Boudreau
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface InconsistentHashCode {

}
