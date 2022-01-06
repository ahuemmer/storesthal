package com.github.ahuemmer.storesthal.helpers;

import com.github.ahuemmer.storesthal.Storesthal;
import com.github.ahuemmer.storesthal.StoresthalException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

public class PrimitiveValueRetriever {

    private PrimitiveValueRetriever() {}

    private static int httpCalls = 0;

    /**
     * Retrieve a primitive value (no special object...) from the given URL.
     * @param url       The URL to retrieve the primitive from.
     * @param doCache   Whether to cache the results or not. See cacheName parameter for details.
     * @param cacheName The name of the cache to used when retrieving the primitive or NULL, if no cache is to be used.
     * @return The primitive retrieved.
     * @throws com.github.ahuemmer.storesthal.StoresthalException If it was not possible to retrieve a Primitive
     */
    public static <T> T getPrimitive(Class<T> primitiveClass, String url, boolean doCache, String cacheName) throws StoresthalException {

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new StoresthalException("Could not create URI from url\"" + url + "\"!", e);
        }

        T result;
        String cacheNameToUse = Storesthal.COMMON_CACHE_NAME;

        if (doCache) {
            if (cacheName != null) {
                cacheNameToUse = cacheName;
            }
            result = CacheManager.getObjectFromCache(uri, primitiveClass, cacheNameToUse);
            if (result != null) {
                return result;
            }
        }

        RestTemplate restTemplate = new RestTemplate();

        try {
            httpCalls += 1;
            result = restTemplate.getForObject(uri, primitiveClass);
        } catch (RestClientException e) {
            throw new StoresthalException("Unable to extract scalar of type \""+primitiveClass.getName()+"\" from url \""+url+"\"!", e);
        }

        if (doCache) {
            CacheManager.putObjectInCache(uri, result, cacheNameToUse);
        }

        return result;
    }

    public static void resetStatistics() {
        httpCalls = 0;
    }

    public static int getHttpCalls() {
        return httpCalls;
    }

}
