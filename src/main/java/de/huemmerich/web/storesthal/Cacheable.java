package de.huemmerich.web.storesthal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation marks any object that can be retrieved via REST/JSON (and therefore by {@link WSObjectStore} AND
 * then cached in order to make subsequent accesses to the same object (URL) faster.
 * In this case and if the cache size isn't exhausted, the object will not be retrieved by a REST HTTP request again
 * but instead be fetched from the local memory.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cacheable {

    /**
     * The name of the cache to use for this kind of object.
     * It makes sense to give a unique name to the cache for each kind of {@link Cacheable} object. Thus, the cache
     * size for every single cache can be individually set and the specific cache can also be emptied individually.
     * Cache statistics etc. do as well rely on the cache name.
     * I. e. the full qualified class name of the specific object class is a good candidate for the cache name.
     * Nevertheless, neither using one cache name per object class nor using a cache name at all is enforced!
     * If no cache name is used, the objects will be stored in a cache denoted by {@link WSObjectStore#COMMON_CACHE_NAME},
     * regardless of the specific class.
     * @return The name of the object cache.
     */
    String cacheName() default WSObjectStore.COMMON_CACHE_NAME;

    /**
     * The size of the cache.
     * If you specify a size of n here, a maximum amount of n objects will be stored in the cache denoted by {@link #cacheName()}.
     * If the (n+1)th is to be inserted, the object last recently accessed (!) will therefore be removed from the cache - so
     * the cache type is "LRU".
     * If multiple classes are annotated with {@link Cacheable} and the same {@link #cacheName()} but with different
     * cache sizes, the cache will be initialized with the size of the object class that is first encountered
     * by {@link WSObjectStore}. Is this can lead to undesired behavior (and it makes no sense it all), using different
     * cache size parameters for the same cache is discouraged.
     * @return The size of the cache denoted by {@link #cacheName()}, see notes above.
     */
    int cacheSize() default 1000;
}
