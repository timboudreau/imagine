/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.paint.api.components;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.geom.AffineTransform;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import javax.swing.JPanel;

/**
 * Root panel which implements SharedLayoutPanel to allow descendants that have
 * an LDPLayout or similar to share layout information, so nested components
 * will be aligned.
 *
 * @author Tim Boudreau
 */
public class SharedLayoutRootPanel extends JPanel implements SharedLayoutData {

    private SharedLayoutData data;
    private Runnable onShow;

    public SharedLayoutRootPanel() {
        super(new VerticalFlowLayout());
        setBorder(new FlexEmptyBorder());
        if (TitledPanel2.isDebugLayout()) {
            setBackground(new Color(Color.HSBtoRGB(ThreadLocalRandom.current().nextFloat(), 0.425F, 0.73F)));
        }
    }

    public SharedLayoutRootPanel(Component first, Component... contents) {
        this();
        add(first);
        for (Component c : contents) {
            add(c);
        }
    }

    public SharedLayoutRootPanel(double scaleFonts, Component first, Component... contents) {
        this(scaleFonts);
        add(first);
        for (Component c : contents) {
            add(c);
        }
    }

    public SharedLayoutRootPanel(double scaleFonts) {
        this();
        if (scaleFonts != 1 && scaleFonts > 0) {
            setUI(new FontManagingPanelUI(
                    AffineTransform.getScaleInstance(scaleFonts, scaleFonts)));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("SharedLayoutRootPanel-")
                .append(Integer.toString(System.identityHashCode(this), 36))
                .append(" talking to ");
        String dataString;
        if (data == this) {
            dataString = "self";
        } else if (data == null) {
            dataString = "<nothing>";
        } else {
            dataString = data.toString();
        }
        sb.append(dataString);

        return sb.toString();
    }

    public SharedLayoutRootPanel onShow(Runnable onShow) {
        this.onShow = onShow;
        return this;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        data = SharedLayoutData.find(this);
        if (data == null) {
            data = new DefaultSharedLayoutData();
        }
        for (LayoutDataProvider p : registered) {
            data.register(p);
        }
        registered.clear();
        if (onShow != null) {
            onShow.run();
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        data = null;
    }

    private SharedLayoutData data() {
        if (data != null) {
            return data;
        }
        if (getParent() instanceof SharedLayoutRootPanel) {
            return ((SharedLayoutRootPanel) getParent()).data();
        }
        return SharedLayoutData.find(this);
    }

    private boolean hasExpandables;
    private boolean checkExpandables;
    @Override
    protected final void addImpl(Component comp, Object constraints, int index) {
        hasExpandables = isExpandable(comp);
        super.addImpl(comp, constraints, index);
    }

    private boolean isExpandable(Component comp) {
        return comp instanceof TitledPanel2;
    }

    @Override
    public final void remove(int index) {
        Component old = getComponent(index);
        super.remove(index);
        int count = getComponentCount();
        if (hasExpandables && isExpandable(old)) {
            checkExpandables = count > 0;
        } else if (count == 0) {
            hasExpandables = false;
            checkExpandables= false;
        }
    }

    @Override
    public final void removeAll() {
        super.removeAll();
        checkExpandables= false;
        hasExpandables = false;
    }

    @Override
    public final void remove(Component comp) {
        super.remove(comp);
        int count = getComponentCount();
        if (hasExpandables && isExpandable(comp)) {
            checkExpandables = count > 0;
        } else if (count == 0) {
            hasExpandables = false;
            checkExpandables = false;
        }
    }

    boolean hasExpandables() {
        // Allows borders to check if there is going to be a big left margin already
        if (checkExpandables) {
            for (Component c : getComponents()) {
                if (isExpandable(c)) {
                    hasExpandables = true;
                    break;
                }
            }
            checkExpandables = false;
        }
        return hasExpandables;
    }

    @Override
    public int xPosForColumn(int column, Container requester) {
        if (data == null) {
            return 0;
        }
        return data().xPosForColumn(column, requester);
    }

    private Set<LayoutDataProvider> registered = new HashSet<>();

    @Override
    public void register(LayoutDataProvider p) {
        if (data == null) {
            registered.add(p);
            return;
        }
        data.register(p);
    }

    @Override
    public void unregister(LayoutDataProvider p) {
        if (data == null) {
            registered.remove(p);
            return;
        }
        data.unregister(p);
    }

    @Override
    public void expanded(LayoutDataProvider p, boolean state) {
        if (data == null) {
            return;
        }
        data.expanded(p, state);
    }

    @Override
    public int indentFor(Container requester) {
        if (data == null) {
            return 0;
        }
        return data.indentFor(requester);
    }
}
