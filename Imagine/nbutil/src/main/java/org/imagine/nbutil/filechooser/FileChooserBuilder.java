package org.imagine.nbutil.filechooser;

/**
 *
 * @author Tim Boudreau
 */
import com.mastfrog.function.QuadConsumer;
import com.mastfrog.swing.FlexEmptyBorder;
import com.mastfrog.swing.FlexEmptyBorder.Side;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.HeadlessException;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.SELECTED_FILES_CHANGED_PROPERTY;
import static javax.swing.JFileChooser.SELECTED_FILE_CHANGED_PROPERTY;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import org.imagine.nbutil.ComponentUtils;
import org.openide.awt.Mnemonics;
import org.openide.util.BaseUtilities;
import org.openide.util.NbBundle.Messages;
import org.openide.util.NbPreferences;
import org.openide.util.Parameters;

/**
 * <p>
 * This is a fork of my original org.openide.filesystems.FileChooserBuilder,
 * with the following differences:
 * <ul>
 * <li>IconProvider API is exposed, and can be used with multiple layers of icon
 * providers</li>
 * <li>Cleaner API for setting files/directories only mode that doesn't include
 * the possibility of neither</li>
 * <li>Allows setting and updating the accessory panel</li>
 * <li>Includes an optional recent folders panel at the top of the file chooser,
 * similar to contrib/quick-file-chooser</li>
 * <li>Ability to set the initial file selection</li>
 * </ul>
 * </p><p>
 * Utility class for working with JFileChoosers. In particular, remembering the
 * last-used directory for a given file is made transparent. You pass an ad-hoc
 * string key to the constructor (the fully qualified name of the calling class
 * is good for uniqueness, and there is a constructor that takes a
 * <code>Class</code> object as an argument for this purpose). That key is used
 * to look up the most recently-used directory from any previous invocations
 * with the same key. This makes it easy to have your user interface
 * &ldquo;remember&rdquo; where the user keeps particular types of files, and
 * saves the user from having to navigate through the same set of directories
 * every time they need to locate a file from a particular place.
 * <p/>
 * <code>FileChooserBuilder</code>'s methods each return <code>this</code>, so
 * it is possible to chain invocations to simplify setting up a file chooser.
 * Example usage:
 * <pre>
 *      <font color="gray">//The default dir to use if no value is stored</font>
 *      File home = new File (System.getProperty("user.home") + File.separator + "lib");
 *      <font color="gray">//Now build a file chooser and invoke the dialog in one line of code</font>
 *      <font color="gray">//&quot;libraries-dir&quot; is our unique key</font>
 *      File toAdd = new FileChooserBuilder ("libraries-dir").setTitle("Add Library").
 *              setDefaultWorkingDirectory(home).setApproveText("Add").showOpenDialog();
 *      <font color="gray">//Result will be null if the user clicked cancel or closed the dialog w/o OK</font>
 *      if (toAdd != null) {
 *          //do something
 *      }
 * </pre>
 * <p/>
 * Instances of this class are intended to be thrown away after use. Typically
 * you create a builder, set it to create file choosers as you wish, then use it
 * to show a dialog or create a file chooser you then do something with.
 * <p/>
 * Supports the most common subset of JFileChooser functionality; if you need to
 * do something exotic with a file chooser, you are probably better off creating
 * your own.
 * <p/>
 * <b>Note:</b> If you use the constructor that takes a <code>Class</code>
 * object, please use <code>new FileChooserBuilder(MyClass.class)</code>, not
 * <code>new FileChooserBuilder(getClass())</code>. This avoids unexpected
 * behavior in the case of subclassing.
 *
 * @author Tim Boudreau
 */
public class FileChooserBuilder {

    private String title;
    private String approveText;
    //Just in case...
    private final String dirKey;
    private File failoverDir = new File(System.getProperty("user.home"));
    private File initialSelection;
    private FileFilter filter;
    private boolean fileHiding;
    private boolean controlButtonsShown = true;
    private String aDescription;
    private static final boolean DONT_STORE_DIRECTORIES
            = Boolean.getBoolean("forget.recent.dirs");
    private SelectionApprover approver;
    private final List<FileFilter> filters = new ArrayList<>(3);
    private boolean useAcceptAllFileFilter = true;
    private FileKinds fileKinds = FileKinds.DIRECTORIES_AND_FILES;
    private AccessoryInfo<?> accessory;
    private IconProvider iconProvider;
    private final Set<FileFilter> fileFilters = new LinkedHashSet<>();
    private boolean confirm;
    private String forcedExtension;

