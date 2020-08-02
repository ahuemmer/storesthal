package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.complextestobjects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A big bunch of general tests for the whole thing. This version works with annotations (see {@link HALRelation}), but
 * there is a tiny derivation of this test class ({@link AnnotationlessGeneralStoresthalTest}) doing so without.
 */
public class GeneralStoresthalTest extends AbstractJsonTemplateBasedTest {

    /**
     * Reset the statistics and empty the caches before each test run.
     */
    @BeforeEach
    public void init() {
        Storesthal.resetStatistics();
        Storesthal.clearAllCaches();
    }

    /**
     * Make sure, a "complex" object (having some attributes of different data types) can be correctly retrieved.
     * @throws StoresthalException if something fails.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canRetrieveComplexObject() throws StoresthalException, IOException {

        configureServerMock("/complexObjects/1", "complexObject1.json");
        serverMock.start();

        ComplexObject test = Storesthal.getObject("http://localhost:"+serverMock.port()+"/complexObjects/1", ComplexObject.class);

        assertNotNull(test);
        assertEquals(1, test.getCategoryId());
        assertEquals(10157977, test.getColor());
        assertEquals("", test.getComment());
        assertEquals("Test!", test.getName());
        assertEquals(2, test.getNumber());
        assertEquals("expense", test.getType());
    }

    /**
     * Make sure, a "complex" object having one relation to one single child object can be correctly retrieved.
     * @throws StoresthalException if something fails.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canRetrieveComplexObjectWithSingleChild() throws StoresthalException, IOException {
        configureServerMock("/complexObjectsWithSingleChildren/1", "complexObjectWithSingleChild1.json");
        configureServerMock("/complexChildren/1", "simpleObject1.json", Map.of("name","Testchild!", "tags", "[ \"tag_a\", \"tag_b\"]"));

        serverMock.start();

        ComplexObjectWithSingleChild test = Storesthal.getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithSingleChildren/1", ComplexObjectWithSingleChild.class);

        assertNotNull(test);
        assertEquals(4711, test.getCategoryId());
        assertEquals(101579, test.getColor());
        assertEquals("oi...", test.getComment());
        assertEquals("Test2!", test.getName());
        assertEquals(3, test.getNumber());
        assertEquals("income", test.getType());

        ChildObject child = test.getChild();
        assertNotNull(child);
        assertEquals("Testchild!", child.getChildName());
        assertEquals(2, child.getTags().size());
        assertEquals("tag_a", child.getTags().get(0));
        assertEquals("tag_b", child.getTags().get(1));
    }

    /**
     * Make sure, a "complex" object having multiple children (relation implemented as an abstract {@link List} here)
     * can be correctly retrieved.
     * @throws StoresthalException if something fails.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canRetrieveComplexObjectWithMultipleChildren1() throws StoresthalException, IOException {

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "itsme...","categoryId","1508", "name", "Test3!", "number", "9", "type", "neither", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleObject2.json", Map.of("objectId","12345", "name", "Testchild 1!", "tags", "[\"tag\"]"));
        configureServerMock("/complexChildren2/2", "simpleObject2.json", Map.of("objectId","815", "name", "Testchild 2!", "tags", "[ \"green\", \"big\", \"fluffy\"]"));
        configureServerMock("/complexChildren2/3", "simpleObject2.json", Map.of("objectId","4711", "name", "Testchild 3!", "tags", "null"));

        serverMock.start();

        ComplexObjectWithMultipleChildren1 test = Storesthal.getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren1.class);

        assertNotNull(test);
        assertEquals(1508, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("itsme...", test.getComment());
        assertEquals("Test3!", test.getName());
        assertEquals(9, test.getNumber());
        assertEquals("neither", test.getType());

        List<ChildObject> children = test.getChildren();
        assertNotNull(children);
        assertEquals(3, children.size());
        assertEquals("Testchild 1!", children.get(0).getChildName());
        assertEquals("Testchild 2!", children.get(1).getChildName());
        assertEquals("Testchild 3!", children.get(2).getChildName());

        assertEquals(12345, children.get(0).getChildId());
        assertEquals(1, children.get(0).getTags().size());
        assertEquals("tag", children.get(0).getTags().get(0));

        assertEquals(815, children.get(1).getChildId());
        assertEquals(3, children.get(1).getTags().size());
        assertEquals("green", children.get(1).getTags().get(0));
        assertEquals("big", children.get(1).getTags().get(1));
        assertEquals("fluffy", children.get(1).getTags().get(2));

        assertEquals(4711, children.get(2).getChildId());
        assertNull(children.get(2).getTags());
    }

    /**
     * Make sure, a "complex" object having multiple children (relation implemented as a {@link java.util.LinkedList} here)
     * can be correctly retrieved.
     * (Pretty much the same as before (in {@link #canRetrieveComplexObjectWithMultipleChildren1()}), but now we use
     * {@link ComplexObjectWithMultipleChildren2} which has a concrete implementation of a collection
     * ({@link java.util.LinkedList}) instead of an interface.)
     * @throws StoresthalException if something fails.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canRetrieveComplexObjectWithMultipleChildren2() throws StoresthalException, IOException {
        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "hello!", "categoryId","4711", "name", "Blubb", "number", "45648", "type", "type",  "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleObject2.json", Map.of("objectId","12345", "name", "Testchild 1!", "tags", "null"));
        configureServerMock("/complexChildren2/2", "simpleObject2.json", Map.of("objectId","815", "name", "Testchild 2!", "tags", "null"));
        configureServerMock("/complexChildren2/3", "simpleObject2.json", Map.of("objectId","4711", "name", "Testchild 3!", "tags", "null"));

        serverMock.start();

        ComplexObjectWithMultipleChildren2 test = Storesthal.getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren2.class);

        assertNotNull(test);
        assertEquals(4711, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("hello!", test.getComment());
        assertEquals("Blubb", test.getName());
        assertEquals(45648, test.getNumber());
        assertEquals("type", test.getType());

        List<ChildObject> children = test.getChildren();
        assertNotNull(children);
        assertEquals(3, children.size());
        assertEquals("Testchild 1!", children.get(0).getChildName());
        assertEquals("Testchild 2!", children.get(1).getChildName());
        assertEquals("Testchild 3!", children.get(2).getChildName());

        assertEquals(12345, children.get(0).getChildId());
        assertEquals(815, children.get(1).getChildId());
        assertEquals(4711, children.get(2).getChildId());

    }

    /**
     * Make sure, a "complex" object having multiple children (relation implemented as an Array here)
     * canNOT be retrieved (at the moment...).
     * (Pretty much the same as the two before (in {@link #canRetrieveComplexObjectWithMultipleChildren1()} and
     * {@link #canRetrieveComplexObjectWithMultipleChildren2()}), but now we use
     * {@link ComplexObjectWithMultipleChildren3} which has an array as collection instead of a (Linked)List.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canRetrieveComplexObjectWithMultipleChildrenInArray() throws IOException {
        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "", "categoryId","9999", "name", "Xyz123!", "number", "1", "type", "mööööp", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleObject2.json", Map.of("objectId","12345", "name", "Testchild 1!"));
        configureServerMock("/complexChildren2/2", "simpleObject2.json", Map.of("objectId","815", "name", "Testchild 2!"));
        configureServerMock("/complexChildren2/3", "simpleObject2.json", Map.of("objectId","4711", "name", "Testchild 3!"));

        serverMock.start();

        assertThrows(StoresthalException.class, () -> {
            ComplexObjectWithMultipleChildren3 test = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren3.class);
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

    /**
     * Make sure, an object structure of a parent object having multiple children each of which having a back-reference
     * to the parent object can be correctly retrieved and only one single instance of the parent object is created.
     * @throws StoresthalException if something fails.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canRetrieveComplexObjectWithMultipleChildrenAndParentRelation() throws StoresthalException, IOException {

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "887766", "comment", "", "categoryId","12345", "name", "Äußerst umlautig!", "number", "-1",  "type", "This is a type. Is it? Really??? Yes...", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleChildObjectWithParentRelation.json", Map.of("childId","654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/2", "simpleChildObjectWithParentRelation.json", Map.of("childId","158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/3", "simpleChildObjectWithParentRelation.json", Map.of("childId","1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

        serverMock.start();

        Storesthal.resetStatistics();

        ComplexObjectWithMultipleChildren4 test = Storesthal.getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren4.class);

        assertNotNull(test);
        assertEquals(12345, test.getCategoryId());
        assertEquals(887766, test.getColor());
        assertEquals("", test.getComment());
        assertEquals("Äußerst umlautig!", test.getName());
        assertEquals(-1, test.getNumber());
        assertEquals("This is a type. Is it? Really??? Yes...", test.getType());

        List<ChildObjectWithParentRelation> children = test.getChildren();
        assertNotNull(children);
        assertEquals(3, children.size());
        assertEquals("Testchild with parent 1.", children.get(0).getChildName());
        assertEquals("Testchild with parent 2.", children.get(1).getChildName());
        assertEquals("Testchild with parent 3.", children.get(2).getChildName());

        assertEquals(654321, children.get(0).getChildId());
        assertEquals(158, children.get(1).getChildId());
        assertEquals(1147, children.get(2).getChildId());

        Storesthal.printStatistics();

        assertEquals(4,(Integer) Storesthal.getStatistics().get("httpCalls"));

        for (ChildObjectWithParentRelation child: children) {
            //Use == here --> really the same object!
            assertSame(test, child.getParent());
        }
    }

    /**
     * Make sure, an object structure of a parent object having multiple children each of which having a back-reference
     * to a collection of parent objects can be correctly retrieved and only one single instance of the parent object is
     * created.
     * (This is pretty much like {@link #canRetrieveComplexObjectWithMultipleChildrenAndParentRelation()} above, but
     * here the possible parents are stored in a collection inside the child objects.)
     * @throws StoresthalException if something fails.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canRetrieveComplexObjectWithMultipleChildrenAndParentRelationCollection() throws StoresthalException, IOException {

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "456456", "comment", "abcABC", "categoryId","123459876", "name", "Das ist ein Name.", "number", "12", "type", "", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren2/1",
                "http://localhost:${port}/complexChildren2/2",
                "http://localhost:${port}/complexChildren2/3"}
        ), "parent", ""));
        configureServerMock("/complexChildren2/1", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/2", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/3", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

        serverMock.start();

        Storesthal.resetStatistics();

        ComplexObjectWithMultipleChildren5 test = Storesthal.getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren5.class);

        assertNotNull(test);
        assertEquals(123459876, test.getCategoryId());
        assertEquals(456456, test.getColor());
        assertEquals("abcABC", test.getComment());
        assertEquals("Das ist ein Name.", test.getName());
        assertEquals(12, test.getNumber());
        assertEquals("", test.getType());

        List<ChildObjectWithParentRelationCollection> children = test.getChildren();
        assertNotNull(test.getChildren());
        assertEquals(3, test.getChildren().size());


        assertEquals("Testchild with parent 1.", children.get(0).getChildName());
        assertEquals("Testchild with parent 2.", children.get(1).getChildName());
        assertEquals("Testchild with parent 3.", children.get(2).getChildName());

        assertEquals(654321, children.get(0).getChildId());
        assertEquals(158, children.get(1).getChildId());
        assertEquals(1147, children.get(2).getChildId());

        Storesthal.printStatistics();

        assertEquals(4,(Integer) Storesthal.getStatistics().get("httpCalls"));

        for (ChildObjectWithParentRelationCollection child: children) {
            assertEquals(1, child.getParents().size());
            //Use == here --> really the same object!
            assertSame(test, child.getParents().get(0));
        }
    }

    /**
     * Make sure, a very complex object structure with many levels of relation can be correctly retrieved.
     * Please note: The test structure created is not completely "logic" in a sense of correct
     * "parent-child-grandchild"-relations. This is intentional as it allows testing such structures as well.
     * @throws StoresthalException if something fails.
     * @throws IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    public void canHandleVeryComplexObjectStructure() throws IOException, StoresthalException {
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
        configureServerMock("/complexChildren3/2", "complexObjectWithMultipleChildren1.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId","789456123", "name", "of...","number","-7894", "type", "*", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/1\"}"));
        configureServerMock("/complexChildren3/3", "complexObjectWithMultipleChildren1.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId","0", "name", "mind!","number","574389", "type", "${myType}", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMock("/complexChildren3/4", "complexObjectWithMultipleChildren1.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId","55", "name", "Lorem","number","1186","type", "Object Mark IV", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/3\"}"));
        configureServerMock("/complexChildren3/5", "complexObjectWithMultipleChildren1.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId","3521", "name", "ipsum","number","-7561","type", "Knödel", "children", createJsonHrefArray(new String[] {}), "parent", ""));

        serverMock.start();

        Storesthal.resetStatistics();

        ComplexObjectWithMultipleChildren6 test = Storesthal.getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren6.class);

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
        assertSame(subChild1, subChild.getParent());
        assertNull(subChild.getChildren());


        //  END Subchild nr. 3.1

        //  Subchild nr 3.2

        subChild = test.getChildren().get(2).getChildren().get(1);
        assertEquals(574389, subChild.getNumber());
        assertEquals(818147, subChild.getColor());
        assertEquals("I'm the third subchild", subChild.getComment());
        assertNull(subChild.getParent());
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
        assertSame(subChild.getParent(),test.getChildren().get(2).getChildren().get(1));
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

    @Test
    public void canRetrieveCollections() throws IOException, StoresthalException {
        configureServerMock("/collection/coll", "collection.json");
        serverMock.start();

        Storesthal.resetStatistics();

        ArrayList<ChildObject> children = Storesthal.getCollection("http://localhost:"+serverMock.port()+"/collection/coll", ChildObject.class);

        assertEquals(4,children.size());
        assertEquals(759034, children.get(2).getChildId());
        assertEquals("collObject673896873", children.get(3).getChildName());
    }

    @Test
    public void canRetrieveComplexCollections() throws IOException, StoresthalException {
        configureServerMock("/collection/coll", "complexCollection.json",
                Map.of("children1432",
                        createJsonHrefArray(new String[] {
                                "http://localhost:${port}/complexChildren2/1",
                                "http://localhost:${port}/complexChildren2/2",
                                "http://localhost:${port}/complexChildren2/3"}
                        ),
                        "children52",
                        createJsonHrefArray(new String[] {
                                "http://localhost:${port}/complexChildren2/4",
                                "http://localhost:${port}/complexChildren2/5"}
                        ),
                        "children7486465",
                        createJsonHrefArray(new String[] {
                                "http://localhost:${port}/complexChildren2/6",
                                "http://localhost:${port}/complexChildren2/1"}
                        )
                        , "parent", "", "types", "[]"));

        configureServerMock("/complexChildren2/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "1345", "comment", "Number 1...", "categoryId","5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/1"
                }
        ), "parent", ""));
        configureServerMock("/complexChildren2/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "584390", "comment", "Number 2...", "categoryId","1", "name", "just...","number","8", "types", "[\"xyxyxy\"]", "children", createJsonHrefArray(new String[] {
                }
        ), "parent", ""));
        configureServerMock("/complexChildren2/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "468", "comment", "Number 3...", "categoryId","1111", "name", "a...","number","-24","types","[\"3\", \"blah\", \"pups\"]","children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/2",
                        "http://localhost:${port}/complexChildren3/3",
                        "http://localhost:${port}/complexChildren3/4",
                        "http://localhost:${port}/complexChildren3/5"
                }
        ), "parent", ""));
        configureServerMock("/complexChildren2/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "5431", "comment", "Number 4...", "categoryId","5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/6"
                }
        ), "parent", ""));
        configureServerMock("/complexChildren2/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "43289", "comment", "Number 5...", "categoryId","10101", "name", "blah", "number", "45465", "types", "[\"some type\"]", "children", createJsonHrefArray(new String[] {}
        ), "parent", ""));
        configureServerMock("/complexChildren2/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "5324", "comment", "Number [6]...", "categoryId","5234789", "name", "5834543", "number", "-17", "types", "[]", "children", createJsonHrefArray(new String[] {
                "http://localhost:${port}/complexChildren3/3"
                }
        ), "parent", ""));


        configureServerMock("/complexChildren3/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "747474", "comment", "I'm the first subchild", "categoryId","10000", "name", "state...","number","null","types", "[\"   \"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/1\"}"));
        configureServerMock("/complexChildren3/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId","789456123", "name", "of...","number","-7894", "types", "[\"*\"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/1\"}"));
        configureServerMock("/complexChildren3/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId","0", "name", "mind!","number","574389", "types", "[\"${myType}\"]", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMock("/complexChildren3/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId","55", "name", "Lorem","number","1186","types", "[\"Object Mark IV\"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/3\"}"));
        configureServerMock("/complexChildren3/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId","3521", "name", "ipsum","number","-7561","types", "[\"Knödel\"]", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMock("/complexChildren3/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "456123", "comment", "I'm the sixth subchild", "categoryId","2323", "name", "dolor","number","5743534","types", "[\"Knödel\", \"heyho\"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/4\"}"));

        serverMock.start();

        System.out.println("http://localhost:"+serverMock.port()+"/collection/coll");

/*        try {
            Thread.sleep(30000);
        }
        catch (Exception e) {}*/

