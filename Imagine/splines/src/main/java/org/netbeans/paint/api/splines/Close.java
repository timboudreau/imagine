package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Collections;
import java.util.Iterator;

public class Close implements Entry {
    DefaultPathModel model;

    public void perform(GeneralPath path) {
        path.closePath();
    }

    public void draw(Graphics2D g) {
        //do nothing
    }
    
    public Close clone() {
        return new Close();
    }

    public Rectangle getDrawBounds(Rectangle r, int areaSize) {
        return new Rectangle (0,0);
    }

    public ControlPointImpl[] getControlPoints() {
        return new ControlPointImpl[0];
    }

    public int size() {
        return 1;
    }

    public int hit(Point2D pt, double areaSize) {
        return 0;
    }
    
//    public boolean equals(Object o) {
//        return o instanceof Close;
//    }
//    
//    public int hashCode() {
//        return 17;
//    }
    
    public boolean equals(Object o) {
        return o == this ? true : o instanceof Close && ((Close) o).model == model && (model == null || ((Close) o).model.indexOf(o) == model.indexOf(o));
    }
    
    public int hashCode() {
        return model == null ? -2 : model.indexOf(this);
    }

    @Override
    public Kind kind() {
        return Kind.Close;
    }

    @Override
    public Iterator<ControlPoint> iterator() {
        return Collections.<ControlPoint>emptySet().iterator();
    }
}