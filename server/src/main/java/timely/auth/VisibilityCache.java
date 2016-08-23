package timely.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.apache.accumulo.core.security.ColumnVisibility;
import timely.Configuration;
import timely.TimelyConfiguration;

import java.util.concurrent.TimeUnit;

public class VisibilityCache {

    private static volatile Cache<String, ColumnVisibility> CACHE = null;

    /** Used for testing */
    public static synchronized void init() {
        if (CACHE == null) {
            long defaultExpiration = Configuration.VISIBILITY_EXPIRATION_DEFAULT;
            int initialCapacity = Configuration.VISIBILITY_CACHE_INITIAL_CAPACITY_DEFAULT;
            long maxCapacity = Configuration.VISIBILITY_CACHE_MAX_CAPACITY_DEFAULT;
            CACHE = Caffeine.newBuilder().expireAfterAccess(defaultExpiration, TimeUnit.MINUTES)
                    .initialCapacity(initialCapacity).maximumSize(maxCapacity).build();
        }
    }

    public static synchronized void init(TimelyConfiguration config) {
        if (CACHE == null) {
            long expireMinutes = config.getVisibilityCache().getExpirationMinutes();
            int initialCapacity = config.getVisibilityCache().getInitialCapacity();
            long maxCapacity = config.getVisibilityCache().getMaxCapacity();
            CACHE = Caffeine.newBuilder().expireAfterAccess(expireMinutes, TimeUnit.MINUTES)
                    .initialCapacity(initialCapacity).maximumSize(maxCapacity).build();
        }
    }

    public static ColumnVisibility getColumnVisibility(String visibilityString) {
        return CACHE.get(visibilityString, key -> generateNormalizedVisibility(key));
    }

    private static final ColumnVisibility generateNormalizedVisibility(String visibilityString) {
        // it would be nice if Accumulo would make it easier to do this.
        return new ColumnVisibility(new ColumnVisibility(visibilityString).flatten());
    }
}
