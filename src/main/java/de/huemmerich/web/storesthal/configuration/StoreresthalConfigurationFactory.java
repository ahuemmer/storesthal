package de.huemmerich.web.storesthal.configuration;

import de.huemmerich.web.storesthal.Cacheable;
import de.huemmerich.web.storesthal.HALRelation;

/**
 * Factory for {@link StoresthalConfiguration}s. These are not to be modified after creation, therefore this factory
 * will initialize an instance once and return in then. (See description at {@link StoresthalConfiguration}).
 */
public class StoreresthalConfigurationFactory {

    /**
     * The default size of an object cache, if {@link Cacheable#cacheSize()} is not given.
     */
    private int defaultCacheSize= StoresthalConfiguration.DEFAULT_DEFAULT_CACHE_SIZE;

    /**
     * Controls whether caching is disabled.
     * See {@link #setDisableCaching(boolean)} for details.
     */
    private boolean disableCaching= StoresthalConfiguration.DEFAULT_CACHING_DISABLED;

    /**
     * Controls whether the object store works without annotations.
     * It'll try to find relation "target" setters by their name only then.
     */
    private boolean annotationless= StoresthalConfiguration.DEFAULT_ANNOTATIONLESS;

    public static final StoresthalConfiguration DEFAULT_CONFIGURATION=getDefaultConfiguration();

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
     * @return This StoresthalConfiguration factory (fluent interface)
     */
    public StoreresthalConfigurationFactory setDefaultCacheSize(int defaultCacheSize) {
        this.defaultCacheSize = defaultCacheSize;
        return this;
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
     * @param disableCaching Whether to completely disable caching or not (default: false)     *
     * @return This StoresthalConfiguration factory (fluent interface)
     */
    public StoreresthalConfigurationFactory setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
        return this;
    }

    /**
     * Return whether annotations (esp. {@link HALRelation}) shall be taken into account when searching for setters.
     * @return "true", if annotations they will be not be taken into account
     */
    public boolean isAnnotationless() {
        return annotationless;
    }

    /**
     * Controls whether annotations (esp. {@link HALRelation}) shall be taken into account when searching for setters.
     * ("true" means, the will be not be taken into account!)
     * @param annotationless Whether to work annotationless or not
     * @return This StoresthalConfiguration factory (fluent interface)
     */
    public StoreresthalConfigurationFactory setAnnotationless(boolean annotationless) {
        this.annotationless = annotationless;
        return this;
    }

    /**
     * Returns a customized {@link StoresthalConfiguration} using the parameters applied by the setters.
     * @return Customized configuration instance
     */
    public StoresthalConfiguration getConfiguration() {
        StoresthalConfiguration result = new StoresthalConfiguration();
        result.setAnnotationless(this.annotationless);
        result.setDefaultCacheSize(this.defaultCacheSize);
        result.setDisableCaching(this.disableCaching);
        return result;
    }

    /**
     * Returns a {@link StoresthalConfiguration} initialized with the default values (see public static vars
     * of {@link StoresthalConfiguration}.
     * @return Default configuration instance
     */
    public static StoresthalConfiguration getDefaultConfiguration() {
        StoresthalConfiguration result = new StoresthalConfiguration();
        result.setAnnotationless(StoresthalConfiguration.DEFAULT_ANNOTATIONLESS);
        result.setDefaultCacheSize(StoresthalConfiguration.DEFAULT_DEFAULT_CACHE_SIZE);
        result.setDisableCaching(StoresthalConfiguration.DEFAULT_CACHING_DISABLED);
        return result;
    }

}
