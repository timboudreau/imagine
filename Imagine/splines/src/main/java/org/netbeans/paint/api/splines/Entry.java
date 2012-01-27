package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.io.Serializable;
import org.openide.util.NbBundle;

public interface Entry extends Iterable<ControlPoint>, Cloneable, Serializable {

    public void perform(GeneralPath path);

    public void draw(Graphics2D g);

    public Rectangle getDrawBounds(Rectangle r, int areaSize);

    public ControlPoint[] getControlPoints();

    public int size();

    public Object clone();

    public Kind kind();

    public enum Kind {

        MoveTo,
        LineTo,
        CurveTo,
        QuadTo,
        Close;

        @Override
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