        Storesthal.resetStatistics();

        ArrayList<ComplexObjectWithMultipleChildren7> objects = Storesthal.getCollection("http://localhost:"+serverMock.port()+"/collection/coll", ComplexObjectWithMultipleChildren7.class);

        assertEquals(3,objects.size());

        // Object nr. 1
        ComplexObjectWithMultipleChildren7 test = objects.get(0);
        assertEquals(234, test.getCategoryId());
        assertEquals("Object No. 1", test.getComment());
        assertEquals(43432, test.getColor());
        assertEquals(3, test.getChildren().size());
        assertEquals(543890, test.getNumber());
        assertEquals("complexCollObject1432", test.getName());
        assertEquals(0, test.getTypes().size());

        //Object nr. 1, Child nr. 1

        assertEquals(1345,test.getChildren().get(0).getColor());
        assertEquals("is...", test.getChildren().get(0).getName());
        assertEquals("Number 1...",test.getChildren().get(0).getComment());
        assertEquals(5,test.getChildren().get(0).getCategoryId());
        assertEquals(1,test.getChildren().get(0).getChildren().size());
        assertEquals(5547, test.getChildren().get(0).getNumber());
        assertEquals(1, test.getChildren().get(0).getTypes().size());
        assertEquals("$myGreatType", test.getChildren().get(0).getTypes().get(0));

