/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.imagine.helpimpl;

import org.imagine.help.spi.HelpComponentManager;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import static javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import com.mastfrog.geometry.EqPointDouble;
import org.imagine.help.api.HelpItem;
import org.imagine.markdown.uiapi.Markdown;
import org.openide.util.NbPreferences;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.WindowManager;

/**
 *
 * @author Tim Boudreau
 */
@ServiceProvider(service = HelpComponentManager.class)
public class HelpComponentManagerImpl extends HelpComponentManager {

    private HelpBubbleComponent component;
    private HelpItem last;
    private final LinkedList<HelpSeriesItem> queue = new LinkedList<>();

    HelpBubbleComponent component() {
        if (component == null) {
            component = new HelpBubbleComponent();
        }
        return component;
    }

    @Override
    protected void open(HelpItem item) {
        HelpTopComponent.open(item);
    }

    @Override
    protected void enqueueNextPopup(HelpItem item, Component target) {
        if (!isEnabled(item)) {
            return;
        }
        HelpSeriesItem seriesItem = new HelpSeriesItem(item, target);
        if (!queue.contains(seriesItem)) {
            queue.add(seriesItem);
        }
    }

    private static final class HelpSeriesItem {

        final HelpItem item;
        final Reference<Component> component;

        public HelpSeriesItem(HelpItem item, Component component) {
            this.item = item;
            this.component = new WeakReference<>(component);
        }

        public boolean isViable() {
            Component c = component.get();
            boolean result = c != null && c.isDisplayable() && c.isShowing();
            if (result && c instanceof JComponent) {
                result = !((JComponent) c).getVisibleRect().isEmpty();
            }
            return result;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + Objects.hashCode(this.item);
            hash = 29 * hash + Objects.hashCode(this.component);
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
            final HelpSeriesItem other = (HelpSeriesItem) obj;
            if (!Objects.equals(this.item, other.item)) {
                return false;
            }
            if (!Objects.equals(this.component, other.component)) {
                return false;
            }
            return true;
        }
    }

    private static final RequestProcessor HELPLOAD
            = new RequestProcessor("async-help-load", 1);

    private final AsyncHelpTask helper = new AsyncHelpTask();
    static class AsyncHelpTask implements Runnable {

        private final Map<HelpItem, Consumer<Markdown>> pending = new LinkedHashMap<>();
        private final RequestProcessor.Task task = HELPLOAD.create(this);
        private final Map<Markdown, Consumer<Markdown>> displayQueue = new LinkedHashMap<>();

        void add(HelpItem item, Consumer<Markdown> consumer) {
            if (pending.put(item, consumer) == null) {
                task.schedule(100);
            }
        }

        boolean touch() {
            boolean result = false;
            synchronized(displayQueue) {
                if (!displayQueue.isEmpty()) {
                    EventQueue.invokeLater(this);
                    result = true;
                }
            }
            synchronized(pending) {
                if (!pending.isEmpty()) {
                    task.schedule(100);
                    result = true;
                }
            }
            return result;
        }

        @Override
        public void run() {
            if (!EventQueue.isDispatchThread()) {
                HelpItem item = null;
                Consumer<Markdown> consumer = null;
                Markdown md = null;
                synchronized (pending) {
                    Iterator<Map.Entry<HelpItem, Consumer<Markdown>>> iter = pending.entrySet().iterator();
                    if (iter.hasNext()) {
                        Map.Entry<HelpItem, Consumer<Markdown>> e = iter.next();
                        pending.remove(e.getKey());
                        item = e.getKey();
                        consumer = e.getValue();
                        md = e.getKey().getContent(Markdown.class);
                    }
                }
                if (md != null && item != null && consumer != null) {
                    synchronized (displayQueue) {
                        if (displayQueue.put(md, consumer) == null) {
                            EventQueue.invokeLater(this);
                        }
                    }
                }
            } else {
                synchronized(displayQueue) {
                    Iterator<Map.Entry<Markdown, Consumer<Markdown>>> iter = displayQueue.entrySet().iterator();
                    if (iter.hasNext()) {
                        Map.Entry<Markdown, Consumer<Markdown>> e = iter.next();
                        displayQueue.remove(e.getKey());
                        e.getValue().accept(e.getKey());
                    }
                }
            }
        }
    }

