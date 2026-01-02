package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren7;
import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildrenWithLinks7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComplexCollectionTest extends AbstractJsonTemplateBasedTest {

    /**
     * Reset the statistics and empty the caches before each test run.
     */
    @BeforeEach
    public void init() {
        Storesthal.resetStatistics();
        Storesthal.clearAllCaches();
    }
    
    /**
     * Make sure, a collection of very complex object structures with many levels of relation can be correctly retrieved.
     * Please note: The test structure created is not completely "logic" in a sense of correct
     * "parent-child-grandchild"-relations. This is intentional as it allows testing such structures as well.
     *
     * @throws StoresthalException if something fails.
     * @throws java.io.IOException if the JSON template for the mocked service answer can't be accessed.
     */
    @Test
    @DisplayName("retrieves a complex collection")
    public void retrieves_a_complex_collection() throws IOException, StoresthalException {
        configureServerMockWithResponseFile("/collection/coll", "complexCollection.json",
                Map.of("children1432",
                        createJsonHrefArray(new String[]{
                                "http://localhost:${port}/complexChildren2/1",
                                "http://localhost:${port}/complexChildren2/2",
                                "http://localhost:${port}/complexChildren2/3"}
                        ),
                        "children52",
                        createJsonHrefArray(new String[]{
                                "http://localhost:${port}/complexChildren2/4",
                                "http://localhost:${port}/complexChildren2/5"}
                        ),
                        "children7486465",
                        createJsonHrefArray(new String[]{
                                "http://localhost:${port}/complexChildren2/6",
                                "http://localhost:${port}/complexChildren2/1"}
                        )
                        , "parent", "", "types", "[]"));

        configureServerMockWithResponseFile("/complexChildren2/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "1345", "comment", "Number 1...", "categoryId", "5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/1"
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "584390", "comment", "Number 2...", "categoryId", "1", "name", "just...", "number", "8", "types", "[\"xyxyxy\"]", "children", createJsonHrefArray(new String[]{
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "468", "comment", "Number 3...", "categoryId", "1111", "name", "a...", "number", "-24", "types", "[\"3\", \"blah\", \"pups\"]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/2",
                        "http://localhost:${port}/complexChildren3/3",
                        "http://localhost:${port}/complexChildren3/4",
                        "http://localhost:${port}/complexChildren3/5"
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "5431", "comment", "Number 4...", "categoryId", "5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/6"
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "43289", "comment", "Number 5...", "categoryId", "10101", "name", "blah", "number", "45465", "types", "[\"some type\"]", "children", createJsonHrefArray(new String[]{}
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "5324", "comment", "Number [6]...", "categoryId", "5234789", "name", "5834543", "number", "-17", "types", "[]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/3"
                }
        ), "parent", ""));


        configureServerMockWithResponseFile("/complexChildren3/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "747474", "comment", "I'm the first subchild", "categoryId", "10000", "name", "state...", "number", "null", "types", "[\"   \"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/1\"}"));
        configureServerMockWithResponseFile("/complexChildren3/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId", "789456123", "name", "of...", "number", "-7894", "types", "[\"*\"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/1\"}"));
        configureServerMockWithResponseFile("/complexChildren3/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId", "0", "name", "mind!", "number", "574389", "types", "[\"${myType}\"]", "children", createJsonHrefArray(new String[]{}), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren3/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId", "55", "name", "Lorem", "number", "1186", "types", "[\"Object Mark IV\"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/3\"}"));
        configureServerMockWithResponseFile("/complexChildren3/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId", "3521", "name", "ipsum", "number", "-7561", "types", "[\"Knödel\"]", "children", createJsonHrefArray(new String[]{}), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren3/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "456123", "comment", "I'm the sixth subchild", "categoryId", "2323", "name", "dolor", "number", "5743534", "types", "[\"Knödel\", \"heyho\"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/4\"}"));

        serverMock.start();

        System.out.println("http://localhost:" + serverMock.port() + "/collection/coll");

        Storesthal.resetStatistics();

        ArrayList<ComplexObjectWithMultipleChildren7> objects = Storesthal.getCollectionWithoutLinks("http://localhost:" + serverMock.port() + "/collection/coll", ComplexObjectWithMultipleChildren7.class);

        assertEquals(3, objects.size());

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

        assertEquals(1345, test.getChildren().get(0).getColor());
        assertEquals("is...", test.getChildren().get(0).getName());
        assertEquals("Number 1...", test.getChildren().get(0).getComment());
        assertEquals(5, test.getChildren().get(0).getCategoryId());
        assertEquals(1, test.getChildren().get(0).getChildren().size());
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

        assertEquals(584390, test.getChildren().get(1).getColor());
        assertEquals("just...", test.getChildren().get(1).getName());
        assertEquals("Number 2...", test.getChildren().get(1).getComment());
        assertEquals(1, test.getChildren().get(1).getCategoryId());
        assertNull(test.getChildren().get(1).getChildren());
        assertEquals(8, test.getChildren().get(1).getNumber());
        assertEquals(1, test.getChildren().get(1).getTypes().size());
        assertEquals("xyxyxy", test.getChildren().get(1).getTypes().get(0));

        //(Child nr. 2 has no subchildren...)

        //END Child nr. 2


        //Child nr. 3
        assertEquals(468, test.getChildren().get(2).getColor());
        assertEquals("a...", test.getChildren().get(2).getName());
        assertEquals("Number 3...", test.getChildren().get(2).getComment());
        assertEquals(1111, test.getChildren().get(2).getCategoryId());
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
        assertEquals(5431, test.getChildren().get(0).getColor());
        assertEquals("is...", test.getChildren().get(0).getName());
        assertEquals("Number 4...", test.getChildren().get(0).getComment());
        assertEquals(5, test.getChildren().get(0).getCategoryId());
        assertEquals(1, test.getChildren().get(0).getChildren().size());
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
        assertEquals(43289, test.getChildren().get(1).getColor());
        assertEquals("blah", test.getChildren().get(1).getName());
        assertEquals("Number 5...", test.getChildren().get(1).getComment());
        assertEquals(10101, test.getChildren().get(1).getCategoryId());
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

        assertEquals(5324, test.getChildren().get(0).getColor());
        assertEquals("5834543", test.getChildren().get(0).getName());
        assertEquals("Number [6]...", test.getChildren().get(0).getComment());
        assertEquals(5234789, test.getChildren().get(0).getCategoryId());
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
        assertEquals(1345, test.getColor());
        assertEquals("is...", test.getName());
        assertEquals("Number 1...", test.getComment());
        assertEquals(5, test.getCategoryId());
        assertEquals(1, test.getChildren().size());
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

    @Test
    @DisplayName("retrieves a complex collection, maintaining its links")
    public void retrieves_a_complex_collection_maintaining_its_links() throws IOException, StoresthalException {
        configureServerMockWithResponseFile("/collection/coll", "complexCollection.json",
                Map.of("children1432",
                        createJsonHrefArray(new String[]{
                                "http://localhost:${port}/complexChildren2/1",
                                "http://localhost:${port}/complexChildren2/2",
                                "http://localhost:${port}/complexChildren2/3"}
                        ),
                        "children52",
                        createJsonHrefArray(new String[]{
                                "http://localhost:${port}/complexChildren2/4",
                                "http://localhost:${port}/complexChildren2/5"}
                        ),
                        "children7486465",
                        createJsonHrefArray(new String[]{
                                "http://localhost:${port}/complexChildren2/6",
                                "http://localhost:${port}/complexChildren2/1"}
                        )
                        , "parent", "", "types", "[]"));

        configureServerMockWithResponseFile("/complexChildren2/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "1345", "comment", "Number 1...", "categoryId", "5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/1"
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "584390", "comment", "Number 2...", "categoryId", "1", "name", "just...", "number", "8", "types", "[\"xyxyxy\"]", "children", createJsonHrefArray(new String[]{
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "468", "comment", "Number 3...", "categoryId", "1111", "name", "a...", "number", "-24", "types", "[\"3\", \"blah\", \"pups\"]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/2",
                        "http://localhost:${port}/complexChildren3/3",
                        "http://localhost:${port}/complexChildren3/4",
                        "http://localhost:${port}/complexChildren3/5"
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "5431", "comment", "Number 4...", "categoryId", "5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/6"
                }
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "43289", "comment", "Number 5...", "categoryId", "10101", "name", "blah", "number", "45465", "types", "[\"some type\"]", "children", createJsonHrefArray(new String[]{}
        ), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren2/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "5324", "comment", "Number [6]...", "categoryId", "5234789", "name", "5834543", "number", "-17", "types", "[]", "children", createJsonHrefArray(new String[]{
                        "http://localhost:${port}/complexChildren3/3"
                }
        ), "parent", ""));


        configureServerMockWithResponseFile("/complexChildren3/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "747474", "comment", "I'm the first subchild", "categoryId", "10000", "name", "state...", "number", "null", "types", "[\"   \"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/1\"}"));
        configureServerMockWithResponseFile("/complexChildren3/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId", "789456123", "name", "of...", "number", "-7894", "types", "[\"*\"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/1\"}"));
        configureServerMockWithResponseFile("/complexChildren3/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId", "0", "name", "mind!", "number", "574389", "types", "[\"${myType}\"]", "children", createJsonHrefArray(new String[]{}), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren3/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId", "55", "name", "Lorem", "number", "1186", "types", "[\"Object Mark IV\"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/3\"}"));
        configureServerMockWithResponseFile("/complexChildren3/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId", "3521", "name", "ipsum", "number", "-7561", "types", "[\"Knödel\"]", "children", createJsonHrefArray(new String[]{}), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren3/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "456123", "comment", "I'm the sixth subchild", "categoryId", "2323", "name", "dolor", "number", "5743534", "types", "[\"Knödel\", \"heyho\"]", "children", createJsonHrefArray(new String[]{}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/4\"}"));

        serverMock.start();

        System.out.println("http://localhost:" + serverMock.port() + "/collection/coll");

        Storesthal.resetStatistics();

        ArrayList<EntityModel<ComplexObjectWithMultipleChildrenWithLinks7>> objects = Storesthal.getCollection("http://localhost:" + serverMock.port() + "/collection/coll", ComplexObjectWithMultipleChildrenWithLinks7.class);

        assertEquals(3, objects.size());

        // Object nr. 1
        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1 = objects.get(0);
        assertEquals(234, object_1.getContent().getCategoryId());
        assertEquals("Object No. 1", object_1.getContent().getComment());
        assertEquals(43432, object_1.getContent().getColor());
        assertEquals(3, object_1.getContent().getChildren().size());
        assertEquals(543890, object_1.getContent().getNumber());
        assertEquals("complexCollObject1432", object_1.getContent().getName());
        assertEquals(0, object_1.getContent().getTypes().size());
        assertNull(object_1.getContent().getParent());

        assertFalse(object_1.hasLink("parent"));
        assertTrue(object_1.hasLink("children"));
        assertEquals(3, object_1.getLinks("children").size());
        assertTrue(object_1.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren2/1", "children")));
        assertTrue(object_1.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren2/2", "children")));
        assertTrue(object_1.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren2/3", "children")));

        assertTrue(object_1.hasLink("self"));
        assertTrue(object_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexCollObjects/1432", object_1.getLink("self").get().getHref());

        //Object nr. 1, Child nr. 1

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_1 = object_1.getContent().getChildren().get(0);

        assertEquals(1345, object_1_1.getContent().getColor());
        assertEquals("is...", object_1_1.getContent().getName());
        assertEquals("Number 1...", object_1_1.getContent().getComment());
        assertEquals(5, object_1_1.getContent().getCategoryId());
        assertEquals(1, object_1_1.getContent().getChildren().size());
        assertEquals(5547, object_1_1.getContent().getNumber());
        assertEquals(1, object_1_1.getContent().getTypes().size());
        assertEquals("$myGreatType", object_1_1.getContent().getTypes().get(0));

        assertNull(object_1_1.getContent().getParent());
        assertFalse(object_1_1.hasLink("parent"));
        assertTrue(object_1_1.hasLink("children"));
        assertEquals(1, object_1_1.getLinks("children").size());
        assertTrue(object_1_1.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/1", "children")));

        assertTrue(object_1_1.hasLink("self"));
        assertTrue(object_1_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/1", object_1_1.getLink("self").get().getHref());

        //  Subchild nr. 1.1

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_1_1 = object_1_1.getContent().getChildren().get(0);
        assertEquals(747474, object_1_1_1.getContent().getColor());
        assertEquals("I'm the first subchild", object_1_1_1.getContent().getComment());
        assertNotNull(object_1_1_1.getContent().getParent());
        assertSame(object_1_1_1.getContent().getParent(), object_1.getContent().getChildren().get(0));
        assertSame(object_1_1_1.getContent().getParent(), object_1_1);
        assertEquals(10000, object_1_1_1.getContent().getCategoryId());
        assertEquals("state...", object_1_1_1.getContent().getName());
        assertEquals(1, object_1_1_1.getContent().getTypes().size());
        assertEquals("   ", object_1_1_1.getContent().getTypes().get(0));
        assertNull(object_1_1_1.getContent().getChildren());
        assertNull(object_1_1_1.getContent().getNumber());

        assertTrue(object_1_1_1.hasLink("parent"));
        assertTrue(object_1_1_1.getLink("parent").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/1", object_1_1_1.getLink("parent").get().getHref());

        assertTrue(object_1_1_1.hasLink("self"));
        assertTrue(object_1_1_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/1", object_1_1_1.getLink("self").get().getHref());

        assertFalse(object_1_1_1.hasLink("children"));

        //  End subchild nr. 1.1

        //END Child nr. 1

        //Child nr. 2

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_2 = object_1.getContent().getChildren().get(1);

        assertEquals(584390, object_1_2.getContent().getColor());
        assertEquals("just...", object_1_2.getContent().getName());
        assertEquals("Number 2...", object_1_2.getContent().getComment());
        assertEquals(1, object_1_2.getContent().getCategoryId());
        assertNull(object_1_2.getContent().getChildren());
        assertEquals(8, object_1_2.getContent().getNumber());
        assertEquals(1, object_1_2.getContent().getTypes().size());
        assertEquals("xyxyxy", object_1_2.getContent().getTypes().get(0));
        assertNull(object_1_2.getContent().getParent());

        assertFalse(object_1_2.hasLink("children"));
        assertFalse(object_1_2.hasLink("parent"));

        assertTrue(object_1_2.hasLink("self"));
        assertTrue(object_1_2.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/2", object_1_2.getLink("self").get().getHref());

        //(Child nr. 2 has no subchildren...)

        //END Child nr. 2

        //Child nr. 3
        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_3 = object_1.getContent().getChildren().get(2);

        assertEquals(468, object_1_3.getContent().getColor());
        assertEquals("a...", object_1_3.getContent().getName());
        assertEquals("Number 3...", object_1_3.getContent().getComment());
        assertEquals(1111, object_1_3.getContent().getCategoryId());
        assertEquals(4, object_1_3.getContent().getChildren().size());
        assertEquals(-24, object_1_3.getContent().getNumber());
        assertEquals(3, object_1_3.getContent().getTypes().size());
        assertEquals("3", object_1_3.getContent().getTypes().get(0));
        assertEquals("blah", object_1_3.getContent().getTypes().get(1));
        assertEquals("pups", object_1_3.getContent().getTypes().get(2));
        assertEquals(4, object_1_3.getContent().getChildren().size());

        assertFalse(object_1_3.hasLink("parent"));

        assertTrue(object_1_3.hasLink("children"));
        assertEquals(4, object_1_3.getLinks("children").size());
        assertTrue(object_1_3.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/2", "children")));
        assertTrue(object_1_3.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/3", "children")));
        assertTrue(object_1_3.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/4", "children")));
        assertTrue(object_1_3.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/5", "children")));

        assertTrue(object_1_3.hasLink("self"));
        assertTrue(object_1_3.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/3", object_1_3.getLink("self").get().getHref());


        //  Subchild nr 3.1

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_3_1 = object_1_3.getContent().getChildren().get(0);
        assertEquals(-7894, object_1_3_1.getContent().getNumber());
        assertEquals(3, object_1_3_1.getContent().getColor());
        assertEquals("I'm the second subchild", object_1_3_1.getContent().getComment());
        assertEquals(789456123, object_1_3_1.getContent().getCategoryId());
        assertEquals("of...", object_1_3_1.getContent().getName());
        assertEquals(1, object_1_3_1.getContent().getTypes().size());
        assertEquals("*", object_1_3_1.getContent().getTypes().get(0));
        assertSame(object_1_1_1, object_1_3_1.getContent().getParent());
        assertNull(object_1_3_1.getContent().getChildren());

        assertFalse(object_1_3_1.hasLink("children"));

        assertTrue(object_1_3_1.hasLink("parent"));
        assertTrue(object_1_3_1.getLink("parent").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/1", object_1_3_1.getLink("parent").get().getHref());

        assertTrue(object_1_3_1.hasLink("self"));
        assertTrue(object_1_3_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/2", object_1_3_1.getLink("self").get().getHref());

        //  END Subchild nr. 3.1


        //  Subchild nr 3.2

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_3_2 = object_1_3.getContent().getChildren().get(1);

        assertEquals(574389, object_1_3_2.getContent().getNumber());
        assertEquals(818147, object_1_3_2.getContent().getColor());
        assertEquals("I'm the third subchild", object_1_3_2.getContent().getComment());
        assertNull(object_1_3_2.getContent().getParent());
        assertEquals(0, object_1_3_2.getContent().getCategoryId());
        assertEquals("mind!", object_1_3_2.getContent().getName());
        assertEquals(1, object_1_3_2.getContent().getTypes().size());
        assertEquals("${myType}", object_1_3_2.getContent().getTypes().get(0));
        assertNull(object_1_3_2.getContent().getChildren());

        assertFalse(object_1_3_2.hasLink("children"));

        assertFalse(object_1_3_2.hasLink("parent"));

        assertTrue(object_1_3_2.hasLink("self"));
        assertTrue(object_1_3_2.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/3", object_1_3_2.getLink("self").get().getHref());

        //  END Subchild nr. 3.2

        //  Subchild nr 3.3

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_3_3 = object_1_3.getContent().getChildren().get(2);

        assertEquals(1186, object_1_3_3.getContent().getNumber());
        assertEquals(29141, object_1_3_3.getContent().getColor());
        assertEquals("I'm the fourth subchild", object_1_3_3.getContent().getComment());
        assertEquals(55, object_1_3_3.getContent().getCategoryId());
        assertEquals("Lorem", object_1_3_3.getContent().getName());
        assertEquals(1, object_1_3_3.getContent().getTypes().size());
        assertEquals("Object Mark IV", object_1_3_3.getContent().getTypes().get(0));
        assertSame(object_1_3_3.getContent().getParent(), object_1_3_2);
        assertNull(object_1_3_3.getContent().getChildren());

        assertFalse(object_1_3_3.hasLink("children"));

        assertTrue(object_1_3_3.hasLink("parent"));
        assertTrue(object_1_3_3.getLink("parent").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/3", object_1_3_3.getLink("parent").get().getHref());

        assertTrue(object_1_3_3.hasLink("self"));
        assertTrue(object_1_3_3.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/4", object_1_3_3.getLink("self").get().getHref());

        //  END Subchild nr. 3.3

        //  Subchild nr 3.4

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_1_3_4 = object_1_3.getContent().getChildren().get(3);
        assertEquals(-7561, object_1_3_4.getContent().getNumber());
        assertEquals(222222, object_1_3_4.getContent().getColor());
        assertEquals("I'm the fifth subchild", object_1_3_4.getContent().getComment());
        assertEquals(3521, object_1_3_4.getContent().getCategoryId());
        assertEquals("ipsum", object_1_3_4.getContent().getName());
        assertEquals(1, object_1_3_4.getContent().getTypes().size());
        assertEquals("Knödel", object_1_3_4.getContent().getTypes().get(0));
        assertNull(object_1_3_4.getContent().getChildren());

        assertFalse(object_1_3_4.hasLink("children"));
        assertFalse(object_1_3_4.hasLink("parent"));

        assertTrue(object_1_3_4.hasLink("self"));
        assertTrue(object_1_3_4.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/5", object_1_3_4.getLink("self").get().getHref());

        //  END Subchild nr. 3.4

        //END Object nr. 1, Child nr. 3


        //Object nr. 2

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_2 = objects.get(1);
        assertEquals(438290, object_2.getContent().getCategoryId());
        assertEquals("Object No. 2", object_2.getContent().getComment());
        assertEquals(532, object_2.getContent().getColor());
        assertEquals(2, object_2.getContent().getChildren().size());
        assertEquals(456, object_2.getContent().getNumber());
        assertEquals("complexCollObject52", object_2.getContent().getName());
        assertNull(object_2.getContent().getTypes());

        assertFalse(object_2.hasLink("parent"));

        assertTrue(object_2.hasLink("children"));
        assertEquals(2, object_2.getLinks("children").size());
        assertTrue(object_2.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren2/4", "children")));
        assertTrue(object_2.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren2/5", "children")));

        assertTrue(object_2.hasLink("self"));
        assertTrue(object_2.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexCollObjects/52", object_2.getLink("self").get().getHref());

        // Object nr. 2, child nr. 1
        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_2_1 = object_2.getContent().getChildren().get(0);

        assertEquals(5431, object_2_1.getContent().getColor());
        assertEquals("is...", object_2_1.getContent().getName());
        assertEquals("Number 4...", object_2_1.getContent().getComment());
        assertEquals(5, object_2_1.getContent().getCategoryId());
        assertEquals(1, object_2_1.getContent().getChildren().size());
        assertEquals(5547, object_2_1.getContent().getNumber());
        assertEquals(1, object_2_1.getContent().getTypes().size());
        assertEquals("$myGreatType", object_2_1.getContent().getTypes().get(0));

        assertFalse(object_2_1.hasLink("parent"));

        assertTrue(object_2_1.hasLink("children"));
        assertEquals(1, object_2_1.getLinks("children").size());
        assertTrue(object_2_1.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/6", "children")));

        assertTrue(object_2_1.hasLink("self"));
        assertTrue(object_2_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/4", object_2_1.getLink("self").get().getHref());

        // Object nr. 2, Child nr. 1, subchild Nr. 1 (the only one)

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_2_1_1 = object_2_1.getContent().getChildren().get(0);

        assertEquals(5743534, object_2_1_1.getContent().getNumber());
        assertEquals(456123, object_2_1_1.getContent().getColor());
        assertEquals("I'm the sixth subchild", object_2_1_1.getContent().getComment());
        assertEquals(2323, object_2_1_1.getContent().getCategoryId());
        assertEquals("dolor", object_2_1_1.getContent().getName());
        assertEquals(2, object_2_1_1.getContent().getTypes().size());
        assertEquals("Knödel", object_2_1_1.getContent().getTypes().get(0));
        assertEquals("heyho", object_2_1_1.getContent().getTypes().get(1));
        assertNull(object_2_1_1.getContent().getChildren());
        assertNotNull(object_2_1_1.getContent().getParent());
        assertSame(object_2_1, object_2_1_1.getContent().getParent());

        assertFalse(object_2_1_1.hasLink("children"));

        assertTrue(object_2_1_1.hasLink("parent"));
        assertTrue(object_2_1_1.getLink("parent").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/4", object_2_1_1.getLink("parent").get().getHref());

        assertTrue(object_2_1_1.hasLink("self"));
        assertTrue(object_2_1_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/6", object_2_1_1.getLink("self").get().getHref());


        // Object nr. 2, Child nr. 2

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_2_2 = object_2.getContent().getChildren().get(1);

        assertEquals(43289, object_2_2.getContent().getColor());
        assertEquals("blah", object_2_2.getContent().getName());
        assertEquals("Number 5...", object_2_2.getContent().getComment());
        assertEquals(10101, object_2_2.getContent().getCategoryId());
        assertEquals(45465, object_2_2.getContent().getNumber());
        assertEquals(1, object_2_2.getContent().getTypes().size());
        assertEquals("some type", object_2_2.getContent().getTypes().get(0));

        // Object nr. 2, child nr. 2 has no children
        assertNull(object_2_2.getContent().getChildren());

        assertFalse(object_2_2.hasLink("children"));

        assertFalse(object_2_2.hasLink("parent"));

        assertTrue(object_2_2.hasLink("self"));
        assertTrue(object_2_2.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/5", object_2_2.getLink("self").get().getHref());

        // Object nr. 3
        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_3 = objects.get(2);
        assertEquals(543890, object_3.getContent().getCategoryId());
        assertNull(object_3.getContent().getComment());
        assertNull(object_3.getContent().getColor());
        assertNull(object_3.getContent().getParent());
        assertEquals(2, object_3.getContent().getChildren().size());
        assertEquals(542, object_3.getContent().getNumber());
        assertEquals("complexCollObject7486465", object_3.getContent().getName());
        assertEquals(4, object_3.getContent().getTypes().size());
        assertEquals("some ", object_3.getContent().getTypes().get(0));
        assertEquals("list ", object_3.getContent().getTypes().get(1));
        assertEquals("of   ", object_3.getContent().getTypes().get(2));
        assertEquals("types", object_3.getContent().getTypes().get(3));

        assertFalse(object_3.hasLink("parent"));

        assertTrue(object_3.hasLink("children"));
        assertEquals(2, object_3.getLinks("children").size());
        assertTrue(object_3.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren2/6", "children")));
        assertTrue(object_3.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren2/1", "children")));

        assertTrue(object_3.hasLink("self"));
        assertTrue(object_3.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexCollObjects/7486465", object_3.getLink("self").get().getHref());


        //Object nr. 3, Child nr. 1

        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_3_1 = object_3.getContent().getChildren().get(0);
        assertEquals(5324, object_3_1.getContent().getColor());
        assertEquals("5834543", object_3_1.getContent().getName());
        assertEquals("Number [6]...", object_3_1.getContent().getComment());
        assertEquals(5234789, object_3_1.getContent().getCategoryId());
        assertEquals(-17, object_3_1.getContent().getNumber());
        assertEquals(0, object_3_1.getContent().getTypes().size());
        assertEquals(1, object_3_1.getContent().getChildren().size());
        assertSame(object_1_3_2, object_3_1.getContent().getChildren().get(0));

        assertFalse(object_3_1.hasLink("parent"));

        assertTrue(object_3_1.hasLink("children"));
        assertEquals(1, object_3_1.getLinks("children").size());
        assertTrue(object_3_1.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/3", "children")));

        assertTrue(object_3_1.hasLink("self"));
        assertTrue(object_3_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/6", object_3_1.getLink("self").get().getHref());

        // Object nr. 3, Child nr. 1, subchild nr. 1 (the only one)
        // Object nr. 3, child nr. 1 has one child, the same as object nr. 1, Child nr. 3
        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_3_1_1 = object_3_1.getContent().getChildren().get(0);
        assertSame(object_3_1_1, objects.get(0).getContent().getChildren().get(2).getContent().getChildren().get(1));
        assertSame(object_3_1_1, object_1_3_2);
        assertEquals(574389, object_3_1_1.getContent().getNumber());
        assertEquals(818147, object_3_1_1.getContent().getColor());
        assertEquals("I'm the third subchild", object_3_1_1.getContent().getComment());
        assertNull(object_3_1_1.getContent().getParent());
        assertEquals(0, object_3_1_1.getContent().getCategoryId());
        assertEquals("mind!", object_3_1_1.getContent().getName());
        assertEquals(1, object_3_1_1.getContent().getTypes().size());
        assertEquals("${myType}", object_3_1_1.getContent().getTypes().get(0));
        assertNull(object_3_1_1.getContent().getChildren());

        assertNull(object_3_1_1.getContent().getChildren());

        assertFalse(object_3_1_1.hasLink("children"));

        assertFalse(object_3_1_1.hasLink("parent"));

        assertTrue(object_3_1_1.hasLink("self"));
        assertTrue(object_3_1_1.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren3/3", object_3_1_1.getLink("self").get().getHref());


        //Object nr. 3, child nr. 2 equals object nr. 1, child nr. 1!
        //Object nr. 3, Child nr. 2
        EntityModel<ComplexObjectWithMultipleChildrenWithLinks7> object_3_2 = object_3.getContent().getChildren().get(1);
        assertSame(object_3_2, object_1_1);
        assertEquals(1345, object_3_2.getContent().getColor());
        assertEquals("is...", object_3_2.getContent().getName());
        assertEquals("Number 1...", object_3_2.getContent().getComment());
        assertEquals(5, object_3_2.getContent().getCategoryId());
        assertEquals(1, object_3_2.getContent().getChildren().size());
        assertEquals(5547, object_3_2.getContent().getNumber());
        assertEquals(1, object_3_2.getContent().getTypes().size());
        assertEquals("$myGreatType", object_3_2.getContent().getTypes().get(0));

        assertNull(object_3_2.getContent().getParent());
        assertFalse(object_3_2.hasLink("parent"));
        assertTrue(object_3_2.hasLink("children"));
        assertEquals(1, object_3_2.getLinks("children").size());
        assertTrue(object_3_2.getLinks("children").contains(Link.of("http://localhost:" + serverMock.port() + "/complexChildren3/1", "children")));

        assertTrue(object_3_2.hasLink("self"));
        assertTrue(object_3_2.getLink("self").isPresent());
        assertEquals("http://localhost:" + serverMock.port() + "/complexChildren2/1", object_3_2.getLink("self").get().getHref());

        assertEquals(13, Storesthal.getStatistics().get("httpCalls"));

        Storesthal.resetStatistics();

    }


}
