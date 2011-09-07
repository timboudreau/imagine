/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.actions;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import net.dev.java.imagine.spi.tools.Tool;
import net.java.dev.imagine.api.image.Layer;

/**
 *
 * @author eppleton
 */
public abstract class GenericToolAction implements ActionListener {

    private final Layer context;
    private final Tool tool;

    public GenericToolAction(Layer context, Tool tool) {
        this.context = context;
        this.tool = tool;
    }

    public void actionPerformed(ActionEvent ev) {

        if (tool.canAttach(context)) {
            SelectedToolContextContributor.setSelectedTool(tool);
        }

    }
}
