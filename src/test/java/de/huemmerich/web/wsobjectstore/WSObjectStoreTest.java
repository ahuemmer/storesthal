package de.huemmerich.web.wsobjectstore;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockSettings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import de.huemmerich.web.wsobjectstore.complextestobjects.*;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.commons.text.StringSubstitutor;
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

@WireMockSettings(failOnUnmatchedRequests = true)
public class WSObjectStoreTest {

    /**
     * The folder (within the test resources) where the json files are stored.
     */
    private static final String JSON_RESOURCE_FOLDER="json";

    @InjectServer
    WireMockServer serverMock;

    @ConfigureWireMock
    Options options = wireMockConfig()
            .dynamicPort();
            //.notifier(new ConsoleNotifier(true));

    private void configureServerMock(String url, String responseFileName, Map<String, String> additionalSubstitutions) throws IOException {

        Map<String, String> substitutions =  new HashMap<>();
        substitutions.put("port",String.valueOf(serverMock.port()));

        if (additionalSubstitutions!=null) {
            substitutions.putAll(additionalSubstitutions);
        }

        serverMock.addStubMapping(stubFor(get(urlEqualTo(url))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody(getJsonFileContent(responseFileName, substitutions)))));
    }

    private void configureServerMock(String url, String responseFileName) throws IOException {
        configureServerMock(url,responseFileName,null);
    }

    @Test
    public void testWSObjectStoreGetComplexObject1() throws WSObjectStoreException, IOException {

        configureServerMock("/complexObjects/1", "complexObject1.json");
        serverMock.start();

        ComplexObject1 test = new WSObjectStore().<ComplexObject1>getObject("http://localhost:"+serverMock.port()+"/complexObjects/1", ComplexObject1.class);

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
        configureServerMock("/complexChildren/1", "simpleChildObject1.json");

        serverMock.start();

        ComplexObjectWithSingleChild test = new WSObjectStore().<ComplexObjectWithSingleChild>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithSingleChildren/1", ComplexObjectWithSingleChild.class);

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

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json");
        configureServerMock("/complexChildren2/1", "simpleChildObject2.json", Map.of("childId","12345", "childName", "Testchild 1!"));
        configureServerMock("/complexChildren2/2", "simpleChildObject2.json", Map.of("childId","815", "childName", "Testchild 2!"));
        configureServerMock("/complexChildren2/3", "simpleChildObject2.json", Map.of("childId","4711", "childName", "Testchild 3!"));

        serverMock.start();

        ComplexObjectWithMultipleChildren1 test = new WSObjectStore().<ComplexObjectWithMultipleChildren1>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren1.class);

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
        configureServerMock("/complexObjectsWithMultipleChildren2/1", "complexObjectWithMultipleChildren1.json");
        configureServerMock("/complexChildren2/1", "simpleChildObject2.json", Map.of("childId","12345", "childName", "Testchild 1!"));
        configureServerMock("/complexChildren2/2", "simpleChildObject2.json", Map.of("childId","815", "childName", "Testchild 2!"));
        configureServerMock("/complexChildren2/3", "simpleChildObject2.json", Map.of("childId","4711", "childName", "Testchild 3!"));

        serverMock.start();

        ComplexObjectWithMultipleChildren2 test = new WSObjectStore().<ComplexObjectWithMultipleChildren2>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren2/1", ComplexObjectWithMultipleChildren2.class);

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
    //Pretty much the same as the two before (in testWSObjectStoreGetComplexObjectWithMultipleChildren1), but now we use
    //ComplexObjectWithMultipleChildren3 which has an array as collection insted of a (Linked)List.
    public void testWSObjectStoreGetComplexObjectWithMultipleChildrenInArray() throws WSObjectStoreException, IOException {
        configureServerMock("/complexObjectsWithMultipleChildren2/1", "complexObjectWithMultipleChildren1.json");
        configureServerMock("/complexChildren2/1", "simpleChildObject2.json", Map.of("childId","12345", "childName", "Testchild 1!"));
        configureServerMock("/complexChildren2/2", "simpleChildObject2.json", Map.of("childId","815", "childName", "Testchild 2!"));
        configureServerMock("/complexChildren2/3", "simpleChildObject2.json", Map.of("childId","4711", "childName", "Testchild 3!"));

        serverMock.start();

        assertThrows(WSObjectStoreException.class, () -> {
            ComplexObjectWithMultipleChildren3 test = new WSObjectStore().<ComplexObjectWithMultipleChildren3>getObject("http://localhost:" + serverMock.port() + "/complexObjectsWithMultipleChildren2/1", ComplexObjectWithMultipleChildren3.class);
        });

        //ARRAYS ARE NOT SUPPORTED (YET??)
        //Whenever this is the case, the following assertions should succeed:

        /*assertNotNull(test);
        assertEquals(1508, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("itsme...", test.getComment());
        assertEquals("Test3!", test.getName());
        assertEquals(9, test.getNumber());
        assertEquals("neither", test.getType());

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

        configureServerMock("/complexObjectsWithMultipleChildren1/1", "complexObjectWithMultipleChildren1.json");
        configureServerMock("/complexChildren2/1", "simpleChildObjectWithParentRelation.json", Map.of("childId","654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/2", "simpleChildObjectWithParentRelation.json", Map.of("childId","158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren1/1"));
        configureServerMock("/complexChildren2/3", "simpleChildObjectWithParentRelation.json", Map.of("childId","1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren1/1"));

        serverMock.start();

        WSObjectStore.resetStatistics();

        ComplexObjectWithMultipleChildren4 test = new WSObjectStore().<ComplexObjectWithMultipleChildren4>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1", ComplexObjectWithMultipleChildren4.class);

        assertNotNull(test);
        assertEquals(1508, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("itsme...", test.getComment());
        assertEquals("Test3!", test.getName());
        assertEquals(9, test.getNumber());
        assertEquals("neither", test.getType());

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

        configureServerMock("/complexObjectsWithMultipleChildren2/1", "complexObjectWithMultipleChildren1.json");
        configureServerMock("/complexChildren2/1", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","654321", "childName", "Testchild with parent 1.", "parent", "/complexObjectsWithMultipleChildren2/1"));
        configureServerMock("/complexChildren2/2", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","158", "childName", "Testchild with parent 2.", "parent", "/complexObjectsWithMultipleChildren2/1"));
        configureServerMock("/complexChildren2/3", "simpleChildObjectWithParentRelationCollection.json", Map.of("childId","1147", "childName", "Testchild with parent 3.", "parent", "/complexObjectsWithMultipleChildren2/1"));

        serverMock.start();

        WSObjectStore.resetStatistics();

        ComplexObjectWithMultipleChildren5 test = new WSObjectStore().<ComplexObjectWithMultipleChildren5>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren2/1", ComplexObjectWithMultipleChildren5.class);

        assertNotNull(test);
        assertEquals(1508, test.getCategoryId());
        assertEquals(22101579, test.getColor());
        assertEquals("itsme...", test.getComment());
        assertEquals("Test3!", test.getName());
        assertEquals(9, test.getNumber());
        assertEquals("neither", test.getType());

        List<ComplexChildWithParentRelationCollection> children = test.getChildren();
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

        for (ComplexChildWithParentRelationCollection child: children) {
            assertEquals(1, child.getParents().size());
            //Use == here --> really the same object!
            assertTrue(test==child.getParents().get(0));
        }
    }

    private static String getJsonFileContent(String fileName, Map<String,String> substitutes) throws IOException {
        File file = new File(WSObjectStoreTest.class.getClassLoader().getResource(JSON_RESOURCE_FOLDER+"/"+fileName).getFile());
        String fileContent = new String(Files.readAllBytes(file.toPath()), Charset.forName("UTF-8"));
        StringSubstitutor sub = new StringSubstitutor(substitutes, "${", "}");
        return sub.replace(fileContent,substitutes);
    }

}
