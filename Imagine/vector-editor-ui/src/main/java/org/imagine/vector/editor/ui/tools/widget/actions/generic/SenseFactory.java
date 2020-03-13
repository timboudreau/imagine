/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.imagine.vector.editor.ui.tools.widget.actions.generic;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Predicate;
import org.openide.util.Lookup;

/**
 * Pool for things that listen on lookups.
 *
 * @author Tim Boudreau
 */
final class SenseFactory<A> {

    private final Class<A> aType;
    private final Predicate<Sense<A>> aPred;
    private final Map<Lookup, Sense<A>> senseForLookup = new WeakHashMap<>(10);

    SenseFactory(Class<A> aType) {
        this((a) -> a != null, aType);
    }

    SenseFactory(Predicate<A> aPred, Class<A> aType) {
        this(aType, (s) -> aPred.test(s.get()));
    }

    SenseFactory(Class<A> aType, Predicate<Sense<A>> aPred) {
        this.aType = aType;
        this.aPred = aPred;
    }

    SenseIt attachToLookup(Lookup lookup) {
        return new SenseItImpl(senseForLookup(lookup));
    }

    synchronized Sense<A> senseForLookup(Lookup lookup) {
        Sense<A> result = senseForLookup.get(lookup);
        if (result == null) {
            result = new Sense<>(aType, lookup);
            senseForLookup.put(lookup, result);
        }
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 61 * hash + Objects.hashCode(this.aType);
        hash = 61 * hash + Objects.hashCode(this.aPred);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SenseFactory<?> other = (SenseFactory<?>) obj;
        if (!Objects.equals(this.aType, other.aType)) {
            return false;
        }
        return Objects.equals(this.aPred, other.aPred);
    }

    class SenseItImpl implements SenseIt {

        final Sense<A> sense;

        public SenseItImpl(Sense<A> sense) {
            this.sense = sense;
        }

        @Override
        public boolean getAsBoolean() {
            return aPred.test(sense);
        }

        @Override
        public void listen(Runnable r) {
            sense.listen(r);
        }

        @Override
        public void unlisten(Runnable r) {
            sense.unlisten(r);
        }
    }

}
