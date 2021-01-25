/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components.explorer;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import org.netbeans.paint.api.components.TitledPanel2;
import com.mastfrog.swing.layout.VerticalFlowLayout;
import org.openide.cookies.InstanceCookie;
import org.openide.explorer.ExplorerManager;
import org.openide.explorer.view.BeanTreeView;
import org.openide.explorer.view.ChoiceView;
import org.openide.explorer.view.ListView;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.Repository;
import org.openide.loaders.DataObject;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Exceptions;
import org.openide.util.Lookup;

/**
 * A customizer panel for fills and other things which are stored as
 * InstanceDataObjects in the system filesystem, which may provide customizers.
 * Allows for visual selection and customization in the expanded state.
 *
 * @author Tim Boudreau
 */
public class FolderPanel<T> extends TitledPanel2 implements ExplorerManager.Provider {

    private final ExplorerManager mgr;
    private final Class<T> selectionType;
    private final PCL pcl = new PCL();
    private T selection;
    private Component oldCustomizer;
    private final JPanel inner;
    public static final String PROP_SELECTION = "selection";

    /**
     * Create a default action using a ListView as the selection tool in the
     * expanded state, and a combo box otherwise, with the default customization
     * action.
     *
     * @param <T> The type of object getSelection() returns
     * @param folder The folder path in the system filesystem
     * @param selectionType The object type for selection
     * @return A folder panel
     */
    public static <T> FolderPanel<T> create(String folder, Class<T> selectionType) {
        return create(folder, selectionType, false);
    }

    /**
     * Create a default action using a ListView as the selection tool in the
     * expanded state, and a combo box otherwise, with the default customization
     * action.
     *
     * @param <T> The type of object getSelection() returns
     * @param folder The folder path in the system filesystem
     * @param selectionType The object type for selection
     * @param customizeAction An object which is passed the lookup of the node
     * for the folder in the system filesystem that is being viewed.
     * @return A folder panel
     */
    public static <T> FolderPanel<T> create(String folder, Class<T> selectionType, Consumer<Lookup> customizeAction) {
        return create(folder, selectionType, "", customizeAction);
    }

    public static <T> FolderPanel<T> create(String folder, Class<T> selectionType, String prefix, Consumer<Lookup> customizeAction) {
        return create(folder, selectionType, false, customizeAction);
    }

    public static <T> FolderPanel<T> create(String folder, Class<T> selectionType, boolean useTree) {
        return create(folder, selectionType, "", useTree);
    }

    public static <T> FolderPanel<T> create(String folder, Class<T> selectionType, String prefix, boolean useTree) {
        return create(folder, selectionType, useTree, prefix, null);
    }

    public static <T> FolderPanel<T> create(String folder, Class<T> selectionType, boolean useTree, Consumer<Lookup> customizeAction) {
        return create(folder, selectionType, useTree, "", customizeAction);
    }

