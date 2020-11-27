package org.netbeans.paint.api.cursor;

import java.awt.Cursor;
import javax.swing.JComponent;
import com.mastfrog.geometry.Quadrant;

/**
 * Provides the set of custom cursors used in the application.
 *
 * @author Tim Boudreau
 */
public interface Cursors {

    public static Cursors forDarkBackgrounds() {
        return CursorsImpl.darkBackgroundCursors();
    }

    public static Cursors forBrightBackgrounds() {
        return CursorsImpl.brightBackgroundCursors();
    }

    /**
     * Get the cursors instance for this component based on its background
     * colors.
     *
     * @param comp
     * @return
     */
    public static Cursors forComponent(JComponent comp) {
        return CursorsImpl.cursorsForComponent(comp);
    }

    public Cursor star();

    public Cursor barbell();

    public Cursor x();

    public Cursor hin();

    public Cursor no();

    public Cursor horizontal();

    public Cursor vertical();

    public Cursor southWestNorthEast();

    public Cursor southEastNorthWest();

    public Cursor rhombus();

    public Cursor rhombusFilled();

    public Cursor triangleDown();

    public Cursor triangleDownFilled();

    public Cursor triangleRight();

    public Cursor triangleRightFilled();

    public Cursor triangleLeft();

    public Cursor triangleLeftFilled();

    public Cursor arrowsCrossed();

    public Cursor multiMove();

    public Cursor rotate();

    public Cursor rotateMany();

    public Cursor dottedRect();

    public Cursor arrowPlus();

    public Cursor shortArrow();

    public Cursor closeShape();

    public Cursor arrowTilde();

    public Cursor cursorPerpendicularTo(double angle);

    public Cursor cursorPerpendicularToQuadrant(Quadrant quad);
}
