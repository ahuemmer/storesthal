package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.cachetestobjects.*;
import de.huemmerich.web.wsobjectstore.complextestobjects.ComplexChildWithParentRelation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;
import java.util.Vector;

import static org.junit.jupiter.api.Assertions.*;

public class CacheTest extends AbstractJsonTemplateBasedTest {

    protected final static String CHILD_CACHE_NAME = "children";
    protected final static String PARENT_CACHE_NAME = "parents";
    protected final static String SMALL_SIZE_CACHE_NAME = "de.huemmerich.web.wsobjectstore.cachetestobjects.SmallSizedCacheObject";

    @BeforeEach
    public void clearCaches() {
        WSObjectStore.resetStatistics();
        WSObjectStore.clearAllCaches();
    }

    @Test
    public void simpleCacheTest() throws IOException, WSObjectStoreException {
        configureServerMock("/complexChildren1/1", "simpleChildObjectWithParentRelation.json", Map.of("childId", "12", "childName", "Testchild 1!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/2", "simpleChildObjectWithParentRelation.json", Map.of("childId", "23", "childName", "Testchild 2!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/3", "simpleChildObjectWithParentRelation.json", Map.of("childId", "34", "childName", "Testchild 3!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/4", "simpleChildObjectWithParentRelation.json", Map.of("childId", "45", "childName", "Testchild 4!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/5", "simpleChildObjectWithParentRelation.json", Map.of("childId", "56", "childName", "Testchild 5!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/6", "simpleChildObjectWithParentRelation.json", Map.of("childId", "67", "childName", "Testchild 6!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/7", "simpleChildObjectWithParentRelation.json", Map.of("childId", "78", "childName", "Testchild 7!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/8", "simpleChildObjectWithParentRelation.json", Map.of("childId", "89", "childName", "Testchild 8!", "parent", "/parentObjects/124"));
        configureServerMock("/parentObjects/124", "complexObject1.json");

        serverMock.start();

        Vector<ChildWithParentRelation> testChildren = new Vector<>();

        for (int i = 1; i < 9; i++) {
            testChildren.add((i - 1), WSObjectStore.<ChildWithParentRelation>getObject("http://localhost:" + serverMock.port() + "/complexChildren1/" + i, ChildWithParentRelation.class));
        }

        for (int i = 0; i < 7; i++) {
            assertEquals("Testchild " + (i + 1) + "!", testChildren.get(i).getChildName());
            assertNotNull(testChildren.get(i).getParent());
            //Using == here intentionally!
            assertTrue(testChildren.get(i).getParent() == testChildren.get(i + 1).getParent());
        }

        assertEquals(9, (Integer) WSObjectStore.getStatistics().get("httpCalls"));
        assertEquals(7, (Integer) ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertNull(((Map) WSObjectStore.getStatistics().get("cacheHits")).get(CHILD_CACHE_NAME));

    }

    @Test
    public void uncacheableObjectTest() throws WSObjectStoreException, IOException {

        configureServerMock("/objects/1", "simpleObject2.json", Map.of("objectId", "5483790", "name", "Test 1... 2... 3..."));

        UncacheableParentObject lastObject = null;

        for (int i = 0; i < 10; i++) {
            UncacheableParentObject po = WSObjectStore.<UncacheableParentObject>getObject("http://localhost:" + serverMock.port() + "/objects/1", UncacheableParentObject.class);
            assertEquals(5483790, po.getId());
            assertEquals("Test 1... 2... 3...", po.getName());
            if (i != 0) {
                //Objects must be equal but not same...
                assertEquals(lastObject, po);
                assertFalse(lastObject == po);
            }
            lastObject = po;
        }

        assertEquals(10, (Integer) WSObjectStore.getStatistics().get("httpCalls"));
    }

    @Test
    public void clearCacheTest() throws IOException, WSObjectStoreException {

        configureServerMock("/objects/1", "complexObject1.json", Map.of("childId", "12", "childName", "Testchild 1!", "parent", "/parentObjects/124"));

        serverMock.start();

        assertNull(((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(0, WSObjectStore.getStatistics().get("httpCalls"));

        WSObjectStore.<ParentObject>getObject("http://localhost:" + serverMock.port() + "/objects/1", ParentObject.class);

        assertNull(((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(1, WSObjectStore.getStatistics().get("httpCalls"));

        for (int i = 0; i < 10; i++) {
            WSObjectStore.<ParentObject>getObject("http://localhost:" + serverMock.port() + "/objects/1", ParentObject.class);
        }

        assertEquals(10, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(1, WSObjectStore.getStatistics().get("httpCalls"));
        assertEquals(1, WSObjectStore.getCachedObjectCount(PARENT_CACHE_NAME));

        WSObjectStore.clearCache(PARENT_CACHE_NAME, false);
        assertEquals(0, WSObjectStore.getCachedObjectCount(PARENT_CACHE_NAME));

        assertEquals(10, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(1, WSObjectStore.getStatistics().get("httpCalls"));

        WSObjectStore.<ParentObject>getObject("http://localhost:" + serverMock.port() + "/objects/1", ParentObject.class);

        assertEquals(10, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(2, WSObjectStore.getStatistics().get("httpCalls"));

        for (int i = 0; i < 10; i++) {
            WSObjectStore.<ParentObject>getObject("http://localhost:" + serverMock.port() + "/objects/1", ParentObject.class);
        }

        assertEquals(20, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(2, WSObjectStore.getStatistics().get("httpCalls"));
        assertEquals(1, WSObjectStore.getCachedObjectCount(PARENT_CACHE_NAME));

        WSObjectStore.clearAllCaches(true);
        assertEquals(0, WSObjectStore.getCachedObjectCount(PARENT_CACHE_NAME));
        assertEquals(0, WSObjectStore.getStatistics().get("httpCalls"));

        WSObjectStore.<ParentObject>getObject("http://localhost:" + serverMock.port() + "/objects/1", ParentObject.class);

        assertEquals(0, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(1, WSObjectStore.getStatistics().get("httpCalls"));

        for (int i = 0; i < 10; i++) {
            WSObjectStore.<ParentObject>getObject("http://localhost:" + serverMock.port() + "/objects/1", ParentObject.class);
        }


        assertEquals(1, WSObjectStore.getCachedObjectCount(PARENT_CACHE_NAME));
        assertEquals(10, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertEquals(1, WSObjectStore.getStatistics().get("httpCalls"));

    }

    @Test
    public void cacheSizeTest() throws IOException, WSObjectStoreException {

        for (int i = 1; i < 9; i++) {
            configureServerMock("/objects/" + i, "simpleChildObjectWithParentRelation.json", Map.of("childId", String.valueOf(i), "childName", "Testchild " + i + "!", "parent", "/parentObjects/" + i));
            configureServerMock("/parentObjects/" + i, "simpleObject2.json", Map.of("objectId", String.valueOf(i), "name", "Testparent " + i));
        }

        for (int i = 1; i < 5; i++) {
            configureServerMock("/objects/" + (i + 8), "simpleChildObjectWithParentRelation.json", Map.of("childId", String.valueOf(i + 8), "childName", "Testchild " + (i + 8) + "!", "parent", "/parentObjects/" + i));
        }

        serverMock.start();

        for (int i = 1; i < 6; i++) {
            ChildWithParentRelation2 test = WSObjectStore.<ChildWithParentRelation2>getObject("http://localhost:" + serverMock.port() + "/objects/"+i, ChildWithParentRelation2.class);
            assertEquals(i, test.getChildId());
            assertEquals("Testchild "+i+"!", test.getChildName());
            assertNotNull(test.getParent());
            assertEquals("Testparent "+i, test.getParent().getName());
        }

        //Parent cache should now be "full", having no hits
        assertEquals(5, WSObjectStore.getCachedObjectCount(SMALL_SIZE_CACHE_NAME));
        assertNull(((Map) WSObjectStore.getStatistics().get("cacheHits")).get(SMALL_SIZE_CACHE_NAME));
        assertEquals(10, WSObjectStore.getStatistics().get("httpCalls"));

        for (int i = 6; i < 9; i++) {
            ChildWithParentRelation2 test = WSObjectStore.<ChildWithParentRelation2>getObject("http://localhost:" + serverMock.port() + "/objects/"+i, ChildWithParentRelation2.class);
            assertEquals(i, test.getChildId());
            assertEquals("Testchild "+i+"!", test.getChildName());
            assertNotNull(test.getParent());
            assertEquals("Testparent "+i, test.getParent().getName());
        }

        //Cache is still full and having no hits, as the older objects were replaced by the newer ones
        assertEquals(5, WSObjectStore.getCachedObjectCount(SMALL_SIZE_CACHE_NAME));
        assertNull(((Map) WSObjectStore.getStatistics().get("cacheHits")).get(SMALL_SIZE_CACHE_NAME));
        assertEquals(16, WSObjectStore.getStatistics().get("httpCalls"));

        for (int i = 9; i < 13; i++) {
            ChildWithParentRelation2 test = WSObjectStore.<ChildWithParentRelation2>getObject("http://localhost:" + serverMock.port() + "/objects/"+i, ChildWithParentRelation2.class);
            assertEquals(i, test.getChildId());
            assertEquals("Testchild "+i+"!", test.getChildName());
            assertNotNull(test.getParent());
            assertEquals("Testparent "+(i-8), test.getParent().getName());
        }

        //Still no cache hits, as objects were evicted...
        assertEquals(5, WSObjectStore.getCachedObjectCount(SMALL_SIZE_CACHE_NAME));
        assertNull(((Map) WSObjectStore.getStatistics().get("cacheHits")).get(SMALL_SIZE_CACHE_NAME));
        assertEquals(24, WSObjectStore.getStatistics().get("httpCalls"));

        for (int i = 9; i < 13; i++) {
            ChildWithParentRelation2 test = WSObjectStore.<ChildWithParentRelation2>getObject("http://localhost:" + serverMock.port() + "/objects/"+i, ChildWithParentRelation2.class);
            assertEquals(i, test.getChildId());
            assertEquals("Testchild "+i+"!", test.getChildName());
            assertNotNull(test.getParent());
            assertEquals("Testparent "+(i-8), test.getParent().getName());
        }

        //Now we've got some hits
        assertEquals(5, WSObjectStore.getCachedObjectCount(SMALL_SIZE_CACHE_NAME));
        assertEquals(4, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(CHILD_CACHE_NAME));
        //Is still null here as cache isn't accessed directly:
        assertNull(((Map) WSObjectStore.getStatistics().get("cacheHits")).get(SMALL_SIZE_CACHE_NAME));
        assertEquals(24, WSObjectStore.getStatistics().get("httpCalls"));

        for (int i = 1; i < 5; i++) {
            SmallSizedCacheObject test = WSObjectStore.<SmallSizedCacheObject>getObject("http://localhost:" + serverMock.port() + "/parentObjects/"+i, SmallSizedCacheObject.class);
            assertEquals("Testparent "+i, test.getName());
            assertEquals(i, test.getId());
        }

        assertEquals(5, WSObjectStore.getCachedObjectCount(SMALL_SIZE_CACHE_NAME));
        assertEquals(4, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(CHILD_CACHE_NAME));
        assertEquals(4, ((Map) WSObjectStore.getStatistics().get("cacheHits")).get(SMALL_SIZE_CACHE_NAME));
        assertEquals(24, WSObjectStore.getStatistics().get("httpCalls"));

    }
}