/*
 * Class taken from here: https://stackoverflow.com/questions/23772102/lru-cache-in-java-with-generics-and-o1-operations
 * and modified
 */

package de.huemmerich.web.wsobjectstore;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class LRUCacheTest {

    private LRUCache<Integer, Integer> c;

    public LRUCacheTest() {
        this.c = new LRUCache("test", 2);
    }

    @Test
    @Order(1)
    public void testCacheStartsEmpty() {
        assertEquals("test", c.getCacheName());
        assertEquals(null, c.get(1));
    }

    @Test
    @Order(2)
    public void testSetBelowCapacity() {
        c.put(1, 1);
        assertEquals(1, c.get(1));
        assertEquals(null, c.get(2));
        c.put(2, 4);
        assertEquals(1, c.get(1));
        assertEquals(4, c.get(2));
    }

    @Test
    @Order(3)
    public void testCapacityReachedOldestRemoved() {
        c.put(1, 1);
        c.put(2, 4);
        c.put(3, 9);
        assertEquals(null, c.get(1));
        assertEquals(4, c.get(2));
        assertEquals(9, c.get(3));
    }

    @Test
    @Order(4)
    public void testGetRenewsEntry() {
        c.put(1, 1);
        c.put(2, 4);
        assertEquals(1, c.get(1));
        c.put(3, 9);
        assertEquals(1, c.get(1));
        assertEquals(null, c.get(2));
        assertEquals(9, c.get(3));
    }
}