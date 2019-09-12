/*
 * Class taken from here: https://stackoverflow.com/questions/23772102/lru-cache-in-java-with-generics-and-o1-operations
 * and adapted a little!
 */

package de.huemmerich.web.storesthal;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Test class for the {@link LRUCache}.
 * Taken from
 * <a href="https://stackoverflow.com/questions/23772102/lru-cache-in-java-with-generics-and-o1-operations">here</a>
 * and adapted.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LRUCacheTest {

    /**
     * LRUCache instance under test.
     */
    private final LRUCache<Integer, Integer> c;

    /**
     * Create a new LRUCache called test in constructor.
     */
    public LRUCacheTest() {
        this.c = new LRUCache<>("test", 2);
    }

    /**
     * Make sure, the cache is empty after creation.
     */
    @Test
    @Order(1)
    public void cacheStartsEmpty() {
        assertEquals("test", c.getCacheName());
        assertNull(c.get(1));
    }

    /**
     * Make sure, values are fetched correctly from cache if they have been inserted before and `null` is returned,
     * if nothing has been inserted for the specified key.
     */
    @Test
    @Order(2)
    public void canFetchObjects() {
        c.put(1, 1);
        assertEquals(1, c.get(1));
        assertNull(c.get(2));
        c.put(2, 4);
        assertEquals(1, c.get(1));
        assertEquals(4, c.get(2));
    }

    /**
     * Make sure, the cache size isn't exceeded.
     */
    @Test
    @Order(3)
    public void testCapacityReachedOldestRemoved() {
        c.put(1, 1);
        c.put(2, 4);
        c.put(3, 9);
        assertNull(c.get(1));
        assertEquals(4, c.get(2));
        assertEquals(9, c.get(3));
    }

    /**
     * Make sure, the least <i>accessed</i> element is evicted from cache once it's capacity is reached.
     */
    @Test
    @Order(4)
    public void testGetRenewsEntry() {
        c.put(1, 1);
        c.put(2, 4);
        assertEquals(1, c.get(1));
        c.put(3, 9);
        assertEquals(1, c.get(1));
        assertNull(c.get(2));
        assertEquals(9, c.get(3));
    }
}