        //  Subchild nr. 1.1

        ComplexObjectWithMultipleChildren7 subChild1 = test.getChildren().get(0).getChildren().get(0);
        assertEquals(747474, subChild1.getColor());
        assertEquals("I'm the first subchild", subChild1.getComment());
        assertNotNull(subChild1.getParent());
        assertSame(subChild1.getParent(), test.getChildren().get(0));
        assertEquals(10000, subChild1.getCategoryId());
        assertEquals("state...", subChild1.getName());
        assertEquals(1, subChild1.getTypes().size());
        assertEquals("   ", subChild1.getTypes().get(0));
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
        assertEquals(1, test.getChildren().get(1).getTypes().size());
        assertEquals("xyxyxy", test.getChildren().get(1).getTypes().get(0));

        //(Child nr. 2 has no subchildren...)

        //END Child nr. 2


        //Child nr. 3
        assertEquals(468,test.getChildren().get(2).getColor());
        assertEquals("a...", test.getChildren().get(2).getName());
        assertEquals("Number 3...",test.getChildren().get(2).getComment());
        assertEquals(1111,test.getChildren().get(2).getCategoryId());
        assertNull(test.getChildren().get(1).getChildren());
        assertEquals(-24, test.getChildren().get(2).getNumber());
        assertEquals(3, test.getChildren().get(2).getTypes().size());
        assertEquals("3", test.getChildren().get(2).getTypes().get(0));
        assertEquals("blah", test.getChildren().get(2).getTypes().get(1));
        assertEquals("pups", test.getChildren().get(2).getTypes().get(2));
        assertEquals(4, test.getChildren().get(2).getChildren().size());

