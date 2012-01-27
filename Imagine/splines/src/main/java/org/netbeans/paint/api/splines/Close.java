package org.netbeans.paint.api.splines;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
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

    @Override
    public int size() {
        return 1;
    }
    
    @Override
    public String toString() {
        return "    gp.closePath();\n";
    }

    @Override
    public boolean equals(Object o) {
        return o == this ? true : o instanceof Close && ((Close) o).model == model && (model == null || ((Close) o).model.indexOf(o) == model.indexOf(this));
    }
    
    @Override
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