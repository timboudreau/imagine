package net.dev.java.imagine.spi.tool.impl;

import net.dev.java.imagine.api.tool.Category;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDefinition;

/**
 * Internal Api to enable non-public constructors for Tool & Category.
 *
 * @author Tim Boudreau
 */
public interface ToolFactory {
    public Tool create (ToolDefinition def);
    public Category newCategory(String name);
}
