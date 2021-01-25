package org.netbeans.paint.tools.spi;

import java.awt.Cursor;
import org.imagine.editor.api.ImageEditorBackground;
import com.mastfrog.geometry.uirect.ResizeMode;
import static com.mastfrog.geometry.uirect.ResizeMode.BOTTOM_EDGE;
import static com.mastfrog.geometry.uirect.ResizeMode.NORTHEAST;
import static com.mastfrog.geometry.uirect.ResizeMode.NORTHWEST;
import static com.mastfrog.geometry.uirect.ResizeMode.SOUTHEAST;
import static com.mastfrog.geometry.uirect.ResizeMode.SOUTHWEST;
import static com.mastfrog.geometry.uirect.ResizeMode.TOP_EDGE;
import com.mastfrog.swing.cursor.Cursors;

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
