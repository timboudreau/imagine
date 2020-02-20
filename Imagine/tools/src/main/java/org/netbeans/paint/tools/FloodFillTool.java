/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import net.dev.java.imagine.api.selection.Selection;
import net.dev.java.imagine.api.tool.aspects.Attachable;
import net.dev.java.imagine.spi.tool.Tool;
import net.dev.java.imagine.spi.tool.ToolDef;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.dev.java.imagine.api.tool.aspects.CustomizerProvider;
import net.dev.java.imagine.api.tool.aspects.PaintParticipant;
import net.java.dev.imagine.api.image.Layer;
import net.java.dev.imagine.api.image.Surface;
import net.java.dev.imagine.api.toolcustomizers.AggregateCustomizer;
import net.java.dev.imagine.api.toolcustomizers.Constants;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.utils.java2d.TrackingGraphics;
import org.openide.util.Lookup;
import org.openide.util.Lookup.Provider;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author Tim Boudreau
 */

@ToolDef(name="FLOOD_FILL", iconPath="org/netbeans/paint/tools/resources/floodfill.png")
@Tool(Surface.class)
public class FloodFillTool extends MouseAdapter implements /* Tool, MouseListener, */ CustomizerProvider, PaintParticipant, Attachable {

    //Flood fill algorithm from http://www.codecodex.com/wiki/index.php?title=Implementing_the_flood_fill_algorithm
    private Repainter repainter;
    private Surface surface;
    private Lookup.Provider layer;
    public FloodFillTool(Surface surface) {
        this.surface = surface;
    }
    
    @Override
    public void attach(Provider on) {
        this.layer = on;
        getCustomizer(); //xxx initialization?
    }

    @Override
    public void detach() {
        this.layer = null;
    }
    
    public Lookup getLookup() {
        return Lookups.singleton(this);
    }

    public boolean canAttach(Layer layer) {
        return layer.getSurface().getImage() != null;
    }
    
    private Customizer<Integer> threshC = Customizers.getCustomizer(Integer.class, Constants.THRESHOLD, 0, 255);
    private Customizer<Color> colorC = Customizers.getCustomizer(Color.class, Constants.FOREGROUND);
    
    public Customizer getCustomizer() {
        return new AggregateCustomizer("foo", threshC, colorC);
    }
    
    private int threshold = 0;
    @Override
    public void mouseClicked(MouseEvent e) {
            BufferedImage img = surface.getImage();
            /*
            if (img == null) {
                RasterConverter conv = RasterConverter.getDefault();
                if (conv != null) {
                    Layer newLayer = RasterConverter.askUserToConvert(layer, null, this);
                    if (newLayer != null) {
                        surface = newLayer.getSurface();
                        img = surface.getImage();
                        if (img == null) {
                            return;
                        }
                        surface.setTool(this);
                    }
                }
            }
            */
            if (img == null) {
                return;
            }
            threshold = threshC.get();
            Color color = colorC.get();
            surface.beginUndoableOperation(toString()); //XXX
            try {
                WritableRaster raster = img.getRaster();
                int red = color.getRed();
                int green = color.getGreen();
                int blue = color.getBlue();
                int alpha = 255;
                Point p = e.getPoint();
                Selection s = layer.getLookup().lookup(Selection.class);
                if (s != null) {
                    selection = s.asShape();
                }
                this.floodLoop(raster, p.x, p.y, new int[] { red, green, blue, alpha });
                selection = null;
                repainter.requestRepaint();
                if (surface.getGraphics() instanceof TrackingGraphics) {
                    System.err.println("Area modified: " + minX + "," + minY + "," + (maxX - minX)+ "," + (maxY - minY));
                    ((TrackingGraphics) surface.getGraphics()).areaModified(minX, minY,
                            maxX - minX, maxY - minY);
                }
            } finally {
                surface.endUndoableOperation();
            }
    }
    private Shape selection;

    int checkPixel(int[] oldPix, int[] newPix) {
        if (threshold == 0) {
            return (newPix[0] == oldPix[0] &&
                    newPix[1] == oldPix[1] &&
                    newPix[2] == oldPix[2] &&
                    newPix[3] == oldPix[3] ? 1 : 0);
        } else {
            return (checkThreshold(newPix[0], oldPix[0]) &&
                    checkThreshold(newPix[1], oldPix[1]) &&
                    checkThreshold (newPix[2], oldPix[2]) &&
                    checkThreshold (newPix[3], oldPix[3]) ? 1 : 0);
        }
    }
    
