package timely.store;

import timely.TimelyConfiguration;

public class MetaCacheFactory {

    private static MetaCache cache = null;

    public static MetaCache getCache() {
        if (null != cache) {
            return cache;
        } else {
            throw new RuntimeException("MetaCache not initialized.");
        }
    }

    public static final synchronized MetaCache getCache(TimelyConfiguration conf) {
        if (null == cache || cache.isClosed()) {
            if (null == conf) {
                throw new RuntimeException("Configuration cannot be null");
            }
            cache = new MetaCacheImpl(conf);
        }
        return cache;
    }

    public static final synchronized void close() {
        if (null != cache) {
            cache.close();
        }
        cache = null;
    }

}
