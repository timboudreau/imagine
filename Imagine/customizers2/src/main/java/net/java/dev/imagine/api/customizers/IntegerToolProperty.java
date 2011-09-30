package net.java.dev.imagine.api.customizers;

/**
 *
 * @author Tim Boudreau
 */
final class IntegerToolProperty<R extends Enum> extends AbstractToolProperty<Integer, R> {
    private final int defaultValue;

    public IntegerToolProperty(R name, int defaultValue) {
        super(name, Integer.class);
        this.defaultValue = defaultValue;
    }
   

    @Override
    protected Integer load() {
        return getPreferences().getInt(name().name(), defaultValue);
    }

    @Override
    protected void save(Integer t) {
        getPreferences().putInt(name().name(), t == null ? defaultValue : t.intValue());
    }
}
