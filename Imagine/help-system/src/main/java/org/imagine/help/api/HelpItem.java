package org.imagine.help.api;

import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import javax.swing.JComponent;
import org.imagine.help.impl.HeadingComparator;
import org.imagine.help.impl.HelpComponentManagerTrampoline;

/**
 * A help item under a help topic; generally implementations of HelpItem are
 * generated by an annotation processor that processes <code>&#064;Help</code>
 * annotations (the module must have help-annotation-processors as a
 * <i>provided</i> dependency on its compile classpath or annotation processor
 * path - with Maven, do <u><b>not</b></u>
 * list it as a <i>direct</i> [non-provided, non-optional] dependency or it will
 * wreak havoc with other dependencies your project may have in non-obvious ways
 * - if weird things happen in your build, check this first!).
 *
 * <h3>Associating Help Items with Components and Contexts</h3>
 * <p>
 * There are several ways to associate help with the activity the user is doing,
 * such that the current task gets the appropriate help whether or not the code
 * supplying the context has direct access to the components involved.
 * Components may supply help either via a client property, or by implementing
 * <code>HelpItem.Provider</code>, so help may be associated with components
 * where subclassing is not appropriate; the selection context lookup
 * <code>Utilities.actionsGlobalContext</code> may contain a
 * <code>HelpItem</code> <i>or</i> a <code>HelpItem.Provider</code>.
 * </p><p>
 * The main help action will look up the appropriate help item according to the
 * following (non-normative, subject to change) algorithm: Find the current help
 * context according to the following algorithm:
 * </p>
 * <ol>
 * <li>If the currently focused component is a JComponent and has a HelpItem as
 * the client property _help or _popupHelp, return that</li>
 * <li>If the Utilities.actionsGlobalContext() lookup contains a HelpItem,
 * return that</li>
 * <li>If the Utilities.actionsGlobalContext() lookup contains a
 * HelpItem.Provider, which returns a non-null help item, return that</li>
 * <li>If the currently focused component has an ancestor that implements
 * HelpItem.Provider, return that</li>
 * <li>If the root pane of the current focus owner, or if none, that of the
 * application main window has a HelpItem as the client property _help, return
 * that</li>
 * </ol>
 * <p>
 * Global, application-wide help - the fallback if no other context is detected,
 * should, as the above suggests, be associated with the root pane of the main
 * window on startup.
 * </p>
 * <h3>Associating Help Items with a Shared Topic</h3>
 * Help items can be associated with a topic in several ways:
 * <ol>
 * <li>The <code>&#064;Topic</code> annotation can be applied to a class or a
 * package, and will be inherited by all help items defined within that class or
 * package unless they explicitly override it.</li>
 * <li>Set the topic field for each locale inside the annotation that generated
 * the help item - this mechanism exists to allow one help item in a class or
 * package to have a different topic where appropriate. Prefer the
 * <code>&#064; Topic</code> annotation any place you are going to use the same
 * topic more than once, otherwise it can be tedious to find all the items that
 * need editing, should you need to change it</li>
 * </ol>
 * <h3>Headings</h3>
 * Each help item has a heading, which is simply the first line of text from the
 * markup, ignoring markup characters; preferably this is short. Popup help that
 * does not need a heading should be marked as <code>noIndex</code> to avoid
 * polluting the help index with small, context-specific bits of text.
 *
 * @author Tim Boudreau
 */
public interface HelpItem {

    /**
     * Get the content of the help item, for the system default locale, parsed
     * as the passed type; the type passed must be supported by the underlying
     * help system implementation (with the default implementation, the only
     * type supported is <code>Markdown.class</code> from the
     * <code>markdown-grammar</code> sibling project. This mechanism exists
     * mainly to ensure that modules which implement HelpItem do not wind up
     * inheriting a dependency on a particular parser or parsing mechanism; you
     * annotate your class with <code>&#064;Help</code> and some markdown
     * associated with various locales, and magically, the help system renders
     * it nicely, and how that happens is a separate concern.
     *
     * @param <T> The type requested
     * @param type The type requested
     * @return An object of type T representing the markup associated with this
     * help item, parsed in some machine-friendly fashion, or null if either an
     * unsupported type was requested, or if no help exists for the default
     * locale and no fallback help is provided.
     */
    default <T> T getContent(Class<T> type) {
        return getContent(type, Locale.getDefault());
    }

    /**
     * Get the content of the help item, for the system default locale, parsed
     * as the passed type; the type passed must be supported by the underlying
     * help system implementation (with the default implementation, the only
     * type supported is <code>Markdown.class</code> from the
     * <code>markdown-grammar</code> sibling project. This mechanism exists
     * mainly to ensure that modules which implement HelpItem do not wind up
     * inheriting a dependency on a particular parser or parsing mechanism; you
     * annotate your class with <code>&#064;Help</code> and some markdown
     * associated with various locales, and magically, the help system renders
     * it nicely, and how that happens is a separate concern.
     *
     * @param <T> The type requested
     * @param type The type requested
     * @param locale The locale requested - if it is a specific country variant
     * such as <code>en-ZA</code> and no markup exists for South African
     * English, but a more general set of markup associated with English "en"
     * does exist, or some other dialect of English, those will be used in that
     * order when present
     * @return An object of type T representing the markup associated with this
     * help item, parsed in some machine-friendly fashion, or null if either an
     * unsupported type was requested, or if no help exists for the default
     * locale and no fallback help is provided.
     */
    <T> T getContent(Class<T> type, Locale locale);