        //  Subchild nr 3.1

        ComplexObjectWithMultipleChildren7 subChild = test.getChildren().get(2).getChildren().get(0);
        assertEquals(-7894, subChild.getNumber());
        assertEquals(3, subChild.getColor());
        assertEquals("I'm the second subchild", subChild.getComment());
        assertEquals(789456123, subChild.getCategoryId());
        assertEquals("of...", subChild.getName());
        assertEquals(1, subChild.getTypes().size());
        assertEquals("*", subChild.getTypes().get(0));
        assertSame(subChild1, subChild.getParent());
        assertNull(subChild.getChildren());


        //  END Subchild nr. 3.1

        //  Subchild nr 3.2

        subChild = test.getChildren().get(2).getChildren().get(1);
        assertEquals(574389, subChild.getNumber());
        assertEquals(818147, subChild.getColor());
        assertEquals("I'm the third subchild", subChild.getComment());
        assertNull(subChild.getParent());
        assertEquals(0, subChild.getCategoryId());
        assertEquals("mind!", subChild.getName());
        assertEquals(1, subChild.getTypes().size());
        assertEquals("${myType}", subChild.getTypes().get(0));
        assertNull(subChild.getChildren());

        //  END Subchild nr. 3.2


        //  Subchild nr 3.3

        subChild = test.getChildren().get(2).getChildren().get(2);
        assertEquals(1186, subChild.getNumber());
        assertEquals(29141, subChild.getColor());
        assertEquals("I'm the fourth subchild", subChild.getComment());
        assertEquals(55, subChild.getCategoryId());
        assertEquals("Lorem", subChild.getName());
        assertEquals(1, subChild.getTypes().size());
        assertEquals("Object Mark IV", subChild.getTypes().get(0));
        assertSame(subChild.getParent(),test.getChildren().get(2).getChildren().get(1));
        assertNull(subChild.getChildren());