    /**
     * Create a new FileChooserBuilder using the name of the passed class as the
     * metadata for looking up a starting directory from previous application
     * sessions or invocations.
     *
     * @param type A non-null class object, typically the calling class
     */
    public FileChooserBuilder(Class type) {
        this(type.getName());
    }

    /**
     * Create a new FileChooserBuilder. The passed key is used as a key into
     * NbPreferences to look up the directory the file chooser should initially
     * be rooted on.
     *
     * @param dirKey A non-null ad-hoc string. If a FileChooser was previously
     * used with the same string as is passed, then the initial directory
     */
    public FileChooserBuilder(String dirKey) {
        Parameters.notNull("dirKey", dirKey);
        this.dirKey = dirKey;
    }

    /**
     * Force the passed file extension to be appended to the selected file
     * <i>when this builder is used to create a Save dialog only</i>.
     *
     * @param ext The extension to apply to the file name before testing if it
     * already exists, and to return to the user
     * @return this
     */
    public FileChooserBuilder forceExtension(String ext) {
        if (ext == null || ext.length() == 0 || ".".equals(ext)) {
            forcedExtension = null;
        } else {
            if (ext.charAt(0) == '.') {
                ext = ext.substring(1);
            }
            this.forcedExtension = ext;
        }
        return this;
    }

    public FileChooserBuilder confirmOverwrites() {
        confirm = true;
        return this;
    }

    public <C extends JComponent> FileChooserBuilder setAccessory(Supplier<C> comp, QuadConsumer<Integer, Path, C, Integer> onChange) {
        this.accessory = new AccessoryInfo<>(comp, onChange);
        return this;
    }

    public <C extends JComponent> FileChooserBuilder setAccessory(Supplier<C> comp) {
        return setAccessory(comp, null);
    }

    public <C extends JComponent> FileChooserBuilder setAccessory(C comp) {
        return setAccessory(() -> comp, null);
    }

    static final class AccessoryInfo<C extends JComponent> implements PropertyChangeListener {

        private final Supplier<C> comp;
        private final QuadConsumer<Integer, Path, C, Integer> onChange;
        // internal state
        private File[] currentFiles = new File[0];
        private C component;

        public AccessoryInfo(Supplier<C> comp, QuadConsumer<Integer, Path, C, Integer> onChange) {
            this.comp = comp;
            this.onChange = onChange;
        }

        private C component() {
            if (component == null) {
                component = comp.get();
            }
            return component;
        }

        void attach(JFileChooser chooser) {
            C c = component();
            chooser.setAccessory(c);
            if (onChange != null) {
                chooser.addPropertyChangeListener(SELECTED_FILE_CHANGED_PROPERTY, this);
                chooser.addPropertyChangeListener(SELECTED_FILES_CHANGED_PROPERTY, this);
            }
        }

        void detach(JFileChooser chooser) {
            if (component != null && chooser.getAccessory() == component) {
                chooser.setAccessory(null);
            }
            if (onChange != null) {
                chooser.removePropertyChangeListener(SELECTED_FILE_CHANGED_PROPERTY, this);
                chooser.removePropertyChangeListener(SELECTED_FILES_CHANGED_PROPERTY, this);
            }
        }

        void setCurrentFile(File f) {
            setCurrentFiles(f == null ? new File[0] : new File[]{f});
        }

        void setCurrentFiles(File[] fls) {
            if (fls == null) {
                fls = new File[0];
            }
            if (!Arrays.equals(currentFiles, fls)) {
                if (fls == null || fls.length == 0) {
                    onChange.accept(0, null, component, 1);
                    return;
                }
                for (int i = 0; i < fls.length; i++) {
                    Path p = fls[i].toPath();
                    onChange.accept(0, p, component, fls.length);
                }
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String name = evt.getPropertyName();
            if (name == null) {
                return;
            }
            switch (evt.getPropertyName()) {
                case SELECTED_FILES_CHANGED_PROPERTY:
                    File[] fls = (File[]) evt.getNewValue();
                    setCurrentFiles(fls);
                    break;
                case SELECTED_FILE_CHANGED_PROPERTY:
                    File file = (File) evt.getNewValue();
                    setCurrentFile(file);
                    break;
            }
        }

    }

    public FileChooserBuilder setFileKinds(FileKinds types) {
        this.fileKinds = types;
        return this;
    }

