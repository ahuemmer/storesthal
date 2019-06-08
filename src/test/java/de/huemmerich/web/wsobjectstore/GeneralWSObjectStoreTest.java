package de.huemmerich.web.wsobjectstore;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockSettings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.Options;
import de.huemmerich.web.wsobjectstore.complextestobjects.*;
import org.apache.commons.text.StringSubstitutor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.*;

public class GeneralWSObjectStoreTest extends AbstractJsonTemplateBasedTest {

    @BeforeEach
    public void clearCaches() {
        WSObjectStore.resetStatistics();
        WSObjectStore.clearAllCaches();
    }

    @Test
    public void testWSObjectStoreGetComplexObject1() throws WSObjectStoreException, IOException {

        configureServerMock("/complexObjects/1", "complexObject1.json");
        serverMock.start();

        ComplexObject1 test = WSObjectStore.<ComplexObject1>getObject("http://localhost:"+serverMock.port()+"/complexObjects/1", ComplexObject1.class);

        assertNotNull(test);
        assertEquals(1, test.getCategoryId());
        assertEquals(10157977, test.getColor());
        assertEquals("", test.getComment());
        assertEquals("Test!", test.getName());
        assertEquals(2, test.getNumber());
        assertEquals("expense", test.getType());
    }

    @Test
    public void testWSObjectStoreGetComplexObjectWithSingleChild() throws WSObjectStoreException, IOException {
        configureServerMock("/complexObjectsWithSingleChildren/1", "complexObjectWithSingleChild1.json");
        configureServerMock("/complexChildren/1", "simpleObject1.json", Map.of("name","Testchild!"));

        serverMock.start();

        ComplexObjectWithSingleChild test = WSObjectStore.<ComplexObjectWithSingleChild>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithSingleChildren/1", ComplexObjectWithSingleChild.class);

        assertNotNull(test);
        assertEquals(4711, test.getCategoryId());
        assertEquals(101579, test.getColor());
        assertEquals("oi...", test.getComment());
        assertEquals("Test2!", test.getName());
        assertEquals(3, test.getNumber());
        assertEquals("income", test.getType());

        ComplexChild1 child = test.getChild();
        assertNotNull(child);
        assertEquals("Testchild!", child.getChildName());
    }

    @Test
    public void testWSObjectStoreGetComplexObjectWithMultipleChildren1() throws WSObjectStoreException, IOException {

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "itsme...","categoryId","1508", "name", "Test3!", "number", "9", "type", "neither", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
        "http://localhost:${port}/complexChildren2/2",
        "http://localhost:${port}/complexChildren2/3"}
    ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleObject2.json", Map.of("objectId","12345", "name", "Testchild 1!"));
        configureServerMock("/complexChildren2/2", "simpleObject2.json", Map.of("objectId","815", "name", "Testchild 2!"));
        configureServerMock("/complexChildren2/3", "simpleObject2.json", Map.of("objectId","4711", "name", "Testchild 3!"));

        serverMock.start();

