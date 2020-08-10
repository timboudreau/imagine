/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.vector.editor.ui.palette;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DragSourceListener;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.TransferHandler;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.utils.java2d.GraphicsUtils;
import org.netbeans.paint.api.cursor.Cursors;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle.Messages;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
abstract class AbstractTileFactory<T, X extends TransferHandler> implements TileFactory<T> {

    private static final String TILE_IMAGE_REF_CLIENT_PROPERTY = "tileImage";
    protected final DragSource dragSource = new DragSource();
    private final DGL dgl = new DGL();
    private X transferHandler;
    private final PalettePopupListener palettePopupListener = new PalettePopupListener();
    private final Set<JComponent> paletteComponents = new WeakSet<>();
    private final TilePopupListener popup = new TilePopupListener();

    protected abstract X createTransferHandler();

    private final Class<? super T> type;

    protected AbstractTileFactory(Class<? super T> type) {
        this.type = type;
    }

    protected boolean useDragAndDropImage(Tile<T> tile) {
        return true;
    }

    @Override
    public Tile<? super T> createTile(String tileName, T obj) {
        Tile<? super T> tile = createTile(tileName);
        if (obj != null) {
            tile.setItem(obj);
        }
        tile.addMouseListener(popup);
        configureDragAndDrop(tile);
        return tile;
    }

    @Override
    public final X getTransferHandler() {
        if (transferHandler == null) {
            transferHandler = createTransferHandler();
        }
        return transferHandler;
    }

    protected boolean allowDropOnTiles() {
        return true;
    }

    protected int dndActions(Tile<? super T> tile) {
        return DnDConstants.ACTION_COPY
                | DnDConstants.ACTION_COPY_OR_MOVE
                | DnDConstants.ACTION_MOVE;
    }

    protected void configureDragAndDrop(Tile<? super T> tile) {
        tile.setTransferHandler(createTransferHandler());
        if (allowDropOnTiles()) {
            DropTarget target = new DropTarget(tile,
                    DnDConstants.ACTION_COPY_OR_MOVE, dgl);
            tile.setDropTarget(target);
        }
        dragSource.createDefaultDragGestureRecognizer(tile,
                dndActions(tile),
                dgl);
    }

    protected boolean acceptDrag(DragGestureEvent evt) {
        return true;
    }