    /**
     * Get the topic this help item falls under, in the default locale.
     *
     * @return A topic, or null
     */
    default String topic() {
        return topic(Locale.getDefault());
    }

    /**
     * Get the topic this help item falls under, in the default locale, or the
     * nearest language equivalent, if available.
     *
     * @return A heading line or null
     */
    default String heading() {
        return heading(Locale.getDefault());
    }

    /**
     * Get the plain text of this help item, with all markup elided and newlines
     * inserted between paragraphs, headings, list items and other appropriate
     * places, in the default locale or nearest same-language equivalent, if
     * available.
     *
     * @return A string or null
     */
    default String asPlainText() {
        return asPlainText(Locale.getDefault());
    }

    /**
     * Get the topic this help item falls under, in the requested locale, or the
     * nearest language equivalent, if available.
     *
     * @param locale the requested locale
     * @return A topic, or null
     */
    String topic(Locale locale);

    /**
     * Get the topic this help item falls under, in the default locale, or the
     * nearest language equivalent, if available.
     *
     * @return A heading line or null
     */
    String heading(Locale locale);

    /**
     * Get the plain text of this help item, with all markup elided and newlines
     * inserted between paragraphs, headings, list items and other appropriate
     * places, in the default locale or nearest same-language equivalent, if
     * available.
     *
     * @param locale The requested locale
     * @return A string or null
     */
    String asPlainText(Locale locale);

    /**
     * Request that the help-system implementation open this item in a window or
     * in some other fashion.
     */
    default void open() {
        HelpComponentManagerTrampoline.getInstance().open(this);
    }

    /**
     * Request that the help-system implementation open this item in a popup
     * visible above other content and indicating that it applies to the
     * component which received the mouse event.
     *
     * @param evt A mouse event
     */
    default void activate(MouseEvent evt) {
        HelpComponentManagerTrampoline.getInstance().activate(this, evt);
    }

    /**
     * Request that the help-system implementation open this item in a popup
     * visible above other content and indicating that it applies to the passed
     * component.
     *
     * @param evt A mouse event
     */
    default void activate(Component target) {
        HelpComponentManagerTrampoline.getInstance().activate(this, target);
    }

    /**
     * Associate this help item with the passed component, so that invoking the
     * global help action will open this item when it or a child of it has focus
     * and no intervening ancestor of the focused component supplies a different
     * help item.
     *
     * @param comp A component
     * @return The previous help item, if any, associated with the passed
     * component
     */
    default HelpItem associateWith(JComponent comp) {
        Object old = comp.getClientProperty("_help");
        if (old == this) {
            return null;
        }
        comp.putClientProperty("_help", this);
        return old instanceof HelpItem ? (HelpItem) old : null;
    }

    /**
     * Associate this help item with the passed component, so that that
     * component gaining focus will open this item in a popup when it gains
     * focus, if the user has not previously dismissed this help item.
     *
     * @param comp A component
     * @return The previous help item, if any, associated with the passed
     * component
     */
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

    /**
     * Break the association between a component and this help item, if any.
     *
     * @param comp A component
     */
    default boolean detach(JComponent comp) {
        Object help = comp.getClientProperty("_help");
        if (help == this) {
            comp.removeFocusListener(HelpItemFocusListener.INSTANCE);
            return true;
        }
        return false;
    }

    /**
     * Enqueue this help item to either be shown as pop-up help indicating its
     * association with the passed component, or to be shown as soon as the user
     * dismisses the currently popped-up help item if one is present; used to
     * present the user with a series of informational guides when presented
     * with a UI they may not have seen before.
     *
     * @param associateWith A component
     */
    default void enqueueInSeries(JComponent associateWith) {
        HelpComponentManagerTrampoline.getInstance().enqueue(this, associateWith);
    }

    /**
     * Get the programmatic, non-localized, unqualified name of this help item.
     *
     * @return
     */
    String name();

    /**
     * Get the unique, system-wide identifier for this item - typically
     * implemented as <code>getClass().getPackageName() + '.' + name()</code>.
     *
     * @return
     */
    default String identifier() {

        return getClass().getPackage().getName() + '.' + name();
    }

    /**
     * Get a list of topics related to this topic, if any.
     *
     * @return A list of other help items
     */
    default List<? extends HelpItem> related() {
        return Collections.emptyList();
    }

    /**
     * Get a comparator that alphabetizes help items case-insensitively by
     * heading.
     *
     * @param loc The locale to use when comparing items
     * @return
     */
    static Comparator<HelpItem> headingComparator(Locale loc) {
        return new HeadingComparator(loc);
    }

    /**
     * Look up a help item by its qualified id.
     *
     * @param qualifiedId The id
     * @return A help item or null if no such ID is registered
     */
    static HelpItem find(String qualifiedId) {
        return HelpComponentManagerTrampoline.getIndexTrampoline().resolve(qualifiedId);
    }

    public interface Provider {

        public HelpItem getHelpItem();
    }
}
