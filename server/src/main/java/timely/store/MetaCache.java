package timely.store;

import java.util.Collection;
import java.util.Iterator;

import timely.api.model.Meta;

public interface MetaCache extends Iterable<Meta> {

    void add(Meta meta);

    boolean contains(Meta meta);

    void addAll(Collection<Meta> c);

    Iterator<Meta> iterator();

    void close();

    boolean isClosed();

}
