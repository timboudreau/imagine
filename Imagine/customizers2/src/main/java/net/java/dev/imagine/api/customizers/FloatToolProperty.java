package net.java.dev.imagine.api.customizers;

/**
 *
 * @author Tim Boudreau
 */
final class FloatToolProperty<R extends Enum> extends AbstractToolProperty<Float, R> {
    private final float defValue;

    public FloatToolProperty(R name, float defValue) {
        super(name, Float.class);
        this.defValue = defValue;
    }
    

    @Override
    protected Float load() {
        return getPreferences().getFloat(name().name(), defValue);
    }

    @Override
    protected void save(Float t) {
        getPreferences().putFloat(name().name(), defValue);
    }
    
}