        //  END Subchild nr. 3.3

        //  Subchild nr 3.4

        subChild = test.getChildren().get(2).getChildren().get(3);
        assertEquals(-7561, subChild.getNumber());
        assertEquals(222222, subChild.getColor());
        assertEquals("I'm the fifth subchild", subChild.getComment());
        assertEquals(3521, subChild.getCategoryId());
        assertEquals("ipsum", subChild.getName());
        assertEquals(1, subChild.getTypes().size());
        assertEquals("Knödel", subChild.getTypes().get(0));
        assertNull(subChild.getChildren());

        //  END Subchild nr. 3.4

        //END Object nr. 1, Child nr. 3

        //Object nr. 2

        test = objects.get(1);
        assertEquals(438290, test.getCategoryId());
        assertEquals("Object No. 2", test.getComment());
        assertEquals(532, test.getColor());
        assertEquals(2, test.getChildren().size());
        assertEquals(456, test.getNumber());
        assertEquals("complexCollObject52", test.getName());
        assertNull(test.getTypes());

        // Object nr. 2, child nr. 1
        assertEquals(5431,test.getChildren().get(0).getColor());
        assertEquals("is...", test.getChildren().get(0).getName());
        assertEquals("Number 4...",test.getChildren().get(0).getComment());
        assertEquals(5,test.getChildren().get(0).getCategoryId());
        assertEquals(1,test.getChildren().get(0).getChildren().size());
        assertEquals(5547, test.getChildren().get(0).getNumber());
        assertEquals(1, test.getChildren().get(0).getTypes().size());
        assertEquals("$myGreatType", test.getChildren().get(0).getTypes().get(0));
        assertEquals(1, test.getChildren().get(0).getChildren().size());

