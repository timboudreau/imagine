/*
 * PathModel.java
 *
 * Created on July 19, 2006, 3:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.netbeans.paint.api.splines;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;
import java.util.Set;
import javax.swing.event.ChangeListener;
import org.netbeans.paint.api.splines.Entry.Kind;

/**
 *
 * @author Tim Boudreau
 */
public interface PathModel <T extends Entry> extends List <T>, Shape, Serializable {
    void addChangeListener (ChangeListener cl);
    void removeChangeListener (ChangeListener cl);
    void setPoint (ControlPoint node, Point2D where);
    T add(Kind kind, double x, double y);
    T add(int index, Kind kind, double x, double y);
    void translate (double offX, double offY);
    void transform(AffineTransform xform);
    Set<ControlPoint> mainControlPoints();
    Set<ControlPoint> allControlPoints();
    Set<Edge> allEdges();
    Edge[] getPathEdges();
    void addShape(Shape shape);
    void setToShape(Shape shape);
}
