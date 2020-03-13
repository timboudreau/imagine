package org.imagine.vector.editor.ui.palette;

import java.awt.BorderLayout;
import org.openide.windows.TopComponent;

/**
 *
 * @author Tim Boudreau
 */
public class ShapesPaletteTC extends TopComponent {

    public ShapesPaletteTC() {
        setLayout(new BorderLayout());
        add(PaintPalettes.createShapesPaletteComponent(), BorderLayout.CENTER);
    }

    @Override
    public String getDisplayName() {
        return Bundle.ShapesPalette();
    }

    public static void openPalette() {
        for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
            if (tc instanceof ShapesPaletteTC) {
                tc.requestVisible();
                return;
            }
        }
        ShapesPaletteTC nue = new ShapesPaletteTC();
        nue.open();
        nue.requestVisible();
    }

    public static void closePalette() {
        for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
            if (tc instanceof ShapesPaletteTC) {
                tc.close();
                return;
            }
        }
    }

}
