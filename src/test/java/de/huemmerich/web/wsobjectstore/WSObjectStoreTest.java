package de.huemmerich.web.wsobjectstore;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockSettings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import de.huemmerich.web.wsobjectstore.complextestobjects.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WireMockSettings(failOnUnmatchedRequests = true)
public class WSObjectStoreTest {

    @InjectServer
    WireMockServer serverMock;

    @ConfigureWireMock
    Options options = wireMockConfig()
            .dynamicPort()
            .notifier(new ConsoleNotifier(true));

    @Test
    public void testWSObjectStoreGetComplexObject1() throws WSObjectStoreException {
        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexObjects/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexObjects/1\"}},\n" +
                                "    \"color\": 10157977,\n" +
                                "    \"comment\": \"\",\n" +
                                "    \"category_id\": 1,\n" +
                                "    \"name\": \"Test!\",\n" +
                                "    \"number\": 2,\n" +
                                "    \"type\": \"expense\"\n" +
                                "}"))));
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
    public void testWSObjectStoreGetComplexObjectWithSingleChild() throws WSObjectStoreException {
        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexObjectsWithSingleChildren/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexObjectsWithSingleChildren/1\"}, \"child\":{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren/1\"}},\n" +
                                "    \"color\": 101579,\n" +
                                "    \"comment\": \"oi...\",\n" +
                                "    \"category_id\": 4711,\n" +
                                "    \"name\": \"Test2!\",\n" +
                                "    \"number\": 3,\n" +
                                "    \"type\": \"income\"\n" +
                                "}"))));
        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren/1\"}},\n" +
                                "    \"name\": \"Testchild!\"\n" +
                                "}"))));

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
    public void testWSObjectStoreGetComplexObjectWithMultipleChildren1() throws WSObjectStoreException {
        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexObjectsWithMultipleChildren1/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren1/1\"}, \"children\":[{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/1\"},{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/2\"},{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/3\"}]},\n" +
                                "    \"color\": 22101579,\n" +
                                "    \"comment\": \"itsme...\",\n" +
                                "    \"category_id\": 1508,\n" +
                                "    \"name\": \"Test3!\",\n" +
                                "    \"number\": 9,\n" +
                                "    \"type\": \"neither\"\n" +
                                "}"))));

        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren2/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/1\"}},\n" +
                                "    \"childId\": 12345,\n" +
                                "    \"name\": \"Testchild 1!\"\n" +
                                "}"))));

        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren2/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/2\"}},\n" +
                                "    \"childId\": 815,\n" +
                                "    \"name\": \"Testchild 2!\"\n" +
                                "}"))));

        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren2/3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/3\"}},\n" +
                                "    \"childId\": 4711,\n" +
                                "    \"name\": \"Testchild 3!\"\n" +
                                "}"))));

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

    }

    @Test
    //Pretty much the same as before (in testWSObjectStoreGetComplexObjectWithMultipleChildren1), but now we use
    //ComplexObjectWithMultipleChildren2 which has a concrete implementation of a collection (LinkedList) instead
    //of an interface.
    public void testWSObjectStoreGetComplexObjectWithMultipleChildren2() throws WSObjectStoreException {
        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexObjectsWithMultipleChildren2/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren2/1\"}, \"children\":[{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/1\"},{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/2\"},{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/3\"}]},\n" +
                                "    \"color\": 22101579,\n" +
                                "    \"comment\": \"itsme...\",\n" +
                                "    \"category_id\": 1508,\n" +
                                "    \"name\": \"Test3!\",\n" +
                                "    \"number\": 9,\n" +
                                "    \"type\": \"neither\"\n" +
                                "}"))));

        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren2/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/1\"}},\n" +
                                "    \"childId\": 12345,\n" +
                                "    \"name\": \"Testchild 1!\"\n" +
                                "}"))));

        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren2/2"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/2\"}},\n" +
                                "    \"childId\": 815,\n" +
                                "    \"name\": \"Testchild 2!\"\n" +
                                "}"))));

        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren2/3"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren2/3\"}},\n" +
                                "    \"childId\": 4711,\n" +
                                "    \"name\": \"Testchild 3!\"\n" +
                                "}"))));

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

    }

}