    /**
     * Provide an implementation of BadgeProvider which will "badge" the icons
     * of some files.
     *
     * @param provider A badge provider which will alter the icon of files or
     * folders that may be of particular interest to the user
     * @return this
     */
    public FileChooserBuilder setBadgeProvider(BadgeProvider provider) {
        if (this.iconProvider == null) {
            this.iconProvider = new BadgeIconProvider(provider);
        } else {
            this.iconProvider = this.iconProvider.and(new BadgeIconProvider(provider));
        }
        return this;
    }

    /**
     * Provide custom icons for some or all files.
     *
     * @param provider A badge provider which will alter the icon of files or
     * folders that may be of particular interest to the user
     * @return this
     */
    public FileChooserBuilder setIconProvider(IconProvider icons) {
        if (this.iconProvider == null) {
            this.iconProvider = this.iconProvider.and(icons);
        } else {
            this.iconProvider = icons;
        }
        return this;
    }

    /**
     * Set the dialog title for any JFileChoosers created by this builder.
     *
     * @param val A localized, human-readable title
     * @return this
     */
    public FileChooserBuilder setTitle(String val) {
        title = val;
        return this;
    }

    /**
     * Set the text on the OK button for any file chooser dialogs produced by
     * this builder.
     *
     * @param val A short, localized, human-readable string
     * @return this
     */
    public FileChooserBuilder setApproveText(String val) {
        approveText = val;
        return this;
    }

    /**
     * Set a file filter which filters the list of selectable files.
     *
     * @param filter
     * @return this
     */
    public FileChooserBuilder setFileFilter(FileFilter filter) {
        this.filter = filter;
        this.fileFilters.add(filter);
        return this;
    }

    /**
     * Determines whether the <code>AcceptAll FileFilter</code> is used as an
     * available choice in the choosable filter list. If false, the
     * <code>AcceptAll</code> file filter is removed from the list of available
     * file filters. If true, the <code>AcceptAll</code> file filter will become
     * the the actively used file filter.
     *
     * @param accept whether the <code>AcceptAll FileFilter</code> is used
     * @return this
     * @since 8.3
     */
    public FileChooserBuilder setAcceptAllFileFilterUsed(boolean accept) {
        useAcceptAllFileFilter = accept;
        return this;
    }

    /**
     * Set the current directory which should be used <b>only if</b>
     * a last-used directory cannot be found for the key string passed into this
     * builder's constructor.
     *
     * @param dir A directory to root any created file choosers on if there is
     * no stored path for this builder's key
     * @return this
     */
    public FileChooserBuilder setDefaultWorkingDirectory(File dir) {
        failoverDir = dir;
        return this;
    }

    /**
     * Set the current directory which should be used <b>only if</b>
     * a last-used directory cannot be found for the key string passed into this
     * builder's constructor.
     *
     * @param dir A directory to root any created file choosers on if there is
     * no stored path for this builder's key
     * @return this
     */
    public FileChooserBuilder setDefaultWorkingDirectory(Path dir) {
        failoverDir = dir == null ? null : dir.toFile();
        return this;
    }

    /**
     * Enable file hiding in any created file choosers
     *
     * @param fileHiding Whether or not to hide files. Default is no.
     * @return this
     */
    public FileChooserBuilder setFileHiding(boolean fileHiding) {
        this.fileHiding = fileHiding;
        return this;
    }

    /**
     * Show/hide control buttons
     *
     * @param val Whether or not to hide files. Default is no.
     * @return this
     */
    public FileChooserBuilder setControlButtonsAreShown(boolean val) {
        this.controlButtonsShown = val;
        return this;
    }

    /**
     * Set the accessible description for any file choosers created by this
     * builder
     *
     * @param aDescription The description
     * @return this
     */
    public FileChooserBuilder setAccessibleDescription(String aDescription) {
        this.aDescription = aDescription;
        return this;
    }

    private boolean showRecentFolders = true;

    public FileChooserBuilder dontShowRecentFolders() {
        return showRecentFolders(false);
    }

    public FileChooserBuilder showRecentFolders() {
        return showRecentFolders(true);
    }

    public FileChooserBuilder showRecentFolders(boolean showRecentFolders) {
        this.showRecentFolders = showRecentFolders;
        return this;
    }

