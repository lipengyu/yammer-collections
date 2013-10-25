package com.yammer.collections.guava.azure;

import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.microsoft.windowsazure.services.core.storage.StorageErrorCode;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.microsoft.windowsazure.services.table.client.CloudTableClient;
import com.microsoft.windowsazure.services.table.client.TableOperation;
import com.microsoft.windowsazure.services.table.client.TableQuery;
import com.yammer.metrics.Metrics;
import com.yammer.metrics.core.Timer;
import com.yammer.metrics.core.TimerContext;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.yammer.collections.guava.azure.StringEntityUtil.EXTRACT_VALUE;
import static com.yammer.collections.guava.azure.StringEntityUtil.decode;
import static com.yammer.collections.guava.azure.StringEntityUtil.encode;
// TODO this should be renamed to azure table

public class BaseAzureTable implements Table<String, String, String> {
    private static final Timer GET_TIMER = createTimerFor("get"); // TODO remove metrics dep
    private static final Timer PUT_TIMER = createTimerFor("put");
    private static final Timer REMOVE_TIMER = createTimerFor("remove");
    private static final Function<StringEntity, String> COLUMN_KEY_EXTRACTOR = new Function<StringEntity, String>() {
        @Override
        public String apply(StringEntity input) {
            return decode(input.getRowKey());
        }
    };
    private static final Function<StringEntity, String> ROW_KEY_EXTRACTOR = new Function<StringEntity, String>() {
        @Override
        public String apply(StringEntity input) {
            return decode(input.getPartitionKey());
        }
    };
    ;
    private final String tableName;
    private final StringTableCloudClient stringCloudTableClient;
    private final StringTableRequestFactory stringTableRequestFactory;

    /* package */ BaseAzureTable(String tableName, StringTableCloudClient stringCloudTableClient, StringTableRequestFactory stringTableRequestFactory) {
        this.tableName = tableName;
        this.stringCloudTableClient = stringCloudTableClient;
        this.stringTableRequestFactory = stringTableRequestFactory;
    }

    public BaseAzureTable(String secretieTableName, CloudTableClient tableClient) {
        this(secretieTableName, new StringTableCloudClient(tableClient), new StringTableRequestFactory());
    }

    private static Timer createTimerFor(String name) {
        return Metrics.newTimer(BaseAzureTable.class, name);
    }

    @Override
    public boolean contains(Object rowString, Object columnString) {
        return get(rowString, columnString) != null;
    }

    @Override
    public boolean containsRow(Object rowString) {
        return (rowString instanceof String) && !row((String) rowString).isEmpty();
    }

    @Override
    public boolean containsColumn(Object columnString) {
        return (columnString instanceof String) && !column((String) columnString).isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        if (!(value instanceof String)) {
            return false;
        }

        TableQuery<StringEntity> valueQuery = stringTableRequestFactory.containsValueQuery(tableName, encode((String) value));
        return stringCloudTableClient.execute(valueQuery).iterator().hasNext();
    }

    @Override
    public String get(Object rowString, Object columnString) {
        return entityToValue(rawGet(rowString, columnString));
    }

    private String entityToValue(StringEntity stringEntity) {
        return stringEntity == null ? null : decode(stringEntity.getValue());
    }

    private StringEntity rawGet(Object rowString, Object columnString) {
        if (!(rowString instanceof String && columnString instanceof String)) {
            return null;
        }

        String row = encode((String) rowString);
        String column = encode((String) columnString);

        TableOperation retrieveEntityOperation = stringTableRequestFactory.retrieve(row, column);

        try {
            return timedTableOperation(GET_TIMER, retrieveEntityOperation);
        } catch (StorageException e) {
            throw Throwables.propagate(e);
        }
    }

    private StringEntity timedTableOperation(Timer contextSpecificTimer, TableOperation tableOperation) throws StorageException {
        TimerContext context = contextSpecificTimer.time();
        try {
            return stringCloudTableClient.execute(tableName, tableOperation);
        } finally {
            context.stop();
        }
    }

    @Override
    public boolean isEmpty() {
        return cellSet().isEmpty();
    }

