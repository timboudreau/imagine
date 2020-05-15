package org.netbeans.paint.tools.waffle;

import org.netbeans.paint.tools.spi.PathCreator;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JComponent;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.spi.tool.ToolElement;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.geometry.EqLine;
import org.imagine.geometry.EqPointDouble;
import org.netbeans.paint.api.components.explorer.Customizable;
import static org.netbeans.paint.tools.spi.PathCreator.REGISTRATION_PATH;

/**
 *
 * @author Tim Boudreau
 */
@ToolElement(folder = REGISTRATION_PATH, name = "Waffle", position = 100)
public class WaffleCreator implements PathCreator, Customizable {

//    private static final Customizer<Boolean> close = Customizers.getCustomizer(
//            Boolean.class, "Close", true);
    private static final Customizer<Double> frequency = Customizers
            .getCustomizer(Double.class, "Frequency", 1D, 500D, 30D);
    private static final Customizer<Double> offsetX = Customizers
            .getCustomizer(Double.class, "OffsetX", 2D, 250D, 10D);
    private static final Customizer<Double> offsetY = Customizers
            .getCustomizer(Double.class, "OffsetY", 1D, 250D, 20D);

    @Override
    public JComponent getCustomizer() {
        return new AggregateCustomizer(Bundle.waffle(), frequency, offsetX, offsetY).getComponent();
    }

    @Override
    public Shape create(boolean close, boolean commit, List<EqPointDouble> points, EqPointDouble nextProposal) {
        switch (points.size()) {
            case 0:
                return null;
            case 1:
                if (nextProposal == null) {
                    return null;
                }
                points = new ArrayList<>(points);
                points.add(nextProposal);
                break;
        }
        if (points.size() < 2) {
            return null;
        }
        Path2D.Double p = new Path2D.Double(Path2D.WIND_EVEN_ODD);
        Iterator<? extends EqPointDouble> iter = points.iterator();
        EqPointDouble prev = iter.next();
        p.moveTo(prev.x, prev.y);
        int ix = 0;
        while (iter.hasNext()) {
            EqPointDouble curr = iter.next();
            applyPoints(ix++, prev, curr, p);
            prev = curr;
        }
        if (close) {
            p.closePath();
        }
        return p;
    }

    private void applyPoints(int index, EqPointDouble prev, EqPointDouble curr, Path2D into) {
        EqLine line = new EqLine(prev, curr);
        double ang = line.angle();
        double len = line.length();
        double freq = frequency.get();
        double count = len / freq;
        int loops;
        if (count < 1) {
            count = loops = 1;
        } else if (len % freq != 0) {
            loops = (int) Math.floor(count);
            double lastPos = freq * Math.floor(count);
            if (lastPos == 0) {
                lastPos = freq;
                count = loops = 1;
            } else if (lastPos - (freq * loops) > freq % 2) {
                count = ++loops;
            }
        } else {
            loops = (int) Math.floor(count);
        }
        double distancePer = len / count;
        EqPointDouble lastPoint = prev.copy();
        for (int i = 0; i < loops; i++) {
            EqLine seg = EqLine.forAngleAndLength(lastPoint.x, lastPoint.y, ang, distancePer);
            lastPoint = applyPoints(index, i, seg, into);
        }
    }

    private EqPointDouble applyPoints(int index, int subIndex, EqLine seg, Path2D into) {
        int item = index + subIndex;

        EqPointDouble cp1 = controlPoint1(item, seg);
        EqPointDouble cp2 = controlPoint2(item, seg);
        into.curveTo(cp1.x, cp1.y, cp2.x, cp2.y, seg.x2, seg.y2);
        return seg.getP2();
    }

    private EqPointDouble controlPoint1(int lineIndex, EqLine line) {
        double offset = offsetX.get();
        if (lineIndex % 2 == 0) {
            offset *= -1;
        }
        line.shiftPerpendicular(offset);
        EqPointDouble result = line.midPoint();
        line.shiftPerpendicular(-offset);
        return result;

    }

    private EqPointDouble controlPoint2(int lineIndex, EqLine line) {
        double offset = offsetY.get();
        if (lineIndex % 2 == 0) {
            offset *= -1;
        }
        line.shiftPerpendicular(offset);
        EqPointDouble result = line.midPoint();
        line.shiftPerpendicular(-offset);
        return result;
    }

}