    @Override
    protected void popup(HelpItem item, MouseEvent evt) {
        if (component != null && component.isShowing() && last == item) {
            return;
        }
        if (!isEnabled(item)) {
            return;
        }
        helper.add(item, md -> {
            last = item;
            JRootPane root = SwingUtilities.getRootPane(((Component) evt.getSource()));
            Component c = (Component) evt.getSource();
            Point convertedPoint = SwingUtilities.convertPoint(c, evt.getPoint(), root);
            HelpBubbleComponent comp = component();
            comp.setMarkdown(md, convertedPoint);
            configureWindow(root, comp);
        });
    }

    @Override
    protected void popup(HelpItem item, Component target) {
        if (component != null && component.isShowing() && last == item) {
            return;
        }
        if (!isEnabled(item)) {
            return;
        }
        helper.add(item, md -> {
            last = item;
            JRootPane root = SwingUtilities.getRootPane(target);
            System.out.println("activate really ");
            Rectangle converted = SwingUtilities.convertRectangle(target, new Rectangle(0, 0, target.getWidth(), target.getHeight()), root);
            Point2D center = new EqPointDouble(converted.getCenterX(), converted.getCenterY());

            HelpBubbleComponent comp = component();
            comp.setMarkdown(md, center);
            configureWindow(root, comp);
        });
    }

    private void configureWindow(JRootPane root, JComponent comp) {
        JComponent gp = (JComponent) root.getGlassPane();
        if (comp.getParent() != gp) {
            System.out.println("Set up glass pane");
            gp.setLayout(new BorderLayout());
            gp.add(comp, BorderLayout.CENTER);
        }
        if (!gp.isVisible()) {
            gp.setBounds(new Rectangle(0, 0, root.getWidth(), root.getHeight()));
            System.out.println("set glass pane visible");
            gp.setVisible(true);
            gp.doLayout();
            gp.invalidate();
            gp.revalidate();
            gp.repaint();
            configureDismissMonitoring(root);
        }
        comp.setBounds(0, 0, root.getWidth(), root.getHeight());
    }

