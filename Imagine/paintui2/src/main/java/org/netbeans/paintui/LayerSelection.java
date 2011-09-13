/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paintui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collections;
import net.dev.java.imagine.api.selection.Selection;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.PictureImplementation;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.netbeans.paint.api.util.GraphicsUtils;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
class LayerSelection implements Transferable, ClipboardOwner {
    private final boolean allLayers;
    private final boolean isCut;
    private final PictureImplementation picture;

    LayerSelection(PictureImplementation pi, boolean allLayers, boolean isCut) {
        this.allLayers = allLayers;
        this.picture = pi;
        this.isCut = isCut;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return new DataFlavor[]{DataFlavor.imageFlavor, PaintTopComponent.LAYER_DATA_FLAVOR};
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(DataFlavor.imageFlavor) || flavor.equals(PaintTopComponent.LAYER_DATA_FLAVOR);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (flavor.equals(DataFlavor.imageFlavor)) {
            Dimension d = picture.getSize();
            LayerImplementation layer = picture.getActiveLayer();
            if (!allLayers && layer == null) {
                return null;
            }
            Selection sel = layer == null ? null : layer.getLookup().lookup(Selection.class);
            Shape clip = sel == null ? null : sel.asShape();
            BufferedImage nue = new BufferedImage(d.width, d.height, GraphicsUtils.DEFAULT_BUFFERED_IMAGE_TYPE);
            Graphics2D g = nue.createGraphics();
            GraphicsUtils.setHighQualityRenderingHints(g);
            if (clip != null) {
                g.setClip(clip);
            }
            if (allLayers) {
                picture.paint(g, null, false);
            } else {
                layer.paint(g, null, false);
            }
            g.dispose();
            if (!isCut) {
                Iterable<LayerImplementation> toCutFrom = allLayers ? picture.getLayers() : Collections.singleton(layer);
                for (LayerImplementation l : toCutFrom) {
                    if (!l.isVisible()) {
                        continue;
                    }
                    SurfaceImplementation impl = l.getSurface();
                    impl.beginUndoableOperation(NbBundle.getMessage(PictureScene.PI.class, "CUT", l.getName())); //NOI18N
                    Graphics2D gg = impl.getGraphics();
                    try {
                        Shape cl = clip;
                        if (cl == null) {
                            cl = layer.getBounds();
                        }
                        if (sel != null) {
                            gg.setColor(new Color(0, 0, 0, 0));
                        }
                        gg.setClip(clip);
                        Rectangle bds = clip.getBounds();
                        gg.setBackground(new Color(0, 0, 0, 0));
                        gg.clearRect(bds.x, bds.y, bds.width, bds.height);
                    } finally {
                        gg.dispose();
                        impl.endUndoableOperation();
                    }
                }
            }
            if (clip != null) {
                Rectangle r = clip.getBounds();
                if (r.width != 0 && r.height != 0) {
                    nue = nue.getSubimage(r.x, r.y, Math.min(nue.getWidth(), r.width), Math.min(nue.getHeight(), r.height));
                }
            }
            return nue;
        } else if (flavor.equals(PaintTopComponent.LAYER_DATA_FLAVOR)) {
            //XXX pending
            return null;
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        //do nothing
    }
    
}
