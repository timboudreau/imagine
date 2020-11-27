package org.netbeans.paint.tools.minidesigner;

import java.util.function.BiConsumer;
import java.util.function.Supplier;
import com.mastfrog.geometry.EqLine;
import com.mastfrog.geometry.EqPointDouble;

/**
 *
 * @author Tim Boudreau
 */
interface PointsProvider extends Supplier<EqLine> {

    void withStartAndEnd(BiConsumer<EqPointDouble, EqPointDouble> c);

    default EqLine get() {
        EqLine ln = new EqLine();
        withStartAndEnd(ln::setLine);
        return ln;
    }
}
