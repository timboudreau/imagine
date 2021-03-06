package net.dev.java.imagine.spi.tool.impl;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JToggleButton;
import net.dev.java.imagine.api.tool.SelectedTool;
import net.dev.java.imagine.api.tool.Tool;
import net.dev.java.imagine.spi.tool.Tools;
import org.openide.filesystems.FileObject;
import org.openide.util.ContextAwareAction;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.Utilities;
import org.openide.util.actions.Presenter;

/**
 * A generic action for setting the selected tool
 *
 * @author Tim Boudreau
 */
public final class ToolAction implements Action, ContextAwareAction, Presenter.Menu, Presenter.Toolbar {

    private final String name;
    private final PropertyChangeSupport supp = new PropertyChangeSupport(this);
    private Map<String, Object> pairs;
    private final Lookup lkp;
    public static final String TOOL_NAME_ATTRIBUTE = "toolName";

    public ToolAction(String name, Lookup lkp) {
        this.name = name;
        this.lkp = lkp;
    }

    public ToolAction(String name) {
        this(name, Utilities.actionsGlobalContext());
    }

    private final LookupListener ll = new LookupListener() {
        @Override
        public void resultChanged(LookupEvent le) {
            ToolAction.this.resultChanged(le);
        }
    };
    private final SelectedTool.Observer obs = new SelectedTool.Observer() {

        @Override
        public void selectedToolChanged(Tool old, Tool nue) {
            ToolAction.this.selectedToolChanged(old, nue);
        }
    };

    public static ToolAction create(FileObject folder) {
        String nm = (String) folder.getAttribute(TOOL_NAME_ATTRIBUTE);
        if (nm == null) {
            Logger.getLogger(ToolAction.class.getName()).log(Level.WARNING,
                    "{0}" + " missing " + TOOL_NAME_ATTRIBUTE + " attribute. "
                    + "Tool action will probably not work.", folder.getPath());
            nm = folder.getNameExt();
        }
        return new ToolAction(nm);
    }
    private boolean warned;

    private Tool tool() {
        Tool result = Tools.getDefault().get(name);
        if (result == null && !warned) {
            warned = true;
            Logger.getLogger(ToolAction.class.getName()).log(Level.WARNING,
                    "No tool exists named {0}", name);
        }
        return result;
    }
    private final Set<Lookup.Result<?>> results = new HashSet<>();

    private void addNotify() {
        Tool tool = tool();
        if (tool != null) {
            for (Class<?> c : tool.getTypes()) {
                Lookup.Result<?> r = result(c);
                results.add(r);
                r.addLookupListener(ll);
                r.allInstances();
            }
            SelectedTool.getDefault().addObserver(obs);
        }
    }

    private <T> Lookup.Result<T> result(Class<T> type) {
        return lkp.lookupResult(type);
    }

    private void removeNotify() {
        for (Lookup.Result<?> r : results) {
            r.removeLookupListener(ll);
        }
        results.clear();
        SelectedTool.getDefault().removeObserver(obs);
    }