        // Object nr. 2, Child nr. 1, subchild Nr. 1 (the only one)
        subChild = test.getChildren().get(0).getChildren().get(0);
        assertEquals(5743534, subChild.getNumber());
        assertEquals(456123, subChild.getColor());
        assertEquals("I'm the sixth subchild", subChild.getComment());
        assertEquals(2323, subChild.getCategoryId());
        assertEquals("dolor", subChild.getName());
        assertEquals(2, subChild.getTypes().size());
        assertEquals("Knödel", subChild.getTypes().get(0));
        assertEquals("heyho", subChild.getTypes().get(1));
        assertNull(subChild.getChildren());


        // Object nr. 2, Child nr. 2
        assertEquals(43289,test.getChildren().get(1).getColor());
        assertEquals("blah", test.getChildren().get(1).getName());
        assertEquals("Number 5...",test.getChildren().get(1).getComment());
        assertEquals(10101,test.getChildren().get(1).getCategoryId());
        assertEquals(45465, test.getChildren().get(1).getNumber());
        assertEquals(1, test.getChildren().get(1).getTypes().size());
        assertEquals("some type", test.getChildren().get(1).getTypes().get(0));

        // Object nr. 2, child nr. 2 has no children
        assertNull(test.getChildren().get(1).getChildren());

