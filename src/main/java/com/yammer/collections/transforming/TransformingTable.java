/**
 * Copyright (c) Microsoft Corporation
 * All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * THIS CODE IS PROVIDED *AS IS* BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, EITHER
 * EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION ANY IMPLIED WARRANTIES OR CONDITIONS
 * OF TITLE, FITNESS FOR A PARTICULAR PURPOSE, MERCHANTABLITY OR NON-INFRINGEMENT.
 *
 * See the Apache Version 2.0 License for specific language governing permissions and limitations under
 * the License.
 */
package com.yammer.collections.transforming;

import com.google.common.base.Function;
import com.google.common.collect.Maps;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.yammer.collections.transforming.TransformationUtil.safeTransform;

@SuppressWarnings({"ClassWithTooManyFields", "ClassWithTooManyMethods"})
public class TransformingTable<R, C, V, R1, C1, V1> implements Table<R, C, V> {
    private final Function<Cell<R, C, V>, Cell<R1, C1, V1>> toBackingCellFunction = new Function<Cell<R, C, V>, Cell<R1, C1, V1>>() {
        @Override
        public Cell<R1, C1, V1> apply(Cell<R, C, V> input) {
            return Tables.immutableCell(
                    toRowFunction.apply(input.getRowKey()),
                    toColumnFunction.apply(input.getColumnKey()),
                    toValueFunction.apply(input.getValue())
            );
        }
    };
    private final Function<Cell<R1, C1, V1>, Cell<R, C, V>> fromBackingCellFunction =
            new Function<Cell<R1, C1, V1>, Cell<R, C, V>>() {
                @Override
                public Cell<R, C, V> apply(Cell<R1, C1, V1> input) {
                    return Tables.immutableCell(
                            fromRowFunction.apply(input.getRowKey()),
                            fromColumnFunction.apply(input.getColumnKey()),
                            fromValueFunction.apply(input.getValue())
                    );
                }
            };
    private final Function<C, C1> toColumnFunction;
    private final Function<C1, C> fromColumnFunction;
    private final Function<R, R1> toRowFunction;
    private final Function<R1, R> fromRowFunction;
    private final Function<V, V1> toValueFunction;
    private final Function<V1, V> fromValueFunction;
    private final Table<R1, C1, V1> backingTable;
    private final Function<Map<C, V>, Map<C1, V1>> toRowMapValueFunction;
    private final Function<Map<C1, V1>, Map<C, V>> fromRowMapValueFunction;
    private final Function<Map<R, V>, Map<R1, V1>> toColumnMapValueFunction;
    private final Function<Map<R1, V1>, Map<R, V>> fromColumnMapValueFunction;

    private TransformingTable(
            Table<R1, C1, V1> backingTable,
            Function<R, R1> toRowFunction,
            Function<R1, R> fromRowFunction,
            Function<C, C1> toColumnFunction,
            Function<C1, C> fromColumnFunction,
            Function<V, V1> toValueFunction,
            Function<V1, V> fromValueFunction) {
        this.backingTable = checkNotNull(backingTable);
        this.toRowFunction = checkNotNull(toRowFunction);
        this.fromRowFunction = checkNotNull(fromRowFunction);
        this.toColumnFunction = checkNotNull(toColumnFunction);
        this.fromColumnFunction = checkNotNull(fromColumnFunction);
        this.toValueFunction = checkNotNull(toValueFunction);
        this.fromValueFunction = checkNotNull(fromValueFunction);
        toRowMapValueFunction = createToMapTransformation(
                toColumnFunction,
                toValueFunction
        );
        fromRowMapValueFunction = createFromMapTransformation(
                toColumnFunction, fromColumnFunction,
                toValueFunction, fromValueFunction
        );
        toColumnMapValueFunction = createToMapTransformation(
                toRowFunction,
                toValueFunction
        );
        fromColumnMapValueFunction = createFromMapTransformation(
                toRowFunction, fromRowFunction,
                toValueFunction, fromValueFunction
        );
    }

    public static <R, C, V, R1, C1, V1> Table<R, C, V> create(
            Table<R1, C1, V1> backingTable,
            Function<R, R1> toRowFunction,
            Function<R1, R> fromRowFunction,
            Function<C, C1> toColumnFunction,
            Function<C1, C> fromColumnFunction,
            Function<V, V1> toValueFunction,
            Function<V1, V> fromValueFunction) {
        return new TransformingTable<R, C, V, R1, C1, V1>(
                backingTable,
                toRowFunction, fromRowFunction,
                toColumnFunction, fromColumnFunction,
                toValueFunction, fromValueFunction
        );
    }

    private static <K, V, K1, V1> Function<Map<K, V>, Map<K1, V1>> createFromMapTransformation(
            final Function<K1, K> toKeyFunction,
            final Function<K, K1> fromKeyFunction,
            final Function<V1, V> toValueFunction,
            final Function<V, V1> fromValueFunction) {
        return new Function<Map<K, V>, Map<K1, V1>>() {
            @Override
            public Map<K1, V1> apply(Map<K, V> cvMap) {
                return TransformingMap.create(
                        cvMap,
                        toKeyFunction, fromKeyFunction,
                        toValueFunction, fromValueFunction
                );
            }
        };
    }

