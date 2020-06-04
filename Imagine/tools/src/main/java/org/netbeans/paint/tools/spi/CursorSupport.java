package org.netbeans.paint.tools.spi;

import java.awt.Cursor;
import org.imagine.editor.api.ImageEditorBackground;
import org.imagine.geometry.uirect.ResizeMode;
import static org.imagine.geometry.uirect.ResizeMode.BOTTOM_EDGE;
import static org.imagine.geometry.uirect.ResizeMode.NORTHEAST;
import static org.imagine.geometry.uirect.ResizeMode.NORTHWEST;
import static org.imagine.geometry.uirect.ResizeMode.SOUTHEAST;
import static org.imagine.geometry.uirect.ResizeMode.SOUTHWEST;
import static org.imagine.geometry.uirect.ResizeMode.TOP_EDGE;
import org.netbeans.paint.api.cursor.Cursors;

/**
 *
 * @author Tim Boudreau
 */
public class CursorSupport {

    public static Cursor cursor(ResizeMode mode) {
        Cursors cur = ImageEditorBackground.getDefault().style().isBright()
                ? Cursors.forBrightBackgrounds() : Cursors.forDarkBackgrounds();
        switch (mode) {
            case LEFT_EDGE :
            case RIGHT_EDGE :
                return cur.horizontal();
            case NORTHEAST:
            case SOUTHWEST:
                return cur.southWestNorthEast();
            case SOUTHEAST:
            case NORTHWEST:
                return cur.southEastNorthWest();
            case BOTTOM_EDGE:
            case TOP_EDGE:
                return cur.vertical();
            default:
                return Cursor.getDefaultCursor();
        }
    }
}