    protected Cursor dragCursor() {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    protected abstract boolean isOurTile(Component tile);

    protected BufferedImage getTileImage(Tile<?> tile) {
        Reference<BufferedImage> imgRef = (Reference<BufferedImage>) tile.getClientProperty(TILE_IMAGE_REF_CLIENT_PROPERTY);
        if (imgRef != null) {
            BufferedImage result = imgRef.get();
            if (result != null) {
                return result;
            }
        }
        Dimension sz = tile.getSize();
        sz.width /= 2;
        sz.height /= 2;
        BufferedImage result = GraphicsUtils.newBufferedImage(sz.width,
                sz.height, g -> {
                    GraphicsUtils.setHighQualityRenderingHints(g);
                    g.scale(0.5, 0.5);
                    tile.paint(g);
                });
        imgRef = new WeakReference<>(result);
        tile.putClientProperty(TILE_IMAGE_REF_CLIENT_PROPERTY, imgRef);
        return result;
    }

    protected Collection<? extends DataFlavor> supportedDataFlavors() {
        return Collections.emptySet();
    }

    protected Collection<? extends DataFlavor> paletteSupportedDataFlavors() {
        return Collections.emptySet();
    }

    final boolean hasSupportedDataFlavor(Collection<? extends DataFlavor> flavors) {
        Set<DataFlavor> all = new HashSet<>(supportedDataFlavors());
        all.retainAll(flavors);
        return !all.isEmpty();
    }

    final boolean hasPaletteSupportedDataFlavor(Collection<? extends DataFlavor> flavors) {
        Set<DataFlavor> all = new HashSet<>(paletteSupportedDataFlavors());
        all.retainAll(flavors);
        return !all.isEmpty();
    }

    protected boolean canDropOnTile(DropTargetDragEvent evt) {
        return true;
    }

    private boolean _canDropOnTile(DropTargetDragEvent evt) {
        return hasSupportedDataFlavor(evt.getCurrentDataFlavorsAsList())
                && canDropOnTile(evt);
    }

    protected boolean canDropOnPalette(DropTargetDragEvent evt) {
        return true;
    }

    private boolean _canDropOnPalette(DropTargetDragEvent evt) {
        return hasPaletteSupportedDataFlavor(evt.getCurrentDataFlavorsAsList())
                && canDropOnPalette(evt);
    }

    @SuppressWarnings("element-type-mismatch")
    private boolean isPaletteComponent(Component comp) {
        return paletteComponents.contains(comp);
    }

    private boolean doHandleDrop(DropTargetDropEvent evt) {
        if (isPaletteComponent(evt.getDropTargetContext().getComponent())) {
            Set<DataFlavor> all = new HashSet<>(paletteSupportedDataFlavors());
            all.retainAll(evt.getCurrentDataFlavorsAsList());
            if (!all.isEmpty()) {
                int result = handleAddToPalette(all, evt.getTransferable(), evt.getDropTargetContext().getComponent());
                if (result != -1) {
                    evt.acceptDrop(result);
                }
                return result != -1;
            }
            return false;
        } else {
            Set<DataFlavor> all = new HashSet<>(supportedDataFlavors());
            all.retainAll(evt.getCurrentDataFlavorsAsList());
            if (!all.isEmpty()) {
                int result = handleDropOnTile(all, evt.getTransferable(), evt.getDropTargetContext().getComponent());
                if (result != -1) {
                    evt.acceptDrop(result);
                }
                return result != -1;
            }
            return false;
        }
    }

    public void attachPaletteComponent(JComponent paletteComponent) {
        paletteComponents.add(paletteComponent);
        paletteComponent.addMouseListener(palettePopupListener);
        DropTarget dt = new DropTarget(paletteComponent,
                DnDConstants.ACTION_COPY_OR_MOVE, dgl);
        paletteComponent.setDropTarget(dt);
    }

    protected boolean acceptPaletteDrag(Component comp, DragGestureEvent e) {
        return true;
    }

    protected int handleDropOnTile(Set<DataFlavor> supportedFlavors, Transferable xfer, Component target) {
        return -1;
    }

    protected int handleAddToPalette(Set<DataFlavor> supportedFlavors, Transferable xfer, Component target) {
        return -1;
    }

    @Messages({
        "paste=&Paste"
    })
    private class PalettePasteAction extends AbstractAction {

        private final JComponent target;

        public PalettePasteAction(JComponent target) {
            this.target = target;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Transferable xfer = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
            if (xfer != null && hasPaletteSupportedDataFlavor(Arrays.asList(xfer.getTransferDataFlavors()))) {
                Set<DataFlavor> flavors = new LinkedHashSet<>(paletteSupportedDataFlavors());
                flavors.retainAll(Arrays.asList(xfer.getTransferDataFlavors()));
                AbstractTileFactory.this.handleAddToPalette(flavors, xfer, target);
            }
        }

        @Override
        public boolean isEnabled() {
            Transferable xfer = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(this);
            if (xfer == null) {
                return false;
            }
            return hasPaletteSupportedDataFlavor(Arrays.asList(xfer.getTransferDataFlavors()));
        }

    }

    @Messages({
        "copy=Cop&y"
    })
    private class TileCopyAction extends AbstractAction {

        private final Tile<T> tile;

        TileCopyAction(Tile<T> tile) {
            this.tile = tile;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Transferable xfer = createTransferable(tile);
            Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
            ClipboardOwner owner = xfer instanceof ClipboardOwner
                    ? (ClipboardOwner) xfer : new CBOwner();
            cb.setContents(xfer, owner);
        }
    }

    static final class CBOwner implements ClipboardOwner {

        @Override
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            // do nothing
        }
    }

    @Messages({
        "# {0} - itemName",
        "DELETE=Delete {0}"
    })
    private class DeleteFromPaletteAction extends AbstractAction {