    private static KeyStroke ESCAPE = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0, true);

    private void configureDismissMonitoring(JRootPane root) {
        ActionMap am = root.getActionMap();
        InputMap im = root.getInputMap(WHEN_IN_FOCUSED_WINDOW);
        DismissAction dismiss = dismissAction(root);
        Action oldEscapeAction = am.get("dismiss");
        Object oldEscapeMapping = im.get(ESCAPE);
        am.put("dismiss", dismiss);
        im.put(ESCAPE, "dismiss");
        if (oldEscapeAction != null) {
            root.putClientProperty("oldEscapeAction", oldEscapeAction);
        }
        if (oldEscapeMapping != null) {
            root.putClientProperty("oldEscapeMapping", oldEscapeMapping);
        }
        attachAwtEventListener(dismiss);
    }

    private void deconfigureDismissMonitoring(JRootPane root) {
        ActionMap am = root.getActionMap();
        InputMap im = root.getInputMap(WHEN_IN_FOCUSED_WINDOW);
        am.remove("dismiss");
        if ("dismiss".equals(im.get(ESCAPE))) {
            im.remove(ESCAPE);
        }
        Action oldEscapeAction = (Action) root.getClientProperty("oldEscapeAction");
        if (oldEscapeAction != null) {
            root.putClientProperty("oldEscapeAction", null);
            am.put("dismiss", oldEscapeAction);
        }
        Object oldEscapeMapping = root.getClientProperty("oldEscapeMapping");
        if (oldEscapeMapping != null) {
            root.putClientProperty("oldEscapeMapping", null);
            im.put(ESCAPE, oldEscapeMapping);
        }
        DismissAction action = (DismissAction) root.getClientProperty("dismiss-help");
        detachAwtEventListener(action);
    }

    private AWTEventListener lastListener;

    private void detachAwtEventListener(AWTEventListener l) {
        Toolkit.getDefaultToolkit().removeAWTEventListener(l);
        if (l == lastListener) {
            lastListener = null;
        }
    }

    private void attachAwtEventListener(AWTEventListener l) {
        if (lastListener == l) {
            return;
        } else if (lastListener != null) {
            Toolkit.getDefaultToolkit().removeAWTEventListener(lastListener);
        }
        lastListener = l;
        Toolkit.getDefaultToolkit().addAWTEventListener(l, MouseEvent.MOUSE_EVENT_MASK);
    }

    DismissAction dismissAction(JRootPane pane) {
        DismissAction result = (DismissAction) pane.getClientProperty("_dismissHelp");
        if (result != null) {
            return result;
        }
        result = new DismissAction(pane);
        pane.putClientProperty("_dismissHelp", result);
        return result;
    }

    class DismissAction extends AbstractAction implements AWTEventListener {

        private final JRootPane root;

        DismissAction(JRootPane root) {
            this.root = root;
            putValue(NAME, "dismiss-help");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            dismissPopupGesturePerformed(root);
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            // Note, if we use MOUSE_PRESSED or MOUSE_RELEASED here,
            // then that event, delivered after the focus event that
            // triggered opening, will instantly close the help bubble
            if (event.getID() == MouseEvent.MOUSE_PRESSED && event instanceof MouseEvent) {
                MouseEvent me = (MouseEvent) event;
                if (component.isEventOnBubble(me)) {
                    dismissPopupGesturePerformed(root);
                    me.consume();
                }
            }
        }
    }

    @Override
    protected void popup(JComponent target) {
        boolean found = findHelpItem(target, (comp, item) -> {
            popup(item, comp);
        });
        if (!found) {
            System.out.println("no help item");
            removePopup();
        }
    }

    private boolean findHelpItem(JComponent target, BiConsumer<JComponent, HelpItem> c) {
        Object o = target.getClientProperty("_help");
        if (o instanceof HelpItem) {
            HelpItem item = (HelpItem) o;
            c.accept(target, item);
            return true;
        }
        Container parent = target.getParent();
        if (parent instanceof JComponent) {
            return findHelpItem((JComponent) parent, c);
        }
        return false;
    }

    @Override
    protected void dismissPopupGesturePerformed(JRootPane root) {
        if (component != null) {
            dismissForSession(last);
            if (!helper.touch()) {
            while (!queue.isEmpty()) {
                HelpSeriesItem next = queue.pop();
                if (isEnabled(next.item) && next.isViable()) {
                    Component c = next.component.get();
                    if (c != null) {
                        popup(next.item, c);
                        return;
                    }
                }
            }
            deactivate(root);
            }
        }
    }

    @Override
    protected void removePopup() {
        if (component != null && component.isDisplayable()) {
            JFrame mainWindow = (JFrame) WindowManager.getDefault().getMainWindow();
            JRootPane root = mainWindow.getRootPane();
            deactivate(root);
        }
    }

    private void deactivate(JRootPane root) {
        if (component != null && component.isDisplayable()) {
            JComponent gp = (JComponent) root.getGlassPane();
            gp.remove(component);
            gp.setVisible(false);
            deconfigureDismissMonitoring(root);
        }
    }

    private Set<String> sessionDismissals = new HashSet<>();

    private void dismissForSession(HelpItem item) {
        sessionDismissals.add(item.identifier());
    }

    private void dismissPermanently(HelpItem item) {
        sessionDismissals.add(item.identifier());
        prefs().putBoolean(item.identifier() + "-enabled", false);
    }

    private boolean isEnabled(HelpItem item) {
        String ident = item.identifier();
        if (sessionDismissals.contains(ident)) {
            return false;
        }
        Preferences prefs = prefs();
        return prefs.getBoolean(ident + "-enabled", true);
    }

    private Preferences prefs() {
        return NbPreferences.forModule(HelpComponentManagerImpl.class);
    }
}
