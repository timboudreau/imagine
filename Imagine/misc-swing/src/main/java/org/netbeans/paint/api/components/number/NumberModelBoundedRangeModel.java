/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.netbeans.paint.api.components.number;

import javax.swing.BoundedRangeModel;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;

/**
 *
 * @author Tim Boudreau
 */
final class NumberModelBoundedRangeModel<T extends Number> implements BoundedRangeModel {

    private final NumberModel<T> mdl;
    private final ChangeSupport supp = new ChangeSupport(this);

    NumberModelBoundedRangeModel(NumberModel<T> mdl) {
        this.mdl = mdl;
    }

    @Override
    public int getMinimum() {
        return mdl.constraints().minimum().intValue();
    }

    @Override
    public void setMinimum(int newMinimum) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMaximum() {
        return mdl.constraints().maximum().intValue();
    }

    @Override
    public void setMaximum(int newMaximum) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getValue() {
        return mdl.get().intValue();
    }

    @Override
    public void setValue(int newValue) {
        mdl.setValue(newValue);
    }

    private boolean adjusting;
    @Override
    public void setValueIsAdjusting(boolean b) {
        adjusting = true;
    }

    @Override
    public boolean getValueIsAdjusting() {
        return adjusting;
    }

    private int extent = 1;
    @Override
    public int getExtent() {
        return extent;
    }

    @Override
    public void setExtent(int newExtent) {
        if (extent != newExtent) {
            extent = newExtent;
            supp.fireChange();
        }
    }

    @Override
    public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
        if (min != getMinimum() || max != getMaximum()) {
            throw new IllegalArgumentException("Cannot set min / max/ext");
        }
        boolean changed = false;
        if (adjusting != this.adjusting) {
            this.adjusting = adjusting;
            changed = true;
        }
        if (value != mdl.get().intValue()) {
            mdl.setValue(value);
            changed = true;
        }
        if (changed) {
            supp.fireChange();
        }
    }

    @Override
    public void addChangeListener(ChangeListener x) {
        supp.addChangeListener(x);
    }

    @Override
    public void removeChangeListener(ChangeListener x) {
        supp.removeChangeListener(x);
    }

}
