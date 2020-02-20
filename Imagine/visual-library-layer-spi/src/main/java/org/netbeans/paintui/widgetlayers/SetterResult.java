package org.netbeans.paintui.widgetlayers;

/**
 * Calls which set properties on the layer that might be handled internally by a
 * layer are delegated to the WidgetFactory, which can return one of these
 * values to indicate if the normal handling of the setter should proceed. For
 * example, a layer which shows text and uses that text for its name might want
 * to handle setName() in its own way.
 */
public enum SetterResult {
    HANDLED, NOT_HANDLED

}
