package org.imagine.help.api;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.Locale;
import javax.swing.JComponent;
import org.imagine.help.impl.HeadingComparator;
import org.imagine.help.impl.HelpComponentManagerTrampoline;

/**
 *
 * @author Tim Boudreau
 */
public interface HelpItem {

    default <T> T getContent(Class<T> type) {
        return getContent(type, Locale.getDefault());
    }

    <T> T getContent(Class<T> type, Locale locale);

    default String topic() {
        return topic(Locale.getDefault());
    }

    default String heading() {
        return heading(Locale.getDefault());
    }

    default String asPlainText() {
        return asPlainText(Locale.getDefault());
    }

    String topic(Locale locale);

    String heading(Locale locale);

    String asPlainText(Locale locale);

    default void activate(MouseEvent evt) {
        HelpComponentManagerTrampoline.INSTANCE.activate(this, evt);
    }

    default void activate(Component target) {
        HelpComponentManagerTrampoline.INSTANCE.activate(this, target);
    }

    default void attach(JComponent comp) {
        Object old = comp.getClientProperty("_help");
        if (old == this) {
            return;
        }
        comp.putClientProperty("_help", this);
        if (old == null) {
            comp.addFocusListener(HelpItemFocusListener.INSTANCE);
        }
    }

    default void detach(JComponent comp) {
        Object help = comp.getClientProperty("_help");
        if (help == this) {
            comp.removeFocusListener(HelpItemFocusListener.INSTANCE);
        }
    }

    default void enqueueInSeries(JComponent associateWith) {
        HelpComponentManagerTrampoline.INSTANCE.enqueue(this, associateWith);
    }

    String name();

    default String identifier() {
        return getClass().getName().replace('.', '-') + "-" + name();
    }

    static Comparator<HelpItem> headingComparator(Locale loc) {
        return new HeadingComparator(loc);
    }
}