    /**
     * Create a JFileChooser that conforms to the parameters set in this
     * builder.
     *
     * @return A file chooser
     */
    public JFileChooser createFileChooser() {
        JFileChooser result = new SavedDirFileChooser(dirKey, failoverDir,
                force, approver, showRecentFolders ? new A(dirKey) : null, showRecentFolders,
                initialSelection, fileFilters, confirm, forcedExtension);
        prepareFileChooser(result);
        return result;
    }

    private boolean force = false;

    /**
     * Force use of the failover directory - i.e. ignore the directory key
     * passed in.
     *
     * @param val
     * @return this
     */
    public FileChooserBuilder forceUseOfDefaultWorkingDirectory(boolean val) {
        this.force = val;
        return this;
    }

    /**
     * Tries to find an appropriate component to parent the file chooser to when
     * showing a dialog.
     *
     * @return this
     */
    private Component findDialogParent() {
        Component parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        if (parent == null) {
            parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
        }
        if (parent == null) {
            Frame[] f = Frame.getFrames();
            parent = f.length == 0 ? null : f[f.length - 1];
        }
        return parent;
    }

    /**
     * Show an open dialog that allows multiple selection.
     *
     * @return An array of files, or null if the user cancelled the dialog
     */
    public File[] showMultiOpenDialog() {
        JFileChooser chooser = createFileChooser();
        chooser.setMultiSelectionEnabled(true);
        int result = chooser.showOpenDialog(findDialogParent());
        if (JFileChooser.APPROVE_OPTION == result) {
            File[] files = chooser.getSelectedFiles();
            return files == null ? new File[0] : files;
        } else {
            return null;
        }
    }

    public Path[] showMultiOpenDialogNIO() {
        File[] result = showMultiOpenDialog();
        if (result == null) {
            return new Path[0];
        }
        Path[] paths = new Path[result.length];
        for (int i = 0; i < result.length; i++) {
            paths[i] = result[i].toPath();
        }
        return paths;
    }

    /**
     * Show an open dialog with a file chooser set up according to the
     * parameters of this builder.
     *
     * @return A file if the user clicks the accept button and a file or folder
     * was selected at the time the user clicked cancel.
     */
    public File showOpenDialog() {
        JFileChooser chooser = createFileChooser();
        if (Boolean.getBoolean("nb.native.filechooser")) { //NOI18N
            FileDialog fileDialog = createFileDialog(chooser.getCurrentDirectory());
            if (null != fileDialog) {
                return showFileDialog(fileDialog, FileDialog.LOAD);
            }
        }
        chooser.setMultiSelectionEnabled(false);
        int dlgResult = chooser.showOpenDialog(findDialogParent());
        if (JFileChooser.APPROVE_OPTION == dlgResult) {
            File result = chooser.getSelectedFile();
            if (result != null && !result.exists()) {
                result = null;
            }
            return result;
        } else {
            return null;
        }
    }

    public Path showOpenDialogNIO() {
        File result = showOpenDialog();
        return result == null ? null : result.toPath();
    }

