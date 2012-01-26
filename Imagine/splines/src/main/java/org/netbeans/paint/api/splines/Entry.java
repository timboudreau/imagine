package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import org.openide.util.NbBundle;

public interface Entry extends Iterable<ControlPoint> {

    public void perform(GeneralPath path);

    public void draw(Graphics2D g);

    public Rectangle getDrawBounds(Rectangle r, int areaSize);

    public ControlPoint[] getControlPoints();

    public int size();

    public int hit(Point2D pt, double areaSize);

    public Object clone();

    public Kind kind();

    public enum Kind {

        MoveTo,
        LineTo,
        CurveTo,
        QuadTo,
        Close;

        public String toString() {
            return NbBundle.getMessage(Kind.class, name());
        }

        public static Kind forName(String s) {
            for (Kind k : values()) {
                if (s.equalsIgnoreCase(k.name())) {
                    return k;
                }
            }
            return null;
        }
    }
}
