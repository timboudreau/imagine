package org.netbeans.paint.api.components.number;

import java.util.Objects;
import javax.swing.SpinnerModel;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
final class NumberModelSpinnerModel<T extends Number> implements SpinnerModel {

    private final NumberModel<T> mdl;
    private final ChangeSupport supp = new ChangeSupport(this);

    NumberModelSpinnerModel(NumberModel<T> mdl) {
        this.mdl = mdl;
    }

    @Override
    public Object getValue() {
        return mdl.get();
    }

    @Override
    public void setValue(Object value) {
        T old = mdl.get();
        if (value instanceof Number) {
            mdl.setValue((Number) value);
        } else if (value instanceof String) {
            mdl.setValue((String) value);
        }
        if (!Objects.equals(old, mdl.get())) {
            supp.fireChange();
        }
    }

    @Override
    public Object getNextValue() {
        return mdl.nextValue();
    }

    @Override
    public Object getPreviousValue() {
        return mdl.prevValue();
    }

    @Override
    public void addChangeListener(ChangeListener l) {
        supp.addChangeListener(l);
    }

    @Override
    public void removeChangeListener(ChangeListener l) {
        supp.removeChangeListener(l);
    }

}
