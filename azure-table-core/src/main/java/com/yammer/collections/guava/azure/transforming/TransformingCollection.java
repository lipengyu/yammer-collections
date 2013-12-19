package com.yammer.collections.guava.azure.transforming;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

public class TransformingCollection<F, T> extends AbstractCollection<F> {
    private final Collection<T> backingCollection;
    private final Function<F, T> toFunction;
    private final Function<T, F> fromFunction;

    /**
     * This implementation will break if the following is not satisfied:
     * <p/>
     * - for every element F f, fromFunction(toFunction(f)) = f
     * - for every element T f, toFunction(FromFunction(t)) = t
     * <p/>
     * i.e., fromFunction is a bijection and the toFunction is its reverse
     */
    public TransformingCollection(Collection<T> backingCollection, Function<F, T> toFunction, Function<T, F> fromFunction) {
        this.backingCollection = backingCollection;
        this.toFunction = toFunction;
        this.fromFunction = fromFunction;
    }

    @Override
    public int size() {
        return backingCollection.size();
    }

    @Override
    public boolean isEmpty() {
        return backingCollection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        try {
            return backingCollection.contains(toFunction.apply((F) o));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public Iterator<F> iterator() {
        return Iterators.transform(backingCollection.iterator(), fromFunction);
    }

    @Override
    public boolean add(F f) {
        return backingCollection.add(toFunction.apply(f));
    }

    @Override
    public boolean remove(Object o) {
        try {
            return backingCollection.remove(toFunction.apply((F) o));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @Override
    public void clear() {
        backingCollection.clear();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder(43, 17);
        for (F f : this) {
            builder.append(f);
        }
        return builder.build();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }
        if (o == this) {
            return true;
        }
        if (!(o instanceof Collection)) {
            return false;
        }
        Collection s = (Collection) o;

        Iterator<?> i = s.iterator();
        for (F f : this) {
            if (!i.hasNext() || !f.equals(i.next())) {
                return false;
            }
        }

        return !i.hasNext();
    }

}
