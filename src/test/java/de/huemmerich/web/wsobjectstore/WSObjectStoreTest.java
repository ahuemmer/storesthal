package de.huemmerich.web.wsobjectstore;

import com.github.jenspiegsa.wiremockextension.ConfigureWireMock;
import com.github.jenspiegsa.wiremockextension.InjectServer;
import com.github.jenspiegsa.wiremockextension.WireMockSettings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.common.ConsoleNotifier;
import com.github.tomakehurst.wiremock.core.Options;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import de.huemmerich.web.wsobjectstore.complextestobjects.*;
import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

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
    public void testWSObjectStoreGetComplexObject1() {
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
    public void testWSObjectStoreGetComplexObjectWithSingleChild() {
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
    public void testWSObjectStoreGetComplexObjectWithMultipleChildren() {
        serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexObjectsWithMultipleChildren/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren/1\"}, \"child\":{\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren/1\"}},\n" +
                                "    \"color\": 22101579,\n" +
                                "    \"comment\": \"itsme...\",\n" +
                                "    \"category_id\": 1508,\n" +
                                "    \"name\": \"Test3!\",\n" +
                                "    \"number\": 9,\n" +
                                "    \"type\": \"neither\"\n" +
                                "}"))));

        //TODO: Ausformulieren, ...

        /*serverMock.addStubMapping(stubFor(get(urlEqualTo("/complexChildren/1"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/hal+json;charset=UTF-8")
                        .withBody("{\n" +
                                "\"_links\": {\"self\": {\"href\":\"http://localhost:"+serverMock.port()+"/complexChildren/1\"}},\n" +
                                "    \"name\": \"Testchild!\"\n" +
                                "}"))));*/

        serverMock.start();

        ComplexObjectWithMultipleChildren test = new WSObjectStore().<ComplexObjectWithMultipleChildren>getObject("http://localhost:"+serverMock.port()+"/complexObjectsWithMultipleChildren/1", ComplexObjectWithMultipleChildren.class);

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
        //assert...
    }

}
