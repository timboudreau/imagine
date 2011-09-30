package net.java.dev.imagine.api.customizers;

/**
 *
 * @author Tim Boudreau
 */
final class DoubleToolProperty<R extends Enum> extends AbstractToolProperty<Double, R> {
    private final double defValue;

    public DoubleToolProperty(R name, double defValue) {
        super(name, Double.class);
        this.defValue = defValue;
    }
    

    @Override
    protected Double load() {
        return getPreferences().getDouble(name().name(), defValue);
    }

    @Override
    protected void save(Double t) {
        System.out.println("Save " + name() + " as "+ t);
        getPreferences().putDouble(name().name(), defValue);
    }
    
}
