package org.imagine.vector.editor.ui.palette;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;
import net.dev.java.imagine.api.tool.aspects.Customizer;
import net.java.dev.imagine.api.toolcustomizers.Customizers;
import org.imagine.awt.dnd.PaintKeyDropSupport;
import org.imagine.awt.io.PaintKeyIO;
import org.imagine.awt.key.PaintKey;
import org.imagine.io.KeyWriter;
import org.imagine.nbutil.filechooser.FileChooserBuilder;
import org.imagine.nbutil.filechooser.FileKinds;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.awt.Mnemonics;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.NbBundle.Messages;

/**
 *
 * @author Tim Boudreau
 */
final class PaintKeyTileFactory extends AbstractTileFactory<PaintKey<?>, TransferHandler> {

    private final PaletteBackend<? extends PaintKey<?>> storage;
    private final int prefDim;

    public PaintKeyTileFactory(PaletteBackend<? extends PaintKey<?>> storage) {
        this(storage, 128);
    }

    public PaintKeyTileFactory(PaletteBackend<? extends PaintKey<?>> storage, int prefDim) {
        super(PaintKey.class);
        this.storage = storage;
        this.prefDim = prefDim;
    }

    @Override
    public Tile<? super PaintKey<?>> createTile(String tileName) {
        return new PaintTile(tileName, storage);
    }

    @Override
    public Dimension getPreferredTileSize() {
        return new Dimension(prefDim, prefDim);
    }

    @Override
    public PaletteBackend<? extends PaintKey<?>> storage() {
        return storage;
    }

    @Override
    protected TransferHandler createTransferHandler() {
        return PaintKeyDropSupport.createTransferHandler(comp -> {
            return ((PaintTile) comp).item();
        });
    }

    @Override
    public Transferable createTransferable(Tile<PaintKey<?>> obj) {
        Transferable result = PaintKeyDropSupport.createTransferrable(obj.item());
        return result;
    }

    @Override
    protected boolean isOurTile(Component tile) {
        return tile instanceof PaintTile;
    }

    @Override
    protected Collection<? extends DataFlavor> supportedDataFlavors() {
        return PaintKeyDropSupport.allFlavors();
    }

    @Override
    protected int handleDropOnTile(Set<DataFlavor> supportedFlavors, Transferable xfer, Component target) {
        return DnDConstants.ACTION_COPY;
    }

    @Override
    protected void populateTileMenu(JPopupMenu menu, Tile<PaintKey<?>> tile) {
        JMenuItem item = new JMenuItem(new SaveFillAction(tile.item()));
        Mnemonics.setLocalizedText(item, Bundle.Save());
        menu.add(item, 0);
    }

    @Override
    protected void populatePaletteMenu(JPopupMenu menu, JComponent palette) {
        JMenuItem item = new JMenuItem(new LoadFillAction());
        Mnemonics.setLocalizedText(item, Bundle.loadFill());
        menu.add(item);
    }

    @Override
    protected boolean onTileDoubleClick(MouseEvent e, Tile<PaintKey<?>> tile) {
        PaintKey<?> pk = tile.item();
        Customizer<? extends Paint> cus = Customizers.getCustomizer((Class) tile.item().kind().type(), tile.getName(), pk.toPaint());
        if (cus != null) {
            JComponent comp = cus.getComponent();
            NotifyDescriptor notif = new NotifyDescriptor(comp,
                    Bundle.edit(tile.getName()), NotifyDescriptor.OK_CANCEL_OPTION,
                    NotifyDescriptor.PLAIN_MESSAGE,
                    new Object[]{NotifyDescriptor.OK_OPTION, NotifyDescriptor.CANCEL_OPTION},
                    NotifyDescriptor.OK_OPTION);
            if (NotifyDescriptor.OK_OPTION.equals(DialogDisplayer.getDefault().notify(notif))) {
                Paint paint = cus.get();
                if (paint != null) {
                    PaintKey<?> revised = PaintKey.forPaint(paint);
                    if (!revised.equals(tile.item())) {
                        ((PaletteBackend) storage()).save(tile.getName(), revised,
                                (thrown, nm) -> {
                                    if (thrown != null) {
                                        Exceptions.printStackTrace((Throwable) thrown);
                                    }
                                });
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Messages({"loadFill=&Load Fill...", "loadFillButtonText=Load To Palette"})
    private final class LoadFillAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            File file = new FileChooserBuilder("savePaints")
                    .setDefaultWorkingDirectory(new File(System.getProperty("user.home")))
                    .setTitle(Bundle.SaveFill())
                    .setFileFilter(new FillFileFilter())
                    .setFileHiding(true)
                    .setFileKinds(FileKinds.FILES_ONLY)
                    .setApproveText(Bundle.loadFillButtonText())
                    .showOpenDialog();
            if (file != null) {
                try (FileChannel c = FileChannel.open(file.toPath(), READ)) {
                    PaintKey<?> key = PaintKeyIO.read(c);
                    String name = file.getName();
                    int ix = name.lastIndexOf('.');
                    if (ix > 0) {
                        name = name.substring(0, ix);
                    }
                    ((PaletteBackend) storage()).save(name, (PaintKey) key, (thrown, nm) -> {
                        if (thrown != null) {
                            Exceptions.printStackTrace((Throwable) thrown);
                        }
                    });
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    @NbBundle.Messages({"Save=Save As...", "SaveFill=Save Fill"})
    private static final class SaveFillAction extends AbstractAction {

        private final PaintKey<?> key;

        SaveFillAction(PaintKey<?> key) {
            super(Bundle.Save());
            this.key = key;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File file = new FileChooserBuilder("savePaints")
                    .setDefaultWorkingDirectory(new File(System.getProperty("user.home")))
                    .setTitle(Bundle.SaveFill())
                    .setFileFilter(new FillFileFilter())
                    .addDefaultFileFilters()
                    .setFileHiding(true)
                    .setFileKinds(FileKinds.FILES_ONLY)
                    .showSaveDialog();
            if (file != null) {
                if (!file.getName().endsWith(".fill")) {
                    file = new File(file.getParent(), file.getName() + ".fill");
                }
                try (FileChannel c = FileChannel.open(file.toPath(), CREATE, TRUNCATE_EXISTING, WRITE)) {
                    KeyWriter<List<ByteBuffer>> bw = PaintKeyIO.binaryWriter(key);
                    PaintKeyIO.write(c, key);
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
    }

    @Messages("fillFiles=Shape Files")
    private static final class FillFileFilter extends FileFilter {

        @Override
        public boolean accept(File f) {
            if (f.isHidden()) {
                return false;
            }
            return f.isDirectory() || f.getName().endsWith(".fill");
        }

        @Override
        public String getDescription() {
            return Bundle.fillFiles();
        }
    }
}