    @Override
    public int size() {
        return cellSet().size();
    }

    @Override
    public void clear() { // TODO do a javadoc
        for (Cell<String, String, String> cell : cellSet()) {
            remove(cell.getRowKey(), cell.getColumnKey());
        }
    }

    @Override
    public String put(String rowString, String columnString, String value) {
        TableOperation putStringieOperation = stringTableRequestFactory.put(encode(rowString), encode(columnString), encode(value));

        try {
            return entityToValue(timedTableOperation(PUT_TIMER, putStringieOperation));
        } catch (StorageException e) {
            throw Throwables.propagate(e);
        }
    }

    @Override
    public void putAll(Table<? extends String, ? extends String, ? extends String> table) {
        for (Cell<? extends String, ? extends String, ? extends String> cell : table.cellSet()) {
            put(cell.getRowKey(), cell.getColumnKey(), cell.getValue());
        }
    }

    @Override
    public String remove(Object rowString, Object columnString) {
        StringEntity entityToBeDeleted = rawGet(rowString, columnString);

        if (entityToBeDeleted == null) {
            return null;
        }

        TableOperation deleteStringieOperation = stringTableRequestFactory.delete(entityToBeDeleted);

        try {
            return entityToValue(timedTableOperation(REMOVE_TIMER, deleteStringieOperation));
        } catch (StorageException e) {
            if (notFound(e)) {
                return null;
            }
            throw Throwables.propagate(e);
        }
    }

    private boolean notFound(StorageException e) {
        return StorageErrorCode.RESOURCE_NOT_FOUND.toString().equals(e.getErrorCode())
                || "ResourceNotFound".equals(e.getErrorCode());
    }

    @Override
    public Map<String, String> row(String rowString) {
        return new ColumnView(this, rowString, stringCloudTableClient, stringTableRequestFactory);
    }

    @Override
    public Map<String, String> column(String columnString) {
        return new RowView(this, columnString, stringCloudTableClient, stringTableRequestFactory);
    }

    @Override
    public Set<Cell<String, String, String>> cellSet() {
        return new CellSetMutableView(this, stringCloudTableClient, stringTableRequestFactory);
    }

    @Override // TODO delete operations may make sense on these, decide
    public Set<String> rowKeySet() {
        return SetView.fromCollectionView(
                new TableCollectionView<>(this, ROW_KEY_EXTRACTOR, stringCloudTableClient, stringTableRequestFactory)
        );
    }

    @Override // TODO delete operations may make sense on these, decide
    public Set<String> columnKeySet() {
        return SetView.fromCollectionView(
                new TableCollectionView<>(this, COLUMN_KEY_EXTRACTOR, stringCloudTableClient, stringTableRequestFactory)
        );
    }

    @Override
    public Collection<String> values() {
        return new TableCollectionView<>(this, EXTRACT_VALUE, stringCloudTableClient, stringTableRequestFactory);
    }

    @Override
    public Map<String, Map<String, String>> rowMap() {
        return new RowMapView(this);
    }

    @Override
    public Map<String, Map<String, String>> columnMap() {
        // TODO didn't tdd this, didn't know how to spec it
        return Tables.transpose(this).rowMap();
    }

    public String getTableName() {
        return tableName;
    }

    private static final class TableCollectionView<E> extends CollectionView<E> {
        private final BaseAzureTable baseAzureTable;
        private final StringTableCloudClient stringTableCloudClient;
        private final StringTableRequestFactory stringTableRequestFactory;

        public TableCollectionView(BaseAzureTable baseAzureTable, Function<StringEntity, E> typeExtractor, StringTableCloudClient stringTableCloudClient, StringTableRequestFactory stringTableRequestFactory) {
            super(typeExtractor);
            this.baseAzureTable = baseAzureTable;
            this.stringTableCloudClient = stringTableCloudClient;
            this.stringTableRequestFactory = stringTableRequestFactory;
        }

        protected Iterable<StringEntity> getBackingIterable() {
            TableQuery<StringEntity> query = stringTableRequestFactory.selectAll(baseAzureTable.getTableName());
            return stringTableCloudClient.execute(query);
        }
    }
}