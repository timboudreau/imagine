package net.java.dev.imagine.api.customizers;

import java.awt.BasicStroke;

/**
 *
 * @author Tim Boudreau
 */
final class StrokeToolProperty<R extends Enum> extends AbstractToolProperty<BasicStroke, R> {

    public StrokeToolProperty(R name) {
        super(name, BasicStroke.class);
    }

    @Override
    protected BasicStroke load() {
        float f = getPreferences().getFloat(name().name() + "_width", 1.0F);
        return new BasicStroke(f);
    }

    @Override
    protected void save(BasicStroke t) {
        getPreferences().putFloat(name().name() + "_width", t.getLineWidth());
    }
    
}
