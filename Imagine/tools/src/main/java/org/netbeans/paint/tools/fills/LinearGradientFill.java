/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.fills;

import java.awt.LinearGradientPaint;
import java.awt.Paint;
import javax.swing.JComponent;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.api.components.explorer.Customizable;
import org.netbeans.paint.tools.spi.Fill;

/**
 *
 * @author Tim Boudreau
 */
public class LinearGradientFill implements Fill, Customizable {

    private final Customizer<LinearGradientPaint> cust;

    public LinearGradientFill() {
        cust = Customizers.getCustomizer(LinearGradientPaint.class, "Linear");
    }

    @Override
    public Paint getPaint() {
        return cust.get();
    }

    @Override
    public JComponent getCustomizer() {
        return cust.getComponent();
    }
}
