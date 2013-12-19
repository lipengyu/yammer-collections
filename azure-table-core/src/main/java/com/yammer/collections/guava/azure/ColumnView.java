package com.yammer.collections.guava.azure;


import com.google.common.base.Function;
import com.microsoft.windowsazure.services.table.client.TableQuery;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.yammer.collections.guava.azure.StringEntityUtil.EXTRACT_VALUE;
import static com.yammer.collections.guava.azure.StringEntityUtil.decode;
import static com.yammer.collections.guava.azure.StringEntityUtil.encode;

// TODO no timers here as of yet
class ColumnView implements Map<String, String> {
    // TODO these are probably extractable
    private static final Function<StringEntity, String> EXTRACT_COLUMN_KEY = new Function<StringEntity, String>() {
        @Override
        public String apply(StringEntity input) {
            return decode(input.getRowKey());
        }
    };
    private final Function<StringEntity, Entry<String, String>> extractEntry;
    private final BaseAzureTable baseAzureTable;
    private final String rowKey;
    private final StringTableCloudClient stringTableCloudClient;
    private final StringTableRequestFactory stringTableRequestFactory;

    public ColumnView(final BaseAzureTable baseAzureTable,
                      final String rowKey,
                      StringTableCloudClient stringTableCloudClient,
                      StringTableRequestFactory stringTableRequestFactory) {
        this.baseAzureTable = baseAzureTable;
        this.rowKey = rowKey;
        this.stringTableCloudClient = stringTableCloudClient;
        this.stringTableRequestFactory = stringTableRequestFactory;
        extractEntry = new Function<StringEntity, Entry<String, String>>() {
            @Override
            public Entry<String, String> apply(StringEntity input) {
                return new ColumnMapEntry(rowKey, decode(input.getRowKey()), baseAzureTable);
            }
        };
    }

    @Override // TODO this requires a javadoc to explain that this is a very expensive operation
    public int size() {
        return entrySet().size();
    }

    @Override
    public boolean isEmpty() {
        return entrySet().isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return baseAzureTable.contains(rowKey, key);
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof String)) {
            return false;
        }

        TableQuery<StringEntity> valueQuery = stringTableRequestFactory.containsValueForRowQuery(baseAzureTable.getTableName(), encode(rowKey),
                encode((String) value));
        return stringTableCloudClient.execute(valueQuery).iterator().hasNext();
    }

    @Override
    public String get(Object key) {
        return baseAzureTable.get(rowKey, key);
    }

    @Override
    public String put(String key, String value) {
        return baseAzureTable.put(rowKey, key, value);
    }

    @Override
    public String remove(Object key) {
        return baseAzureTable.remove(rowKey, key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> m) {
        for (Entry<? extends String, ? extends String> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {// TODO this requires a javadoc to explain that this is a very expensive operation
        for (String columnKey : keySet()) {
            remove(columnKey);
        }
    }

    @Override
    public Set<String> keySet() {
        // TODO this should have a view that is mutable (makes sense)
        return
                SetView.fromSetCollectionView(
                        new ColumnMapSetView<>(baseAzureTable, rowKey, EXTRACT_COLUMN_KEY, stringTableCloudClient, stringTableRequestFactory)
                );
    }

    @Override
    public Collection<String> values() {
        return new ColumnMapSetView<>(baseAzureTable, rowKey, EXTRACT_VALUE, stringTableCloudClient, stringTableRequestFactory);
    }

    @Override
    public Set<Entry<String, String>> entrySet() { // TODO this should have a view that is mutable (makes sense)
        return SetView.fromSetCollectionView(
                new ColumnMapSetView<>(baseAzureTable, rowKey, extractEntry, stringTableCloudClient, stringTableRequestFactory)
        );
    }

    private static class ColumnMapEntry implements Entry<String, String> {
        private final String columnKey;
        private final String rowKey;
        private final BaseAzureTable azureTable;

        private ColumnMapEntry(String rowKey, String columnKey, BaseAzureTable azureTable) {
            this.rowKey = rowKey;
            this.columnKey = columnKey;
            this.azureTable = azureTable;
        }

        @Override
        public String getKey() {
            return columnKey;
        }

        @Override
        public String getValue() {
            return azureTable.get(rowKey, columnKey);
        }

        @Override
        public String setValue(String value) {
            return azureTable.put(rowKey, columnKey, value);
        }
    }

    private static class ColumnMapSetView<E> extends CollectionView<E> {
        private final BaseAzureTable baseAzureTable;
        private final String rowKey;
        private final StringTableCloudClient stringTableCloudClient;
        private final StringTableRequestFactory stringTableRequestFactory;

        public ColumnMapSetView(
                BaseAzureTable baseAzureTable,
                String rowKey,
                Function<StringEntity, E> typeExtractor,
                StringTableCloudClient stringTableCloudClient,
                StringTableRequestFactory stringTableRequestFactory) {
            super(typeExtractor);
            this.baseAzureTable = baseAzureTable;
            this.rowKey = rowKey;
            this.stringTableCloudClient = stringTableCloudClient;
            this.stringTableRequestFactory = stringTableRequestFactory;
        }

        @Override
        protected Iterable<StringEntity> getBackingIterable() {
            TableQuery<StringEntity> selectAllForRowQuery = stringTableRequestFactory.selectAllForRow(baseAzureTable.getTableName(), encode(rowKey));
            return stringTableCloudClient.execute(selectAllForRowQuery);
        }
    }
}
