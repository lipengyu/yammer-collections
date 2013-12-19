package com.yammer.collections.guava.azure;


import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.microsoft.windowsazure.services.table.client.TableQuery;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;

public class PossibleSetCollectionView<E> extends AbstractSet<E> {
    private final StringAzureTable stringAzureTable;
    private final StringTableCloudClient stringTableCloudClient;
    private final StringTableRequestFactory stringTableRequestFactory;
    private final Function<StringEntity, E> typeExtractor;

    public PossibleSetCollectionView(StringAzureTable stringAzureTable,
                                     Function<StringEntity, E> typeExtractor,
                                     StringTableCloudClient stringTableCloudClient,
                                     StringTableRequestFactory stringTableRequestFactory) {
        this.stringAzureTable = stringAzureTable;
        this.typeExtractor = typeExtractor;
        this.stringTableCloudClient = stringTableCloudClient;
        this.stringTableRequestFactory = stringTableRequestFactory;
    }

    @Override
    public int size() {
        // TODO can this be optimized through a direct query
        return Iterables.size(getBackingIterable());
    }

    @Override
    public boolean isEmpty() {
        return !iterator().hasNext();
    }

    @Override
    public boolean contains(Object o) {
        return Iterables.contains(
                Iterables.transform(getBackingIterable(), typeExtractor),
                o);
    }

    private Iterable<StringEntity> getBackingIterable() {
        TableQuery<StringEntity> query = stringTableRequestFactory.selectAll(stringAzureTable.getTableName());
        return stringTableCloudClient.execute(query);
    }

    @Override
    public Iterator<E> iterator() {
        return Iterables.transform(
                getBackingIterable(),
                typeExtractor).iterator();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean add(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!contains(o)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
