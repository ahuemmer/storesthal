package com.github.ahuemmer.storesthal.configuration;

import com.github.ahuemmer.storesthal.Cacheable;
import com.github.ahuemmer.storesthal.HALRelation;
import com.github.ahuemmer.storesthal.Storesthal;

/**
 * This class holds the configuration of the runtime behavior of the Storesthal.
 * The configuration itself is not to be changed at runtime (meaning: after calling {@link Storesthal#init(StoresthalConfiguration)}),
 * as this might lead to unexpected results. Therefore all setters are package-private and new configuration instances
 * are to be created using {@link StoreresthalConfigurationFactory}.
 */
public class StoresthalConfiguration {

    /**
     * Empty constructor - package private in order to be accessible only by {@link StoreresthalConfigurationFactory}, for the reasons mentioned above.
     */
    StoresthalConfiguration(){}

    /**
     * The default for the default of the size of an object cache.
     */
    public static final int DEFAULT_DEFAULT_CACHE_SIZE=1000;

    /**
     * The default setting (true or false) for annotationless mode.
     */
    public static final boolean DEFAULT_ANNOTATIONLESS=false;


    /**
     *  Indicates whether caching is disabled by default
     */
    public static final boolean DEFAULT_CACHING_DISABLED=false;

    /**
     * The default size of an object cache, if {@link Cacheable#cacheSize()} is not given.
     */
    private int defaultCacheSize=DEFAULT_DEFAULT_CACHE_SIZE;

    /**
     * Controls whether caching is disabled.
     * See {@link #setDisableCaching(boolean)} for details.
     */
    private boolean disableCaching=DEFAULT_CACHING_DISABLED;

    /**
     * Controls whether the object store works without annotations.
     * It'll try to find relation "target" setters by their name only then.
     */
    private boolean annotationless=DEFAULT_ANNOTATIONLESS;

    /**
     * Get the default size of an object cache.
     * @return Default cache size
     */
    public int getDefaultCacheSize() {
        return defaultCacheSize;
    }

    /**
     * Sets the default size of an object cache. This applies to every object class that has a {@link Cacheable}
     * annotation, but no explicit {@link Cacheable#cacheSize()} setting.
     * @param defaultCacheSize The default cache size (default: 1000)
     */
    void setDefaultCacheSize(int defaultCacheSize) {
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
    void setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
    }

    /**
     * Return whether annotations (esp. {@link HALRelation}) shall be taken into account when searching for setters.
     * @return "true", if annotations will not be taken into account
     */
    public boolean isAnnotationless() {
        return annotationless;
    }

    /**
     * Controls whether annotations (esp. {@link HALRelation}) shall be taken into account when searching for setters.
     * ("true" means, the will be not be taken into account!)
     */
    void setAnnotationless(boolean annotationless) {
        this.annotationless = annotationless;
    }

}
