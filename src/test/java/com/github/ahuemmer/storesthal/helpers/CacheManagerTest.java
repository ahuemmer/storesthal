package com.github.ahuemmer.storesthal.helpers;

import com.github.ahuemmer.storesthal.Storesthal;
import com.github.ahuemmer.storesthal.configuration.StoresthalConfigurationFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This class is not intended to be a complete test suite for the {@link com.github.ahuemmer.storesthal.helpers.CacheManager}
 * class, but rather to increase its test coverage where the other tests don't apply.
 */

@ExtendWith(OutputCaptureExtension.class)
public class CacheManagerTest {

    @Nested
    @DisplayName("with caching enabled")
    class With_caching_enabled {

        CacheManager instance;

        @BeforeEach
        void setUp() {
            instance = CacheManager.getInstance(StoresthalConfigurationFactory.getDefaultConfiguration());
            CacheManager.clearAllCaches(true);
        }

        @Test
        @DisplayName("creates an instance")
        void creates_an_instance() {
            assertNotNull(instance);
        }

        @Test
        @DisplayName("puts_an_object_in_the_cache_with_links_and_returns_it")
        void puts_an_object_in_the_cache_with_links_and_returns_it() {

            URI uri = URI.create("https://test");

            CacheManager.putObjectInCache(uri, "test", "testCache", null);
            assertEquals("test", CacheManager.getObjectFromCache(uri, String.class, "testCache", false));

            CacheManager.putObjectInCache(uri, "test2", "testCache2", null, true);
            assertEquals("test2", CacheManager.getObjectFromCache(uri, String.class, "testCache2", false, true));

        }

        @Test
        @DisplayName("puts_an_object_in_the_cache_without_links_and_returns_it")
        void puts_an_object_in_the_cache_without_links_and_returns_it() {

            URI uri = URI.create("https://test");

            CacheManager.putObjectInCache(uri, "test", "testCache", null, false);
            assertEquals("test", CacheManager.getObjectFromCache(uri, String.class, "testCache", false, false));

        }

        @Test
        @DisplayName("logs a warning when clearCache is called with \"withLinks\" parameter being null")
        void logs_a_warning_when_clearCache_is_called_with_withLinks_parameter_being_null(CapturedOutput output) {
            CacheManager.clearCache("test", true, null);
            assertTrue(output.getOut().contains("Please supply a valid boolean parameter"));
        }

        @Test
        @DisplayName("returns the cached objects count correctly")
        void returns_the_cached_objects_count_correctly() {
            URI uri1 = URI.create("https://test1");
            URI uri2 = URI.create("https://test2");

            CacheManager.putObjectInCache(uri1, "test1", "testCache", null);
            CacheManager.putObjectInCache(uri2, "test2", "testCache", null);

            assertEquals(2, CacheManager.getCachedObjectCount("testCache"));

            assertEquals(0, CacheManager.getCachedObjectCount("this cache does not exist"));
        }
    }

    @Nested
    @DisplayName("with caching disabled")
    class With_caching_disabled {

        CacheManager instance;

        @BeforeEach
        void setUp() {
            Storesthal.init(new StoresthalConfigurationFactory().setDisableCaching(true).getConfiguration());
            CacheManager.clearAllCaches(true);
        }

        @Test
        @DisplayName("getObjectFromCache returns null")
        void getObjectFromCache_returns_null() {
            URI uri = URI.create("https://test");

            CacheManager.putObjectInCache(uri, "test", "testCache", null);
            assertNull(CacheManager.getObjectFromCache(uri, String.class, "testCache", false));

        }
    }

}
