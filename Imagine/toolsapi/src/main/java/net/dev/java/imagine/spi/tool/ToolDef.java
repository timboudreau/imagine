package net.dev.java.imagine.spi.tool;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.dev.java.imagine.api.tool.Category;

/**
 * Defines the visual attributes of a single logical tool, which may have
 * multiple implementations that work against different kinds of Layer with
 * different Lookup contents.
 * <p/>
 * Most of the values have defaults which can be used for rapid development,
 * but which are not terribly desirable for production use.  If the icon is
 * not specified, a question-mark icon will be used;  if the name is not specified,
 * the package name of the annotated class will be used (so that all classes
 * annotated with &#064;Tool in the same package will share this tool definition
 * if they do not explicitly specify a name).
 * <p/>
 * A ToolDef is annotation works together with one or more classes annotated with
 * &#064;Tool which have the same name, to define the functionality of a tool.
 *
 * @author Tim Boudreau
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ToolDef {

    /**
     * The default icon, if none is specified.
     */
    //"net/java/dev/imagine/toolsapi/unknown.png";
    public static final String DEFAULT_ICON_PATH = "net/dev/java/imagine/api/tool/unknown.png"; 
    public static final String DEFAULT_NAME = "_default";
    public static final String DEFAULT_CATEGORY = "drawing";
    public static final String DEFAULT_BUNDLE = "_defaultBundle";
    public static final String DEFAULT_HELP_CTX = "org.openide.util.HelpCtx.DEFAULT_HELP";

    /**
     * / delimited path on the classpath to a resource bundle containing
     * the localized names, e.g. <code>com/foo/Bundle</code>.  If not specified,
     * a variant of the annotated class's package name will be used.
     * @return 
     */
    String displayNameBundle() default DEFAULT_BUNDLE;

    /**
     * The path on the classpath to an image file
     * @return The icon path
     */
    String iconPath() default DEFAULT_ICON_PATH;

    /**
     * The programmatic name or ID of this tool
     * @return 
     */
    String name() default DEFAULT_NAME;

    /**
     * The programmatic category of this tool
     * @return 
     */
    String category() default DEFAULT_CATEGORY;

    /**
     * The help context, if any
     * @return 
     */
    String getHelpCtx() default DEFAULT_HELP_CTX;
    
    /**
     * Optional ordering attribute to determine precedence in menus, etc.
     * @return 
     */
    int position() default -1;
}
