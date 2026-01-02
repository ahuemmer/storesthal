package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.complextestobjects.ChildObject;
import com.github.ahuemmer.storesthal.complextestobjects.ChildObjectWithParentRelation;
import com.github.ahuemmer.storesthal.complextestobjects.ChildObjectWithParentRelationCollection;
import com.github.ahuemmer.storesthal.complextestobjects.ChildObjectWithParentRelationWithLinks;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObject;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren1;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren2;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren3;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren4;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren5;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren6;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildrenWithLinks1;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildrenWithLinks2;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildrenWithLinks3;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildrenWithLinks4;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildrenWithLinks5;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildrenWithLinks6;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithSingleChild;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A big bunch of general tests for the whole thing. This version works with annotations (see {@link HALRelation}), but
 * there is a tiny derivation of this test class ({@link AnnotationlessGeneralStoresthalTest}) doing so without.
 */
public class GeneralStoresthalTest extends AbstractJsonTemplateBasedTest {

    @Nested
    @DisplayName("complex object handling")
    public class Complex_object_handling {

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
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object")
        public void retrieves_a_complex_object() throws StoresthalException, IOException {

            configureServerMockWithResponseFile("/complexObjects/1", "complexObject1.json");
            serverMock.start();

            ComplexObject test = Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjects/1", ComplexObject.class);

            assertNotNull(test);
            assertEquals(1, test.getCategoryId());
            assertEquals(10157977, test.getColor());
            assertEquals("", test.getComment());
            assertEquals("Test!", test.getName());
            assertEquals(2, test.getNumber());
            assertEquals("expense", test.getType());
        }

