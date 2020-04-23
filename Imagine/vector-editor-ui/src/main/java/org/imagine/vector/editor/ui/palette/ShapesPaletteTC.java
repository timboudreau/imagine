package org.imagine.vector.editor.ui.palette;

import org.netbeans.paint.api.components.OneComponentLayout;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
// If we enable this, LayerBuilder crashes the bulid.
//@ConvertAsProperties(
//        dtd = "-//org.imagine.vector.editor.ui//PaintsPaletteTC//EN",
//        autostore = false
//)
@Messages("SHAPES_PALETTE=Shapes")
public final class ShapesPaletteTC extends AbstractPaletteTC {

    public ShapesPaletteTC() {
        setLayout(new OneComponentLayout());
        add(PaintPalettes.createShapesPaletteComponent());
        setHtmlDisplayName(Bundle.SHAPES_PALETTE());
        setDisplayName(Bundle.SHAPES_PALETTE());
        setName(preferredID());
        setIcon(ImageUtilities.loadImage("org/imagine/inspectors/gradientfill.png", false));
    }

    @Override
    protected void componentActivated() {
        PaintPalettes.activated(this);
    }

    @Override
    public String getDisplayName() {
        return Bundle.ShapesPalette();
    }

    @Override
    protected String preferredID() {
        return "shapesPalette";
    }

    @Override
    public void open() {
        System.out.println("Shapes Palette tc opening");
        Mode mode = WindowManager.getDefault().findMode("palettes");
        if (mode != null) {
            System.out.println("   docking into " + mode);
            mode.dockInto(this);
        }
        super.open();
    }

    private static ShapesPaletteTC INSTANCE;

    public static synchronized ShapesPaletteTC getInstance() {
        if (INSTANCE == null) {
            INSTANCE = (ShapesPaletteTC) WindowManager.getDefault().findTopComponent("shapesPalette");
        }
        return INSTANCE;
    }

    public static synchronized ShapesPaletteTC getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new ShapesPaletteTC();
        }
        return INSTANCE;
    }

    public static void openPalette() {
        ShapesPaletteTC nue = getInstance();
        nue.open();
        if (PaintPalettes.wasLastActive(nue)) {
            nue.requestVisible();
        }
    }

    public static void closePalette() {
        if (INSTANCE != null && INSTANCE.isOpened()) {
            INSTANCE.close();
            return;
        }
        for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
            if (tc instanceof ShapesPaletteTC) {
                tc.close();
                return;
            }
        }
    }

    @Override
    public Object writeReplace() {
        return new ResolvableHelper();
    }

    static final class ResolvableHelper implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        public Object readResolve() {
            return getDefault();
        }
    }
}
