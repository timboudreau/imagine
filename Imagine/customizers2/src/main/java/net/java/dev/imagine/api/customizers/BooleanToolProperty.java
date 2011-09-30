/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.java.dev.imagine.api.customizers;

/**
 *
 * @author Tim Boudreau
 */
final class BooleanToolProperty<R extends Enum> extends AbstractToolProperty<Boolean, R>{
    private final boolean defaultValue;
    BooleanToolProperty(R r, boolean defaultValue) {
        super (r, Boolean.class);
        this.defaultValue = defaultValue;
    }

    @Override
    protected Boolean load() {
        return getPreferences().getBoolean(name().name(), defaultValue);
    }

    @Override
    protected void save(Boolean t) {
        getPreferences().putBoolean(name().name(), t == null ? false : t.booleanValue());
    }
    
}
