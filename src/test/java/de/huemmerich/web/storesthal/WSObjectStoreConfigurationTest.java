package de.huemmerich.web.storesthal;

import de.huemmerich.web.storesthal.configuration.WSObjectStoreConfiguration;
import de.huemmerich.web.storesthal.configuration.WSObjectStoreConfigurationFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for the {@link WSObjectStoreConfiguration} and {@link WSObjectStoreConfigurationFactory}.
 */
public class WSObjectStoreConfigurationTest {

    /**
     * Make sure, a default configuration can be created and is initialized correctly.
     */
    @Test
    public void canCreateDefaultConfiguration() {
        WSObjectStoreConfiguration conf = WSObjectStoreConfigurationFactory.getDefaultConfiguration();
        assertEquals(WSObjectStoreConfiguration.DEFAULT_DEFAULT_CACHE_SIZE, conf.getDefaultCacheSize());
        assertEquals(WSObjectStoreConfiguration.DEFAULT_ANNOTATIONLESS, conf.isAnnotationless());
        assertEquals(WSObjectStoreConfiguration.DEFAULT_CACHING_DISABLED, conf.isCachingDisabled());
    }

    /**
     * Make sure, a configuration based on some custom values can be created and modified with the values being
     * taken over correctly.
     */
    @Test
    public void canCreateCustomConfiguration() {
        WSObjectStoreConfigurationFactory factory = new WSObjectStoreConfigurationFactory();

        WSObjectStoreConfiguration conf = factory
                .setAnnotationless(true)
                .setDefaultCacheSize(10)
                .setDisableCaching(true)
                .getConfiguration();

        assertEquals(10, conf.getDefaultCacheSize());
        assertTrue(conf.isAnnotationless());
        assertTrue(conf.isCachingDisabled());

        assertEquals(10, factory.getDefaultCacheSize());
        assertTrue(factory.isAnnotationless());
        assertTrue(factory.isCachingDisabled());

        conf = factory.setDefaultCacheSize(1234).getConfiguration();

        assertEquals(1234, conf.getDefaultCacheSize());
        assertTrue(conf.isAnnotationless());
        assertTrue(conf.isCachingDisabled());

        assertEquals(1234, factory.getDefaultCacheSize());
        assertTrue(factory.isAnnotationless());
        assertTrue(factory.isCachingDisabled());


    }

}
