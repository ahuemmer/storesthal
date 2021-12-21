package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.complextestobjects.ComplexObjectWithMultipleChildren7;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

public class ComplexCollectionTest extends AbstractJsonTemplateBasedTest {

    /**
     * Reset the statistics and empty the caches before each test run.
     */
    @BeforeEach
    public void init() {
        Storesthal.resetStatistics();
        Storesthal.clearAllCaches();
    }

    @Test
    /**
     * Make sure, a collection of very complex object structures with many levels of relation can be correctly retrieved.
     * Please note: The test structure created is not completely "logic" in a sense of correct
     * "parent-child-grandchild"-relations. This is intentional as it allows testing such structures as well.
     * @throws StoresthalException if something fails.
     * @throws java.io.IOException if the JSON template for the mocked service answer can't be accessed.
     */
    public void canRetrieveComplexCollections() throws IOException, StoresthalException {
        configureServerMockWithResponseFile("/collection/coll", "complexCollection.json",
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

        configureServerMockWithResponseFile("/complexCollObjects/1432", "complexObjectWithMultipleChildren2.json",
                Map.of("children",
                        createJsonHrefArray(new String[] {
                                "http://localhost:${port}/complexChildren2/1",
                                "http://localhost:${port}/complexChildren2/2",
                                "http://localhost:${port}/complexChildren2/3"}
                        ), "parent", "", "color", "43432", "categoryId", "234", "name", "complexCollObject1432", "number", "543890", "types", "[]", "comment", "Object No. 1"));

        configureServerMockWithResponseFile("/complexChildren2/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "1345", "comment", "Number 1...", "categoryId","5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/1"
                }
        ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexCollObjects/1432\"}"));
        configureServerMockWithResponseFile("/complexChildren2/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "584390", "comment", "Number 2...", "categoryId","1", "name", "just...","number","8", "types", "[\"xyxyxy\"]", "children", createJsonHrefArray(new String[] {
                }
        ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexCollObjects/1432\"}"));
        configureServerMockWithResponseFile("/complexChildren2/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "468", "comment", "Number 3...", "categoryId","1111", "name", "a...","number","-24","types","[\"3\", \"blah\", \"pups\"]","children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/2",
                        "http://localhost:${port}/complexChildren3/3",
                        "http://localhost:${port}/complexChildren3/4",
                        "http://localhost:${port}/complexChildren3/5"
                }
        ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexCollObjects/1432\"}"));
        configureServerMockWithResponseFile("/complexChildren2/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "5431", "comment", "Number 4...", "categoryId","5", "name", "is...", "number", "5547", "types", "[\"$myGreatType\"]", "children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/6"
                }
        ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexCollObjects/52\"}"));
        configureServerMockWithResponseFile("/complexChildren2/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "43289", "comment", "Number 5...", "categoryId","10101", "name", "blah", "number", "45465", "types", "[\"some type\"]", "children", createJsonHrefArray(new String[] {}
        ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexCollObjects/52\"}"));
        configureServerMockWithResponseFile("/complexChildren2/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "5324", "comment", "Number [6]...", "categoryId","5234789", "name", "5834543", "number", "-17", "types", "[]", "children", createJsonHrefArray(new String[] {
                        "http://localhost:${port}/complexChildren3/3"
                }
        ), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexCollObjects/7486465\"}"));


        configureServerMockWithResponseFile("/complexChildren3/1", "complexObjectWithMultipleChildren2.json", Map.of("color", "747474", "comment", "I'm the first subchild", "categoryId","10000", "name", "state...","number","null","types", "[\"   \"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/1\"}"));
        configureServerMockWithResponseFile("/complexChildren3/2", "complexObjectWithMultipleChildren2.json", Map.of("color", "3", "comment", "I'm the second subchild", "categoryId","789456123", "name", "of...","number","-7894", "types", "[\"*\"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/1\"}"));
        configureServerMockWithResponseFile("/complexChildren3/3", "complexObjectWithMultipleChildren2.json", Map.of("color", "818147", "comment", "I'm the third subchild", "categoryId","0", "name", "mind!","number","574389", "types", "[\"${myType}\"]", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren3/4", "complexObjectWithMultipleChildren2.json", Map.of("color", "29141", "comment", "I'm the fourth subchild", "categoryId","55", "name", "Lorem","number","1186","types", "[\"Object Mark IV\"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren3/3\"}"));
        configureServerMockWithResponseFile("/complexChildren3/5", "complexObjectWithMultipleChildren2.json", Map.of("color", "222222", "comment", "I'm the fifth subchild", "categoryId","3521", "name", "ipsum","number","-7561","types", "[\"Knödel\"]", "children", createJsonHrefArray(new String[] {}), "parent", ""));
        configureServerMockWithResponseFile("/complexChildren3/6", "complexObjectWithMultipleChildren2.json", Map.of("color", "456123", "comment", "I'm the sixth subchild", "categoryId","2323", "name", "dolor","number","5743534","types", "[\"Knödel\", \"heyho\"]", "children", createJsonHrefArray(new String[] {}), "parent", ",\"parent\": {\"href\":\"http://localhost:${port}/complexChildren2/4\"}"));

        serverMock.start();

        System.out.println("Serving at: http://localhost:"+serverMock.port()+"/collection/coll");

        /*try {
            Thread.sleep(3000000);
        }
        catch (Exception e) {}*/

        Storesthal.resetStatistics();

        ArrayList<ComplexObjectWithMultipleChildren7> objects = Storesthal.getCollection("http://localhost:"+serverMock.port()+"/collection/coll", ComplexObjectWithMultipleChildren7.class);

        assertEquals(3,objects.size());

        // Object nr. 1
        // ============
        // self-href: /complexCollObjects/1432
        // children:
        //   1.: /complexChildren2/1
        //   2.: /complexChildren2/2
        //   3.: /complexChildren2/3
        // category_id: 234
        // comment: Object No. 1
        // color: 43432
        // name: complexCollObject1432
        // number: 543890
        // types: []
        ComplexObjectWithMultipleChildren7 test = objects.get(0);
        assertEquals(234, test.getCategoryId());
        assertEquals("Object No. 1", test.getComment());
        assertEquals(43432, test.getColor());
        assertEquals(3, test.getChildren().size());
        assertEquals(543890, test.getNumber());
        assertEquals("complexCollObject1432", test.getName());
        assertEquals(0, test.getTypes().size());

        // Object nr. 1, child nr. 1
        // =========================
        // self-href: /complexChildren2/1
        // children:
        //   1.: /complexChildren3/1
        // parent: /complexCollObjects/1432 (=O1)
        // color: 1345
        // comment: Number 1...
        // category_id: 5
        // name: is...
        // number: 5547
        // types": [$myGreatType]

        assertEquals(1345,test.getChildren().get(0).getColor());
        assertEquals("is...", test.getChildren().get(0).getName());
        assertEquals("Number 1...",test.getChildren().get(0).getComment());
        assertEquals(5,test.getChildren().get(0).getCategoryId());
        assertEquals(1,test.getChildren().get(0).getChildren().size());
        assertEquals(5547, test.getChildren().get(0).getNumber());
        assertEquals(1, test.getChildren().get(0).getTypes().size());
        assertEquals("$myGreatType", test.getChildren().get(0).getTypes().get(0));
        assertSame(test, test.getChildren().get(0).getParent());

        // Object nr. 1, child nr. 1, subchild nr. 1
        // =========================================
        // self-href: /complexChildren3/1
        // children: []
        // parent: /complexChildren2/1 (= O1C1)
        // color: 747474
        // comment: I'm the first subchild,
        // category_id: 10000
        // name: state...
        // number: null
        // types: [   ]

        ComplexObjectWithMultipleChildren7 subChild1 = test.getChildren().get(0).getChildren().get(0);
        assertEquals(747474, subChild1.getColor());
        assertEquals("I'm the first subchild", subChild1.getComment());
        assertNotNull(subChild1.getParent());
        assertSame(subChild1.getParent(), test.getChildren().get(0));
        assertEquals(10000, subChild1.getCategoryId());
        assertEquals("state...", subChild1.getName());
        assertEquals(1, subChild1.getTypes().size());
        assertEquals("   ", subChild1.getTypes().get(0));
        assertSame(test.getChildren().get(0), subChild1.getParent());
        assertNull(subChild1.getChildren());
        assertNull(subChild1.getNumber());

        // Object nr. 1, child nr. 2
        // =========================
        // self-href: /complexChildren2/2
        // children: []
        // parent: /complexCollObjects/1432 (=O1)
        // color: 584390
        // comment: Number 2...
        // category_id: 1
        // name: just...
        // number: 8
        // types: [xyxyxy]

        assertEquals(584390,test.getChildren().get(1).getColor());
        assertEquals("just...", test.getChildren().get(1).getName());
        assertEquals("Number 2...",test.getChildren().get(1).getComment());
        assertEquals(1,test.getChildren().get(1).getCategoryId());
        assertNull(test.getChildren().get(1).getChildren());
        assertEquals(8, test.getChildren().get(1).getNumber());
        assertEquals(1, test.getChildren().get(1).getTypes().size());
        assertEquals("xyxyxy", test.getChildren().get(1).getTypes().get(0));
        assertSame(test, test.getChildren().get(1).getParent());

        //(Child nr. 2 of object nr. 1. has no subchildren...)

        // Object nr. 1, child nr. 3
        // =========================
        // self-href: /complexChildren2/3
        // children
        //   1.: /complexChildren3/2
        //   2.: /complexChildren3/3
        //   3.: /complexChildren3/4
        //   4.: /complexChildren3/5
        //  parent: /complexCollObjects/1432 (=O1)
        //  color: 468
        //  comment: Number 3...
        //  category_id: 1111
        //  name: a...
        //  number": -24
        //  types: [3,blah,pups]

        assertEquals(468,test.getChildren().get(2).getColor());
        assertEquals("a...", test.getChildren().get(2).getName());
        assertEquals("Number 3...",test.getChildren().get(2).getComment());
        assertEquals(1111,test.getChildren().get(2).getCategoryId());
        assertEquals(-24, test.getChildren().get(2).getNumber());
        assertEquals(3, test.getChildren().get(2).getTypes().size());
        assertEquals("3", test.getChildren().get(2).getTypes().get(0));
        assertEquals("blah", test.getChildren().get(2).getTypes().get(1));
        assertEquals("pups", test.getChildren().get(2).getTypes().get(2));
        assertEquals(4, test.getChildren().get(2).getChildren().size());
        assertSame(test, test.getChildren().get(2).getParent());

        // Object nr. 1, child nr. 3, subchild nr. 1
        // =========================================
        // self-href: /complexChildren3/2
        // children: []
        // parent: /complexChildren3/1 (=O1C1S1 (!))
        // color: 3
        // omment: I'm the second subchild
        // category_id: 789456123
        // name: of...
        // number: -7894
        // types: [*]

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

        // Object nr. 1, child nr. 3, subchild nr. 2
        // =========================================
        // self-href: /complexChildren3/3
        // children: []
        // color: 818147
        // comment: I'm the third subchild
        // category_id: 0
        // name: mind!
        // number: 574389
        // types: [${myType}]

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

        // Object nr. 1, child nr. 3, subchild nr 3:
        // =========================================
        // self-href: /complexChildren3/4
        // children: []
        // parent: complexChildren3/3 (=O1C3S2 (!))
        // color: 29141
        // comment: I'm the fourth subchild
        // category_id: 55
        // name: Lorem
        // number: 1186
        // types: [Object Mark IV]

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

        // Object nr. 1, child nr. 3, subchild nr. 4:
        // ==========================================
        // self-href: /complexChildren3/5
        // children: []
        // color": 222222
        // comment: I'm the fifth subchild
        // category_id: 3521
        // name: ipsum
        // number": -7561
        // types: [Knödel]

        subChild = test.getChildren().get(2).getChildren().get(3);
        assertEquals(-7561, subChild.getNumber());
        assertEquals(222222, subChild.getColor());
        assertEquals("I'm the fifth subchild", subChild.getComment());
        assertEquals(3521, subChild.getCategoryId());
        assertEquals("ipsum", subChild.getName());
        assertEquals(1, subChild.getTypes().size());
        assertEquals("Knödel", subChild.getTypes().get(0));
        assertNull(subChild.getChildren());

        // Object nr. 2
        // ============
        // self-href: /complexCollObjects/52
        // children:
        //   1.: /complexChildren2/4
        //   2.: /complexChildren2/5
        // name: complexCollObject52
        // category_id: 438290
        // comment: Object No. 2
        // color: 532
        // number: 456
        test = objects.get(1);
        assertEquals(2, test.getChildren().size());

        // Check child-parent-relationship
        assertSame(test, test.getChildren().get(0).getParent());
        assertSame(test, test.getChildren().get(1).getParent());


        assertEquals(438290, test.getCategoryId());
        assertEquals("Object No. 2", test.getComment());
        assertEquals(532, test.getColor());
        assertEquals(2, test.getChildren().size());
        assertEquals(456, test.getNumber());
        assertEquals("complexCollObject52", test.getName());
        assertNull(test.getTypes());

        // Object nr. 2, child nr. 1
        // =========================
        // self-href: http://localhost:60549//complexChildren2/4
        // children:
        //   1.: http://localhost:60549/complexChildren3/6
        // parent: http://localhost:60549/complexCollObjects/52 (=O2)
        // color: 5431
        // comment: Number 4...
        // category_id: 5
        // name: is...
        // number: 5547
        // types: [$myGreatType]

        assertEquals(5431,test.getChildren().get(0).getColor());
        assertEquals("is...", test.getChildren().get(0).getName());
        assertEquals("Number 4...",test.getChildren().get(0).getComment());
        assertEquals(5,test.getChildren().get(0).getCategoryId());
        assertEquals(1,test.getChildren().get(0).getChildren().size());
        assertEquals(5547, test.getChildren().get(0).getNumber());
        assertEquals(1, test.getChildren().get(0).getTypes().size());
        assertEquals("$myGreatType", test.getChildren().get(0).getTypes().get(0));
        assertEquals(1, test.getChildren().get(0).getChildren().size());
        assertSame(test, test.getChildren().get(0).getParent());

        // Object nr. 2, Child nr. 1, subchild nr. 1 (the only one)
        // ========================================================
        // self-href: http://localhost:60549//complexChildren3/6
        // children: []
        // parent: http://localhost:60549/complexChildren2/4 (=O2C1)
        // color: 456123
        // comment: I'm the sixth subchild
        // category_id: 2323
        // name: dolor
        // number: 5743534
        // types: [Knödel,heyho]

        subChild = test.getChildren().get(0).getChildren().get(0);
        assertEquals(5743534, subChild.getNumber());
        assertEquals(456123, subChild.getColor());
        assertEquals("I'm the sixth subchild", subChild.getComment());
        assertEquals(2323, subChild.getCategoryId());
        assertEquals("dolor", subChild.getName());
        assertEquals(2, subChild.getTypes().size());
        assertEquals("Knödel", subChild.getTypes().get(0));
        assertEquals("heyho", subChild.getTypes().get(1));
        assertSame(test.getChildren().get(0), subChild.getParent());
        assertNull(subChild.getChildren());

        // Object nr. 2, child nr. 2:
        // ==========================
        // self-href: http://localhost:61312//complexChildren2/5
        // children: []
        // parent: http://localhost:61312/complexCollObjects/52 (=O2)
        // color: 43289
        // comment: Number 5...
        // category_id: 10101
        // name: blah
        // number: 45465
        // types: [some type]

        assertEquals(43289,test.getChildren().get(1).getColor());
        assertEquals("blah", test.getChildren().get(1).getName());
        assertEquals("Number 5...",test.getChildren().get(1).getComment());
        assertEquals(10101,test.getChildren().get(1).getCategoryId());
        assertEquals(45465, test.getChildren().get(1).getNumber());
        assertEquals(1, test.getChildren().get(1).getTypes().size());
        assertEquals("some type", test.getChildren().get(1).getTypes().get(0));
        assertSame(test, test.getChildren().get(1).getParent());
        // Object nr. 2, child nr. 2 has no children
        assertNull(test.getChildren().get(1).getChildren());

        // Object nr. 3
        // ============
        // self-href: http://localhost:61312/complexCollObjects/7486465
        // children:
        //   1.: http://localhost:61312/complexChildren2/6
        //   2.: http://localhost:61312/complexChildren2/1
        // name: complexCollObject7486465
        // category_id: 543890
        // comment: null
        // number: 542
        // types: [some, list, of, types]
        test = objects.get(2);

        // Check child-parent-relationship
        assertSame(test, test.getChildren().get(0).getParent());
        assertSame(objects.get(0), test.getChildren().get(1).getParent()); //!

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

        // Object nr. 3, child nr. 1
        // =========================
        // self-href: http://localhost:65002//complexChildren2/6
        // children
        //   1.: http://localhost:65002/complexChildren3/3 (=O1C3S2 (!))
        // parent: http://localhost:65002/complexCollObjects/7486465
        // color: 5324,
        // comment: Number [6]...
        // category_id: 5234789
        // name: 5834543
        // number: -17
        // types: []

        assertEquals(5324,test.getChildren().get(0).getColor());
        assertEquals("5834543", test.getChildren().get(0).getName());
        assertEquals("Number [6]...",test.getChildren().get(0).getComment());
        assertEquals(5234789,test.getChildren().get(0).getCategoryId());
        assertEquals(-17, test.getChildren().get(0).getNumber());
        assertEquals(0, test.getChildren().get(0).getTypes().size());
        assertEquals(1, test.getChildren().get(0).getChildren().size());
        assertSame(test, test.getChildren().get(0).getParent());

        // Object nr. 3, child nr. 1, subchild nr. 1
        // =========================================
        // (is the same as object nr. 1, child nr. 3, subchild nr. 2!)
        //
        // self-href: http://localhost:65002//complexChildren3/3
        // children: []
        // color: 818147
        // comment: I'm the third subchild
        // category_id: 0
        // name: mind!
        // number: 574389
        // types: [${myType}]

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

        // Object nr. 3, Child nr. 2
        // =========================
        // (is the same as object nr. 1, child nr. 1!)
        //
        // self-href: http://localhost:65002//complexChildren2/1
        // children
        //   1.: http://localhost:65002/complexChildren3/1
        // parent: http://localhost:65002/complexCollObjects/1432
        // color: 1345
        // comment: Number 1...
        // category_id: 5
        // name: is...
        // number: 5547
        // types: [$myGreatType]

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

        // Object nr. 3, Child nr. 2, subchild nr. 1
        // =========================================
        // (is the same as object nr. 1, child nr. 1, subchild nr. 1
        // (because its parent is the same as object nr. 1, child nr. 1))
        //
        // self-href: /complexChildren3/1
        // children: []
        // parent: /complexChildren2/1 (= O1C1)
        // color: 747474
        // comment: I'm the first subchild
        // category_id: 10000
        // name: state...
        // number: null
        // types: [   ]

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

        assertEquals(13, Storesthal.getStatistics().get("httpCalls"));

        Storesthal.resetStatistics();

    }


}
