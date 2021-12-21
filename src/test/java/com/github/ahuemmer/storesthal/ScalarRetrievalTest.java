package com.github.ahuemmer.storesthal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ScalarRetrievalTest extends AbstractJsonTemplateBasedTest {

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

        assertThrows(StoresthalException.class, () -> {
            Storesthal.getInteger("http://localhost:" + serverMock.port() + "/get/a/faulty/integer");
        });

        test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/a/negative/integer");
        assertEquals(-101001, test);

        test = Storesthal.getInteger("http://localhost:"+serverMock.port()+"/get/another/negative/integer");
        assertEquals(-65478564, test);

    }

    @Test
    public void willRetrieveIntegersWitChommonCacheUsage() throws StoresthalException {

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



}
