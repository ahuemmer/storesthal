package com.github.ahuemmer.storesthal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("rawtypes")
public class PrimitiveRetrievalTest extends AbstractJsonTemplateBasedTest {

    private static final String TEST_CACHE_NAME="com.github.ahuemmer.storesthal.test.cache";

    @BeforeEach
    public void init() {
        Storesthal.resetStatistics();
        Storesthal.clearAllCaches();
    }

    @Test
    public void willRetrieveIntegers() throws StoresthalException {
        configureServerMock("/get/an/integer", "10");
        configureServerMock("/get/another/integer", "\"33\"");
        configureServerMock("/get/a/faulty/integer", "a");
        configureServerMock("/get/a/negative/integer", "-101001");
        configureServerMock("/get/another/negative/integer", "\"-65478564\"");
        serverMock.start();

        Integer test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/an/integer");
        assertEquals(10, test);

        test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/another/integer");
        assertEquals(33, test);

        assertThrows(StoresthalException.class, () -> Storesthal.getInteger("http://localhost:" + serverMock.port() + "/get/a/faulty/integer"));

        test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/a/negative/integer");
        assertEquals(-101001, test);

        test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/another/negative/integer");
        assertEquals(-65478564, test);

    }

    @SuppressWarnings("rawtypes")
    @Test
    public void willRetrieveIntegersWithCommonCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/an/integer", "5784390");
        serverMock.start();

        Integer test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/an/integer", true);
        assertEquals(5784390, test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/an/integer", true);
            assertEquals(5784390, test);
        }