        // Object nr. 3
        test = objects.get(2);
        assertEquals(543890, test.getCategoryId());
        assertNull(test.getComment());
        assertNull(test.getColor());
        assertEquals(2, test.getChildren().size());
        assertEquals(542, test.getNumber());
        assertEquals("complexCollObject7486465", test.getName());
        assertEquals(4, test.getTypes().size());
        assertEquals("some ", test.getTypes().get(0));
        assertEquals("list ", test.getTypes().get(1));
        assertEquals("of   ", test.getTypes().get(2));
        assertEquals("types", test.getTypes().get(3));

        //Object nr. 3, Child nr. 1

        assertEquals(5324,test.getChildren().get(0).getColor());
        assertEquals("5834543", test.getChildren().get(0).getName());
        assertEquals("Number [6]...",test.getChildren().get(0).getComment());
        assertEquals(5234789,test.getChildren().get(0).getCategoryId());
        assertEquals(-17, test.getChildren().get(0).getNumber());
        assertEquals(0, test.getChildren().get(0).getTypes().size());
        assertEquals(1, test.getChildren().get(0).getChildren().size());

        // Object nr. 3, child nr. 1 has one children the same as object nr. 1, Child nr. 3
        subChild = test.getChildren().get(0).getChildren().get(0);
        assertSame(subChild, objects.get(0).getChildren().get(2).getChildren().get(1));
        assertEquals(574389, subChild.getNumber());
        assertEquals(818147, subChild.getColor());
        assertEquals("I'm the third subchild", subChild.getComment());
        assertNull(subChild.getParent());
        assertEquals(0, subChild.getCategoryId());
        assertEquals("mind!", subChild.getName());
        assertEquals(1, subChild.getTypes().size());
        assertEquals("${myType}", subChild.getTypes().get(0));
        assertNull(subChild.getChildren());


        //Object nr. 3, child nr. 2 equals object nr. 1, child nr. 1!
        //Object nr. 3, Child nr. 2
        test = objects.get(2).getChildren().get(1);
        assertSame(test, objects.get(0).getChildren().get(0));
        assertEquals(1345,test.getColor());
        assertEquals("is...", test.getName());
        assertEquals("Number 1...",test.getComment());
        assertEquals(5,test.getCategoryId());
        assertEquals(1,test.getChildren().size());
        assertEquals(5547, test.getNumber());
        assertEquals(1, test.getTypes().size());
        assertEquals("$myGreatType", test.getTypes().get(0));

        //  Subchild nr. 3.1

        subChild1 = test.getChildren().get(0);
        assertSame(subChild1, objects.get(0).getChildren().get(0).getChildren().get(0));
        assertEquals(747474, subChild1.getColor());
        assertEquals("I'm the first subchild", subChild1.getComment());
        assertNotNull(subChild1.getParent());
        assertSame(subChild1.getParent(), test);
        assertEquals(10000, subChild1.getCategoryId());
        assertEquals("state...", subChild1.getName());
        assertEquals(1, subChild1.getTypes().size());
        assertEquals("   ", subChild1.getTypes().get(0));
        assertNull(subChild1.getChildren());
        assertNull(subChild1.getNumber());

        //  End subchild nr. 3.1


        assertEquals(13, Storesthal.getStatistics().get("httpCalls"));

        Storesthal.resetStatistics();

    }

}
