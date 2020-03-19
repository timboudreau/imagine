/*
 * The MIT License
 *
 * Copyright 2020 Mastfrog Technologies.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package net.java.dev.imagine.api.vector.util.plot;

import com.mastfrog.function.DoubleTriConsumer;

/**
 * Takes plotted points from a ShapePlotter, and coerces them into an integer
 * space, passing weighted values for surrounding integer coordinates to
 * simulate antialiasing. The result is close to, but not exactly, what Java2D's
 * default antialiasing algorithm does.
 *
 * @author Tim Boudreau
 */
@FunctionalInterface
public interface RasterPlotter extends Plotter {

    void plot(int x, int y, double c);

    default void plot(double x, double y, double c, double tanX1, double tanY1, double tanX2, double tanY2) {
        if (c <= 0) {
            return;
        }
//        System.out.println("X " + x + " " + tanX1 + " " + tanX2);
//        System.out.println("Y " + y + " " + tanY1 + " " + tanY2);
        wdistribute(x, y, tanX1, tanY1, tanX2, tanY2, c, (x1, y1, c1) -> {
            plot((int) x1, (int) y1, c1);
        });
        /*
        int neighborsUsed = Direction.centeredNeighborPercentages(x, y, (dir, factor) -> {
            int dx = (int) (x + dir.xOffset());
            int dy = (int) (y + dir.yOffset());
            plot(dx, dy, c * factor);
        });
        if (neighborsUsed == 0) {
            plot((int) x, (int) y, c);
        }
         */
    }

    default void wdistribute(double x, double y, double tanX1, double tanY1, double tanX2, double tanY2, double amt, DoubleTriConsumer c) {
        c.accept(x, y, amt);
        if (ShapePlotter.ix++ % 1 == 0) {
            ShapePlotter.plotLine(tanX1, tanY1, tanX2, tanY2, 1,
                    (double x1, double y1, double c1, double tanX3, double tanY3, double tanX4, double tanY4) -> {
                        RasterPlotter.this.plot((int) Math.round(x1), (int) Math.round(y1), 0.65D);
                    }, false, -1);
        }
    }

    default void jdistribute(double x, double y, double tanX1, double tanY1, double tanX2, double tanY2, double amt, DoubleTriConsumer c) {
        if (tanX1 == tanX2 && tanY1 == tanY2) {
            c.accept(x, y, amt);
            return;
        }
//        if (Math.abs(tanX1 - x) > 1 && Math.abs(tanY1 - y) > 1) {
//            System.out.println("a. BAD TAN VALS " + x + "," + y + " vs " + tanX1 + "," + tanY1);
//        }
//        if (Math.abs(tanX2 - x) > 1 && Math.abs(tanY2 - y) > 1) {
//            System.out.println("b. BAD TAN VALS " + x + "," + y + " vs " + tanX1 + "," + tanY1);
//        }
        double floorX = Math.floor(x);
        double floorY = Math.floor(y);
        double centerX = floorX + 0.5D;
        double centerY = floorY + 0.5D;

        double offX = x - centerX;
        double offY = y - centerY;

//        RasterPlotter.this.plot((int) tanX1, (int) tanY1, amt * 0.5);
//        RasterPlotter.this.plot((int) x, (int) y, amt);
//        RasterPlotter.this.plot((int) tanX2, (int) tanY2, amt * 0.5);
        Direction.centeredNeighborPercentages(x, y, (dir, pct) -> {
            if (pct == 0) {
                return;
            }
            double x1 = floorX + dir.xOffset();
            double y1 = floorY + dir.yOffset();

//            if (Math.abs(x1 - tanX1) >= Math.abs(x - x1)) {
//                pct *= 1.125;
//            } else {
//                pct *= 0.625;
//            }
//            if (Math.abs(y1 - tanY1) >= Math.abs(x - x1)) {
//                pct *= 1.125;
//            } else {
//                pct *= 0.625;
//            }
//            if (Math.abs(x1 - tanX2) >= Math.abs(x - x1)) {
//                pct *= 1.125;
//            } else {
//                pct *= 0.625;
//            }
//            if (Math.abs(y1 - tanY2) >= Math.abs(y - y1)) {
//                pct *= 1.125;
//            } else {
//                pct *= 0.625;
//            }
            double v = amt * pct;
            RasterPlotter.this.plot((int) x1, (int) y1, v);
        });
    }

    default void distribute(double x, double y, double tanX1, double tanY1, double tanX2, double tanY2, double amt, DoubleTriConsumer c) {
        if (tanX1 == tanX2 && tanY1 == tanY2) {
            c.accept(x, y, amt);
            return;
        }
        double floorX = Math.floor(x);
        double floorY = Math.floor(y);

        Direction.centeredNeighborPercentages(x, y, (dir, pct) -> {
            double x1 = floorX + dir.xOffset();
            double y1 = floorY + dir.yOffset();
            if (pct > 0) {

                double v = amt * pct;
                RasterPlotter.this.plot((int) x1, (int) y1, v);
            }
        });
    }

    static int ceilOrFloor(double a, double b) {
        if (b > a) {
            return (int) Math.floor(b);
        } else if (a > b) {
            return (int) Math.ceil(b);
        }
        return (int) Math.round(b);
    }
}
