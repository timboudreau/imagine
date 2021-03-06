/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.splines;

import java.awt.geom.GeneralPath;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.*;
import static org.junit.Assert.*;
import org.netbeans.paint.api.splines.Entry.Kind;

/**
 *
 * @author tim
 */
public class DefaultPathModelTest {

    @Test
    public void test() {
        assertTrue(true);

        DefaultPathModel<Entry> m = new DefaultPathModel();
        m.add(Kind.MoveTo, 10, 10);
        m.add(Kind.LineTo, 15, 15);
        m.add(Kind.LineTo, 17, 17);
        m.add(new CurveTo(20, 20, 18, 18, 21, 21));

        assertEquals(4, m.size());

        assertEquals(Kind.MoveTo, m.get(0).kind());
        assertEquals(Kind.LineTo, m.get(1).kind());
        assertEquals(Kind.LineTo, m.get(2).kind());
        assertEquals(Kind.CurveTo, m.get(3).kind());


        Edge[] edges = m.getPathEdges();
        assertEquals(3, edges.length);
        assertEquals(Kind.MoveTo, edges[0].getSourcePoint().getEntry().kind());
        assertEquals(Kind.LineTo, edges[0].getTargetPoint().getEntry().kind());

        assertEquals(Kind.LineTo, edges[1].getSourcePoint().getEntry().kind());
        assertEquals(Kind.LineTo, edges[1].getTargetPoint().getEntry().kind());

        assertEquals(Kind.LineTo, edges[2].getSourcePoint().getEntry().kind());
        assertEquals(Kind.CurveTo, edges[2].getTargetPoint().getEntry().kind());

        Set<Edge> allEdges = m.allEdges();
        assertEquals(edges.length + 2, allEdges.size());

        Entry ct = m.get(3);
        Set<Edge> ctEdges = new HashSet<Edge>();
        for (ControlPoint p : ct.getControlPoints()) {
            ctEdges.addAll(Arrays.asList(p.getEdges()));
        }
        assertTrue(allEdges.containsAll(ctEdges));

        GeneralPath path = new GeneralPath();
        for (Entry e : m) {
            e.perform(path);
        }
        assertTrue(ShapeUtils.shapesEqual(m, path, false));

        Edge lines = edges[1];
        lines.translate(1, 1);
        ControlPoint aa = m.get(1).getControlPoints()[0];
        ControlPoint bb = m.get(2).getControlPoints()[0];
        assertEquals(16D, aa.getX(), 0.01D);
        assertEquals(16D, aa.getY(), 0.01D);

        assertEquals(18D, bb.getX(), 0.01D);
        assertEquals(18D, bb.getY(), 0.01D);

        Edge[] edges2 = m.getPathEdges();
        for (int i = 0; i < edges.length; i++) {
            assertEquals("Not equal at " + i, edges[i], edges2[i]);
//            assertSame("not same at " + i, edges[i], edges2[i]);
        }

        m.add(Kind.QuadTo, 30, 30);
        path = new GeneralPath();
        for (Entry e : m) {
            e.perform(path);
        }
        assertTrue(ShapeUtils.shapesEqual(m, path, false));

        assertEquals(m, DefaultPathModel.create(path));
        assertEquals(m, DefaultPathModel.copy(m));
    }

    @Test
    public void x() throws Throwable{
        GeneralPath gp = new GeneralPath();
        gp.moveTo(0.0D, 0.0D);
        gp.lineTo(91.0D, 0.0D);
        gp.quadTo(141.0D, 16.0D, 175.0D, 72.0);
        gp.lineTo(175.0D, 98.0D);
        gp.quadTo(159.0D, 128.0D, 117.0D, 143.0);
        gp.lineTo(52.0D, 143.0D);
        gp.quadTo(8.0D, 122.0D, 0.0D, 86.0);
        gp.lineTo(0.0D, 57.0D);
        gp.curveTo(15.0D, 22.0D, 114.0D, 35.0D, 118.0D, 70.0D);
        gp.curveTo(130.0D, 138.0D, 17.0D, 104.0D, 50.0D, 60.0D);
        gp.curveTo(27.0D, 60.0D, 19.0D, 61.0D, 20.0D, 77.0D);
        gp.curveTo(45.0D, 159.0D, 142.0D, 123.0D, 156.0D, 86.0D);
        gp.curveTo(117.0D, 11.0D, 38.0D, 6.0D, 0.0D, 11.0D);
        gp.closePath();
        
        PathModel mdl = DefaultPathModel.create(gp);
        File f = new File ("/tmp/PathsTemplate.paths");
        ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f));
        out.writeObject(mdl);
        out.close();
        System.err.println("Wrote " + f.getPath());
    }
}
