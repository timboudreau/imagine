package org.imagine.vector.editor.ui.palette;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.LinearGradientPaint;
import java.awt.MultipleGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleSupplier;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JToolBar;
import javax.swing.WindowConstants;
import net.java.dev.imagine.api.vector.elements.PathIteratorWrapper;
import org.imagine.awt.key.PaintKey;

/**
 *
 * @author Tim Boudreau
 */
public class Demo {

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "lcd_hrgb");
        System.setProperty("swing.aatext", "true");
        main1(args);
        main2(args);
//        main2(args);
        main1(args);
    }
    public static void main1(String[] args) {
        InMemoryShapePaletteBackend stor = new InMemoryShapePaletteBackend();
        PaletteStorage.CACHE.put("shapes", stor);
        for (int i = 0; i < 5; i++) {
            stor.addRandom();
        }
        ShapeTileFactory shapeTiles = new ShapeTileFactory(stor);
        PaletteItemsPanel panel = new PaletteItemsPanel(shapeTiles);
        JFrame jf = new JFrame();
        jf.setLayout(new BorderLayout());
        jf.add(panel, BorderLayout.CENTER);
        JToolBar bar = new JToolBar();
        jf.add(bar, BorderLayout.NORTH);
        JButton add = new JButton("Add");

        bar.add(add);
        add.addActionListener(ae -> {
            stor.addRandom();
        });
        jf.setPreferredSize(new Dimension(400, 600));
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.pack();
        jf.setLocationByPlatform(true);
        jf.setVisible(true);
    }

    public static void main2(String[] args) {
        InMemoryPaintPaletteBackend stor = new InMemoryPaintPaletteBackend();
        PaletteStorage.CACHE.put("paints", stor);
        for (int i = 0; i < 12; i++) {
            stor.addRandom();
        }
        PaintKeyTileFactory paintTiles = new PaintKeyTileFactory(stor);
        PaletteItemsPanel panel = new PaletteItemsPanel(paintTiles);
        JFrame jf = new JFrame();
        jf.setLayout(new BorderLayout());
        jf.add(panel, BorderLayout.CENTER);
        JToolBar bar = new JToolBar();
        jf.add(bar, BorderLayout.NORTH);
        JButton add = new JButton("Add");
        bar.add(add);
        add.addActionListener(ae -> {
            stor.addRandom();
        });
        jf.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        jf.setPreferredSize(new Dimension(400, 600));
        jf.pack();
        jf.setLocationByPlatform(true);
        jf.setVisible(true);
    }

    static Random rnd = new Random(5305L);

    static PathIteratorWrapper randomShape() {
        Path2D.Double path = new Path2D.Double(PathIterator.WIND_NON_ZERO);
        int count = rnd.nextInt(5) + 4;
        DoubleSupplier dbl = () -> {
            return (rnd.nextDouble() * 400) + 5;
        };
        for (int i = 0; i < count; i++) {
            if (i == 0) {
                path.moveTo(dbl.getAsDouble(), dbl.getAsDouble());
            } else if (i != count - 1) {
                int t = rnd.nextInt(3);
                switch (t) {
                    case 0:
                        path.lineTo(dbl.getAsDouble(), dbl.getAsDouble());
                        break;
                    case 1:
                        path.curveTo(dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble());
                    case 2:
                        path.quadTo(dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble(), dbl.getAsDouble());
                }
            } else {
                path.closePath();
            }
        }
        return new PathIteratorWrapper(path);
    }

    static Color randomColor() {
        return new Color(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255));
    }

    static PaintKey randomPaint() {
        Paint paint = null;
        int rn = rnd.nextInt(4);
        switch (rn) {
            case 0:
                paint = newGradientPaint();
                break;
            case 1:
                paint = newRadialPaint();
                break;
            case 2:
                paint = newLinearPaint();
                break;
            case 3 :
                paint = randomColor();
                break;
        }
        return PaintKey.forPaint(paint);
    }

    static Point2D randomPoint() {
        float x = rnd.nextFloat() * 640;
        float y = rnd.nextFloat() * 480;
        return new Point2D.Double(x, y);
    }

    static float[] randomFloats() {
        float[] result = new float[rnd.nextInt(20) + 2];
        result[result.length - 1] = 1;
        for (int i = 1; i < result.length - 1; i++) {
            do {
                result[i] = 0.1F + rnd.nextFloat() * 0.8F;
            } while (result[i - 1] == result[i]);
        }
        Arrays.sort(result);
        return result;
    }

    static Color[] randomColors(int count) {
        Color[] result = new Color[count];
        for (int i = 0; i < count; i++) {
            result[i] = randomColor();
        }
        return result;
    }

    static Paint newGradientPaint() {
        return new GradientPaint(randomPoint(), randomColor(),
                randomPoint(), randomColor(), rnd.nextBoolean());
    }

    static <T extends Enum<T>> T randomEnum(Class<T> type) {
        T[] consts = type.getEnumConstants();
        return consts[rnd.nextInt(consts.length)];
    }

    static Paint newRadialPaint() {
        int sz = Math.min(640, 480);
        float rad = rnd.nextFloat() * (float) sz;
        float[] fractions = randomFloats();
        Color[] colors = randomColors(fractions.length);
        return new RadialGradientPaint(randomPoint(), rad,
                randomPoint(), fractions, colors, randomEnum(MultipleGradientPaint.CycleMethod.class), randomEnum(MultipleGradientPaint.ColorSpaceType.class
        ), AffineTransform.getTranslateInstance(0, 0));
    }

    static Paint newLinearPaint() {
        float[] fractions = randomFloats();
        Color[] colors = randomColors(fractions.length);
        return new LinearGradientPaint(randomPoint(),
                randomPoint(), fractions, colors, randomEnum(MultipleGradientPaint.CycleMethod.class), randomEnum(MultipleGradientPaint.ColorSpaceType.class
        ), AffineTransform.getTranslateInstance(0, 0));
    }
}
