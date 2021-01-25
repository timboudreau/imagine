package org.imagine.vector.editor.ui.palette;

import static org.imagine.vector.editor.ui.palette.ShapesPaletteTC.PALETTES_MODE;
import com.mastfrog.swing.layout.OneComponentLayout;
import org.openide.util.ImageUtilities;
import org.openide.util.NbBundle.Messages;
import org.openide.windows.Mode;
import org.openide.windows.TopComponent;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
//@ConvertAsProperties(
//        dtd = "-//org.imagine.vector.editor.ui//PaintsPaletteTC//EN",
//        autostore = false
//)
@Messages({"PAINTS_PALETTE=Fills",
//        "CTL_OpenShapesPaletteAction=Shapes Palette"
})
public final class PaintsPaletteTC extends AbstractPaletteTC {

    static final String PREFERRED_ID = "paintsPalette";

    public PaintsPaletteTC() {
        setLayout(new OneComponentLayout());
        add(PaintPalettes.createPaintPaletteComponent());
        setHtmlDisplayName(Bundle.PAINTS_PALETTE());
        setDisplayName(Bundle.PAINTS_PALETTE());
        setName(preferredID());
        setIcon(ImageUtilities.loadImage("org/imagine/inspectors/fills.svg", false));
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
    public int getPersistenceType() {
        return PERSISTENCE_ALWAYS;
    }

    @Override
    protected String preferredID() {
        return PREFERRED_ID;
    }

    private static PaintsPaletteTC INSTANCE;

    public static synchronized PaintsPaletteTC getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new PaintsPaletteTC();
        }
        return INSTANCE;
    }

    public static synchronized PaintsPaletteTC getInstance() {
        if (INSTANCE == null) {
            INSTANCE = (PaintsPaletteTC) WindowManager.getDefault().findTopComponent(PREFERRED_ID);
        }
        return INSTANCE;
    }

    @Override
    public void open() {
        Mode mode = WindowManager.getDefault().findMode(PALETTES_MODE);
        if (mode != null) {
            mode.dockInto(this);
        }
        super.open();
    }

    public static void closePalette() {
        if (INSTANCE != null && INSTANCE.isOpened()) {
            INSTANCE.close();
            return;
        }
        for (TopComponent tc : TopComponent.getRegistry().getOpened()) {
            if (tc instanceof PaintsPaletteTC) {
                ((PaintsPaletteTC) tc).closeWithoutUpdateOrder();
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
