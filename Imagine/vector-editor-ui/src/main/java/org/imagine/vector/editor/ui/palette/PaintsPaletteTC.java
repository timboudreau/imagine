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
//@ConvertAsProperties(
//        dtd = "-//org.imagine.vector.editor.ui//PaintsPaletteTC//EN",
//        autostore = false
//)
@Messages("PAINTS_PALETTE=Fills")
public final class PaintsPaletteTC extends AbstractPaletteTC {

    public PaintsPaletteTC() {
        setLayout(new OneComponentLayout());
        add(PaintPalettes.createPaintPaletteComponent());
        setHtmlDisplayName(Bundle.PAINTS_PALETTE());
        setDisplayName(Bundle.PAINTS_PALETTE());
        setName(preferredID());
        setIcon(ImageUtilities.loadImage("org/imagine/inspectors/gradientfill.png", false));
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
        return "paintsPalette";
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
            INSTANCE = (PaintsPaletteTC) WindowManager.getDefault().findTopComponent("paintsPalette");
        }
        return INSTANCE;
    }

    @Override
    public void open() {
        System.out.println("PaintsPaletteTC open");
        Mode mode = WindowManager.getDefault().findMode("palettes");
        if (mode != null) {
            System.out.println("  docking into palettes mode");
            mode.dockInto(this);
        }
        super.open();
    }

    public static void openPalette() {
        PaintsPaletteTC nue = getInstance();
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
            if (tc instanceof PaintsPaletteTC) {
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
