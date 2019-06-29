package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.configuration.WSObjectStoreConfiguration;
import de.huemmerich.web.wsobjectstore.configuration.WSObjectStoreConfigurationFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class WSObjectStoreConfigurationTest {

    @Test
    public void canCreateDefaultConfiguration() {
        WSObjectStoreConfiguration conf = WSObjectStoreConfigurationFactory.getDefaultConfiguration();
        assertEquals(WSObjectStoreConfiguration.DEFAULT_DEFAULT_CACHE_SIZE, conf.getDefaultCacheSize());
        assertEquals(WSObjectStoreConfiguration.DEFAULT_ANNOTATIONLESS, conf.isAnnotationless());
        assertEquals(WSObjectStoreConfiguration.DEFAULT_CACHING_DISABLED, conf.isCachingDisabled());
    }

    @Test
    public void canCreateCustomConfiguration() {
        WSObjectStoreConfigurationFactory factory = new WSObjectStoreConfigurationFactory();

        WSObjectStoreConfiguration conf = factory
                .setAnnotationless(true)
                .setDefaultCacheSize(10)
                .setDisableCaching(true)
                .getConfiguration();

        assertEquals(10, conf.getDefaultCacheSize());
        assertEquals(true, conf.isAnnotationless());
        assertEquals(true, conf.isCachingDisabled());

        assertEquals(10, factory.getDefaultCacheSize());
        assertEquals(true, factory.isAnnotationless());
        assertEquals(true, factory.isCachingDisabled());

        conf = factory.setDefaultCacheSize(1234).getConfiguration();

        assertEquals(1234, conf.getDefaultCacheSize());
        assertEquals(true, conf.isAnnotationless());
        assertEquals(true, conf.isCachingDisabled());

        assertEquals(1234, factory.getDefaultCacheSize());
        assertEquals(true, factory.isAnnotationless());
        assertEquals(true, factory.isCachingDisabled());


    }

}
