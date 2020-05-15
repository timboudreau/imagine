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

    static final String PREFERRED_ID = "shapesPalette";
    static final String PALETTES_MODE = "palettes";

    public ShapesPaletteTC() {
        setLayout(new OneComponentLayout());
        add(PaintPalettes.createShapesPaletteComponent());
        setHtmlDisplayName(Bundle.SHAPES_PALETTE());
        setDisplayName(Bundle.SHAPES_PALETTE());
        setName(preferredID());
        setIcon(ImageUtilities.loadImage("org/imagine/inspectors/shapes.svg", false)); //XXX
    }

    @Override
    protected void onComponentActivated() {
        PaintPalettes.activated(this);
    }

    @Override
    public String getDisplayName() {
        return Bundle.ShapesPalette();
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    @Override
    public void open() {
        Mode mode = WindowManager.getDefault().findMode(PALETTES_MODE);
        if (mode != null) {
            mode.dockInto(this);
        }
        super.open();
    }

    private static ShapesPaletteTC INSTANCE;

    public static synchronized ShapesPaletteTC getInstance() {
        if (INSTANCE == null) {
            INSTANCE = (ShapesPaletteTC) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
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
    }

    public static void closePalette() {
        if (INSTANCE != null && INSTANCE.isOpened()) {
            INSTANCE.close();
            return;
        }
        for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
            if (tc instanceof ShapesPaletteTC) {
                ((ShapesPaletteTC) tc).closeWithoutUpdateOrder();
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