    boolean checkThreshold(int a, int b) {
        int test = a - b;
        if (test < 0) test *= -1;
        return test <= threshold;
    }

    int minX;
    int minY;
    int maxX;
    int maxY;
    void floodLoop(WritableRaster raster, int x, int y, int[] fill, int[] old) {
        int fillL, fillR, i;
        int in_line = 1;
        int[] aux = {255, 255, 255, 255};

        Rectangle d = raster.getBounds();

        // find left side, filling along the way
        fillL = fillR = x;
        while (in_line != 0) {
            int[] p = raster.getPixel(fillL, y, aux);
            if (selection != null) {
                if (selection.contains(fillL, y)) {
//                    try {
                        raster.setPixel(fillL, y, fill);
                        maxX = Math.max (fillL, maxX);
                        maxY = Math.max (y, maxY);
                        minX = Math.min (fillL, minX);
                        minY = Math.min (y, minY);
//                    } catch (ArrayIndexOutOfBoundsException e) {
//                        throw new ArrayIndexOutOfBoundsException("Out of bounds: " + fillL + ", " + y);
//                    }
                    
                    fillL--;
                    in_line = (fillL < 0) ? 0 : checkPixel(raster.getPixel(fillL, y, aux), old);
                } else {
                    fillR--;
                    in_line = 0;
                }
            } else {
                fillL--;
                if (fillL < 0) {
                    break;
                }
                in_line = (fillL < 0) ? 0 : checkPixel(raster.getPixel(fillL, y, aux), old);
//                try {
                    raster.setPixel(fillL, y, fill);
                    maxX = Math.max (fillL, maxX);
                    maxY = Math.max (y, maxY);
                    minX = Math.min (fillL, minX);
                    minY = Math.min (y, minY);
//                } catch (ArrayIndexOutOfBoundsException e) {
//                    throw new ArrayIndexOutOfBoundsException("Out of bounds: " + fillL + ", " + y);
//                }
            }
        }
        fillL++;

        // find right side, filling along the way
        in_line = 1;
        while (in_line != 0) {
            if (selection != null) {
                if (selection.contains (fillR, y)) {
//                    try {
                        raster.setPixel(fillR, y, fill);
                        maxX = Math.max (fillR, maxX);
                        maxY = Math.max (y, maxY);
                        minX = Math.min (fillR, minX);
                        minY = Math.min (y, minY);
//                    } catch (ArrayIndexOutOfBoundsException e) {
//                        throw new ArrayIndexOutOfBoundsException("Out of bounds: " + fillR + ", " + y);
//                    }
                    fillR++;
                    in_line = (fillR > d.width - 1) ? 0 : checkPixel(raster.getPixel(fillR, y, aux), old);
                } else {
                    fillR++;
                    in_line = 0;
                }
            } else {
//                try {
                    raster.setPixel(fillR, y, fill);
                    maxX = Math.max (fillR, maxX);
                    maxY = Math.max (y, maxY);
                    minX = Math.min (fillR, minX);
                    minY = Math.min (y, minY);
//                } catch (ArrayIndexOutOfBoundsException e) {
//                    throw new ArrayIndexOutOfBoundsException("Out of bounds: " + fillR + ", " + y);
//                }
                fillR++;
                in_line = (fillR > d.width - 1) ? 0 : checkPixel(raster.getPixel(fillR, y, aux), old);
            }
        }
        fillR--;

        // look up and down
        for (i = fillL; i <= fillR; i++) {
            if (y > 0 && checkPixel(raster.getPixel(i, y - 1, aux), old) != 0) {
                floodLoop(raster, i, y - 1, fill, old);
            }
            if (y < d.height - 1 && checkPixel(raster.getPixel(i, y + 1, aux), old) != 0) {
                floodLoop(raster, i, y + 1, fill, old);
            }
        }

    }

    // Initial method you must call
    void floodLoop(WritableRaster raster, int x, int y, int[] fill) {
        int[] aux = new int[]{255, 255, 255, 255};

        // validation so we don't fall in an infinite loop trying to
        // paint in the same color
        if (checkPixel(raster.getPixel(x, y, aux), fill) != 0) {
            return;
        }

        floodLoop(raster, x, y, fill, raster.getPixel(x, y, aux));
    }

    public void attachRepainter(Repainter repainter) {
        this.repainter = repainter;
    }

    public void paint(Graphics2D g2d, Rectangle layerBounds, boolean commit) {
        //do nothing, we implement PaintParticipant to get a RepaintHandle
    }
}