        ComplexObjectWithMultipleChildren1 test = WSObjectStore.<ComplexObjectWithMultipleChildren1>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren1.class);

        assertNotNull(test);
        assertEquals(1508, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("itsme...", test.getComment());
        assertEquals("Test3!", test.getName());
        assertEquals(9, test.getNumber());
        assertEquals("neither", test.getType());

        List<ComplexChild2> children = test.getChildren();
        assertNotNull(children);
        assertEquals(3, children.size());
        assertEquals("Testchild 1!", children.get(0).getChildName());
        assertEquals("Testchild 2!", children.get(1).getChildName());
        assertEquals("Testchild 3!", children.get(2).getChildName());

        assertEquals(12345, children.get(0).getChildId());
        assertEquals(815, children.get(1).getChildId());
        assertEquals(4711, children.get(2).getChildId());
    }

    @Test
    //Pretty much the same as before (in testWSObjectStoreGetComplexObjectWithMultipleChildren1), but now we use
    //ComplexObjectWithMultipleChildren2 which has a concrete implementation of a collection (LinkedList) instead
    //of an interface.
    public void testWSObjectStoreGetComplexObjectWithMultipleChildren2() throws WSObjectStoreException, IOException {
        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "hello!", "categoryId","4711", "name", "Blubb", "number", "45648", "type", "type",  "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleObject2.json", Map.of("objectId","12345", "name", "Testchild 1!"));
        configureServerMock("/complexChildren2/2", "simpleObject2.json", Map.of("objectId","815", "name", "Testchild 2!"));
        configureServerMock("/complexChildren2/3", "simpleObject2.json", Map.of("objectId","4711", "name", "Testchild 3!"));

        serverMock.start();

        ComplexObjectWithMultipleChildren2 test = WSObjectStore.<ComplexObjectWithMultipleChildren2>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren2.class);

        assertNotNull(test);
        assertEquals(4711, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("hello!", test.getComment());
        assertEquals("Blubb", test.getName());
        assertEquals(45648, test.getNumber());
        assertEquals("type", test.getType());

        List<ComplexChild2> children = test.getChildren();
        assertNotNull(children);
        assertEquals(3, children.size());
        assertEquals("Testchild 1!", children.get(0).getChildName());
        assertEquals("Testchild 2!", children.get(1).getChildName());
        assertEquals("Testchild 3!", children.get(2).getChildName());

        assertEquals(12345, children.get(0).getChildId());
        assertEquals(815, children.get(1).getChildId());
        assertEquals(4711, children.get(2).getChildId());

    }

    @Test
    //Pretty much the same as the two before (in testWSObjectStoreGetComplexObjectWithMultipleChildren1), but now we use
    //ComplexObjectWithMultipleChildren3 which has an array as collection insted of a (Linked)List.
    public void testWSObjectStoreGetComplexObjectWithMultipleChildrenInArray() throws WSObjectStoreException, IOException {
        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "", "categoryId","9999", "name", "Xyz123!", "number", "1", "type", "mööööp", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleObject2.json", Map.of("objectId","12345", "name", "Testchild 1!"));
        configureServerMock("/complexChildren2/2", "simpleObject2.json", Map.of("objectId","815", "name", "Testchild 2!"));
        configureServerMock("/complexChildren2/3", "simpleObject2.json", Map.of("objectId","4711", "name", "Testchild 3!"));

        serverMock.start();

        assertThrows(WSObjectStoreException.class, () -> {
            ComplexObjectWithMultipleChildren3 test = WSObjectStore.<ComplexObjectWithMultipleChildren3>getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren3.class);
        });

        //ARRAYS ARE NOT SUPPORTED (YET??)
        //Whenever this is the case, the following assertions should succeed:

        /*assertNotNull(test);
        assertEquals(1508, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("itsme...", test.getComment());
        assertEquals("Test3!", test.getName());
        assertEquals(9, test.getNumber());
        assertEquals("mööööp", test.getType());

        ComplexChild2[] children = test.getChildren();
        assertNotNull(children);
        assertEquals(3, children.length);
        assertEquals("Testchild 1!", children[0].getChildName());
        assertEquals("Testchild 2!", children[1].getChildName());
        assertEquals("Testchild 3!", children[2].getChildName());

        assertEquals(12345, children[0].getChildId());
        assertEquals(815, children[1].getChildId());
        assertEquals(4711, children[2].getChildId());*/

    }

    @Test
    public void testWSObjectStoreGetComplexObjectWithMultipleChildrenAndParentRelation() throws WSObjectStoreException, IOException {

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "887766", "comment", "", "categoryId","12345", "name", "Äußerst umlautig!", "number", "-1",  "type", "This is a type. Is it? Really??? Yes...", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleChildObjectWithParentRelation.json", Map.of("childId","654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/2", "simpleChildObjectWithParentRelation.json", Map.of("childId","158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/3", "simpleChildObjectWithParentRelation.json", Map.of("childId","1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

        serverMock.start();

        WSObjectStore.resetStatistics();

        ComplexObjectWithMultipleChildren4 test = WSObjectStore.<ComplexObjectWithMultipleChildren4>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren4.class);

        assertNotNull(test);
        assertEquals(12345, test.getCategoryId());
        assertEquals(887766, test.getColor());
        assertEquals("", test.getComment());
        assertEquals("Äußerst umlautig!", test.getName());
        assertEquals(-1, test.getNumber());
        assertEquals("This is a type. Is it? Really??? Yes...", test.getType());

        List<ComplexChildWithParentRelation> children = test.getChildren();
        assertNotNull(children);
        assertEquals(3, children.size());
        assertEquals("Testchild with parent 1.", children.get(0).getChildName());
        assertEquals("Testchild with parent 2.", children.get(1).getChildName());
        assertEquals("Testchild with parent 3.", children.get(2).getChildName());

        assertEquals(654321, children.get(0).getChildId());
        assertEquals(158, children.get(1).getChildId());
        assertEquals(1147, children.get(2).getChildId());

        WSObjectStore.printStatistics();

        assertEquals(4,(Integer) WSObjectStore.getStatistics().get("httpCalls"));

        for (ComplexChildWithParentRelation child: children) {
            //Use == here --> really the same object!
            assertTrue(test==child.getParent());
        }
    }

    @Test
    public void testWSObjectStoreGetComplexObjectWithMultipleChildrenAndParentRelationCollection() throws WSObjectStoreException, IOException {

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "456456", "comment", "abcABC", "categoryId","123459876", "name", "Das ist ein Name.", "number", "12", "type", "", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/2", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/3", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

        serverMock.start();

        WSObjectStore.resetStatistics();

        ComplexObjectWithMultipleChildren5 test = WSObjectStore.<ComplexObjectWithMultipleChildren5>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren5.class);

        assertNotNull(test);
        assertEquals(123459876, test.getCategoryId());
        assertEquals(456456, test.getColor());
        assertEquals("abcABC", test.getComment());
        assertEquals("Das ist ein Name.", test.getName());
        assertEquals(12, test.getNumber());
        assertEquals("", test.getType());

        List<ComplexChildWithParentRelationCollection> children = test.getChildren();
        assertNotNull(test.getChildren());
        assertEquals(3, test.getChildren().size());


        assertEquals("Testchild with parent 1.", children.get(0).getChildName());
        assertEquals("Testchild with parent 2.", children.get(1).getChildName());
        assertEquals("Testchild with parent 3.", children.get(2).getChildName());

        assertEquals(654321, children.get(0).getChildId());
        assertEquals(158, children.get(1).getChildId());
        assertEquals(1147, children.get(2).getChildId());

        WSObjectStore.printStatistics();

        assertEquals(4,(Integer) WSObjectStore.getStatistics().get("httpCalls"));

        for (ComplexChildWithParentRelationCollection child: children) {
            assertEquals(1, child.getParents().size());
            //Use == here --> really the same object!
            assertTrue(test==child.getParents().get(0));
        }
    }

    @Test
    public void testVeryComplexObjectStructure() throws IOException, WSObjectStoreException {
        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "0", "comment", ".", "categoryId","2", "name", "Complexity...", "number", "14", "type", "987epyt", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "1345", "comment", "Number 1...", "categoryId","5", "name", "is...", "number", "5547", "type", "$myGreatType", "children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/1"
        }
        ), "parent", ""));
        configureServerMock("/complexChildren2/2", "complexObjectWithMultipleChildren1.json", Map.of("color", "584390", "comment", "Number 2...", "categoryId","1", "name", "just...","number","8", "type", "xyxyxy", "children", createJsonHrefArray(new String[] {
                }
        ), "parent", ""));
        configureServerMock("/complexChildren2/3", "complexObjectWithMultipleChildren1.json", Map.of("color", "468", "comment", "Number 3...", "categoryId","1111", "name", "a...","number","-24","type","3","children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/2",
                        "http://localhost:${port}/complexChildren3/3",
                        "http://localhost:${port}/complexChildren3/4",
                        "http://localhost:${port}/complexChildren3/5"
                }
        ), "parent", ""));

        configureServerMock("/complexChildren3/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "747474", "comment", "I'm the first subchild", "categoryId","10000", "name", "state...","number","null","type", "   ", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/1\"}"));
        configureServerMock("/complexChildren3/2", "complexObjectWithMultipleChildren1.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId","789456123", "name", "of...","number","-7894", "type", "*", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMock("/complexChildren3/3", "complexObjectWithMultipleChildren1.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId","0", "name", "mind!","number","574389", "type", "${myType}", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMock("/complexChildren3/4", "complexObjectWithMultipleChildren1.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId","55", "name", "Lorem","number","1186","type", "Object Mark IV", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMock("/complexChildren3/5", "complexObjectWithMultipleChildren1.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId","3521", "name", "ipsum","number","-7561","type", "Knödel", "children", createJsonHrefArray(new String[] {}), "parent", ""));

        serverMock.start();

        WSObjectStore.resetStatistics();

        ComplexObjectWithMultipleChildren6 test = WSObjectStore.<ComplexObjectWithMultipleChildren6>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren6.class);

        assertEquals(0,test.getColor());
        assertEquals(".",test.getComment());
        assertEquals(2,test.getCategoryId());
        assertEquals(3,test.getChildren().size());
        assertEquals(14, test.getNumber());
        assertEquals("Complexity...", test.getName());
        assertEquals("987epyt", test.getType());

        //Child nr. 1

        assertEquals(1345,test.getChildren().get(0).getColor());
        assertEquals("is...", test.getChildren().get(0).getName());
        assertEquals("Number 1...",test.getChildren().get(0).getComment());
        assertEquals(5,test.getChildren().get(0).getCategoryId());
        assertEquals(1,test.getChildren().get(0).getChildren().size());
        assertEquals(5547, test.getChildren().get(0).getNumber());
        assertEquals("$myGreatType", test.getChildren().get(0).getType());

        //  Subchild nr. 1.1

        ComplexObjectWithMultipleChildren6 subChild1 = test.getChildren().get(0).getChildren().get(0);
        assertEquals(747474, subChild1.getColor());
        assertEquals("I'm the first subchild", subChild1.getComment());
        assertNotNull(subChild1.getParent());
        assertEquals(1345, subChild1.getParent().getColor());
        assertSame(subChild1.getParent(), test.getChildren().get(0));
        assertEquals(10000, subChild1.getCategoryId());
        assertEquals("state...", subChild1.getName());
        assertEquals("   ", subChild1.getType());
        assertNull(subChild1.getChildren());
        assertNull(subChild1.getNumber());

        //  End subchild nr. 1.1

        //END Child nr. 1

        //Child nr. 2

        assertEquals(584390,test.getChildren().get(1).getColor());
        assertEquals("just...", test.getChildren().get(1).getName());
        assertEquals("Number 2...",test.getChildren().get(1).getComment());
        assertEquals(1,test.getChildren().get(1).getCategoryId());
        assertNull(test.getChildren().get(1).getChildren());
        assertEquals(8, test.getChildren().get(1).getNumber());
        assertEquals("xyxyxy", test.getChildren().get(1).getType());

        //(Child nr. 2 has no subchildren...)

        //END Child nr. 2


        //Child nr. 3
        assertEquals(468,test.getChildren().get(2).getColor());
        assertEquals("a...", test.getChildren().get(2).getName());
        assertEquals("Number 3...",test.getChildren().get(2).getComment());
        assertEquals(1111,test.getChildren().get(2).getCategoryId());
        assertNull(test.getChildren().get(1).getChildren());
        assertEquals(-24, test.getChildren().get(2).getNumber());
        assertEquals("3", test.getChildren().get(2).getType());
        assertEquals(4, test.getChildren().get(2).getChildren().size());

        //  Subchild nr 3.1

        ComplexObjectWithMultipleChildren6 subChild = test.getChildren().get(2).getChildren().get(0);
        assertEquals(-7894, subChild.getNumber());
        assertEquals(3, subChild.getColor());
        assertEquals("I'm the second subchild", subChild.getComment());
        assertEquals(789456123, subChild.getCategoryId());
        assertEquals("of...", subChild.getName());
        assertEquals("*", subChild.getType());
        assertNull(subChild.getChildren());


        //  END Subchild nr. 3.1

        //  Subchild nr 3.2

        subChild = test.getChildren().get(2).getChildren().get(1);
        assertEquals(574389, subChild.getNumber());
        assertEquals(818147, subChild.getColor());
        assertEquals("I'm the third subchild", subChild.getComment());
        assertEquals(0, subChild.getCategoryId());
        assertEquals("mind!", subChild.getName());
        assertEquals("${myType}", subChild.getType());
        assertNull(subChild.getChildren());

        //  END Subchild nr. 3.2


        //  Subchild nr 3.3

        subChild = test.getChildren().get(2).getChildren().get(2);
        assertEquals(1186, subChild.getNumber());
        assertEquals(29141, subChild.getColor());
        assertEquals("I'm the fourth subchild", subChild.getComment());
        assertEquals(55, subChild.getCategoryId());
        assertEquals("Lorem", subChild.getName());
        assertEquals("Object Mark IV", subChild.getType());
        assertNull(subChild.getChildren());

        //  END Subchild nr. 3.3

        //  Subchild nr 3.4

        subChild = test.getChildren().get(2).getChildren().get(3);
        assertEquals(-7561, subChild.getNumber());
        assertEquals(222222, subChild.getColor());
        assertEquals("I'm the fifth subchild", subChild.getComment());
        assertEquals(3521, subChild.getCategoryId());
        assertEquals("ipsum", subChild.getName());
        assertEquals("Knödel", subChild.getType());
        assertNull(subChild.getChildren());

        //  END Subchild nr. 3.4

        //END Child nr. 3
    }

}
