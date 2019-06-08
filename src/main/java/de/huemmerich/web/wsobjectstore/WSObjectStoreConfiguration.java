package de.huemmerich.web.wsobjectstore;

public class WSObjectStoreConfiguration {

    public WSObjectStoreConfiguration(){}

    /**
     * The default size of an object cache, if {@link Cacheable#cacheSize()} is not given.
     */
    private int defaultCacheSize=1000;

    /**
     * Controls whether caching is disabled.
     * See {@link #setDisableCaching(boolean)} for details.
     */
    private boolean disableCaching=false;

    /**
     * Get the default size of an object cache.
     * @return Default cache size
     */
    public int getDefaultCacheSize() {
        return defaultCacheSize;
    }

    /**
     * Sets the default size of an object cache. This applies to every object class that has a {@link Cacheable}
     * annotation, but no explizit {@link Cacheable#cacheSize()} setting.
     * @param defaultCacheSize The default cache size (default: 1000)
     */
    public void setDefaultCacheSize(int defaultCacheSize) {
        this.defaultCacheSize = defaultCacheSize;
    }

    /**
     * Return whether caching is disabled.
     * See {@link #setDisableCaching(boolean)} for details.
     * @return true, if caching is disabled
     */
    public boolean isCachingDisabled() {
        return disableCaching;
    }

    /**
     * Controls whether caching is disabled. @Cacheable annotations will not be considered any more.
     * The only cache that will still exist is the "intermediate cache" of the store, which is necessary to maintain
     * object structure integrity during one single getObject call. The intermediate cache will not be preserved
     * between consecutive getObject calls.
     * @param disableCaching Whether to completely disable caching or not (default: false)
     */
    public void setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
    }
}