    // to be consistent with the behaviour, we need to copy and transform
    private static <K, V, K1, V1> Function<Map<K, V>, Map<K1, V1>> createToMapTransformation(
            final Function<K, K1> toKeyFunction,
            final Function<V, V1> toValueFunction) {
        return new Function<Map<K, V>, Map<K1, V1>>() {
            @Override
            public Map<K1, V1> apply(Map<K, V> cvMap) {
                Map<K1, V1> transformedCopy = Maps.newHashMap();
                for (Map.Entry<K, V> entry : cvMap.entrySet()) {
                    transformedCopy.put(
                            toKeyFunction.apply(entry.getKey()),
                            toValueFunction.apply(entry.getValue())
                    );
                }
                return transformedCopy;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static <F, T> T tryTransforming(Object o, Function<F, T> transfromingFunction) {
        try {
            return transfromingFunction.apply((F) o);
        } catch (ClassCastException ignored) {
            return null;
        }
    }

    @Override
    public boolean contains(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) {
            return false;
        }
        R1 mRowKey = tryTransforming(rowKey, toRowFunction);
        C1 mColumnKey = tryTransforming(columnKey, toColumnFunction);
        return mRowKey != null &&
                mColumnKey != null &&
                backingTable.contains(mRowKey, mColumnKey);
    }

    @Override
    public boolean containsRow(Object rowKey) {
        if(rowKey == null) {
            return false;
        }
        R1 mRowKey = tryTransforming(rowKey, toRowFunction);
        return mRowKey != null && backingTable.containsRow(mRowKey);
    }

    @Override
    public boolean containsColumn(Object columnKey) {
        if(columnKey == null) {
            return false;
        }
        C1 mColumnKey = tryTransforming(columnKey, toColumnFunction);
        return mColumnKey != null && backingTable.containsColumn(mColumnKey);
    }

    @Override
    public boolean containsValue(Object value) {
        if(value == null) {
            return false;
        }
        V1 mValue = tryTransforming(value, toValueFunction);
        return mValue != null && backingTable.containsValue(mValue);
    }

    @Override
    public V get(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) {
            return null;
        }

        R1 mRowKey = tryTransforming(rowKey, toRowFunction);
        C1 mColumnKey = tryTransforming(columnKey, toColumnFunction);

        if (mRowKey == null || mColumnKey == null) {
            return null;
        }

        return safeTransform(backingTable.get(mRowKey, mColumnKey), fromValueFunction);
    }

    @Override
    public boolean isEmpty() {
        return backingTable.isEmpty();
    }

    @Override
    public int size() {
        return backingTable.size();
    }

    @Override
    public void clear() {
        backingTable.clear();
    }

    @Override
    public V put(R rowKey, C columnKey, V value) {
        checkNotNull(rowKey);
        checkNotNull(columnKey);
        checkNotNull(value);

        return safeTransform(
                backingTable.put(
                        toRowFunction.apply(rowKey),
                        toColumnFunction.apply(columnKey),
                        toValueFunction.apply(value)),
                fromValueFunction);
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
        checkNotNull(table);
        for (Cell<? extends R, ? extends C, ? extends V> cell : table.cellSet()) {
            put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }
    }

    @Override
    public V remove(Object rowKey, Object columnKey) {
        if (rowKey == null || columnKey == null) {
            return null;
        }

        R1 mRowKey = tryTransforming(rowKey, toRowFunction);
        C1 mColumnKey = tryTransforming(columnKey, toColumnFunction);

        if (mRowKey == null || mColumnKey == null) {
            return null;
        }

        return safeTransform(backingTable.remove(mRowKey, mColumnKey), fromValueFunction);
    }

    @Override
    public Map<C, V> row(R rowKey) {
        checkNotNull(rowKey);
        return TransformingMap.create(
                backingTable.row(toRowFunction.apply(rowKey)),
                toColumnFunction, fromColumnFunction,
                toValueFunction, fromValueFunction
        );
    }

    @Override
    public Map<R, V> column(C columnKey) {
        checkNotNull(columnKey);
        return TransformingMap.create(
                backingTable.column(toColumnFunction.apply(columnKey)),
                toRowFunction, fromRowFunction,
                toValueFunction, fromValueFunction
        );
    }

    @Override
    public Set<Cell<R, C, V>> cellSet() {
        return TransformingSet.create(
                backingTable.cellSet(),
                toBackingCellFunction,
                fromBackingCellFunction
        );
    }

    @Override
    public Set<R> rowKeySet() {
        return TransformingSet.create(
                backingTable.rowKeySet(), toRowFunction, fromRowFunction
        );
    }

    @Override
    public Set<C> columnKeySet() {
        return TransformingSet.create(
                backingTable.columnKeySet(), toColumnFunction, fromColumnFunction
        );
    }

    @Override
    public Collection<V> values() {
        return TransformingCollection.create(
                backingTable.values(), toValueFunction, fromValueFunction
        );
    }

    @Override
    public Map<R, Map<C, V>> rowMap() {
        return TransformingMap.create(
                backingTable.rowMap(),
                toRowFunction,
                fromRowFunction,
                toRowMapValueFunction,
                fromRowMapValueFunction
        );
    }

    @Override
    public Map<C, Map<R, V>> columnMap() {
        return TransformingMap.create(
                backingTable.columnMap(),
                toColumnFunction,
                fromColumnFunction,
                toColumnMapValueFunction,
                fromColumnMapValueFunction
        );
    }
}
