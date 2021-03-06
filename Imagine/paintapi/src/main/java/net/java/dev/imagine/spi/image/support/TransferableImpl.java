/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.spi.image.support;

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
import java.util.HashSet;
import java.util.Set;
import net.dev.java.imagine.api.selection.Selection;
import net.java.dev.imagine.api.image.RenderingGoal;
import net.java.dev.imagine.spi.image.LayerImplementation;
import net.java.dev.imagine.spi.image.SurfaceImplementation;
import org.imagine.editor.api.AspectRatio;
import org.imagine.editor.api.Zoom;
import org.imagine.utils.java2d.GraphicsUtils;
import org.openide.util.NbBundle;

/**
 *
 * @author Tim Boudreau
 */
class TransferableImpl implements Transferable, ClipboardOwner {

    private final boolean allLayers;
    private final boolean isCut;
    private final AbstractPictureImplementation picture;

    TransferableImpl(AbstractPictureImplementation pi, boolean allLayers, boolean isCut) {
        this.allLayers = allLayers;
        this.picture = pi;
        this.isCut = isCut;
    }

    private Set<DataFlavor> transferFlavors() {
        Set<DataFlavor> customTypes = picture.copyTypes();
        if (customTypes.isEmpty()) {
            return Collections.singleton(DataFlavor.imageFlavor);
        } else {
            customTypes = new HashSet<>(customTypes);
            customTypes.add(DataFlavor.imageFlavor);
        }
        return customTypes;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
        Set<DataFlavor> flavors = transferFlavors();
        return flavors.toArray(new DataFlavor[flavors.size()]);
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return transferFlavors().contains(flavor);
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

            BufferedImage nue = GraphicsUtils.newBufferedImage(d.width, d.height, g -> {
                GraphicsUtils.setHighQualityRenderingHints(g);
                if (clip != null) {
                    g.setClip(clip);
                }
                Rectangle r = new Rectangle(0, 0, d.width, d.height);
                if (allLayers) {
                    picture.paint(RenderingGoal.PRODUCTION, g, r, false, Zoom.ONE_TO_ONE);
                } else {
                    if (layer.isVisible()) {
                        layer.paint(RenderingGoal.PRODUCTION, g, r, false, false, Zoom.ONE_TO_ONE,
                                AspectRatio.create(d));
                    }
                }
            });
            if (isCut) {
                Iterable<LayerImplementation> toCutFrom = allLayers ? picture.getLayers() : Collections.singleton(layer);
                for (LayerImplementation l : toCutFrom) {
                    if (!l.isVisible()) {
                        continue;
                    }
                    SurfaceImplementation impl = l.getSurface();
                    impl.beginUndoableOperation(NbBundle.getMessage(TransferableImpl.class,
                            "CUT", l.getName())); //NOI18N
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
        } else if (transferFlavors().contains(flavor)) {
            return picture.createClipboardContents(flavor);
        } else {
            throw new UnsupportedFlavorException(flavor);
        }
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }
}
