package org.netbeans.paint.api.components.fractions;

import com.mastfrog.util.collections.ArrayUtils;
import com.mastfrog.util.collections.CollectionUtils;
import com.mastfrog.util.strings.Strings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Tim Boudreau
 */
public final class FractionsModel implements Iterable<Fraction> {

    final List<Fraction> fractions;
    private final List<ChangeListener> listeners = new ArrayList<>();

    public FractionsModel() {
        this(null);
    }

    public FractionsModel(float[] values) {
        values = checkAndNormalize(values);
        fractions = new ArrayList<>(values.length);
        FractionValidator minValidator = this::min;
        FractionValidator maxValidator = this::max;
        Consumer<Fraction> changes = this::onChange;
        System.out.println("NORM TO " + Arrays.toString(values));
        for (int i = 0; i < values.length; i++) {
            float f = values[i];
            Fraction fraction = new Fraction(f, minValidator, maxValidator, changes);
            fractions.add(fraction);
        }
    }

    public Fraction zero() {
        return fractions.get(0);
    }

    public Fraction one() {
        return fractions.get(fractions.size() - 1);
    }

    public Fraction get(int ix) {
        return fractions.get(ix);
    }

    @Override
    public Iterator<Fraction> iterator() {
        return CollectionUtils.unmodifiableIterator(fractions.iterator());
    }

    public boolean set(float[] values) {
        values = checkAndNormalize(values);
        float[] old = toFloatArray();
        if (!Arrays.equals(old, values)) {
            this.fractions.clear();
            FractionValidator minValidator = this::min;
            FractionValidator maxValidator = this::max;
            Consumer<Fraction> changes = this::onChange;
            for (int i = 0; i < values.length; i++) {
                float f = values[i];
                Fraction fraction = new Fraction(f, minValidator, maxValidator, changes);
                fractions.add(fraction);
            }
            fire();
            return true;
        }
        return false;
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    @Override
    public String toString() {
        return Strings.join(',', fractions);
    }

    public int size() {
        return fractions.size();
    }

    public Fraction fraction(int index) {
        return fractions.get(index);
    }

    public boolean delete(Fraction frac) {
        return delete(fractions.indexOf(frac));
    }

    public int indexOf(Fraction fraction) {
        return fractions.indexOf(fraction);
    }

    public Fraction nextEditableFraction(Fraction fraction) {
        if (size() < 3) {
            return null;
        }
        int ix = fractions.indexOf(fraction);
        if (ix < 0) {
            return get(1);
        }
        int next = ix + 1;
        if (next >= size() - 1) {
            next = 1;
        }
        return get(next);
    }

    public Fraction previousEditableFraction(Fraction fraction) {
        if (size() < 3) {
            return null;
        }
        int ix = fractions.indexOf(fraction);
        if (ix < 0) {
            return get(1);
        }
        int prev = ix - 1;
        if (prev <= 0) {
            prev = size() - 1;
        }
        return get(prev);
    }

    public Fraction nearest(float val, float tolerance) {
        int ix = nearestFractionIndex(val, tolerance);
        return ix == -1 ? null : fractions.get(ix);
    }

    public int nearestFractionIndex(float val, float tolerance) {
        if (val < 0) {
            return 0;
        } else if (val > 1) {
            return fractions.size() - 1;
        }
        float bestDistance = Float.MAX_VALUE;
        Fraction best = null;
        for (Fraction f : fractions) {
            float curr = f.getValue();
            float distance = Math.abs(curr - val);
            if (distance <= tolerance && distance < bestDistance) {
                bestDistance = distance;
                best = f;
            }
        }
        return best == null ? -1 : fractions.indexOf(best);
    }

    public boolean delete(int index) {
        if (canDelete(index)) {
            fractions.remove(index);
            fire();
            return true;
        }
        return false;
    }

    public boolean canDelete(int index) {
        if (index <= 0 || index >= fractions.size() - 1) {
            return false;
        }
        return true;
    }

    public Fraction add(float value) {
        if (!canAdd(value)) {
            return null;
        }
        Fraction nue = new Fraction(value, this::min, this::max, this::onChange);
        fractions.add(nue);
        Collections.sort(fractions);
        fire();
        return nue;
    }

    public boolean canAdd(float value) {
        return !contains(value);
    }

    public boolean contains(float value) {
        for (Fraction f : fractions) {
            float val = f.getValue();
            float diff = Math.abs(val - value);
            if (diff < 0.000001F) {
                return true;
            }
        }
        return false;
    }

    private void fire() {
        for (ChangeListener c : listeners) {
            c.stateChanged(new ChangeEvent(this));
        }
    }

    private void onChange(Fraction fraction) {
        fire();
    }

    public float[] toFloatArray() {
        float[] result = new float[fractions.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = fractions.get(i).getValue();
        }
        return checkAndNormalize(result);
    }

    private static float[] checkAndNormalize(float[] values) {
        values = values == null ? new float[]{0, 1} : Arrays.copyOf(values, values.length);
        values = ensureContainsOneAndZero(values);
        assert values.length >= 2;
        Arrays.sort(values);
        float min = Float.MAX_VALUE;
        float max = Float.MIN_VALUE;
        float last = max;
        for (int i = 0; i < values.length; i++) {
            float f = values[i];
            if (f < min) {
                min = f;
            }
            if (f > max) {
                max = f;
            }
            if (f == last) {
                float[] newVals = new float[values.length - 1];
                int cursor = 0;
                for (int j = 0; j < values.length; j++) {
                    if (j != i) {
                        newVals[cursor++] = values[j];
                    }
                }
                values = newVals;
                i--;
            }
            last = f;
        }
        scale(values, min, max);
        return values;
    }

    private static float[] scale(float[] values, float min, float max) {
        float range = max - min;
        if (range == 1F) {
            return values;
        }
        float factor = 1F / range;
        ArrayUtils.apply(values, (f) -> f * factor);
        Arrays.sort(values);
        values[0] = 0;
        values[values.length - 1] = 1;
        return values;
    }

    private static float[] ensureContainsOneAndZero(float[] values) {
        if (values == null) {
            return new float[]{0, 1};
        }
        Arrays.sort(values);
        if (values.length == 0) {
            values = new float[]{0, 1};
        } else if (values.length == 1) {
            float f = values[0];
            if (f == 0F || f == 1F) {
                values = new float[]{0, 1};
            } else {
                values = new float[]{0, f, 1};
            }
        }
        if (values[0] != 0.0F) {
            values = ArrayUtils.prepend(0F, values);
        }
        if (values[values.length - 1] != 1.0F) {
            values = ArrayUtils.append(1.0F, values);
        }
        return values;
    }

    private boolean min(Fraction fraction, float val) {
        int ix = fractions.indexOf(fraction);
        if (ix == 0) {
            return val == 0F;
        } else if (ix == fractions.size() - 1) {
            return val == 1F;
        } else if (ix > 0 && ix < fractions.size() - 1) {
            Fraction prev = fractions.get(ix - 1);
            return val > prev.getValue();
        }
        return false;
    }

    private boolean max(Fraction fraction, float val) {
        int ix = fractions.indexOf(fraction);
        if (ix == 0) {
            return val == 0F;
        } else if (ix == fractions.size() - 1) {
            return val == 1F;
        } else if (ix > 0 && ix < fractions.size() - 1) {
            Fraction next = fractions.get(ix + 1);
            return val < next.getValue();
        }
        return false;
    }

}