    /**
     * Show a save dialog with the file chooser set up according to the
     * parameters of this builder.
     *
     * @return A file if the user clicks the accept button and a file or folder
     * was selected at the time the user clicked cancel.
     */
    public File showSaveDialog() {
        JFileChooser chooser = createFileChooser();
        if (Boolean.getBoolean("nb.native.filechooser")) { //NOI18N
            FileDialog fileDialog = createFileDialog(chooser.getCurrentDirectory());
            if (null != fileDialog) {
                return showFileDialog(fileDialog, FileDialog.SAVE);
            }
        }
        int result = chooser.showSaveDialog(findDialogParent());
        if (JFileChooser.APPROVE_OPTION == result) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    public Path showSaveDialogNio() {
        File f = showSaveDialog();
        return f == null ? null : f.toPath();
    }

    private File showFileDialog(FileDialog fileDialog, int mode) {
        String oldFileDialogProp = System.getProperty("apple.awt.fileDialogForDirectories"); //NOI18N
        if (fileKinds == FileKinds.DIRECTORIES_ONLY) {
            System.setProperty("apple.awt.fileDialogForDirectories", "true"); //NOI18N
        }
        fileDialog.setMode(mode);
        fileDialog.setVisible(true);
        if (fileKinds == FileKinds.DIRECTORIES_ONLY) {
            if (null != oldFileDialogProp) {
                System.setProperty("apple.awt.fileDialogForDirectories", oldFileDialogProp); //NOI18N
            } else {
                System.clearProperty("apple.awt.fileDialogForDirectories"); //NOI18N
            }
        }
        if (fileDialog.getDirectory() != null && fileDialog.getFile() != null) {
            String selFile = fileDialog.getFile();
            File dir = new File(fileDialog.getDirectory());
            return new File(dir, selFile);
        }
        return null;
    }

    private void prepareFileChooser(JFileChooser chooser) {
        fileKinds.configure(chooser);
        if (accessory != null) {
            accessory.attach(chooser);
        }
        chooser.setFileHidingEnabled(fileHiding);
        chooser.setControlButtonsAreShown(controlButtonsShown);
        chooser.setAcceptAllFileFilterUsed(useAcceptAllFileFilter);
        if (title != null) {
            chooser.setDialogTitle(title);
        }
        if (approveText != null) {
            chooser.setApproveButtonText(approveText);
        }
        if (iconProvider != null) {
            chooser.setFileView(new CustomFileView(iconProvider,
                    chooser.getFileSystemView()));
        }
        if (filter != null) {
            chooser.setFileFilter(filter);
        }
        if (aDescription != null) {
            chooser.getAccessibleContext().setAccessibleDescription(aDescription);
        }
        if (!filters.isEmpty()) {
            for (FileFilter f : filters) {
                chooser.addChoosableFileFilter(f);
            }
        }
    }

    private FileDialog createFileDialog(File currentDirectory) {
        if (iconProvider != null) {
            return null;
        }
        if (!Boolean.getBoolean("nb.native.filechooser")) {
            return null;
        }
        if (fileKinds == FileKinds.DIRECTORIES_ONLY && !BaseUtilities.isMac()) {
            return null;
        }
        Component parentComponent = findDialogParent();
        Frame parentFrame = (Frame) SwingUtilities.getAncestorOfClass(Frame.class, parentComponent);
        FileDialog fileDialog = new FileDialog(parentFrame);
        if (title != null) {
            fileDialog.setTitle(title);
        }
        if (null != currentDirectory) {
            fileDialog.setDirectory(currentDirectory.getAbsolutePath());
        }
        return fileDialog;
    }

    /**
     * Equivalent to calling
     * <code>JFileChooser.addChoosableFileFilter(filter)</code>. Adds another
     * file filter that can be displayed in the file filters combo box in the
     * file chooser.
     *
     * @param filter The file filter to add
     * @return this
     * @since 7.26.0
     */
    public FileChooserBuilder addFileFilter(FileFilter filter) {
        filters.add(filter);
        return this;
    }

    /**
     * Add all default file filters to the file chooser.
     *
     * @return this
     * @since 8.1
     */
    public FileChooserBuilder addDefaultFileFilters() {
        filters.addAll(FileFilterSupport.findRegisteredFileFilters());
        return this;
    }

    /**
     * Set a selection approver which can display an &quot;Overwrite file?&quot;
     * or similar dialog if necessary, when the user presses the accept button
     * in the file chooser dialog.
     *
     * @param approver A SelectionApprover which will determine if the selection
     * is valid
     * @return this
     * @since 7.26.0
     */
    public FileChooserBuilder setSelectionApprover(SelectionApprover approver) {
        this.approver = approver;
        return this;
    }

    public FileChooserBuilder setInitialSelection(Path file) {
        return setInitialSelection(file == null ? null : file.toFile());
    }

    public FileChooserBuilder setInitialSelection(File file) {
        this.initialSelection = file;
        return this;
    }

    public FileChooserBuilder withFileExtension(String desc, String ext) {
        ExtensionFileFilter filter = new ExtensionFileFilter(ext, desc);
        setFileFilter(filter);
        return this;
    }

    /**
     * Object which can approve the selection (enabling the OK button or
     * equivalent) in a JFileChooser. Equivalent to overriding
     * <code>JFileChooser.approveSelection()</code>
     *
     * @since 7.26.0
     */
    public interface SelectionApprover {

        /**
         * Approve the selection, enabling the dialog to be closed. Called by
         * the JFileChooser's <code>approveSelection()</code> method. Use this
         * interface if you want to, for example, show a dialog asking
         * &quot;Overwrite File X?&quot; or similar.
         *
         * @param selection The selected file(s) at the time the user presses
         * the Open, Save or OK button
         * @return true if the selection is accepted, false if it is not and the
         * dialog should not be closed
         */
        public boolean approve(File[] selection);
    }

    private static final class SavedDirFileChooser extends JFileChooser {

        private final String dirKey;
        private final SelectionApprover approver;
        private final ActionListener a;
        private final boolean showRecentFolders;
        private final boolean confirmOverwrite;
        private final String forcedExtension;

        SavedDirFileChooser(String dirKey, File failoverDir, boolean force,
                SelectionApprover approver, ActionListener a, boolean showRecentFolders,
                File initialSelection, Set<FileFilter> filters, boolean confirmOverwrite,
                String forcedExtension) {
            this.dirKey = dirKey;
            this.approver = approver;
            this.a = a;
            this.showRecentFolders = showRecentFolders;
            this.confirmOverwrite = confirmOverwrite;
            this.forcedExtension = forcedExtension;
            setBorder(new FlexEmptyBorder());
            for (FileFilter filter : filters) {
                addChoosableFileFilter(filter);
            }
            if (force && failoverDir != null && failoverDir.exists() && failoverDir.isDirectory()) {
                setCurrentDirectory(failoverDir);
            } else {
                String path = DONT_STORE_DIRECTORIES ? null
                        : NbPreferences.forModule(FileChooserBuilder.class).get(dirKey, null);
                if (path != null) {
                    File f = new File(path);
                    if (f.exists() && f.isDirectory()) {
                        setCurrentDirectory(f);
                    } else if (failoverDir != null) {
                        setCurrentDirectory(failoverDir);
                    }
                } else if (failoverDir != null) {
                    setCurrentDirectory(failoverDir);
                }
            }
            if (initialSelection != null) {
                if (!initialSelection.isAbsolute()) {
                    File dir = getCurrentDirectory();
                    initialSelection = new File(dir, initialSelection.getPath());
                }
                if (!initialSelection.getParentFile().equals(getCurrentDirectory())) {
                    initialSelection = new File(getCurrentDirectory(), initialSelection.getName());
                }
                setSelectedFile(initialSelection);
            }
        }

        @Override
        public void approveSelection() {
            if (approver != null) {
                File[] selected = getSelectedFiles();
                final File sf = getSelectedFile();
                if ((selected == null || selected.length == 0) && sf != null) {
                    selected = new File[]{sf};
                }
                boolean approved = approver.approve(selected);
                if (approved) {
                    super.approveSelection();
                }
            } else {
                super.approveSelection();
            }
        }

        @Override
        @Messages({
            "# {0} - fileName",
            "fileExists={0} exists.\n\nReplace it?",
            "ttlFileExists=Confirm Overwrite File"
        })
        public int showSaveDialog(Component parent) throws HeadlessException {
            int result = super.showSaveDialog(parent);
            if (result == JFileChooser.APPROVE_OPTION && confirmOverwrite) {
                File f = getSelectedFile();
                if (f != null && forcedExtension != null) {
                    if (!f.getName().endsWith("." + forcedExtension)) {
                        File nue = new File(f.getParent(), f.getName() + "." + forcedExtension);
                        setSelectedFile(nue);
                        f = nue;
                    }
                }
                if (f != null && f.exists()) {
                    // Accessibility - need selectable text
                    JTextArea jta = new JTextArea(Bundle.fileExists(f.getName()));
                    jta.setBorder(BorderFactory.createEmptyBorder());
                    jta.setEditable(false);
                    jta.setColumns(30);
                    jta.setLineWrap(true);
                    jta.setWrapStyleWord(true);
                    jta.setBackground(UIManager.getColor("control"));
                    jta.setForeground(UIManager.getColor("controlText"));
                    jta.setFont(ComponentUtils.getFont(parent));
                    JPanel pnl = new JPanel(new BorderLayout());
                    pnl.setBorder(new FlexEmptyBorder());
                    pnl.add(jta, BorderLayout.CENTER);
                    if (JOptionPane.showConfirmDialog(this, pnl,
                            Bundle.ttlFileExists(),
                            JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) {
                        result = JFileChooser.CANCEL_OPTION;
                    }
                }
            }
            return result;
        }

        @Override
        public int showDialog(Component parent, String approveButtonText) throws HeadlessException {
            int result = super.showDialog(parent, approveButtonText);
            if (result == APPROVE_OPTION) {
                saveCurrentDir();
            }
            return result;
        }

        @Override
        protected JDialog createDialog(Component parent) throws HeadlessException {
            JDialog result = super.createDialog(parent);
            if (showRecentFolders) {
                result.getContentPane().add(createRecentFoldersPanel(dirKey,
                        this, getCurrentDirectory().getPath()),
                        BorderLayout.NORTH);
            }
            result.pack();
            return result;
        }

        private void saveCurrentDir() {
            File dir = super.getCurrentDirectory();
            if (!DONT_STORE_DIRECTORIES && dir != null && dir.exists() && dir.isDirectory()) {
                NbPreferences.forModule(FileChooserBuilder.class).put(dirKey, dir.getPath());
            }
            if (a != null) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        }
    }

    @FunctionalInterface
    public interface IconProvider {

        public Icon getIcon(File file, Icon orig);

        default IconProvider and(IconProvider next) {
            return (file, orig) -> {
                Icon initial = getIcon(file, orig);
                return next.getIcon(file, initial);
            };
        }
    }

    /**
     * Provides "badges" for icons that indicate files or folders of particular
     * interest to the user.
     *
     * @see FileChooserBuilder#setBadgeProvider
     */
    public interface BadgeProvider {

        /**
         * Get the badge the passed file should use.  <b>Note:</b> this method is
         * called for every visible file. The negative test (deciding
         * <i>not</i> to badge a file) should be very, very fast and immediately
         * return null.
         *
         * @param file The file in question
         * @return an icon or null if no change to the appearance of the file is
         * needed
         */
        public Icon getBadge(File file);

        /**
         * Get the x offset for badges produced by this provider. This is the
         * location of the badge icon relative to the real icon for the file.
         *
         * @return a rightward pixel offset
         */
        public int getXOffset();

        /**
         * Get the y offset for badges produced by this provider. This is the
         * location of the badge icon relative to the real icon for the file.
         *
         * @return a downward pixel offset
         */
        public int getYOffset();
    }

    private static final class BadgeIconProvider implements IconProvider {

        private final BadgeProvider badger;

        BadgeIconProvider(BadgeProvider badger) {
            this.badger = badger;
        }

        @Override
        public Icon getIcon(File file, Icon orig) {
            Icon badge = badger.getBadge(file);
            if (badge != null && orig != null) {
                return new MergedIcon(orig, badge, badger.getXOffset(),
                        badger.getYOffset());
            }
            return orig;
        }
    }

    private static final class CustomFileView extends FileView {

        private final IconProvider provider;
        private final FileSystemView view;

        CustomFileView(IconProvider provider, FileSystemView view) {
            this.provider = provider;
            this.view = view;
        }

        @Override
        public Icon getIcon(File f) {
            Icon result = view.getSystemIcon(f);
            result = provider.getIcon(f, result);
            return result;
        }
    }

    private static class MergedIcon implements Icon {

        private final Icon icon1;
        private final Icon icon2;
        private final int xMerge;
        private final int yMerge;

        MergedIcon(Icon icon1, Icon icon2, int xMerge, int yMerge) {
            assert icon1 != null;
            assert icon2 != null;
            this.icon1 = icon1;
            this.icon2 = icon2;

            if (xMerge == -1) {
                xMerge = icon1.getIconWidth() - icon2.getIconWidth();
            }

            if (yMerge == -1) {
                yMerge = icon1.getIconHeight() - icon2.getIconHeight();
            }

            this.xMerge = xMerge;
            this.yMerge = yMerge;
        }

        @Override
        public int getIconHeight() {
            return Math.max(icon1.getIconHeight(), yMerge + icon2.getIconHeight());
        }

        @Override
        public int getIconWidth() {
            return Math.max(icon1.getIconWidth(), yMerge + icon2.getIconWidth());
        }

        @Override
        public void paintIcon(java.awt.Component c, java.awt.Graphics g, int x, int y) {
            icon1.paintIcon(c, g, x, y);
            icon2.paintIcon(c, g, x + xMerge, y + yMerge);
        }
    }

    private static final class A implements ActionListener {

        private final String dirKey;

        A(String dirKey) {
            this.dirKey = dirKey;
        }

        public void actionPerformed(ActionEvent e) {
            JFileChooser jfc = (JFileChooser) e.getSource();
            String path = jfc.getCurrentDirectory().getAbsolutePath();
            String s = NbPreferences.forModule(FileChooserBuilder.class).get("recentDirs." + dirKey, ""); //NOI18N
            String[] dirs = prune(s.split(File.pathSeparator)).split(File.pathSeparator);

            Path p = jfc.getCurrentDirectory().getAbsoluteFile().toPath();
            boolean caseInsensitive = p.getParent().resolve(p.getFileName().toString().toUpperCase())
                    .equals(p.getParent().resolve(p.getFileName().toString().toLowerCase()));

            for (String dir : dirs) {
                boolean equal = caseInsensitive ? dir.equalsIgnoreCase(path) : dir.equals(path);
                if (equal) {
                    return;
                }
            }
            if (s.length() > 0) {
                s = s + File.pathSeparatorChar;
            }
            s += path;

            NbPreferences.forModule(FileChooserBuilder.class).put(dirKey, path);
            NbPreferences.forModule(FileChooserBuilder.class).put("recentDirs." + dirKey, s); //NOI18N
        }
    }

    private static String prune(String[] dirs) {
        Set<String> s = new HashSet<String>(Arrays.asList(dirs));
        StringBuilder sb = new StringBuilder();
        for (Iterator<String> i = s.iterator(); i.hasNext();) {
            String dir = i.next();
            File f = new File(dir);
            if (f.exists() && f.isDirectory()) {
                sb.append(dir);
                if (i.hasNext()) {
                    sb.append(',');
                }
            }
        }
        return sb.toString();
    }

    @Messages("LBL_RECENT_DIRS=Rece&nt Folders") //NOI18N
    private static JComponent createRecentFoldersPanel(final String dirKey, final JFileChooser jfc, String origSelection) {
        String s = NbPreferences.forModule(FileChooserBuilder.class).get("recentDirs." + dirKey, ""); //NOI18N
        String[] dirs = s.split(File.pathSeparator);
        Set<String> set = new HashSet<String>(Arrays.asList(dirs));
        dirs = (String[]) set.toArray(new String[set.size()]);
        JPanel result = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.weightx = 0.25;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        result.setBorder(new FlexEmptyBorder(Side.TOP, Side.LEFT, Side.RIGHT));
        JLabel lbl = new JLabel();
        Mnemonics.setLocalizedText(lbl, Bundle.LBL_RECENT_DIRS());
        result.add(lbl, gbc);
        final JComboBox box = new JComboBox(dirs);
        lbl.setBorder(new FlexEmptyBorder(1, 0, FlexEmptyBorder.Side.RIGHT));
        box.setSelectedItem(origSelection);
        box.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String s = (String) box.getSelectedItem();
                if (s != null) {
                    File f = new File(s);
                    if (f.exists() && f.isDirectory()) {
                        jfc.setCurrentDirectory(new File((String) box.getSelectedItem()));
                    }
                }
            }
        });
        box.setPrototypeDisplayValue("1234512345123451234512345"); //NOI18N
        box.setRenderer(new Ren());
        gbc.weightx = 0.75;
        gbc.gridx++;
        result.add(box, gbc);
        lbl.setLabelFor(box);
        return result;
    }

