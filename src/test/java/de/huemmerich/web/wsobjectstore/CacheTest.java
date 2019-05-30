package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.cachetestobjects.ChildWithParentRelation;
import de.huemmerich.web.wsobjectstore.cachetestobjects.UncacheableParentObject;
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

    @BeforeEach
    public void clearCaches() {
        WSObjectStore.resetStatistics();
        WSObjectStore.clearAllCaches();
    }

    @Test
    public void simpleCacheTest() throws IOException, WSObjectStoreException {
        configureServerMock("/complexChildren1/1", "simpleChildObjectWithParentRelation.json", Map.of("childId","12", "childName", "Testchild 1!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/2", "simpleChildObjectWithParentRelation.json", Map.of("childId","23", "childName", "Testchild 2!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/3", "simpleChildObjectWithParentRelation.json", Map.of("childId","34", "childName", "Testchild 3!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/4", "simpleChildObjectWithParentRelation.json", Map.of("childId","45", "childName", "Testchild 4!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/5", "simpleChildObjectWithParentRelation.json", Map.of("childId","56", "childName", "Testchild 5!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/6", "simpleChildObjectWithParentRelation.json", Map.of("childId","67", "childName", "Testchild 6!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/7", "simpleChildObjectWithParentRelation.json", Map.of("childId","78", "childName", "Testchild 7!", "parent", "/parentObjects/124"));
        configureServerMock("/complexChildren1/8", "simpleChildObjectWithParentRelation.json", Map.of("childId","89", "childName", "Testchild 8!", "parent", "/parentObjects/124"));
        configureServerMock("/parentObjects/124", "complexObject1.json", Map.of("childId","12", "childName", "Testchild 1!", "parent", "/parentObjects/124"));

        serverMock.start();

        Vector<ChildWithParentRelation> testChildren = new Vector<>();

        for (int i=1; i<9; i++) {
            testChildren.add((i-1),WSObjectStore.<ChildWithParentRelation>getObject("http://localhost:"+serverMock.port()+"/complexChildren1/"+i, ChildWithParentRelation.class));
        }

        for(int i=0;i<7;i++) {
            assertEquals("Testchild "+(i+1)+"!", testChildren.get(i).getChildName());
            assertNotNull(testChildren.get(i).getParent());
            //Using == here intentionally!
            assertTrue(testChildren.get(i).getParent()==testChildren.get(i+1).getParent());
        }

        assertEquals(9,(Integer) WSObjectStore.getStatistics().get("httpCalls"));
        assertEquals(7,(Integer)((Map)WSObjectStore.getStatistics().get("cacheHits")).get(PARENT_CACHE_NAME));
        assertNull(((Map)WSObjectStore.getStatistics().get("cacheHits")).get(CHILD_CACHE_NAME));

    }

    @Test
    public void simpleCacheTest2() throws WSObjectStoreException, IOException {

        configureServerMock("/objects/1", "simpleObject2.json", Map.of("objectId","5483790", "name", "Test 1... 2... 3..."));

        UncacheableParentObject lastObject = null;

        for (int i=0;i<10;i++) {
            UncacheableParentObject po = WSObjectStore.<UncacheableParentObject>getObject("http://localhost:"+serverMock.port()+"/objects/1", UncacheableParentObject.class);
            assertEquals(5483790, po.getId());
            assertEquals("Test 1... 2... 3...", po.getName());
            if (i!=0) {
                //Objects must be equal but not same...
                assertEquals(lastObject,po);
                assertFalse(lastObject==po);
            }
            lastObject = po;
        }

        assertEquals(10,(Integer) WSObjectStore.getStatistics().get("httpCalls"));

    }


}
