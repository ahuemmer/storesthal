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

    public static final String CACHE_PREFIX_WITH_LINKS = "withLinks:";
    public static final String CACHE_PREFIX_WITHOUT_LINKS = "withoutLinks:";

    /**
     * A map containing the number of cache misses by cache (name) for statistics creation.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()}.
     */
    private static final Map<String, Integer> cacheMisses = new HashMap<>();
    /**
     * A map containing the number of cache hits by cache (name) for statistics creation.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()}.
     */
    private static final Map<String, Integer> cacheHits = new HashMap<>();
    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    private static CacheManager instance;
    private static StoresthalConfiguration configuration;
    /**
     * All configured object caches are stored in this map, the key is the cache name (see {@link LRUCache#getCacheName()}
     * and {@link Cacheable#cacheName()}).
     */
    private static Map<String, LRUCache<URI, Object>> caches;

    private CacheManager() {
    }

    @SuppressWarnings("InstantiationOfUtilityClass")
    public static CacheManager getInstance(StoresthalConfiguration configuration) {
        if (instance == null) {
            instance = new CacheManager();
            CacheManager.configuration = configuration;
            caches = new HashMap<>();
            clearAllCaches(true);
        }
        return instance;
    }

    /**
     * Try to retrieve an object from the associated cache (or the common cache, if the {@link com.github.ahuemmer.storesthal.Cacheable} annotation does
     * not state an explicit cache name).
     *
     * @param uri         The object's URI
     * @param objectClass The class of the object
     * @param cacheName   The name of the cache to get the object from. Use NULL here for automatic cache name detection
     *                    (default).
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
            cache = getCache(cacheName, null);
        }

        if (configuration.isCachingDisabled() && !(cache.getCacheName().equals(StoresthalConfiguration.INTERMEDIATE_CACHE_NAME))) {
            logger.debug("Caching is disabled!");
            return null;
        }

        T result = (T) cache.get(uri);

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
     *
     * @param uri       The uri of the object
     * @param object    The object to be cached
     * @param cacheName The name of the cache to put the object in. Use NULL here for automatic cache name detection
     *                  (default).
     */
    public static <T> void putObjectInCache(URI uri, Object object, String cacheName, Class<T> collectionItemClass, boolean withLinks) {

        LRUCache<URI, Object> cache;

        if (collectionItemClass != null) {
            if (cacheName == null) {
                cache = getCache(collectionItemClass, true, withLinks);
            } else {
                cache = getCache(cacheName, null);
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
                cache = getCache(cacheName, null);
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
     * @param cls The object class
     * @return The {@link LRUCache} for this object class. If there was no such cache yet, it will be created.
     */
    private static LRUCache<URI, Object> getCache(Class cls, boolean collection, boolean withLinks) {
        //noinspection unchecked
        Cacheable annotation = (Cacheable) cls.getDeclaredAnnotation(Cacheable.class);

        String cacheName;

        if (collection) {
            cacheName = adaptCacheName((annotation != null) ? annotation.collectionCacheName() : StoresthalConfiguration.INTERMEDIATE_CACHE_NAME, withLinks);
        } else {
            cacheName = adaptCacheName((annotation != null) ? annotation.cacheName() : StoresthalConfiguration.INTERMEDIATE_CACHE_NAME, withLinks);
        }


        logger.debug("Cache for object class \"{}\" is named \"{}\".", cls.getCanonicalName(), cacheName);

        int cacheSize = (annotation != null) ? annotation.cacheSize() : configuration.getDefaultCacheSize();

        return getCache(cacheName, cacheSize);
    }

    private static String adaptCacheName(String cacheName, boolean withLinks) {
        if (withLinks) {
            return CACHE_PREFIX_WITH_LINKS + cacheName;
        }

        return CACHE_PREFIX_WITHOUT_LINKS + cacheName;
    }

    /**
     * Get the cache with the specified name.
     *
     * @param cacheName The name of the cache
     * @return The {@link LRUCache} having the given name. If there was no such cache yet, it will be created.
     */
    private static LRUCache<URI, Object> getCache(String cacheName, Integer cacheSize) {

        int newCacheSize = Storesthal.getConfiguration().getDefaultCacheSize();

        if (cacheSize != null) {
            newCacheSize = cacheSize;
        }

        caches.putIfAbsent(cacheName, new LRUCache<>(cacheName, newCacheSize));
        return caches.get(cacheName);
    }

    /**
     * Clear a specific cache using its name (see {@link Cacheable#cacheName()}). Every object stored in the cache
     * will be removed and a new HTTP call will be needed to retrieve the again (which happens automatically once
     * a matching call to {@link Storesthal#getObjectWithoutLinks(String, Class)} occurs).
     *
     * @param cacheName             The cache to clear.
     * @param clearStatisticsAsWell Whether to clear the cache hit and miss statistics of the cache as well (resetting
     *                              both of them to zero).
     */
    public static void clearCache(String cacheName, boolean clearStatisticsAsWell) {

        List<String> cachesToClear = new LinkedList<>();
        cachesToClear.add(cacheName);

        if (!cacheName.startsWith(CACHE_PREFIX_WITH_LINKS) && !cacheName.startsWith(CACHE_PREFIX_WITHOUT_LINKS)) {
            logger.warn("clearCache was called with cache name \"{}\" - please prefix it with CacheManager.CACHE_PREFIX_WITHOUT_LINKS or CacheManager.CACHE_PREFIX_WITH_LINKS. (Primitive values are always stored in the cache prefixed with CacheManager.CACHE_PREFIX_WITHOUT_LINKS.) Going on anyway clearing all matching caches.", cacheName);
            logger.warn("Going on anyway clearing all matching caches.");
            cachesToClear.add(CACHE_PREFIX_WITH_LINKS + cacheName);
            cachesToClear.add(CACHE_PREFIX_WITHOUT_LINKS + cacheName);
        }

        for (String cacheNameToTry : cachesToClear) {
            if (caches.containsKey(cacheNameToTry)) {
                caches.get(cacheNameToTry).clear();
            }
            if (clearStatisticsAsWell) {
                cacheHits.put(cacheNameToTry, 0);
                cacheMisses.put(cacheNameToTry, 0);
            }
        }
    }

    /**
     * Get the number of objects stored in a specific cache.
     *
     * @param cacheName The name of the cache (see {@link Cacheable#cacheName()}).
     * @return The number of objects in the cache. Note, that a zero return value can mean that the cache either is
     * empty or doesn't exist (yet).
     */
    public static int getCachedObjectCount(String cacheName) {
        if (caches.get(cacheName) == null) {
            return 0;
        }
        return caches.get(cacheName).size();
    }

    /**
     * Clear all caches and possibly their related statistics as well.
     *
     * @param clearStatisticsAsWell Whether to clear all cache hit and miss statistics as well (resetting
     *                              all of them to zero).
     */
    public static void clearAllCaches(boolean clearStatisticsAsWell) {
        for (String key : caches.keySet()) {
            clearCache(key, clearStatisticsAsWell);
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
        return Map.of("cacheHits", cacheHits, "cacheMisses", cacheMisses);
    }

    /**
     * Reset all statistics about HTTP calls, cache hits and cache misses.
     */
    public static void resetStatistics() {
        cacheHits.clear();
        cacheMisses.clear();
    }

    public static Map<String, Integer> getCacheHits() {
        return cacheHits;
    }


    public static Map<String, Integer> getCacheMisses() {
        return cacheMisses;
    }
}
