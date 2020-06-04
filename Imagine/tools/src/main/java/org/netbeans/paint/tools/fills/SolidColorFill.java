/*
 * SolidColorFill.java
 *
 * Created on October 15, 2005, 10:49 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
package org.netbeans.paint.tools.fills;

import java.awt.Color;
import java.awt.Component;
import java.awt.Paint;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.spi.tool.ToolElement;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.netbeans.paint.tools.spi.Fill;
import org.openide.util.NbBundle;

/**
 * Does nothing except create a public final class out of BaseFill.
 *
 * @authorTimothy Boudreau
 */
@ToolElement(folder = "fills", name="Solid Color", position=100, icon="org/netbeans/paint/tools/resources/solidfill.png")
public final class SolidColorFill implements Fill {

    private final Customizer<Color> customizer;

    public SolidColorFill() {
        customizer = Customizers.getCustomizer(Color.class, NbBundle.getMessage(BaseFill.class,
                "LBL_Foreground"));
    }

    @Override
    public Paint getPaint() {
        return customizer.get();
    }

    @Override
    public Component getCustomizer() {
        Component result = customizer.getComponent();
        
        return customizer.getComponent();
    }
}
