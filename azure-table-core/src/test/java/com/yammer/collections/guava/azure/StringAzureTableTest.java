package com.yammer.collections.guava.azure;

import com.google.common.collect.Lists;
import com.google.common.collect.Table;
import com.google.common.collect.Tables;
import com.microsoft.windowsazure.services.core.storage.StorageException;
import com.microsoft.windowsazure.services.core.storage.utils.Base64;
import com.microsoft.windowsazure.services.table.client.TableOperation;
import com.microsoft.windowsazure.services.table.client.TableQuery;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StringAzureTableTest {
    private static final String ROW_KEY_1 = "rown_name_1";
    private static final String ROW_KEY_2 = "row_name_2";
    private static final String COLUMN_KEY_1 = "column_key_1";
    private static final String COLUMN_KEY_2 = "column_key_2";
    private static final String NON_EXISTENT_COLUMN_KEY = "non_existent_column_key";
    private static final String VALUE_1 = "value1";
    private static final String VALUE_2 = "value3";
    private static final String TABLE_NAME = "secretie_table";
    private static final String ENCODING = "UTF-8";
    private static final Table.Cell<String, String, String> CELL_1 = Tables.immutableCell(ROW_KEY_1, COLUMN_KEY_1, VALUE_1);
    private static final Table.Cell<String, String, String> CELL_2 = Tables.immutableCell(ROW_KEY_2, COLUMN_KEY_2, VALUE_2);
    @Mock
    private StringTableCloudClient stringTableCloudClientMock;
    @Mock
    private StringTableRequestFactory stringTableRequestFactoryMock;
    private StringAzureTable stringAzureTable;

    @Before
    public void setUp() throws IOException {
        stringAzureTable = new StringAzureTable(TABLE_NAME, stringTableCloudClientMock, stringTableRequestFactoryMock);
    }

    @Test
    public void when_columnKeySet_requested_then_all_keys_returned() throws UnsupportedEncodingException, StorageException {
        //noinspection unchecked
        setAzureTableToContain(CELL_1, CELL_2);

        Set<String> columnKeySet = stringAzureTable.columnKeySet();

        assertThat(columnKeySet, containsInAnyOrder(COLUMN_KEY_1, COLUMN_KEY_2));
    }

    @Test
    public void get_of_an_existing_value_returns_result_from_azure_table_returned() throws StorageException, UnsupportedEncodingException {
        //noinspection unchecked
        setAzureTableToContain(CELL_1);

        String value = stringAzureTable.get(ROW_KEY_1, COLUMN_KEY_1);

        assertThat(value, is(equalTo(VALUE_1)));
    }

    @Test
    public void get_of_non_existing_entry_returns_null() throws UnsupportedEncodingException, StorageException {
        //noinspection unchecked
        setAzureTableToContain(CELL_1);

        String value = stringAzureTable.get(ROW_KEY_2, COLUMN_KEY_2);

        assertThat(value, is(nullValue()));
    }

    @Test(expected = RuntimeException.class)
    public void when_table_client_throws_storage_exception_during_get_then_exception_rethrown() throws StorageException, UnsupportedEncodingException {
        //noinspection unchecked
        setAzureTableToContain(CELL_1);
        setToThrowStorageExceptionOnRetrievalOf(CELL_1);

        stringAzureTable.get(ROW_KEY_1, COLUMN_KEY_1);
    }

    @Test
    public void when_put_then_value_added_or_replaced_in_azure() throws StorageException, UnsupportedEncodingException {
        TableOperation putTableOperationMock = mockPutTableOperation(CELL_2);

        stringAzureTable.put(ROW_KEY_2, COLUMN_KEY_2, VALUE_2);

        verify(stringTableCloudClientMock).execute(TABLE_NAME, putTableOperationMock);
    }

    @Test(expected = RuntimeException.class)
    public void when_table_client_throws_storage_exception_during_put_then_exception_rethrown() throws StorageException, UnsupportedEncodingException {
        TableOperation putTableOperationMock = mockPutTableOperation(CELL_1);
        setupThrowStorageExceptionOnTableOperation(putTableOperationMock);

        stringAzureTable.put(ROW_KEY_1, COLUMN_KEY_1, VALUE_1);
    }

    @Test
    public void when_delete_then_deleted_in_azure() throws StorageException, UnsupportedEncodingException {
        //noinspection unchecked
        setAzureTableToContain(CELL_1);
        TableOperation deleteTableOperationMock = mockDeleteTableOperation(CELL_1);

        stringAzureTable.remove(ROW_KEY_1, COLUMN_KEY_1);

        verify(stringTableCloudClientMock).execute(TABLE_NAME, deleteTableOperationMock);
    }

    @Test
    public void when_key_does_not_exist_then_delete_return_null() throws StorageException {
        stringAzureTable.remove(ROW_KEY_1, NON_EXISTENT_COLUMN_KEY);
    }

    @Test(expected = RuntimeException.class)
    public void when_table_client_throws_storage_exception_during_delete_then_exception_rethrown() throws StorageException, UnsupportedEncodingException {
        //noinspection unchecked
        setAzureTableToContain(CELL_1);
        TableOperation deleteTableOperationMock = mockDeleteTableOperation(CELL_1);
        setupThrowStorageExceptionOnTableOperation(deleteTableOperationMock);

        stringAzureTable.remove(ROW_KEY_1, COLUMN_KEY_1);
    }

    @Test
    public void cellSet_returns_all_table_cells() throws UnsupportedEncodingException, StorageException {
        //noinspection unchecked
        setAzureTableToContain(CELL_1, CELL_2);

        Set<Table.Cell<String, String, String>> cellSet = stringAzureTable.cellSet();

        //noinspection unchecked
        assertThat(cellSet, containsInAnyOrder(CELL_1, CELL_2));
    }

    @Test
    public void when_contains_value_for_row_and_key_then_returns_true() throws UnsupportedEncodingException, StorageException {
        setAzureTableToContain(CELL_1);

        assertThat(stringAzureTable.contains(ROW_KEY_1, COLUMN_KEY_1), is(equalTo(true)));
    }

    @Test
    public void when_does_not_contain_value_for_row_and_key_then_returns_false() throws UnsupportedEncodingException, StorageException {
        assertThat(stringAzureTable.contains(ROW_KEY_1, COLUMN_KEY_1), is(equalTo(false)));
    }

    //
    // Utility methods
    //

    private String encode(String stringToBeEncoded) throws UnsupportedEncodingException {
        return Base64.encode(stringToBeEncoded.getBytes(ENCODING));

    }

    private StringEntity encodedStringEntity(Table.Cell<String, String, String> unEncodedcell) throws UnsupportedEncodingException {
        return new StringEntity(encode(unEncodedcell.getRowKey()), encode(unEncodedcell.getColumnKey()), encode(unEncodedcell.getValue()));
    }

    private void setAzureTableToContain(Table.Cell<String, String, String>... cells) throws UnsupportedEncodingException, StorageException {
        // retrieve setup in general
        TableOperation blanketRetrieveOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(any(String.class), any(String.class))).thenReturn(blanketRetrieveOperationMock);


        // per entity setup
        Collection<StringEntity> encodedStringEntities = Lists.newArrayList();
        for (Table.Cell<String, String, String> cell : cells) {
            encodedStringEntities.add(encodedStringEntity(cell));
            setAzureTableToRetrieve(cell);
        }

        // select query
        TableQuery<StringEntity> tableQuery = mock(TableQuery.class);
        when(stringTableRequestFactoryMock.selectAll(TABLE_NAME)).thenReturn(tableQuery);
        when(stringTableCloudClientMock.execute(tableQuery)).thenReturn(encodedStringEntities);
    }

    private void setAzureTableToRetrieve(Table.Cell<String, String, String> cell) throws UnsupportedEncodingException, StorageException {
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(encode(cell.getRowKey()), encode(cell.getColumnKey()))).thenReturn(retriveTableOperationMock);
        when(stringTableCloudClientMock.execute(TABLE_NAME, retriveTableOperationMock)).thenReturn(encodedStringEntity(cell));
    }

    private void setToThrowStorageExceptionOnRetrievalOf(Table.Cell<String, String, String> cell) throws UnsupportedEncodingException, StorageException {
        TableOperation retriveTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.retrieve(encode(cell.getRowKey()), encode(cell.getColumnKey()))).thenReturn(retriveTableOperationMock);
        setupThrowStorageExceptionOnTableOperation(retriveTableOperationMock);
    }

    private TableOperation mockPutTableOperation(Table.Cell<String, String, String> cell) throws UnsupportedEncodingException {
        TableOperation putTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.put(encode(cell.getRowKey()), encode(cell.getColumnKey()), encode(cell.getValue()))).thenReturn(putTableOperationMock);
        return putTableOperationMock;
    }

    private void setupThrowStorageExceptionOnTableOperation(TableOperation tableOperationMock) throws StorageException {
        StorageException storageExceptionMock = mock(StorageException.class);
        when(stringTableCloudClientMock.execute(TABLE_NAME, tableOperationMock)).thenThrow(storageExceptionMock);
    }

    private TableOperation mockDeleteTableOperation(Table.Cell<String, String, String> cell) throws UnsupportedEncodingException, StorageException {
        TableOperation retrieveOperation = stringTableRequestFactoryMock.retrieve(encode(cell.getRowKey()), encode(cell.getColumnKey()));
        StringEntity result = stringTableCloudClientMock.execute(TABLE_NAME, retrieveOperation);
        TableOperation deleteTableOperationMock = mock(TableOperation.class);
        when(stringTableRequestFactoryMock.delete(result)).thenReturn(deleteTableOperationMock);
        return deleteTableOperationMock;
    }


}