        private final Tile<T> tile;

        DeleteFromPaletteAction(Tile<T> tile) {
            super(Bundle.DELETE(tile.getName()));
            this.tile = tile;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            storage().delete(tile.getName(), (thrown, del) -> {
                if (thrown != null) {
                    Exceptions.printStackTrace(thrown);
                }
            });
        }
    }

    private class TilePopupListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            // Somehow isPopupTrigger is false when drag & drop support is present?
            if (e.isPopupTrigger() || e.getButton() == 3) {
                Tile<T> tile = (Tile<T>) e.getSource();
                JPopupMenu menu = createTilePopupMenu(tile);
                JMenuItem copy = new JMenuItem(new TileCopyAction(tile));
                Mnemonics.setLocalizedText(copy, Bundle.copy());
                tile.populatePopupMenu(menu);
                if (menu.getComponentCount() > 0) {
                    e.consume();
                    menu.show((Component) e.getSource(), e.getX(), e.getY());
                }
            } else if (e.getClickCount() == 2) {
                Tile<T> tile = (Tile<T>) e.getSource();
                if (onTileDoubleClick(e, tile)) {
                    e.consume();
                }
            }
        }
    }

    @Messages({
        "# {0} - what",
        "edit=Edit {0}"})
    protected boolean onTileDoubleClick(MouseEvent e, Tile<T> tile) {
        PaletteItemEditorSupplier<T> supp = PaletteItemEditorSupplier.find(type);
        if (supp != null) {
            supp.edit(onTileEdit(tile.getName()));
            return true;
        }
        Customizer<? super T> cus = Customizers.getCustomizer(type, tile.getName());
        if (cus != null) {
            JComponent comp = cus.getComponent();
            NotifyDescriptor notif = new NotifyDescriptor(comp,
                    Bundle.edit(tile.getName()), NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.PLAIN_MESSAGE,
                    new Object[]{NotifyDescriptor.OK_CANCEL_OPTION, NotifyDescriptor.CANCEL_OPTION},
                    NotifyDescriptor.OK_OPTION);
            if (NotifyDescriptor.OK_OPTION.equals(DialogDisplayer.getDefault().notify(notif))) {
                ((PaletteBackend) storage()).save(tile.getName(), cus.get(), (thrown, nm) -> {
                    if (thrown != null) {
                        Exceptions.printStackTrace((Throwable) thrown);
                    }
                });
            }
        }
        return false;
    }

    private Consumer<T> onTileEdit(String name) {
        return (updated) -> {
            ((PaletteBackend) storage()).save(name, updated, (thrown, nm) -> {
                if (thrown != null) {
                    Exceptions.printStackTrace((Throwable) thrown);
                }
            });
        };
    }

    protected boolean canDeleteFromPalette(Tile<T> tile) {
        return true;
    }

    private class PalettePopupListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            // Somehow isPopupTrigger is false when drag & drop support is present?
            if (e.isPopupTrigger() || e.getButton() == 3) {
                JPopupMenu menu = createPalettePopupMenu((JComponent) e.getSource());
                if (menu.getComponentCount() > 0) {
                    e.consume();
                    menu.show((Component) e.getSource(), e.getX(), e.getY());
                }
            }
        }
    }

    protected void populatePaletteMenu(JPopupMenu menu, JComponent palette) {
        // do nothing
    }

    protected void populateTileMenu(JPopupMenu menu, Tile<T> tile) {
        // do nothing
    }

    private JPopupMenu createPalettePopupMenu(JComponent palette) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem item = new JMenuItem(new PalettePasteAction(palette));
        Mnemonics.setLocalizedText(item, Bundle.paste());
        menu.add(item);
        populatePaletteMenu(menu, palette);
        return menu;
    }

    private JPopupMenu createTilePopupMenu(Tile<T> tile) {
        JPopupMenu menu = new JPopupMenu();
        TileCopyAction copy = new TileCopyAction(tile);
        JMenuItem item = new JMenuItem(copy);
        Mnemonics.setLocalizedText(item, Bundle.copy());
        menu.add(item);
        menu.add(new JSeparator());
        populateTileMenu(menu, tile);
        if (canDeleteFromPalette(tile)) {
            menu.add(new DeleteFromPaletteAction(tile));
        }
        return menu;
    }

    protected Transferable transferableFor(Tile<T> tile) {
        Transferable inner = createTransferable(tile);
        MetaTransferable result = new MetaTransferable(inner);
        result.add(new TileImageTransferable(tile, () -> {
            return getTileImage(tile);
        }));
        return result;
    }

    protected Cursor getDropOKCursor(Component comp) {
        return Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);
    }

    protected Cursor getNoDropCursor(Component comp) {
        return Cursors.forComponent((JComponent) comp).no();
    }

    class DGL implements DragGestureListener, DragSourceListener, DropTargetListener {

        @Override
        @SuppressWarnings("element-type-mismatch")
        public void dragGestureRecognized(DragGestureEvent dge) {
            clearCursor();
            if (acceptDrag(dge)) {
                Component comp = dge.getComponent();
                if (comp instanceof Tile<?> && isOurTile(comp) && useDragAndDropImage((Tile<T>) comp)) {
                    Tile<T> tile = (Tile<T>) comp;
                    Transferable xfer = transferableFor(tile);
                    dge.startDrag(dragCursor(), getTileImage(tile),
                            new Point(5, 5), xfer, this);
                } else if (isOurTile(comp)) {
                    dge.startDrag(dragCursor(), transferableFor((Tile<T>) comp),
                            this);
                }
            }
        }

        @Override
        public void drop(DropTargetDropEvent dtde) {
            doHandleDrop(dtde);
            clearCursor();
        }

        @Override
        public void dragDropEnd(DragSourceDropEvent dsde) {
            // do nothing
            clearCursor();
        }

        @Override
        public void dragEnter(DragSourceDragEvent dsde) {
            // do nothing
        }

        @Override
        @SuppressWarnings("element-type-mismatch")
        public void dragEnter(DropTargetDragEvent dtde) {
            Component comp = dtde.getDropTargetContext().getComponent();
            if (isPaletteComponent(comp)) {
                if (_canDropOnPalette(dtde)) {
                    setCursor(comp, true);
                    dtde.acceptDrag(dtde.getDropAction());
                } else {
                    setCursor(comp, false);
                }
            } else {
                Tile<?> tile = (Tile<?>) dtde.getDropTargetContext().getComponent();
                if (_canDropOnTile(dtde)) {
                    setCursor(tile, true);
                    dtde.acceptDrag(dtde.getDropAction());
                } else {
                    setCursor(tile, false);
                }
            }
        }

        @Override
        public void dragOver(DragSourceDragEvent dsde) {
            // do nothing
        }

        @Override
        public void dropActionChanged(DragSourceDragEvent dsde) {
            // do nothing
        }

        @Override
        public void dragExit(DragSourceEvent dse) {
            // do nothing
        }

        @Override
        public void dragOver(DropTargetDragEvent dtde) {
            // do nothing
        }

        @Override
        public void dropActionChanged(DropTargetDragEvent dtde) {
            // do nothing
        }

        @Override
        public void dragExit(DropTargetEvent dte) {
            // do nothing
            clearCursor();
        }

        private CursorHandler handler;

        private void setCursor(Component comp, boolean dropOk) {
            if (handler != null && handler.component != comp) {
                handler.restore();
                handler = null;
            }
            if (comp != null) {
                handler = new CursorHandler(comp);
                comp.setCursor(dropOk ? getDropOKCursor(comp)
                        : getNoDropCursor(comp));
            }
        }

        private void clearCursor() {
            if (handler != null) {
                handler.restore();
                handler = null;
            }
        }
    }

    class CursorHandler {

        private final Component component;
        private final Cursor originalCursor;

        public CursorHandler(Component component) {
            this.component = component;
            originalCursor = component.getCursor();
        }

        void restore() {
            component.setCursor(originalCursor);
        }

        boolean restore(Component c) {
            component.setCursor(originalCursor);
            return c == component;
        }
    }
}
