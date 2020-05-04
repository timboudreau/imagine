package org.imagine.editor.api.grid;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.imagine.editor.api.snap.SnapKind;
import org.openide.util.NbPreferences;
import org.openide.util.WeakSet;

/**
 *
 * @author Tim Boudreau
 */
public final class SnapSettings implements Serializable {

    private static SnapSettings INSTANCE = new SnapSettings();
    final Set<SnapKind> kinds;
    private boolean enabled = false;
    private transient WeakSet<ChangeListener> listeners;

    public SnapSettings() {
        kinds = SnapKind.kinds(true);
    }

    public SnapSettings(Set<SnapKind> kinds, boolean enabled) {
        this.enabled = enabled;
        this.kinds = EnumSet.copyOf(kinds);
    }

    public SnapSettings copy() {
        return new SnapSettings(kinds, enabled);
    }

    public void copyFrom(SnapSettings settings) {
        boolean changed = false;
        if (!settings.kinds.equals(kinds)) {
            kinds.clear();
            kinds.addAll(settings.kinds);
            changed = true;
        }
        if (settings.isEnabled() != isEnabled()) {
            enabled = !enabled;
            changed = true;
        }
        if (changed) {
            fire();
        }
    }

    public static SnapSettings getGlobal() {
        return INSTANCE;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Set<SnapKind> getSnapKinds() {
        return EnumSet.copyOf(kinds);
    }

    public Set<SnapKind> getEnabledSnapKinds() {
        Set<SnapKind> result = getSnapKinds();
        if (result.contains(SnapKind.GRID) && !Grid.getInstance().isEnabled()) {
            result.remove(SnapKind.GRID);
        }
        return result;
    }

    public void setEnabled(boolean val) {
        if (this.enabled != val) {
            this.enabled = val;
            fire();
        }
    }

    public void addSnapKind(SnapKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Null kind");
        }
        if (!kinds.contains(kind)) {
            kinds.add(kind);
            fire();
        }
    }

    public void removeSnapKind(SnapKind kind) {
        if (kind == null) {
            throw new IllegalArgumentException("Null kind");
        }
        if (kinds.contains(kind)) {
            kinds.remove(kind);
            fire();
        }
    }

    public void setSnapKinds(Set<SnapKind> kinds) {
        if (kinds == null) {
            throw new IllegalArgumentException("Null kinds");
        }
        if (!kinds.equals(this.kinds)) {
            this.kinds.clear();
            this.kinds.addAll(kinds);
            fire();
        }
    }

    public void addChangeListener(ChangeListener cl) {
        if (listeners == null) {
            listeners = new WeakSet<>();
        }
        listeners.add(cl);
    }

    public void removeChangeListener(ChangeListener cl) {
        if (listeners != null) {
            listeners.remove(cl);
        }
    }

    private void fire() {
        if (listeners != null) {
            if (!listeners.isEmpty()) {
                ChangeEvent evt = new ChangeEvent(this);
                // avoid CMEs
                Set<ChangeListener> copy = new HashSet<>(this.listeners);
                for (ChangeListener l : copy) {
                    l.stateChanged(evt);
                }
            }
        }
        if (this == INSTANCE) {
            save();
        }
    }

    private void save() {
        Preferences prefs = NbPreferences.forModule(SnapSettings.class);
        EnumSet<SnapKind> all = EnumSet.allOf(SnapKind.class);
        all.removeAll(kinds);
        for (SnapKind k : kinds) {
            prefs.putBoolean("snap-" + k.name(), true);
        }
        for (SnapKind k : all) {
            prefs.putBoolean("snap-" + k.name(), false);
        }
        prefs.putBoolean("snapEnabled", isEnabled());
    }

    static {
        Set<SnapKind> defaultKinds = SnapKind.kinds(true);
        Preferences prefs = NbPreferences.forModule(SnapSettings.class);
        Set<SnapKind> values = EnumSet.noneOf(SnapKind.class);
        for (SnapKind k : defaultKinds) {
            boolean ena = prefs.getBoolean("snap-" + k.name(),
                    k == SnapKind.POSITION || k == SnapKind.GRID);
            if (ena) {
                values.add(k);
            }
        }
        INSTANCE.kinds.clear();
        INSTANCE.kinds.addAll(values);
        INSTANCE.enabled = prefs.getBoolean("snapEnabled", true);
    }
}