    private static final class Ren extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String s = value == null ? "" : value.toString();
            if (value != null && value.toString().length() > 25) {
                String trunc = "\u2026" + s.substring(s.length() - 23);
                value = trunc;
                list.setToolTipText(s);
            }
            return super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        }
    }

    private static final class ExtensionFileFilter extends FileFilter {

        private final String ext;
        private final String desc;

        public ExtensionFileFilter(String ext) {
            this(ext, null);
        }

        public ExtensionFileFilter(String ext, String desc) {
            this.ext = ext;
            this.desc = desc;
        }

        @Override
        public boolean accept(File f) {
            if (f.isHidden()) {
                return false;
            } else if (f.isDirectory()) {
                return true;
            }
            boolean result = f.getName().endsWith(ext);
            if (!result) {
                return f.getName().toLowerCase().endsWith(ext.toLowerCase());
            }
            return result;
        }

        @Override
        @Messages({"# {0} - extension", "ext={0} Files"})
        public String getDescription() {
            if (desc != null) {
                return desc;
            }
            String e = ext;
            if (e.length() > 1 && e.charAt(0) != '.') {
                e = "." + e;
            }
            return Bundle.ext(e);
        }

        public String toString() {
            return getDescription();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + Objects.hashCode(this.ext);
            hash = 79 * hash + Objects.hashCode(this.desc);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ExtensionFileFilter other = (ExtensionFileFilter) obj;
            if (!Objects.equals(this.ext, other.ext)) {
                return false;
            }
            if (!Objects.equals(this.desc, other.desc)) {
                return false;
            }
            return true;
        }
    }
}
