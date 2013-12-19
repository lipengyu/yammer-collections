package com.yammer.collections.guava.azure.transforming;


import com.google.common.base.Function;
import com.sun.istack.NotNull;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.yammer.collections.guava.azure.transforming.TransformationUtil.safeTransform;

public class TransformingMap<K, V, K1, V1> extends AbstractMap<K, V> {
    private final Map<K1, V1> backingMap;
    private final Function<K, K1> toKeyFunction;
    private final Function<K1, K> fromKeyFunction;
    private final Function<V, V1> toValueFunction;
    private final Function<V1, V> fromValueFunction;
    private final Function<Entry<K, V>, Entry<K1, V1>> toEntryFunction;
    private final Function<Entry<K1, V1>, Entry<K, V>> fromEntryFunction;

    public TransformingMap(
            Map<K1, V1> backingMap,
            final Function<K, K1> toKeyFunction,
            final Function<K1, K> fromKeyFunction,
            final Function<V, V1> toValueFunction,
            final Function<V1, V> fromValueFunction
    ) {
        this.backingMap = backingMap;
        this.toKeyFunction = toKeyFunction;
        this.fromKeyFunction = fromKeyFunction;
        this.toValueFunction = toValueFunction;
        this.fromValueFunction = fromValueFunction;
        this.toEntryFunction = new Function<Entry<K, V>, Entry<K1, V1>>() {
            @Override
            public Entry<K1, V1> apply(java.util.Map.Entry<K, V> kvEntry) {
                return new TransformingEntry<>(
                        kvEntry,
                        toKeyFunction,
                        fromValueFunction,
                        toValueFunction
                );
            }
        };
        this.fromEntryFunction = new

                Function<Entry<K1, V1>, Entry<K, V>>() {
                    @Override
                    public Entry<K, V> apply(java.util.Map.Entry<K1, V1> kvEntry) {
                        return new TransformingEntry<>(
                                kvEntry,
                                fromKeyFunction,
                                toValueFunction,
                                fromValueFunction
                        );
                    }
                };
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new TransformingSet<>(
                backingMap.entrySet(),
                toEntryFunction,
                fromEntryFunction
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsValue(Object o) {
        try {
            return backingMap.containsValue(safeTransform((V) o, toValueFunction));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsKey(Object key) {
        try {
            return backingMap.containsKey(safeTransform((K) key, toKeyFunction));
        } catch (ClassCastException e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public V get(Object key) {
        try {
            return safeTransform(
                    backingMap.get(safeTransform((K) key, toKeyFunction)),
                    fromValueFunction
            );
        } catch (ClassCastException e) {
            return null;
        }
    }

    public V put(K key, V value) {
        K1 tKey = safeTransform(key, toKeyFunction);
        V1 tValue = safeTransform(value, toValueFunction);
        return safeTransform(
                backingMap.put(tKey, tValue),
                fromValueFunction
        );
    }

    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        try {
            return safeTransform(
                    backingMap.remove(safeTransform((K) key, toKeyFunction)),
                    fromValueFunction
            );
        } catch (ClassCastException e) {
            return null;
        }
    }

    public void clear() {
        backingMap.clear();
    }

    @Override
    public Set<K> keySet() {
        return new TransformingSet<>(
                backingMap.keySet(), toKeyFunction, fromKeyFunction
        );
    }

    @Override
    public  Collection<V> values() {
        return new TransformingCollection<>(
                backingMap.values(), toValueFunction, fromValueFunction
        );
    }

    public static class TransformingEntry<K, V, K1, V1> implements Entry<K, V> {
        private final Entry<K1, V1> backingEntry;
        private final Function<K1, K> fromKeyFunction;
        private final Function<V, V1> toValueFunction;
        private final Function<V1, V> fromValueFunction;

        public TransformingEntry(Entry<K1, V1> backingEntry,
                                 Function<K1, K> fromKeyFunction,
                                 Function<V, V1> toValueFunction,
                                 Function<V1, V> fromValueFunction) {
            this.backingEntry = backingEntry;
            this.fromKeyFunction = fromKeyFunction;
            this.toValueFunction = toValueFunction;
            this.fromValueFunction = fromValueFunction;
        }

        @Override
        public K getKey() {
            return safeTransform(backingEntry.getKey(), fromKeyFunction);
        }

        @Override
        public V getValue() {
            return safeTransform(backingEntry.getValue(), fromValueFunction);
        }

        @Override
        public V setValue(V value) {
            return safeTransform(
                    backingEntry.setValue(safeTransform(value, toValueFunction)),
                    fromValueFunction
            );
        }
    }
}
