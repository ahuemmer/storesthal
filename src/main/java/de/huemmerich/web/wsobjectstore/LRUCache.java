/*
 * Whole class taken from here: https://www.codewalk.com/2012/04/least-recently-used-lru-cache-implementation-java.html !
 */

package de.huemmerich.web.wsobjectstore;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

public class LRUCache <K, V> extends LinkedHashMap <K, V> {

    private String cacheName;

    private int capacity; // Maximum number of items in the cache.

    public LRUCache(String cacheName, int capacity) {
        super(capacity+1, 1.0f, true); // Pass 'true' for accessOrder.
        this.capacity = capacity;
        this.cacheName = cacheName;
    }

    public String getCacheName() {
        return cacheName;
    }

    protected boolean removeEldestEntry(Entry entry) {
        return (size() > this.capacity);
    }
}