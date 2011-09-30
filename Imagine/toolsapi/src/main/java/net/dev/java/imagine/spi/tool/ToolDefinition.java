package net.dev.java.imagine.spi.tool;

import javax.swing.Icon;
import net.dev.java.imagine.api.tool.Category;
import org.openide.util.HelpCtx;

/**
 * Object which provides the name and other human-friendly attributes of a
 * logical tool.
 *
 * @author Tim Boudreau
 */
public abstract class ToolDefinition {
    public abstract String displayName();

    public abstract Icon icon();

    public abstract String name();

    public abstract Category category();

    public abstract HelpCtx getHelpCtx();
    
    public abstract String getDescription();
}
