/*
 * Big parts taken from here: https://www.codewalk.com/2012/04/least-recently-used-lru-cache-implementation-java.html !
 */

package de.huemmerich.web.storesthal;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * This class implements a LRU (least recently used) object cache with configurable key and value object types and size.
 * Most of the code is taken from here: https://www.codewalk.com/2012/04/least-recently-used-lru-cache-implementation-java.html
 * (except for the comments)!
 * The implementation is a specially adapted derivation of {@link LinkedHashMap}.
 * @param <K> The type of the "key" objects
 * @param <V> The type of the "value" objects
 */
public class LRUCache <K, V> extends LinkedHashMap <K, V> {

    /**
     * The name of this cache - to be able to easily distinguish it from possible other object caches.
     */
    private final String cacheName;

    /**
     * The maximum number of items in the cache
     */
    private final int capacity;

    /**
     * Construct a new LRUCache with a given name and capacity
     * @param cacheName The name of the cache - to be able to easily distinguish it from possible other object caches.
     * @param capacity The maximum number of items in the cache
     */
    public LRUCache(String cacheName, int capacity) {
        super(capacity+1, 1.0f, true); // Pass 'true' for accessOrder.
        this.capacity = capacity;
        this.cacheName = cacheName;
    }

    /**
     * Get the name of this cache
     * @return The name of this cache
     */
    public String getCacheName() {
        return cacheName;
    }

    /**
     * Whether to remove the "eldest" (or, in this case, least recently accessed) entry from the cache
     * @param entry (Not used)
     * @return True if the least recently used entry is to be removed from the cache
     * (because it has reached its capacity).
     */
    @Override
    protected boolean removeEldestEntry(Entry entry) {
        return (size() > this.capacity);
    }
}