        /**
         * Make sure, a "complex" object (having some attributes of different data types) can be correctly retrieved maintaining its links.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object_with_its_links")
        public void retrieves_a_complex_object_with_its_links() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/complexObjects/1", "complexObject1.json");
            serverMock.start();

            EntityModel<ComplexObject> test = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjects/1", ComplexObject.class);

            assertNotNull(test);

            assertEquals(1, test.getContent().getCategoryId());
            assertEquals(10157977, test.getContent().getColor());
            assertEquals("", test.getContent().getComment());
            assertEquals("Test!", test.getContent().getName());
            assertEquals(2, test.getContent().getNumber());
            assertEquals("expense", test.getContent().getType());

            assertTrue(test.hasLink("self"));
            assertTrue(test.getLinks().hasSize(1));
            assertTrue(test.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjects/1", test.getLink("self").get().getHref());
        }

        /**
         * Make sure, a "complex" object having one relation to one single child object can be correctly retrieved.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with a single child")
        public void retrieves_a_complex_object_with_a_single_child() throws StoresthalException, IOException {
            configureServerMockWithResponseFile("/complexObjectsWithSingleChildren/1", "complexObjectWithSingleChild1.json");
            configureServerMockWithResponseFile("/complexChildren/1", "simpleObject1.json", Map.of("name", "Testchild!", "tags", "[ \"tag_a\", \"tag_b\"]"));

            serverMock.start();

            ComplexObjectWithSingleChild test = Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjectsWithSingleChildren/1", ComplexObjectWithSingleChild.class);

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
         * Make sure, a "complex" object having one relation to one single child object can be correctly retrieved maintaining its links.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with a single child, maintaining its links")
        public void retrieves_a_complex_object_with_a_single_child_maintaining_its_links() throws StoresthalException, IOException {
            configureServerMockWithResponseFile("/complexObjectsWithSingleChildren/1", "complexObjectWithSingleChild1.json");
            configureServerMockWithResponseFile("/complexChildren/1", "simpleObject1.json", Map.of("name", "Testchild!", "tags", "[ \"tag_a\", \"tag_b\"]"));

            serverMock.start();

            EntityModel<ComplexObjectWithSingleChild> test = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithSingleChildren/1", ComplexObjectWithSingleChild.class);

            assertNotNull(test);
            assertEquals(4711, test.getContent().getCategoryId());
            assertEquals(101579, test.getContent().getColor());
            assertEquals("oi...", test.getContent().getComment());
            assertEquals("Test2!", test.getContent().getName());
            assertEquals(3, test.getContent().getNumber());
            assertEquals("income", test.getContent().getType());

            ChildObject child = test.getContent().getChild();
            assertNotNull(child);
            assertEquals("Testchild!", child.getChildName());
            assertEquals(2, child.getTags().size());
            assertEquals("tag_a", child.getTags().get(0));
            assertEquals("tag_b", child.getTags().get(1));

            assertTrue(test.getLinks().hasSize(2));
            assertTrue(test.hasLink("self"));
            assertTrue(test.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithSingleChildren/1", test.getLink("self").get().getHref());

            assertTrue(test.hasLink("child"));
            assertTrue(test.getLink("child").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren/1", test.getLink("child").get().getHref());
        }

        /**
         * Make sure, a "complex" object having multiple children (relation implemented as an abstract {@link List} here)
         * can be correctly retrieved.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children (1 of 2)")
        public void retrieves_a_complex_object_with_multiple_children__1_of_2() throws StoresthalException, IOException {

            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "itsme...", "categoryId", "1508", "name", "Test3!", "number", "9", "type", "neither", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleObject2.json", Map.of("objectId", "12345", "name", "Testchild 1!", "tags", "[\"tag\"]"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleObject2.json", Map.of("objectId", "815", "name", "Testchild 2!", "tags", "[ \"green\", \"big\", \"fluffy\"]"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleObject2.json", Map.of("objectId", "4711", "name", "Testchild 3!", "tags", "null"));

            serverMock.start();

            ComplexObjectWithMultipleChildren1 test = Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren1.class);

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
         * Make sure, a "complex" object having multiple children (relation implemented as an abstract {@link List} here)
         * can be correctly retrieved. maitaining its links.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children maintaining its links (1 of 2)")
        public void retrieves_a_complex_object_with_multiple_children_maintaining_its_links__1_of_2() throws StoresthalException, IOException {

            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "itsme...", "categoryId", "1508", "name", "Test3!", "number", "9", "type", "neither", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleObject2.json", Map.of("objectId", "12345", "name", "Testchild 1!", "tags", "[\"tag\"]"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleObject2.json", Map.of("objectId", "815", "name", "Testchild 2!", "tags", "[ \"green\", \"big\", \"fluffy\"]"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleObject2.json", Map.of("objectId", "4711", "name", "Testchild 3!", "tags", "null"));

            serverMock.start();

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks1> test = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildrenWithLinks1.class);

            assertNotNull(test);
            assertEquals(1508, test.getContent().getCategoryId());
            assertEquals(22101579, test.getContent().getColor());
            assertEquals("itsme...", test.getContent().getComment());
            assertEquals("Test3!", test.getContent().getName());
            assertEquals(9, test.getContent().getNumber());
            assertEquals("neither", test.getContent().getType());

            assertFalse(test.hasLink("parent"));
            assertTrue(test.hasLink("children"));
            assertTrue(test.getLinks().hasSize(4)); // one "self" link and three "children" links

            assertTrue(test.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", test.getLink("self").get().getHref());

            assertEquals(3, test.getLinks("children").size());

            Set<String> expectedHrefs = Set.of("http://localhost:" + serverMock.port() + "/complexChildren2/1", "http://localhost:" + serverMock.port() + "/complexChildren2/2", "http://localhost:" + serverMock.port() + "/complexChildren2/3");

            assertEquals(0, test.getLinks("children").stream().filter(l -> !expectedHrefs.contains(l.getHref())).count());


            List<EntityModel<ChildObject>> children = test.getContent().getChildren();

            assertNotNull(children);
            assertEquals(3, children.size());
            assertEquals("Testchild 1!", children.get(0).getContent().getChildName());
            assertEquals("Testchild 2!", children.get(1).getContent().getChildName());
            assertEquals("Testchild 3!", children.get(2).getContent().getChildName());

            assertEquals(12345, children.get(0).getContent().getChildId());
            assertEquals(1, children.get(0).getContent().getTags().size());
            assertEquals("tag", children.get(0).getContent().getTags().get(0));

            assertEquals(815, children.get(1).getContent().getChildId());
            assertEquals(3, children.get(1).getContent().getTags().size());
            assertEquals("green", children.get(1).getContent().getTags().get(0));
            assertEquals("big", children.get(1).getContent().getTags().get(1));
            assertEquals("fluffy", children.get(1).getContent().getTags().get(2));

            assertEquals(4711, children.get(2).getContent().getChildId());
            assertNull(children.get(2).getContent().getTags());

            for (int i = 0; i < 3; i++) {
                assertTrue(children.get(i).hasLink("self"));
                assertTrue(children.get(i).getLink("self").isPresent());
                assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/" + +(i + 1), children.get(i).getLink("self").get().getHref());
            }
        }

        /**
         * Make sure, a "complex" object having multiple children (relation implemented as a {@link java.util.LinkedList} here)
         * can be correctly retrieved.
         * (Pretty much the same as before (in {@link #retrieves_a_complex_object_with_multiple_children__1_of_2()} ()}), but now we use
         * {@link ComplexObjectWithMultipleChildren2} which has a concrete implementation of a collection
         * ({@link java.util.LinkedList}) instead of an interface.)
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children (2 of 2)")
        public void retrieves_a_complex_object_with_multiple_children__2_of_2() throws StoresthalException, IOException {
            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "hello!", "categoryId", "4711", "name", "Blubb", "number", "45648", "type", "type", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleObject2.json", Map.of("objectId", "12345", "name", "Testchild 1!", "tags", "null"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleObject2.json", Map.of("objectId", "815", "name", "Testchild 2!", "tags", "null"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleObject2.json", Map.of("objectId", "4711", "name", "Testchild 3!", "tags", "null"));

            serverMock.start();

            ComplexObjectWithMultipleChildren2 test = Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren2.class);

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
         * Make sure, a "complex" object having multiple children (relation implemented as a {@link java.util.LinkedList} here)
         * can be correctly retrieved maintaining its links.
         * (Pretty much the same as before (in {@link #retrieves_a_complex_object_with_multiple_children__1_of_2()} ()}), but now we use
         * {@link ComplexObjectWithMultipleChildren2} which has a concrete implementation of a collection
         * ({@link java.util.LinkedList}) instead of an interface.)
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children maintaining its links (2 of 2)")
        public void retrieves_a_complex_object_with_multiple_children_maintaining_its_links__2_of_2() throws StoresthalException, IOException {
            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "hello!", "categoryId", "4711", "name", "Blubb", "number", "45648", "type", "type", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleObject2.json", Map.of("objectId", "12345", "name", "Testchild 1!", "tags", "null"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleObject2.json", Map.of("objectId", "815", "name", "Testchild 2!", "tags", "null"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleObject2.json", Map.of("objectId", "4711", "name", "Testchild 3!", "tags", "null"));

            serverMock.start();

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks2> test = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildrenWithLinks2.class);

            assertNotNull(test);
            assertEquals(4711, test.getContent().getCategoryId());
            assertEquals(22101579, test.getContent().getColor());
            assertEquals("hello!", test.getContent().getComment());
            assertEquals("Blubb", test.getContent().getName());
            assertEquals(45648, test.getContent().getNumber());
            assertEquals("type", test.getContent().getType());

            List<EntityModel<ChildObject>> children = test.getContent().getChildren();
            assertNotNull(children);
            assertEquals(3, children.size());
            assertEquals("Testchild 1!", children.get(0).getContent().getChildName());
            assertEquals("Testchild 2!", children.get(1).getContent().getChildName());
            assertEquals("Testchild 3!", children.get(2).getContent().getChildName());

            assertEquals(12345, children.get(0).getContent().getChildId());
            assertEquals(815, children.get(1).getContent().getChildId());
            assertEquals(4711, children.get(2).getContent().getChildId());

            for (int i = 0; i < 3; i++) {
                assertTrue(children.get(i).hasLink("self"));
                assertTrue(children.get(i).getLink("self").isPresent());
                assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/" + (i + 1), children.get(i).getLink("self").get().getHref());
            }

        }

        /**
         * Make sure, a "complex" object having multiple children (relation implemented as an Array here)
         * canNOT be retrieved (at the moment...).
         * (Pretty much the same as the two before (in {@link #retrieves_a_complex_object_with_multiple_children__1_of_2()} ()} and
         * {@link #retrieves_a_complex_object_with_multiple_children__2_of_2()} ()}), but now we use
         * {@link ComplexObjectWithMultipleChildren3} which has an array as collection instead of a (Linked)List.
         *
         * @throws IOException if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("does not retrieve a complex object with multiple children in an array")
        public void does_not_retrieve_a_complex_object_with_multiple_children_in_an_array() throws IOException {
            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "", "categoryId", "9999", "name", "Xyz123!", "number", "1", "type", "mööööp", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleObject2.json", Map.of("objectId", "12345", "name", "Testchild 1!"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleObject2.json", Map.of("objectId", "815", "name", "Testchild 2!"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleObject2.json", Map.of("objectId", "4711", "name", "Testchild 3!"));

            serverMock.start();

            assertThrows(StoresthalException.class, () -> Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren3.class));

            //ARRAYS ARE NOT SUPPORTED (YET??)
            //Whenever this is the case, the following assertions should succeed:

        }

        /**
         * Make sure, a "complex" object having multiple children (relation implemented as an Array here)
         * canNOT be retrieved (at the moment...) maintaining its links.
         * (Pretty much the same as the two before (in {@link #retrieves_a_complex_object_with_multiple_children__1_of_2()} ()} and
         * {@link #retrieves_a_complex_object_with_multiple_children__2_of_2()} ()}), but now we use
         * {@link ComplexObjectWithMultipleChildren3} which has an array as collection instead of a (Linked)List.
         *
         * @throws IOException if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("does not retrieve a complex object with multiple children in an array maintaining its links")
        public void does_not_retrieve_a_complex_object_with_multiple_children_in_an_array_maintaining_its_links() throws IOException {
            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "22101579", "comment", "", "categoryId", "9999", "name", "Xyz123!", "number", "1", "type", "mööööp", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleObject2.json", Map.of("objectId", "12345", "name", "Testchild 1!"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleObject2.json", Map.of("objectId", "815", "name", "Testchild 2!"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleObject2.json", Map.of("objectId", "4711", "name", "Testchild 3!"));

            serverMock.start();

            assertThrows(StoresthalException.class, () -> Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildrenWithLinks3.class));

            //ARRAYS ARE NOT SUPPORTED (YET??)
            //Whenever this is the case, the following assertions should succeed:

        }

        /**
         * Make sure, an object structure of a parent object having multiple children each of which having a back-reference
         * to the parent object can be correctly retrieved and only one single instance of the parent object is created.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children and \"parent\" relation")
        public void retrieves_a_complex_object_with_multiple_children_and_parent_relation() throws StoresthalException, IOException {

            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "887766", "comment", "", "categoryId", "12345", "name", "Äußerst umlautig!", "number", "-1", "type", "This is a type. Is it? Really??? Yes...", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleChildObjectWithParentRelation.json", Map.of("childId", "654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleChildObjectWithParentRelation.json", Map.of("childId", "158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleChildObjectWithParentRelation.json", Map.of("childId", "1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

            serverMock.start();

            Storesthal.resetStatistics();

            ComplexObjectWithMultipleChildren4 test = Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren4.class);

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

            assertEquals(4, (Integer) Storesthal.getStatistics().get("httpCalls"));

            for (ChildObjectWithParentRelation child : children) {
                //Use == here --> really the same object!
                assertSame(test, child.getParent());
            }
        }

        /**
         * Make sure, an object structure of a parent object having multiple children each of which having a back-reference
         * to the parent object can be correctly retrieved and only one single instance of the parent object is created,
         * maintaining its links.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children and \"parent\" relation maintaining its links")
        public void retrieves_a_complex_object_with_multiple_children_and_parent_relation_maintaining_its_links() throws StoresthalException, IOException {

            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "887766", "comment", "", "categoryId", "12345", "name", "Äußerst umlautig!", "number", "-1", "type", "This is a type. Is it? Really??? Yes...", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleChildObjectWithParentRelation.json", Map.of("childId", "654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleChildObjectWithParentRelation.json", Map.of("childId", "158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleChildObjectWithParentRelation.json", Map.of("childId", "1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

            serverMock.start();

            Storesthal.resetStatistics();

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks4> test = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildrenWithLinks4.class);

            assertNotNull(test);
            assertEquals(12345, test.getContent().getCategoryId());
            assertEquals(887766, test.getContent().getColor());
            assertEquals("", test.getContent().getComment());
            assertEquals("Äußerst umlautig!", test.getContent().getName());
            assertEquals(-1, test.getContent().getNumber());
            assertEquals("This is a type. Is it? Really??? Yes...", test.getContent().getType());

            List<EntityModel<ChildObjectWithParentRelationWithLinks>> children = test.getContent().getChildren();
            assertNotNull(children);
            assertEquals(3, children.size());
            assertEquals("Testchild with parent 1.", children.get(0).getContent().getChildName());
            assertEquals("Testchild with parent 2.", children.get(1).getContent().getChildName());
            assertEquals("Testchild with parent 3.", children.get(2).getContent().getChildName());

            assertEquals(654321, children.get(0).getContent().getChildId());
            assertEquals(158, children.get(1).getContent().getChildId());
            assertEquals(1147, children.get(2).getContent().getChildId());

            Storesthal.printStatistics();

            assertEquals(4, (Integer) Storesthal.getStatistics().get("httpCalls"));

            for (EntityModel<ChildObjectWithParentRelationWithLinks> child : children) {
                //Use == here --> really the same object!
                assertSame(test, child.getContent().getParent());
            }

            for (int i = 0; i < 3; i++) {
                assertTrue(children.get(i).hasLink("self"));
                assertTrue(children.get(i).getLink("self").isPresent());
                assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/" + (i + 1), children.get(i).getLink("self").get().getHref());

                assertTrue(children.get(i).hasLink("parent"));
                assertTrue(children.get(i).getLink("parent").isPresent());
                assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", children.get(i).getLink("parent").get().getHref());
            }

        }

        /**
         * Make sure, an object structure of a parent object having multiple children each of which having a back-reference
         * to a collection of parent objects can be correctly retrieved and only one single instance of the parent object is
         * created.
         * (This is pretty much like {@link #retrieves_a_complex_object_with_multiple_children_and_parent_relation()} ()} above, but
         * here the possible parents are stored in a collection inside the child objects.)
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children and \"parent\" collection")
        public void retrieves_a_complex_object_with_multiple_children_and_parent_relation_collection() throws StoresthalException, IOException {

            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "456456", "comment", "abcABC", "categoryId", "123459876", "name", "Das ist ein Name.", "number", "12", "type", "", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId", "654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId", "158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId", "1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

            serverMock.start();

            Storesthal.resetStatistics();

            ComplexObjectWithMultipleChildren5 test = Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren5.class);

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

            assertEquals(4, (Integer) Storesthal.getStatistics().get("httpCalls"));

            for (ChildObjectWithParentRelationCollection child : children) {
                assertEquals(1, child.getParents().size());
                //Use == here --> really the same object!
                assertSame(test, child.getParents().get(0));
            }
        }

        /**
         * Make sure, an object structure of a parent object having multiple children each of which having a back-reference
         * to a collection of parent objects can be correctly retrieved and only one single instance of the parent object is
         * created and the links of the object are maintained.
         * (This is pretty much like {@link #retrieves_a_complex_object_with_multiple_children_and_parent_relation()} ()} above, but
         * here the possible parents are stored in a collection inside the child objects.)
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("retrieves a complex object with multiple children and \"parent\" collection, maintaining its links")
        public void retrieves_a_complex_object_with_multiple_children_and_parent_relation_collection_maintaining_its_links() throws StoresthalException, IOException {

            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "456456", "comment", "abcABC", "categoryId", "123459876", "name", "Das ist ein Name.", "number", "12", "type", "", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId", "654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/2", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId", "158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
            configureServerMockWithResponseFile("/complexChildren2/3", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId", "1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

            serverMock.start();

            Storesthal.resetStatistics();

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks5> test = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildrenWithLinks5.class);

            assertNotNull(test);
            assertEquals(123459876, test.getContent().getCategoryId());
            assertEquals(456456, test.getContent().getColor());
            assertEquals("abcABC", test.getContent().getComment());
            assertEquals("Das ist ein Name.", test.getContent().getName());
            assertEquals(12, test.getContent().getNumber());
            assertEquals("", test.getContent().getType());

            List<EntityModel<ChildObjectWithParentRelationCollection>> children = test.getContent().getChildren();
            assertNotNull(test.getContent().getChildren());
            assertEquals(3, test.getContent().getChildren().size());

            assertTrue(test.hasLink("self"));
            assertTrue(test.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", test.getLink("self").get().getHref());


            assertEquals(654321, children.get(0).getContent().getChildId());
            assertEquals(158, children.get(1).getContent().getChildId());
            assertEquals(1147, children.get(2).getContent().getChildId());

            Storesthal.printStatistics();

            assertEquals(4, (Integer) Storesthal.getStatistics().get("httpCalls"));

            for (EntityModel<ChildObjectWithParentRelationCollection> child : children) {
                assertEquals(1, child.getContent().getParents().size());
                //Use == here --> really the same object!
                assertSame(test, child.getContent().getParents().get(0));
            }

            for (int i = 0; i < 3; i++) {

                assertEquals("Testchild with parent " + (i + 1) + ".", children.get(i).getContent().getChildName());

                assertTrue(children.get(i).hasLink("self"));
                assertTrue(children.get(i).getLink("self").isPresent());
                assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/" + (i + 1), children.get(i).getLink("self").get().getHref());
            }

        }

        // TODO: Weitermachen, es sind noch nicht alle Methoden (insb. Collections) auf "mit Links" umgestellt.
        //       Am besten zunächst auch die Tests weiter anpassen, bis einer nicht mehr funktioniert.
        //       Weiterhin: Können Child-Objekte auch EntityModels (mit Links!) sein und werden diese dann "automatisch"
        //       richtig behandelt??

        /**
         * Make sure, a very complex object structure with many levels of relation can be correctly retrieved.
         * Please note: The test structure created is not completely "logic" in a sense of correct
         * "parent-child-grandchild"-relations. This is intentional as it allows testing such structures as well.
         *
         * @throws StoresthalException if something fails.
         * @throws IOException         if the JSON template for the mocked service answer can't be accessed.
         */
        @Test
        @DisplayName("handles very complex object structures")
        public void handles_very_complex_object_structures() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "0", "comment", ".", "categoryId", "2", "name", "Complexity...", "number", "14", "type", "987epyt", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "1345", "comment", "Number 1...", "categoryId", "5", "name", "is...", "number", "5547", "type", "$myGreatType", "children", createJsonHrefArray(new String[]{
                            "http://localhost:${port}/complexChildren3/1"
                    }
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/2", "complexObjectWithMultipleChildren1.json", Map.of("color", "584390", "comment", "Number 2...", "categoryId", "1", "name", "just...", "number", "8", "type", "xyxyxy", "children", createJsonHrefArray(new String[]{
                    }
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/3", "complexObjectWithMultipleChildren1.json", Map.of("color", "468", "comment", "Number 3...", "categoryId", "1111", "name", "a...", "number", "-24", "type", "3", "children", createJsonHrefArray(new String[]{
                            "http://localhost:${port}/complexChildren3/2",
                            "http://localhost:${port}/complexChildren3/3",
                            "http://localhost:${port}/complexChildren3/4",
                            "http://localhost:${port}/complexChildren3/5"
                    }
            ), "parent", ""));

            configureServerMockWithResponseFile("/complexChildren3/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "747474", "comment", "I'm the first subchild", "categoryId", "10000", "name", "state...", "number", "null", "type", "   ", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/1\"}"));
            configureServerMockWithResponseFile("/complexChildren3/2", "complexObjectWithMultipleChildren1.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId", "789456123", "name", "of...", "number", "-7894", "type", "*", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/1\"}"));
            configureServerMockWithResponseFile("/complexChildren3/3", "complexObjectWithMultipleChildren1.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId", "0", "name", "mind!", "number", "574389", "type", "${myType}", "children", createJsonHrefArray(new String[]{}), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren3/4", "complexObjectWithMultipleChildren1.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId", "55", "name", "Lorem", "number", "1186", "type", "Object Mark IV", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/3\"}"));
            configureServerMockWithResponseFile("/complexChildren3/5", "complexObjectWithMultipleChildren1.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId", "3521", "name", "ipsum", "number", "-7561", "type", "Knödel", "children", createJsonHrefArray(new String[]{}), "parent", ""));

            serverMock.start();

            Storesthal.resetStatistics();

            ComplexObjectWithMultipleChildren6 test = Storesthal.getObjectWithoutLinks("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren6.class);

            assertEquals(0, test.getColor());
            assertEquals(".", test.getComment());
            assertEquals(2, test.getCategoryId());
            assertEquals(3, test.getChildren().size());
            assertEquals(14, test.getNumber());
            assertEquals("Complexity...", test.getName());
            assertEquals("987epyt", test.getType());

            assertNull(test.getParent());

            //Child nr. 1

            assertEquals(1345, test.getChildren().get(0).getColor());
            assertEquals("is...", test.getChildren().get(0).getName());
            assertEquals("Number 1...", test.getChildren().get(0).getComment());
            assertEquals(5, test.getChildren().get(0).getCategoryId());
            assertEquals(1, test.getChildren().get(0).getChildren().size());
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

            assertEquals(584390, test.getChildren().get(1).getColor());
            assertEquals("just...", test.getChildren().get(1).getName());
            assertEquals("Number 2...", test.getChildren().get(1).getComment());
            assertEquals(1, test.getChildren().get(1).getCategoryId());
            assertNull(test.getChildren().get(1).getChildren());
            assertEquals(8, test.getChildren().get(1).getNumber());
            assertEquals("xyxyxy", test.getChildren().get(1).getType());

            //(Child nr. 2 has no subchildren...)

            //END Child nr. 2

            //Child nr. 3
            assertEquals(468, test.getChildren().get(2).getColor());
            assertEquals("a...", test.getChildren().get(2).getName());
            assertEquals("Number 3...", test.getChildren().get(2).getComment());
            assertEquals(1111, test.getChildren().get(2).getCategoryId());
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
            assertSame(subChild.getParent(), test.getChildren().get(2).getChildren().get(1));
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
        @DisplayName("handles very complex object structures, maintaining their links")
        public void handles_very_complex_object_structures_maintaining_their_links() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "0", "comment", ".", "categoryId", "2", "name", "Complexity...", "number", "14", "type", "987epyt", "children", createJsonHrefArray(new String[]{
                    "http://localhost:${port}/complexChildren2/1",
                    "http://localhost:${port}/complexChildren2/2",
                    "http://localhost:${port}/complexChildren2/3"}
            ), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren2/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "1345", "comment", "Number 1...", "categoryId", "5", "name", "is...", "number", "5547", "type", "$myGreatType", "children", createJsonHrefArray(new String[]{
                            "http://localhost:${port}/complexChildren3/1"
                    }
            ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexObjectsWithMultipleChildren1/1\"}"));
            configureServerMockWithResponseFile("/complexChildren2/2", "complexObjectWithMultipleChildren1.json", Map.of("color", "584390", "comment", "Number 2...", "categoryId", "1", "name", "just...", "number", "8", "type", "xyxyxy", "children", createJsonHrefArray(new String[]{
                    }
            ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexObjectsWithMultipleChildren1/1\"}"));
            configureServerMockWithResponseFile("/complexChildren2/3", "complexObjectWithMultipleChildren1.json", Map.of("color", "468", "comment", "Number 3...", "categoryId", "1111", "name", "a...", "number", "-24", "type", "3", "children", createJsonHrefArray(new String[]{
                            "http://localhost:${port}/complexChildren3/2",
                            "http://localhost:${port}/complexChildren3/3",
                            "http://localhost:${port}/complexChildren3/4",
                            "http://localhost:${port}/complexChildren3/5"
                    }
            ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexObjectsWithMultipleChildren1/1\"}"));

            configureServerMockWithResponseFile("/complexChildren3/1", "complexObjectWithMultipleChildren1.json", Map.of("color", "747474", "comment", "I'm the first subchild", "categoryId", "10000", "name", "state...", "number", "null", "type", "   ", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/1\"}"));
            configureServerMockWithResponseFile("/complexChildren3/2", "complexObjectWithMultipleChildren1.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId", "789456123", "name", "of...", "number", "-7894", "type", "*", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/1\"}"));
            configureServerMockWithResponseFile("/complexChildren3/3", "complexObjectWithMultipleChildren1.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId", "0", "name", "mind!", "number", "574389", "type", "${myType}", "children", createJsonHrefArray(new String[]{}), "parent", ""));
            configureServerMockWithResponseFile("/complexChildren3/4", "complexObjectWithMultipleChildren1.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId", "55", "name", "Lorem", "number", "1186", "type", "Object Mark IV", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/3\"}"));
            configureServerMockWithResponseFile("/complexChildren3/5", "complexObjectWithMultipleChildren1.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId", "3521", "name", "ipsum", "number", "-7561", "type", "Knödel", "children", createJsonHrefArray(new String[]{}), "parent", ""));

            serverMock.start();

            Storesthal.resetStatistics();

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> rootObject = Storesthal.getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildrenWithLinks6.class);

            assertEquals(0, rootObject.getContent().getColor());
            assertEquals(".", rootObject.getContent().getComment());
            assertEquals(2, rootObject.getContent().getCategoryId());
            assertEquals(3, rootObject.getContent().getChildren().size());
            assertEquals(14, rootObject.getContent().getNumber());
            assertEquals("Complexity...", rootObject.getContent().getName());
            assertEquals("987epyt", rootObject.getContent().getType());

            assertNull(rootObject.getContent().getParent());

            assertTrue(rootObject.hasLink("self"));
            assertTrue(rootObject.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", rootObject.getLink("self").get().getHref());

            assertFalse(rootObject.hasLink("parent"));


            //Child nr. 1

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> child_1 = rootObject.getContent().getChildren().get(0);

            assertEquals(1345, child_1.getContent().getColor());
            assertEquals("is...", child_1.getContent().getName());
            assertEquals("Number 1...", child_1.getContent().getComment());
            assertEquals(5, child_1.getContent().getCategoryId());
            assertEquals(1, child_1.getContent().getChildren().size());
            assertEquals(5547, child_1.getContent().getNumber());
            assertEquals("$myGreatType", child_1.getContent().getType());

            assertSame(rootObject, child_1.getContent().getParent());

            assertTrue(child_1.hasLink("self"));
            assertTrue(child_1.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/1", child_1.getLink("self").get().getHref());

            assertTrue(child_1.hasLink("parent"));
            assertTrue(child_1.getLink("parent").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", child_1.getLink("parent").get().getHref());


            //  Subchild nr. 1.1

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> subChild_1_1 = child_1.getContent().getChildren().get(0);
            assertEquals(747474, subChild_1_1.getContent().getColor());
            assertEquals("I'm the first subchild", subChild_1_1.getContent().getComment());
            assertNotNull(subChild_1_1.getContent().getParent());
            assertSame(subChild_1_1.getContent().getParent(), child_1);
            assertEquals(10000, subChild_1_1.getContent().getCategoryId());
            assertEquals("state...", subChild_1_1.getContent().getName());
            assertEquals("   ", subChild_1_1.getContent().getType());
            assertNull(subChild_1_1.getContent().getChildren());
            assertNull(subChild_1_1.getContent().getNumber());

            assertSame(subChild_1_1.getContent().getParent(), child_1);

            assertTrue(subChild_1_1.hasLink("self"));
            assertTrue(subChild_1_1.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/1", subChild_1_1.getLink("self").get().getHref());

            assertTrue(subChild_1_1.hasLink("parent"));
            assertTrue(subChild_1_1.getLink("parent").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/1", subChild_1_1.getLink("parent").get().getHref());

            //  End subchild nr. 1.1

            // END Child nr. 1


            // Child nr. 2

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> child_2 = rootObject.getContent().getChildren().get(1);

            assertEquals(584390, child_2.getContent().getColor());
            assertEquals("just...", child_2.getContent().getName());
            assertEquals("Number 2...", child_2.getContent().getComment());
            assertEquals(1, child_2.getContent().getCategoryId());
            assertNull(child_2.getContent().getChildren());
            assertEquals(8, child_2.getContent().getNumber());
            assertEquals("xyxyxy", child_2.getContent().getType());

            assertSame(rootObject, child_2.getContent().getParent());

            assertTrue(child_2.hasLink("self"));
            assertTrue(child_2.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/2", child_2.getLink("self").get().getHref());

            assertTrue(child_2.hasLink("parent"));
            assertTrue(child_2.getLink("parent").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", child_2.getLink("parent").get().getHref());

            // (Child nr. 2 has no subchildren...)

            // END Child nr. 2

            // Child nr. 3

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> child_3 = rootObject.getContent().getChildren().get(2);

            assertEquals(468, child_3.getContent().getColor());
            assertEquals("a...", child_3.getContent().getName());
            assertEquals("Number 3...", child_3.getContent().getComment());
            assertEquals(1111, child_3.getContent().getCategoryId());
            assertNotNull(child_3.getContent().getChildren());
            assertEquals(-24, child_3.getContent().getNumber());
            assertEquals("3", child_3.getContent().getType());
            assertEquals(4, child_3.getContent().getChildren().size());

            assertSame(rootObject, child_3.getContent().getParent());

            assertTrue(child_3.hasLink("self"));
            assertTrue(child_3.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/3", child_3.getLink("self").get().getHref());

            assertTrue(child_3.hasLink("parent"));
            assertTrue(child_3.getLink("parent").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren1/1", child_3.getLink("parent").get().getHref());

            //  Subchild nr 3.1

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> subChild_3_1 = child_3.getContent().getChildren().get(0);
            assertEquals(-7894, subChild_3_1.getContent().getNumber());
            assertEquals(3, subChild_3_1.getContent().getColor());
            assertEquals("I'm the second subchild", subChild_3_1.getContent().getComment());
            assertEquals(789456123, subChild_3_1.getContent().getCategoryId());
            assertEquals("of...", subChild_3_1.getContent().getName());
            assertEquals("*", subChild_3_1.getContent().getType());
            assertNull(subChild_3_1.getContent().getChildren());

            assertSame(subChild_3_1.getContent().getParent(), subChild_1_1);

            assertTrue(subChild_3_1.hasLink("self"));
            assertTrue(subChild_3_1.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/2", subChild_3_1.getLink("self").get().getHref());

            assertTrue(subChild_3_1.hasLink("parent"));
            assertTrue(subChild_3_1.getLink("parent").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/1", subChild_3_1.getLink("parent").get().getHref());

            //  END Subchild nr. 3.1

            //  Subchild nr 3.2

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> subChild_3_2 = child_3.getContent().getChildren().get(1);
            assertEquals(574389, subChild_3_2.getContent().getNumber());
            assertEquals(818147, subChild_3_2.getContent().getColor());
            assertEquals("I'm the third subchild", subChild_3_2.getContent().getComment());
            assertNull(subChild_3_2.getContent().getParent());
            assertEquals(0, subChild_3_2.getContent().getCategoryId());
            assertEquals("mind!", subChild_3_2.getContent().getName());
            assertEquals("${myType}", subChild_3_2.getContent().getType());
            assertNull(subChild_3_2.getContent().getChildren());

            assertTrue(subChild_3_2.hasLink("self"));
            assertTrue(subChild_3_2.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/3", subChild_3_2.getLink("self").get().getHref());

            assertFalse(subChild_3_2.hasLink("parent"));

            //  END Subchild nr. 3.2

            //  Subchild nr 3.3

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> subChild_3_3 = child_3.getContent().getChildren().get(2);
            assertEquals(1186, subChild_3_3.getContent().getNumber());
            assertEquals(29141, subChild_3_3.getContent().getColor());
            assertEquals("I'm the fourth subchild", subChild_3_3.getContent().getComment());
            assertEquals(55, subChild_3_3.getContent().getCategoryId());
            assertEquals("Lorem", subChild_3_3.getContent().getName());
            assertEquals("Object Mark IV", subChild_3_3.getContent().getType());
            assertSame(subChild_3_3.getContent().getParent(), subChild_3_2);
            assertNull(subChild_3_3.getContent().getChildren());

            assertTrue(subChild_3_3.hasLink("self"));
            assertTrue(subChild_3_3.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/4", subChild_3_3.getLink("self").get().getHref());

            assertTrue(subChild_3_3.hasLink("parent"));
            assertTrue(subChild_3_3.getLink("parent").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/3", subChild_3_3.getLink("parent").get().getHref());

            //  END Subchild nr. 3.3


            //  Subchild nr 3.4

            EntityModel<ComplexObjectWithMultipleChildrenWithLinks6> subChild_3_4 = child_3.getContent().getChildren().get(3);
            assertEquals(-7561, subChild_3_4.getContent().getNumber());
            assertEquals(222222, subChild_3_4.getContent().getColor());
            assertEquals("I'm the fifth subchild", subChild_3_4.getContent().getComment());
            assertEquals(3521, subChild_3_4.getContent().getCategoryId());
            assertEquals("ipsum", subChild_3_4.getContent().getName());
            assertEquals("Knödel", subChild_3_4.getContent().getType());
            assertNull(subChild_3_4.getContent().getParent());
            assertNull(subChild_3_4.getContent().getChildren());

            assertTrue(subChild_3_4.hasLink("self"));
            assertTrue(subChild_3_4.getLink("self").isPresent());
            assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/5", subChild_3_4.getLink("self").get().getHref());

            assertFalse(subChild_3_4.hasLink("parent"));

            //  END Subchild nr. 3.4

            //END Child nr. 3


        }
    }

    @Nested
    @DisplayName("collection handling")
    class CollectionHandling {

        /**
         * Reset the statistics and empty the caches before each test run.
         */
        @BeforeEach
        public void init() {
            Storesthal.resetStatistics();
            Storesthal.clearAllCaches();
        }


        /**
         * Make sure, everything works as desired if the "top-level-object" is a collection.
         */
        @Test
        @DisplayName("retrieves collections")
        public void retrieves_collections() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "collection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            ArrayList<ChildObject> children = Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class);

            assertEquals(4, children.size());
            assertEquals(759034, children.get(2).getChildId());
            assertEquals("collObject673896873", children.get(3).getChildName());
        }

        /**
         * Make sure, everything works as desired if the "top-level-object" is a collection.
         */
        @Test
        @DisplayName("retrieves collections, maintaining their links")
        public void retrieves_collections_maintaining_their_links() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "collection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            ArrayList<EntityModel<ChildObject>> children = Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class);

            assertEquals(4, children.size());
            assertEquals(759034, children.get(2).getContent().getChildId());
            assertEquals("collObject673896873", children.get(3).getContent().getChildName());

            assertTrue(children.get(0).hasLink("self"));
            assertTrue(children.get(0).getLink("self").isPresent());
            assertEquals("/collObjects/1", children.get(0).getLink("self").get().getHref());

            assertTrue(children.get(1).hasLink("self"));
            assertTrue(children.get(1).getLink("self").isPresent());
            assertEquals("/collObjects/2", children.get(1).getLink("self").get().getHref());

            assertTrue(children.get(2).hasLink("self"));
            assertTrue(children.get(2).getLink("self").isPresent());
            assertEquals("/collObjects/759034", children.get(2).getLink("self").get().getHref());

            assertTrue(children.get(3).hasLink("self"));
            assertTrue(children.get(3).getLink("self").isPresent());
            assertEquals("/collObjects/673896873", children.get(3).getLink("self").get().getHref());
        }


    }

    @Nested
    @DisplayName("embedded collection handling")
    public class Embedded_Collection_handling {

        /**
         * Reset the statistics and empty the caches before each test run.
         */
        @BeforeEach
        public void init() {
            Storesthal.resetStatistics();
            Storesthal.clearAllCaches();
        }

        @Test
        @DisplayName("retrieves embedded collections with a given field name")
        public void retrieves_embedded_collections_with_a_given_field_name() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<ChildObject> collectionItems = Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.of("someCollection"));

            assertEquals(4, collectionItems.size());
            assertEquals(673896873, collectionItems.get(3).getChildId());
            assertEquals("collObject2", collectionItems.get(1).getChildName());
        }

        @Test
        @DisplayName("retrieves embedded collections with a given field name, maintaining its links")
        public void retrieves_embedded_collections_with_a_given_field_name_maintaining_its_links() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<EntityModel<ChildObject>> collectionItems = Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.of("someCollection"));

            assertEquals(4, collectionItems.size());
            assertEquals(673896873, collectionItems.get(3).getContent().getChildId());
            assertEquals("collObject2", collectionItems.get(1).getContent().getChildName());
            assertTrue(collectionItems.get(2).hasLink("self"));
            assertTrue(collectionItems.get(2).getLink("self").isPresent());
            assertEquals(Link.of("/collObjects/759034", "self"), collectionItems.get(2).getLink("self").get());
        }

        @Test
        @DisplayName("retrieves embedded collections without given field name")
        public void retrieves_embedded_collections_without_given_field_name() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<ChildObject> collectionItems = Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.empty());

            assertEquals(4, collectionItems.size());
            assertEquals(673896873, collectionItems.get(3).getChildId());
            assertEquals("collObject2", collectionItems.get(1).getChildName());
        }

        @Test
        @DisplayName("retrieves embedded collections without given field name, maintaining their links")
        public void retrieves_embedded_collections_without_given_field_name_maintaining_their_links() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<EntityModel<ChildObject>> collectionItems = Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.empty());

            assertEquals(4, collectionItems.size());
            assertEquals(673896873, collectionItems.get(3).getContent().getChildId());
            assertEquals("collObject2", collectionItems.get(1).getContent().getChildName());
            assertTrue(collectionItems.get(3).hasLink("self"));
            assertTrue(collectionItems.get(3).getLink("self").isPresent());
            assertEquals(Link.of("/collObjects/673896873", "self"), collectionItems.get(3).getLink("self").get());
        }

        @Test
        @DisplayName("returns an empty list when retrieving an embedded collection, if the given field name was not found")
        public void returns_an_empty_list_when_retrieving_an_embedded_collection_if_the_given_field_name_was_not_found() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<ChildObject> collectionItems = Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.of("test"));

            assertTrue(collectionItems.isEmpty());
        }

        @Test
        @DisplayName("returns an empty list when retrieving an embedded collection (maintaining its links), if the given field name was not found")
        public void returns_an_empty_list_when_retrieving_an_embedded_collection_maintaining_its_links_if_the_given_field_name_was_not_found() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<EntityModel<ChildObject>> collectionItems = Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.of("test"));

            assertTrue(collectionItems.isEmpty());
        }


        @Test
        @DisplayName("returns an empty list when retrieving an embedded collection, if there is not matching field")
        public void returns_an_empty_list_when_retrieving_an_embedded_collection_if_there_is_no_matching_field() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection_faulty.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<ChildObject> collectionItems = Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.empty());

            assertTrue(collectionItems.isEmpty());
        }

        @Test
        @DisplayName("returns an empty list when retrieving an embedded collection (maintaining its links), if there is not matching field")
        public void returns_an_empty_list_when_retrieving_an_embedded_collection_maintaining_its_links_if_there_is_no_matching_field() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection_faulty.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<EntityModel<ChildObject>> collectionItems = Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.empty());

            assertTrue(collectionItems.isEmpty());
        }

        @Test
        @DisplayName("throws an exception retrieving an embedded collection, if the given field name is not an array in JSON")
        public void throws_an_exception_retrieving_an_embedded_collection_if_the_given_field_name_is_not_an_array_in_JSON() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection_faulty.json");
            serverMock.start();

            Storesthal.resetStatistics();

            assertThrows(StoresthalException.class, () ->
                    Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.of("someCollection"))
            );
        }

        @Test
        @DisplayName("throws an exception retrieving an embedded collection (maintaining its links), if the given field name is not an array in JSON")
        public void throws_an_exception_retrieving_an_embedded_collection_maintaining_its_links_if_the_given_field_name_is_not_an_array_in_JSON() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollection_faulty.json");
            serverMock.start();

            Storesthal.resetStatistics();

            assertThrows(StoresthalException.class, () ->
                    Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ChildObject.class, Optional.of("someCollection"))
            );
        }

        @Test
        @DisplayName("obeys embedded collection links")
        public void obeys_embedded_collection_links() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollectionWithRelation.json");
            configureServerMockWithResponseFile("/otherobjects/1", "complexObject1.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<ChildObjectWithParentRelation> collectionItems = Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ChildObjectWithParentRelation.class, Optional.of("someCollection"));

            assertEquals(4, collectionItems.size());
            assertEquals(673896873, collectionItems.get(3).getChildId());
            assertEquals("collObject2", collectionItems.get(1).getChildName());
            assertNotNull(collectionItems.get(2).getParent());
        }

        @Test
        @DisplayName("obeys embedded collection links when links are maintained")
        public void obeys_embedded_collection_links_when_links_are_maintained() throws IOException, StoresthalException {
            configureServerMockWithResponseFile("/collection/coll", "embeddedCollectionWithRelation.json");
            configureServerMockWithResponseFile("/otherobjects/1", "complexObject1.json");
            serverMock.start();

            Storesthal.resetStatistics();

            List<EntityModel<ChildObjectWithParentRelation>> collectionItems = Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ChildObjectWithParentRelation.class, Optional.of("someCollection"));

            assertEquals(4, collectionItems.size());
            assertEquals(673896873, collectionItems.get(3).getContent().getChildId());
            assertEquals("collObject2", collectionItems.get(1).getContent().getChildName());
            assertNotNull(collectionItems.get(2).getContent().getParent());

            assertTrue(collectionItems.get(1).hasLink("self"));
            assertTrue(collectionItems.get(1).getLink("self").isPresent());
            assertEquals(Link.of("/collObjects/2", "self"), collectionItems.get(1).getLink("self").get());

            assertTrue(collectionItems.get(2).hasLink("parent"));
            assertTrue(collectionItems.get(2).getLink("parent").isPresent());
            assertEquals(Link.of("http://localhost:" + serverMock.port() + "/otherobjects/1", "parent"), collectionItems.get(2).getLink("parent").get());

        }
    }
}
