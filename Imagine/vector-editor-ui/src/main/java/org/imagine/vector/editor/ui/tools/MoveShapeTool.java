/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools;

import java.lang.annotation.Annotation;
import net.dev.java.imagine.spi.tool.Tool;
import org.imagine.vector.editor.ui.spi.ShapeElement;


/**
 *
 * @author Tim Boudreau
 */
public class MoveShapeTool implements Tool {

    @Override
    public Class<?> value() {
        return ShapeElement.class;
    }

    @Override
    public String name() {
        return "Move Shapes";
    }

    @Override
    public Class<? extends Annotation> annotationType() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