    private void fireEnabledChange() {
        boolean enabled = isEnabled();
//        if (enabled != lastEnabled) {
        supp.firePropertyChange("enabled", !enabled, enabled);
//        }
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        boolean hadListeners = supp.getPropertyChangeListeners().length > 0;
        supp.addPropertyChangeListener(listener);
        if (!hadListeners) {
            addNotify();
        }
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        supp.removePropertyChangeListener(listener);
        if (supp.getPropertyChangeListeners().length == 0) {
            removeNotify();
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        SelectedTool.getDefault().setSelectedTool(tool());
    }

    @Override
    public Action createContextAwareInstance(Lookup lkp) {
        ToolAction result = new ToolAction(name, lkp);
        if (pairs != null && !pairs.isEmpty()) {
            if (result.pairs == null) {
                result.pairs = new HashMap<>();
            }
            result.pairs.putAll(pairs);
        }
        return result;
    }

    private Icon icon;
    private Icon disabledIcon;

    private Icon disabledIcon() {
        Icon icon = icon();
        if (disabledIcon == null && icon.getIconHeight() > 0 && icon.getIconWidth() > 0) {
            disabledIcon = ImageUtilities.createDisabledIcon(icon);
        }
        return disabledIcon;
    }

    private Icon icon() {
        return icon == null ? icon = tool().getIcon() : icon;
    }

    @Override
    public Object getValue(String key) {
        Tool tool = tool();
        if (tool != null) {
            if (Action.NAME.equals(key)) {
                return tool.getDisplayName();
            } else if (Action.LARGE_ICON_KEY.equals(key)) {
                return icon();
            } else if (Action.SMALL_ICON.equals(key)) {
                return icon();
            } else if (Action.SHORT_DESCRIPTION.equals(key)) {
                return tool.getDisplayName();
            } else if (Action.ACTION_COMMAND_KEY.equals(key)) {
                return name;
            } else if (Action.SELECTED_KEY.equals(key)) {
                return SelectedTool.getDefault().isToolSelected(name);
            }
        }
        return pairs == null ? null : pairs.get(key);
    }

    @Override
    public void putValue(String key, Object value) {
        Object old = getValue(key);
        if (pairs == null) {
            pairs = new HashMap<>(3);
        }
        pairs.put(key, value);
        supp.firePropertyChange(key, old, value);
    }

    @Override
    public void setEnabled(boolean b) {
        throw new UnsupportedOperationException("Not supported");
    }
    boolean lastEnabled;
    boolean inIsEnabled;

    @Override
    public boolean isEnabled() {
        if (inIsEnabled) {
            return lastEnabled;
        }
        try {
            inIsEnabled = true;
            Tool tool = tool();
            boolean result = false;
            for (Class<?> type : tool.getTypes()) {
                result = !lkp.lookupResult(type).allItems().isEmpty();
                if (result) {
                    Object o = lkp.lookup(type);
                    if (o instanceof Lookup.Provider) {
                        Lookup.Provider prov = (Lookup.Provider) o;
                        if (!tool.canAttach(prov)) {
                            result = false;
                        }
                    }
                    break;
                }
            }
            if (lastEnabled != result) {
                lastEnabled = result;
                supp.firePropertyChange("enabled", lastEnabled, result);
            }
            return lastEnabled = result;
        } finally {
            inIsEnabled = false;
        }
    }

    public void resultChanged(LookupEvent le) {
        fireEnabledChange();
    }

    //XXX both presenters are probably listener leaks
    @Override
    public JMenuItem getMenuPresenter() {
        return new JCheckBoxMenuItem(this);
    }

    @Override
    public Component getToolbarPresenter() {
        JToggleButton result = new LazyToggleButton(this);
        result.setText("");
        result.setBorderPainted(false);
        result.setDisabledIcon(disabledIcon());
        result.setRolloverEnabled(true);
        Tool tool = tool();
        if (tool != null) {
            if (SelectedTool.getDefault().isToolSelected(tool.getName())) {
                result.setSelected(true);
            }
        }
//        result.setContentAreaFilled(false);
        return result;
    }

    static final AbstractAction dummyAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            throw new UnsupportedOperationException("Action should be replaced");
        }
    };

    static class LazyToggleButton extends JToggleButton {

        private final Action realAction;

        LazyToggleButton(Action a) {
            super(dummyAction);
            realAction = a;
        }

        @Override
        public void addNotify() {
            setAction(realAction);
            super.addNotify();
        }

        @Override
        public void removeNotify() {
            super.removeNotify();
            // So this button doesn't remain held permanently as a listener
            // on the action
            setAction(dummyAction);
        }

    }

    public void selectedToolChanged(Tool old, Tool nue) {
        String oldName = old == null ? "" : old.getName();
        String newName = nue == null ? "" : nue.getName();
        if (oldName.equals(name)) {
            supp.firePropertyChange(Action.SELECTED_KEY, true, false);
        } else if (newName.equals(name)) {
            supp.firePropertyChange(Action.SELECTED_KEY, false, true);
        }
    }
}
