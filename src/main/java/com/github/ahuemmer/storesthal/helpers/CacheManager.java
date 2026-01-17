package com.github.ahuemmer.storesthal.helpers;

import com.github.ahuemmer.storesthal.Cacheable;
import com.github.ahuemmer.storesthal.LRUCache;
import com.github.ahuemmer.storesthal.Storesthal;
import com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.hateoas.EntityModel;

import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class CacheManager {

    /**
     * The key for the cache hits for the number of cache hits for objects retrieved with their links in the statistics map.
     */
    public static final String STATISTICS_ENTRY_CACHE_HITS_WITH_LINKS = "cacheHitsWithLinks";
    /**
     * The key for the cache hits for the number of cache hits for objects retrieved without their links in the statistics map.
     */
    public static final String STATISTICS_ENTRY_CACHE_HITS_WITHOUT_LINKS = "cacheHitsWithoutLinks";
    /**
     * The key for the cache hits for the number of cache misses for objects retrieved with their links in the statistics map.
     */
    public static final String STATISTICS_ENTRY_CACHE_MISSES_WITH_LINKS = "cacheMissesWithLinks";
    /**
     * The key for the cache hits for the number of cache misses for objects retrieved without their links in the statistics map.
     */
    public static final String STATISTICS_ENTRY_CACHE_MISSES_WITHOUT_LINKS = "cacheMissesWithoutLinks";

    /**
     * A map containing the number of cache for caches containing objects retrieved with their links misses by cache (name) for statistics creation.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()}.
     */
    private static final Map<String, Integer> cacheMissesWithLinks = new HashMap<>();

    /**
     * A map containing the number of cache for caches containing objects retrieved without their links misses by cache (name) for statistics creation.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()}.
     */
    private static final Map<String, Integer> cacheMissesWithoutLinks = new HashMap<>();

    /**
     * A map containing the number of cache hits by cache (name) for statistics creation. Only cache his for objects retrieved with their links are considered here.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()}.
     */
    private static final Map<String, Integer> cacheHitsWithLinks = new HashMap<>();

    /**
     * A map containing the number of cache hits by cache (name) for statistics creation. Only cache his for objects retrieved without their links are considered here.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()}.
     */
    private static final Map<String, Integer> cacheHitsWithoutLinks = new HashMap<>();

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);

    /**
     * The only (singleton) instance of this class, accessible via {@link #getInstance(com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration)}
     */
    private static CacheManager instance;

    /**
     * The {@link com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration} this CacheManager was instantiated with.
     */
    private static StoresthalConfiguration configuration;

    /**
     * All configured object caches for objects retrieved with their links are stored in this map, the key is the cache name (see {@link LRUCache#getCacheName()}
     * and {@link Cacheable#cacheName()}).
     */
    private static Map<String, LRUCache<URI, Object>> cachesWithLinks;

    /**
     * All configured object caches for objects retrieved without their links are stored in this map, the key is the cache name (see {@link LRUCache#getCacheName()}
     * and {@link Cacheable#cacheName()}).
     */
    private static Map<String, LRUCache<URI, Object>> cachesWithoutLinks;

    /**
     * Private constructor in order to allow singleton pattern.
     */
    private CacheManager() {
    }

    /**
     * Get the only instance (singleton) of the CacheManager, initialized with the given configuration.
     *
     * @param configuration The configuration applying to the CacheManager singleton instance.
     * @return The CacheManager singleton instance.
     */
    @SuppressWarnings("InstantiationOfUtilityClass")
    public static CacheManager getInstance(StoresthalConfiguration configuration) {
        if (instance == null) {
            instance = new CacheManager();
            CacheManager.configuration = configuration;
            cachesWithLinks = new HashMap<>();
            cachesWithoutLinks = new HashMap<>();
            clearAllCaches(true);
        }
        return instance;
    }

    /**
     * Try to retrieve an object from the associated cache (or the common cache, if the {@link com.github.ahuemmer.storesthal.Cacheable} annotation does
     * not state an explicit cache name).
     * <p>
     * Only the cache for objects with links is considered here, as retrieval with links is the default. To see cache
     * hits for objects retrieved without their links, you can use {@link #getObjectFromCache(java.net.URI, Class, String, boolean, boolean)}.
     *
     * @param uri         The object's URI
     * @param objectClass The class of the object
     * @param cacheName   The name of the cache to get the object from. Use NULL here for automatic cache name detection
     *                    (default).
     * @param collection  Whether the cache for a collection of objects is to be searched.
     * @param <T>         The class of the object to retrieve.
     * @return The cached object instance or NULL, if the cache didn't contain an object for the given URI.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromCache(URI uri, Class objectClass, String cacheName, boolean collection) {
        return getObjectFromCache(uri, objectClass, cacheName, collection, true);

    }

    /**
     * Try to retrieve an object from the associated cache (or the common cache, if the {@link com.github.ahuemmer.storesthal.Cacheable} annotation does
     * not state an explicit cache name).
     *
     * @param uri         The object's URI
     * @param objectClass The class of the object
     * @param cacheName   The name of the cache to get the object from. Use NULL here for automatic cache name detection
     *                    (default).
     * @param collection  Whether the cache for a collection of objects is to be searched.
     * @param withLinks   Whether the cache for objects (/collections) retrieved with or without their links is to be searched.
     * @param <T>         The class of the object to retrieve.
     * @return The cached object instance or NULL, if the cache didn't contain an object for the given URI.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getObjectFromCache(URI uri, Class objectClass, String cacheName, boolean collection, boolean withLinks) {

        logger.debug("Trying to get object with URI {} from cache...", uri);

        LRUCache<URI, Object> cache;
        if (cacheName == null) {
            cache = getCache(objectClass, collection, withLinks);
        } else {
            cache = getCache(cacheName, null, withLinks);
        }

        if (configuration.isCachingDisabled() && !(cache.getCacheName().equals(StoresthalConfiguration.INTERMEDIATE_CACHE_NAME))) {
            logger.debug("Caching is disabled!");
            return null;
        }

        T result = (T) cache.get(uri);

        Map<String, Integer> cacheHits = withLinks ? cacheHitsWithLinks : cacheHitsWithoutLinks;
        Map<String, Integer> cacheMisses = withLinks ? cacheMissesWithLinks : cacheMissesWithoutLinks;

        if (result != null) {
            cacheHits.putIfAbsent(cache.getCacheName(), 0);
            cacheHits.put(cache.getCacheName(), cacheHits.get(cache.getCacheName()) + 1);
            logger.debug("Cache hit for URI {} in cache \"{}\"!", uri, cache.getCacheName());
        } else {
            cacheMisses.putIfAbsent(cache.getCacheName(), 0);
            cacheMisses.put(cache.getCacheName(), cacheMisses.get(cache.getCacheName()) + 1);
            logger.debug("Cache miss for URI {} in cache \"{}\"!", uri, cache.getCacheName());
        }
        return result;
    }

    /**
     * Find the cache an object belongs into and put it there.
     * <p>
     * Only the cache for objects with links is considered here, as retrieval with links is the default. To see cache
     * hits for objects retrieved without their links, you can use {@link #putObjectInCache(java.net.URI, Object, String, Class, boolean)}.
     *
     * @param uri                 The uri of the object
     * @param object              The object to be cached
     * @param cacheName           The name of the cache to put the object in. Use NULL here for automatic cache name detection
     *                            (default).
     * @param collectionItemClass If a collection is queried, the type of the actual collection item.
     */
    public static <T> void putObjectInCache(URI uri, Object object, String cacheName, Class<T> collectionItemClass) {
        putObjectInCache(uri, object, cacheName, collectionItemClass, true);
    }

    /**
     * Find the cache an object belongs into and put it there.
     *
     * @param uri                 The uri of the object
     * @param object              The object to be cached
     * @param cacheName           The name of the cache to put the object in. Use NULL here for automatic cache name detection
     *                            (default).
     * @param collectionItemClass If a collection is queried, the type of the actual collection item.
     * @param withLinks           Whether the cache for objects (/collections) retrieved with or without their links is to be used.
     */
    public static <T> void putObjectInCache(URI uri, Object object, String cacheName, Class<T> collectionItemClass, boolean withLinks) {

        LRUCache<URI, Object> cache;

        if (collectionItemClass != null) {
            if (cacheName == null) {
                cache = getCache(collectionItemClass, true, withLinks);
            } else {
                cache = getCache(cacheName, null, withLinks);
            }
        } else {
            if (cacheName == null) {
                Class realObjectClass;

                if (object instanceof EntityModel<?>) {
                    realObjectClass = ((EntityModel) object).getContent().getClass();
                } else {
                    realObjectClass = object.getClass();
                }
                cache = getCache(realObjectClass, false, withLinks);
            } else {
                cache = getCache(cacheName, null, withLinks);
            }
        }

        if (configuration.isCachingDisabled() && !(cache.getCacheName().equals(StoresthalConfiguration.INTERMEDIATE_CACHE_NAME))) {
            return;
        }

        logger.debug("Putting one object of class \"{}\" into cache named \"{}\" for URI {}", object, cache.getCacheName(), uri);

        cache.put(uri, object);

        logger.debug("\"{}\" cache size is now: {}", cache.getCacheName(), cache.size());

    }

    /**
     * Get the cache for a specific object class.
     *
     * @param cls        The object class
     * @param collection Whether the cache for a collection of objects is to be searched.
     * @param withLinks  Whether the cache for objects (/collections) retrieved with or without their links is to be searched.
     * @return The {@link LRUCache} for this object class. If there was no such cache yet, it will be created.
     */
    private static LRUCache<URI, Object> getCache(Class cls, boolean collection, boolean withLinks) {
        //noinspection unchecked
        Cacheable annotation = (Cacheable) cls.getDeclaredAnnotation(Cacheable.class);

        String cacheName;

        if (collection) {
            cacheName = (annotation != null) ? annotation.collectionCacheName() : StoresthalConfiguration.INTERMEDIATE_CACHE_NAME;
        } else {
            cacheName = (annotation != null) ? annotation.cacheName() : StoresthalConfiguration.INTERMEDIATE_CACHE_NAME;
        }

        logger.debug("Cache for object class \"{}\" is named \"{}\".", cls.getCanonicalName(), cacheName);

        int cacheSize = (annotation != null) ? annotation.cacheSize() : configuration.getDefaultCacheSize();

        return getCache(cacheName, cacheSize, withLinks);
    }

    /**
     * Get the cache with the specified name.
     *
     * @param cacheName The name of the cache
     * @param cacheSize The size of the cache to be created, if a cache with the given name does not exist yet. If NULL, {@link com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration#getDefaultCacheSize()} will be used.
     * @param withLinks Whether a cache for objects retrieved with or without their links is to be used.
     * @return The {@link LRUCache} having the given name. If there was no such cache yet, it will be created.
     */
    private static LRUCache<URI, Object> getCache(String cacheName, Integer cacheSize, boolean withLinks) {

        int newCacheSize = Storesthal.getConfiguration().getDefaultCacheSize();

        if (cacheSize != null) {
            newCacheSize = cacheSize;
        }

        if (withLinks) {
            cachesWithLinks.putIfAbsent(cacheName, new LRUCache<>(cacheName, newCacheSize));
            return cachesWithLinks.get(cacheName);
        }
        cachesWithoutLinks.putIfAbsent(cacheName, new LRUCache<>(cacheName, newCacheSize));
        return cachesWithoutLinks.get(cacheName);
    }

    /**
     * Clear a specific cache using its name (see {@link Cacheable#cacheName()}). Every object stored in the cache
     * will be removed and a new HTTP call will be needed to retrieve the again (which happens automatically once
     * a matching call to {@link Storesthal#getObject(String, Class)} (String, Class)} occurs).
     *
     * @param cacheName             The cache to clear.
     * @param clearStatisticsAsWell Whether to clear the cache hit and miss statistics of the cache as well (resetting
     *                              both of them to zero).
     */
    public static void clearCache(String cacheName, boolean clearStatisticsAsWell) {
        clearCache(cacheName, clearStatisticsAsWell);
    }

    /**
     * Clear a specific cache using its name (see {@link Cacheable#cacheName()}). Every object stored in the cache
     * will be removed and a new HTTP call will be needed to retrieve the again (which happens automatically once
     * a matching call to {@link Storesthal#getObjectWithoutLinks(String, Class)} occurs).
     *
     * @param cacheName             The cache to clear.
     * @param clearStatisticsAsWell Whether to clear the cache hit and miss statistics of the cache as well (resetting
     *                              both of them to zero).
     * @param withLinks             Whether the cache for objects retrieved with or without their links is to be regarded.
     */
    public static void clearCache(String cacheName, boolean clearStatisticsAsWell, Boolean withLinks) {

        List<Map<String, LRUCache<URI, Object>>> cachesToClear = new LinkedList<>();

        if (withLinks == null) {
            logger.warn("clearCache was with the \"withLinks\" parameter set NULL. Please supply a valid boolean parameter indicating wheter the cache for objects with out without links is to be cleared. (Primitive values are always stored in the cache prefixed with CacheManager.CACHE_PREFIX_WITHOUT_LINKS.) Going on anyway clearing all matching caches.", cacheName);
            logger.warn("Going on anyway clearing all matching caches.");
            cachesToClear.add(cachesWithLinks);
            cachesToClear.add(cachesWithoutLinks);
        } else {
            cachesToClear.add(withLinks ? cachesWithLinks : cachesWithoutLinks);
        }

        Map<String, Integer> cacheHits = withLinks ? cacheHitsWithLinks : cacheHitsWithoutLinks;
        Map<String, Integer> cacheMisses = withLinks ? cacheMissesWithLinks : cacheMissesWithoutLinks;

        for (Map<String, LRUCache<URI, Object>> cacheToClear : cachesToClear) {
            if (cacheToClear.containsKey(cacheName)) {
                cacheToClear.get(cacheName).clear();
            }
            if (clearStatisticsAsWell) {
                cacheHits.put(cacheName, 0);
                cacheMisses.put(cacheName, 0);
            }
        }
    }

    /**
     * Get the number of objects stored in a specific cache.
     * Only the cache for objects with links is considered here, as retrieval with links is the default. To see cache
     * hits for objects retrieved without their links, you can use {@link #getCachedObjectCount(String, boolean)}.
     *
     * @param cacheName The name of the cache (see {@link Cacheable#cacheName()}).
     * @return The number of objects in the cache. Note, that a zero return value can mean that the cache either is
     * empty or doesn't exist (yet).
     */
    public static int getCachedObjectCount(String cacheName) {

        LRUCache<URI, Object> cache = cachesWithLinks.get(cacheName);

        if (cache == null) {
            return 0;
        }
        return cache.size();
    }

    /**
     * Get the number of objects stored in a specific cache.
     *
     * @param cacheName The name of the cache (see {@link Cacheable#cacheName()}).
     * @param withLinks Whether the cache for objects retrieved with or without their links is to be regarded.
     * @return The number of objects in the cache. Note, that a zero return value can mean that the cache either is
     * empty or doesn't exist (yet).
     */
    public static int getCachedObjectCount(String cacheName, boolean withLinks) {

        LRUCache<URI, Object> cache = withLinks ? cachesWithLinks.get(cacheName) : cachesWithoutLinks.get(cacheName);

        if (cache == null) {
            return 0;
        }
        return cache.size();
    }

    /**
     * Clear all caches and possibly their related statistics as well.
     *
     * @param clearStatisticsAsWell Whether to clear all cache hit and miss statistics as well (resetting
     *                              all of them to zero).
     */
    public static void clearAllCaches(boolean clearStatisticsAsWell) {
        for (String key : cachesWithLinks.keySet()) {
            clearCache(key, clearStatisticsAsWell, true);
        }
        for (String key : cachesWithoutLinks.keySet()) {
            clearCache(key, clearStatisticsAsWell, false);
        }
    }

    /**
     * Return some information on cache hits and misses
     * ATTN: Only "direct" hits and misses are counted. E. g., if an object is retrieved from cache the sub-object
     * of which is also cached, the sub-object cache hit will not be counted! (Nevertheless the sub-object is correctly
     * retrieved from cache.)
     *
     * @return The cache statistics map
     */
    public static Map<String, Object> getStatistics() {
        return Map.of(CacheManager.STATISTICS_ENTRY_CACHE_HITS_WITH_LINKS, cacheHitsWithLinks, STATISTICS_ENTRY_CACHE_HITS_WITHOUT_LINKS, cacheHitsWithoutLinks, STATISTICS_ENTRY_CACHE_MISSES_WITH_LINKS, cacheMissesWithLinks, STATISTICS_ENTRY_CACHE_MISSES_WITHOUT_LINKS, cacheMissesWithoutLinks);
    }

    /**
     * Reset all statistics about HTTP calls, cache hits and cache misses.
     */
    public static void resetStatistics() {
        cacheHitsWithLinks.clear();
        cacheHitsWithoutLinks.clear();
        cacheMissesWithLinks.clear();
        cacheMissesWithoutLinks.clear();
    }

    /**
     * Returns a map containing the number of hits in all caches for objects retrieved with their links.
     * Only the cache for objects with links is considered here, as retrieval with links is the default. To see cache
     * hits for objects retrieved without their links, you can use {@link #getCacheHits(boolean)}.
     *
     * @return A map containing the number of hits in all caches for objects retrieved with their links.
     */
    public static Map<String, Integer> getCacheHits() {
        return cacheHitsWithLinks;
    }

    /**
     * Returns a map containing the number of hits in all caches for objects retrieved with or without their links.
     *
     * @param withLinks Whether the caches for objects retrieved with or without their links are to be regarded.
     * @return A map containing the number of hits in all caches for objects retrieved with or without their links.
     */
    public static Map<String, Integer> getCacheHits(boolean withLinks) {
        return withLinks ? cacheHitsWithLinks : cacheHitsWithoutLinks;
    }

    /**
     * Returns a map containing the number of misses in all caches for objects retrieved with or without their links.
     *
     * @param withLinks Whether the caches for objects retrieved with or without their links are to be regarded.
     * @return A map containing the number of misses in all caches for objects retrieved with or without their links.
     */
    public static Map<String, Integer> getCacheMisses(boolean withLinks) {
        return withLinks ? cacheMissesWithLinks : cacheMissesWithoutLinks;
    }
}
