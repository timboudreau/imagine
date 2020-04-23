/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.palette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import javax.swing.BorderFactory;
import net.java.dev.imagine.api.image.RenderingGoal;
import org.imagine.vector.editor.ui.spi.ShapeElement;

/**
 *
 * @author Tim Boudreau
 */
final class ShapeTile extends Tile<ShapeElement> {

    public ShapeTile(String name) {
        super(name);
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
    }

    protected PaletteBackend<ShapeElement> storage() {
        return PaletteStorage.<ShapeElement>get("shapes", ShapePaletteStorage::new);
    }

    @Override
    protected void paintContent(ShapeElement item, Graphics2D g, int x, int y, int w, int h) {
        item.paint(RenderingGoal.THUMBNAIL, g, null);
    }

    private final Rectangle2D.Double scratch = new Rectangle2D.Double();

    @Override
    protected AffineTransform recomputeTransform(ShapeElement item, double x, double y, double w, double h) {
        if (w <= 0 || h <= 0) {
            return AffineTransform.getTranslateInstance(0, 0);
        }
        x += 3;
        y += 3;
        w -= 6;
        h -= 6;
        scratch.x = scratch.y = scratch.height = scratch.width = 0;
//        item.addToBounds(r);
        scratch.setFrame(item.shape().getBounds2D());

        double xRatio = w / scratch.width;
        double yRatio = h / scratch.height;
        double ratio;
        if (w > h) {
            ratio = xRatio;
        } else {
            ratio = yRatio;
        }
        AffineTransform result = AffineTransform.getScaleInstance(ratio, ratio);
        double[] d = new double[]{scratch.x, scratch.y};
        result.transform(d, 0, d, 0, 1);
        result.concatenate(AffineTransform.getTranslateInstance(-d[0] + x, -d[1] + y));

//        Rectangle2D b = result.createTransformedShape(r).getBounds2D();
//        if (b.getWidth() > w || b.getHeight() > h) {
//            System.out.println("\nBAD TRANFORM ");
//            System.out.println("orig bounds " + r.x + ", " + r.y + "  " + r.width + " x " + r.height);
//            System.out.println("aval bounds " + x + ", " + y + "  " + w + " x " + h);
//            System.out.println("resu bounds " + b.getX()
//                    + ", " + b.getY() + "  " + b.getWidth() + " x " + b.getHeight());
//        }
        return result;
    }
}