    public static <T> FolderPanel<T> create(String folder, Class<T> selectionType, boolean useTree, String prefix, Consumer<Lookup> customizeAction) {
        if (prefix == null) {
            prefix = "";
        }
        ChoiceView cv = new ChoiceView();
        JScrollPane view;
        if (useTree) {
            BeanTreeView bv;
            view = bv = new BeanTreeView();
            bv.setRootVisible(false);
            bv.setPopupAllowed(false);
        } else {
            ListView lv;
            view = lv = new ListView();
            lv.setPopupAllowed(false);
        }
        ExplorerManager mgr = new ExplorerManager();
        view.setBorder(BorderFactory.createLineBorder(UIManager.getColor("controlShadow")));
        JPanel inner = new JPanel(new VerticalFlowLayout(0));
        inner.add(view);
        Function<Boolean, JComponent> selector = expanded -> {
            return expanded ? inner : cv;
        };
        boolean initiallyExpanded = false;
        String displayName = folder;
        Runnable onCustomize;
        if (customizeAction != null) {
            onCustomize = () -> {
                customizeAction.accept(mgr.getRootContext().getLookup());
            };
        } else {
            onCustomize = () -> {
                String path = mgr.getRootContext().getLookup().lookup(DataObject.class).getPrimaryFile().getPath();
                CustomizeFolderPanel.showDialog(path);
            };
        }
        try {
            FileObject fld = Repository.getDefault().getDefaultFileSystem().getRoot().getFileObject(folder);
            if (fld == null) {
                fld = Repository.getDefault().getDefaultFileSystem().getRoot().createFolder(folder);
            }
            Boolean exp = (Boolean) fld.getAttribute(prefix + "expanded"); //NOI18N
            initiallyExpanded = exp == null ? false : exp;
            DataObject dob = DataObject.find(fld);
            displayName = dob.getNodeDelegate().getDisplayName();
            mgr.setRootContext(new FilterNode(dob.getNodeDelegate(),
                    new NameFilterNodeChildren(dob.getNodeDelegate())));
            String last = (String) fld.getAttribute(prefix + "lastSelection"); //NOI18N
            Node[] n = mgr.getRootContext().getChildren().getNodes(true);
            if (n.length > 0) {
                Node sel = n[0];
                if (last != null) {
                    Node lsel = mgr.getRootContext().getChildren().findChild(last);
                    if (lsel != null) {
                        sel = lsel;
                    }
                }
                // set an initial selection, so the folder isn't selected
                mgr.setSelectedNodes(new Node[]{sel});
            }
        } catch (IOException | PropertyVetoException ex) {
            Exceptions.printStackTrace(ex);
        }
        return new FolderPanel<>(displayName, initiallyExpanded, prefix,
                selectionType, selector, onCustomize, mgr, inner);
    }
    private final String prefix;

    public FolderPanel(String displayName, boolean initiallySelected, String prefix,
            Class<T> selectionType, Function<Boolean, JComponent> factory,
            Runnable onExpand, ExplorerManager mgr, JPanel innerPanel) {
        super(displayName, initiallySelected, factory, onExpand);
        this.inner = innerPanel;
        this.prefix = prefix == null ? "" : prefix;
        this.selectionType = selectionType;
        this.mgr = mgr;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        mgr.addPropertyChangeListener(pcl);
        pcl.propertyChange(null);
        T old = selection;
        selection = null;
        setSelection(old);
        if (old != null && oldCustomizer != null) {
            inner.add(oldCustomizer);
        }
    }

    @Override
    public void removeNotify() {
        if (oldCustomizer != null) {
            inner.remove(oldCustomizer);
//            oldCustomizer = null;
        }
        mgr.removePropertyChangeListener(pcl);
        super.removeNotify();
    }

    @Override
    public ExplorerManager getExplorerManager() {
        return mgr;
    }

    private void setSelection(T t) {
        T old = selection;
        if (old == t) {
            return;
        }
        selection = t;
        if (!isDisplayable()) {
            return;
        }
        if (t instanceof Customizable) {
            Customizable c = (Customizable) t;
            Component comp = c.getCustomizer();
            boolean changed = false;
            if (oldCustomizer != null) {
                inner.remove(oldCustomizer);
                changed = true;
            }
            if (comp != null) {
                oldCustomizer = comp;
                inner.add(comp);
                changed = true;
            }
            if (changed) {
                inner.invalidate();
                inner.revalidate();
                inner.repaint();
            }
        }
        onSelectionChanged(old, selection);
    }

    protected void onSelectionChanged(T old, T nue) {
        firePropertyChange(PROP_SELECTION, old, nue);
    }

    public T getSelection() {
        return selection;
    }

    private class PCL implements PropertyChangeListener {

        public void propertyChange(PropertyChangeEvent evt) {
            if (evt == null
                    || ExplorerManager.PROP_SELECTED_NODES.equals(evt.getPropertyName())) {
                if (mgr.getSelectedNodes().length > 0) {
                    Node n = mgr.getSelectedNodes()[0];
                    InstanceCookie ic = (InstanceCookie) n.getCookie(InstanceCookie.class);
                    try {
                        if (ic != null && selectionType.isAssignableFrom(ic.instanceClass())) {
                            setSelection((T) ic.instanceCreate());
                        } else {
                            setSelection(null);
                        }
                        FileObject fld = mgr.getRootContext().getLookup()
                                .lookup(DataObject.class).getPrimaryFile();
                        fld.setAttribute(prefix + "lastSelection", n.getName()); //NOI18N
                    } catch (Exception cnfe) {
                        throw new IllegalStateException(cnfe);
                    }
                }
            }
        }
    }
}