        //Still, there should have only one http call occured, as the integer was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));


    }

    @Test
    public void willRetrieveIntegersWithSpecificCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/an/integer", "-543909");
        serverMock.start();

        Integer test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/an/integer", TEST_CACHE_NAME);
        assertEquals(-543909, test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/an/integer", TEST_CACHE_NAME);
            assertEquals(-543909, test);
        }

        //Still, there should have only one http call occured, as the integer was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        Storesthal.clearCache(TEST_CACHE_NAME, false);
        for (int i=0; i<10; i++) {
            test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/an/integer", TEST_CACHE_NAME);
            assertEquals(-543909, test);
        }

        assertEquals(2, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(19, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(2, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));


    }

    @Test
    public void willRetrieveDoubles() throws StoresthalException {
        configureServerMock("/get/a/double", "543.432");
        configureServerMock("/get/another/double", "\"33.333\"");
        configureServerMock("/get/a/faulty/double", "b");
        configureServerMock("/get/a/negative/double", "-101001.0011");
        configureServerMock("/get/another/negative/double", "\"-65478564.543\"");
        configureServerMock("/get/an/integer/double", "543543");
        serverMock.start();

        Double test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/a/double");
        assertEquals(543.432, test);

        test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/another/double");
        assertEquals(33.333, test);

        assertThrows(StoresthalException.class, () -> Storesthal.getDouble("http://localhost:" + serverMock.port() + "/get/a/faulty/double"));

        test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/a/negative/double");
        assertEquals(-101001.0011, test);

        test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/another/negative/double");
        assertEquals(-65478564.543, test);

        test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/an/integer/double");
        assertEquals(543543.0, test);

    }

    @Test
    public void willRetrieveDoublesWithCommonCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/a/double", "-56.1234");
        serverMock.start();

        Double test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/a/double", true);
        assertEquals(-56.1234, test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/a/double", true);
            assertEquals(-56.1234, test);
        }

        //Still, there should have only one http call occured, as the integer was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));


    }

    @Test
    public void willRetrieveDoublesWithSpecificCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/a/double", "584390584390");
        serverMock.start();

        Double test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/a/double", TEST_CACHE_NAME);
        assertEquals(584390584390.0, test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/a/double", TEST_CACHE_NAME);
            assertEquals(584390584390.0, test);
        }

        //Still, there should have only one http call occured, as the integer was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        Storesthal.clearCache(TEST_CACHE_NAME, false);
        for (int i=0; i<10; i++) {
            test = Storesthal.getDouble("http://localhost:"+serverMock.port()+"/get/a/double", TEST_CACHE_NAME);
            assertEquals(584390584390.0, test);
        }

        assertEquals(2, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(19, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(2, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));


    }


    @Test
    public void willRetrieveBooleans() throws StoresthalException {
        configureServerMock("/get/a/boolean", "true");
        configureServerMock("/get/another/boolean", "\"false\"");
        configureServerMock("/get/a/faulty/boolean", "c");
        serverMock.start();

        Boolean test = Storesthal.getBoolean("http://localhost:"+serverMock.port()+"/get/a/boolean");
        assertEquals(true, test);

        test = Storesthal.getBoolean("http://localhost:"+serverMock.port()+"/get/another/boolean");
        assertEquals(false, test);

        assertThrows(StoresthalException.class, () -> Storesthal.getBoolean("http://localhost:" + serverMock.port() + "/get/a/faulty/boolean"));

    }

    @Test
    public void willRetrieveBooleansWithCommonCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/a/boolean", "\"true\"");
        serverMock.start();

        Boolean test = Storesthal.getBoolean("http://localhost:"+serverMock.port()+"/get/a/boolean", true);
        assertEquals(true, test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getBoolean("http://localhost:"+serverMock.port()+"/get/a/boolean", true);
            assertEquals(true, test);
        }

        //Still, there should have only one http call occured, as the integer was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));


    }

    @Test
    public void willRetrieveBooleansWithSpecificCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/a/boolean", "false");
        serverMock.start();

        Boolean test = Storesthal.getBoolean("http://localhost:"+serverMock.port()+"/get/a/boolean", TEST_CACHE_NAME);
        assertEquals(false, test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getBoolean("http://localhost:"+serverMock.port()+"/get/a/boolean", TEST_CACHE_NAME);
            assertEquals(false, test);
        }

        //Still, there should have only one http call occured, as the integer was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        Storesthal.clearCache(TEST_CACHE_NAME, false);
        for (int i=0; i<10; i++) {
            test = Storesthal.getBoolean("http://localhost:"+serverMock.port()+"/get/a/boolean", TEST_CACHE_NAME);
            assertEquals(false, test);
        }

        assertEquals(2, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(19, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(2, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));


    }


    @Test
    public void willRetrieveStrings() throws StoresthalException {
        configureServerMock("/get/a/string", "abc");
        configureServerMock("/get/another/string", "\"hey!");
        configureServerMock("/get/an/empty/string", "");
        configureServerMock("/get/a/numeric/string", "534890");
        configureServerMock("/get/a/spaced/string", "Hello! This is a test.");
        //Example taken from en.wikipedia.org:
        configureServerMock("/get/a/json/string", "{ \"firstName\": \"John\", \"lastName\": \"Smith\", \"isAlive\": true, \"age\": 27, \"address\": { \"streetAddress\": \"21 2nd Street\", \"city\": \"New York\", \"state\": \"NY\", \"postalCode\": \"10021-3100\" }, \"phoneNumbers\": [ { \"type\": \"home\", \"number\": \"212 555-1234\" }, {\"type\": \"office\", \"number\": \"646 555-4567\" } ], \"children\": [], \"spouse\": null}");
        serverMock.start();

        String test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/string");
        assertEquals("abc", test);

        test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/another/string");
        assertEquals("\"hey!", test);

        test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/an/empty/string");
        assertNull(test); //!

        test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/numeric/string");
        assertEquals("534890", test);

        test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/spaced/string");
        assertEquals("Hello! This is a test.", test);

        test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/json/string");
        assertEquals("{ \"firstName\": \"John\", \"lastName\": \"Smith\", \"isAlive\": true, \"age\": 27, \"address\": { \"streetAddress\": \"21 2nd Street\", \"city\": \"New York\", \"state\": \"NY\", \"postalCode\": \"10021-3100\" }, \"phoneNumbers\": [ { \"type\": \"home\", \"number\": \"212 555-1234\" }, {\"type\": \"office\", \"number\": \"646 555-4567\" } ], \"children\": [], \"spouse\": null}", test);
        assertTrue(test instanceof String);

    }

    @Test
    public void willRetrieveStringsWithCommonCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/a/string", "fjsdlöfsdkeöl");
        serverMock.start();

        String test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/string", true);
        assertEquals("fjsdlöfsdkeöl", test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/string", true);
            assertEquals("fjsdlöfsdkeöl", test);
        }

        //Still, there should have only one http call occured, as the String was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(Storesthal.COMMON_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(Storesthal.COMMON_CACHE_NAME));


    }

    @Test
    public void willRetrieveStringsWithSpecificCacheUsage() throws StoresthalException {

        assertEquals(0, (Integer) Storesthal.getStatistics().get("httpCalls"));

        configureServerMock("/get/a/string", "\"%$§%§$85943!|💥💜🎉€µ'");
        serverMock.start();

        String test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/string", TEST_CACHE_NAME);
        assertEquals("\"%$§%§$85943!|💥💜🎉€µ'", test);

        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertNull(((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        for (int i=0; i<10; i++) {
            test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/string", TEST_CACHE_NAME);
            assertEquals("\"%$§%§$85943!|💥💜🎉€µ'", test);
        }

        //Still, there should have only one http call occured, as the String was cached already...
        assertEquals(1, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(10, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(1, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));

        Storesthal.clearCache(TEST_CACHE_NAME, false);
        for (int i=0; i<10; i++) {
            test = Storesthal.getString("http://localhost:"+serverMock.port()+"/get/a/string", TEST_CACHE_NAME);
            assertEquals("\"%$§%§$85943!|💥💜🎉€µ'", test);
        }

        assertEquals(2, (Integer) Storesthal.getStatistics().get("httpCalls"));
        assertEquals(19, (Integer) ((Map) Storesthal.getStatistics().get("cacheHits")).get(TEST_CACHE_NAME));
        assertEquals(2, (Integer) ((Map) Storesthal.getStatistics().get("cacheMisses")).get(TEST_CACHE_NAME));


    }

}
