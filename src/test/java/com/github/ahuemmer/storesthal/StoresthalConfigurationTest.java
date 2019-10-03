package com.github.ahuemmer.storesthal;

import com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration;
import com.github.ahuemmer.storesthal.configuration.StoreresthalConfigurationFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test suite for the {@link StoresthalConfiguration} and {@link StoreresthalConfigurationFactory}.
 */
public class StoresthalConfigurationTest {

    /**
     * Make sure, a default configuration can be created and is initialized correctly.
     */
    @Test
    public void canCreateDefaultConfiguration() {
        StoresthalConfiguration conf = StoreresthalConfigurationFactory.getDefaultConfiguration();
        assertEquals(StoresthalConfiguration.DEFAULT_DEFAULT_CACHE_SIZE, conf.getDefaultCacheSize());
        assertEquals(StoresthalConfiguration.DEFAULT_ANNOTATIONLESS, conf.isAnnotationless());
        assertEquals(StoresthalConfiguration.DEFAULT_CACHING_DISABLED, conf.isCachingDisabled());
    }

    /**
     * Make sure, a configuration based on some custom values can be created and modified with the values being
     * taken over correctly.
     */
    @Test
    public void canCreateCustomConfiguration() {
        StoreresthalConfigurationFactory factory = new StoreresthalConfigurationFactory();

        StoresthalConfiguration conf = factory
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
