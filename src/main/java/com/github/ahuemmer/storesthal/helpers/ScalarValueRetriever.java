package com.github.ahuemmer.storesthal.helpers;

import com.github.ahuemmer.storesthal.Storesthal;
import com.github.ahuemmer.storesthal.StoresthalException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;

public class ScalarValueRetriever {

    private ScalarValueRetriever() {};

    private final static CacheManager cacheManager = CacheManager.getInstance(Storesthal.getConfiguration());

    private static int httpCalls = 0;

    /**
     * Retrieve an Integer (just an Integer, no special object...) from the given URL.
     * @param url       The URL to retrieve the integer from.
     * @param cacheName The name of the cache to used when retrieving the integer.
     * @return The integer retrieved.
     * @throws com.github.ahuemmer.storesthal.StoresthalException If it was not possible to retrieve an Integ
     */
    public static Integer getInteger(String url, boolean doCache, String cacheName) throws StoresthalException {

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new StoresthalException("Could not create URI from url\"" + url + "\"!", e);
        }

        Integer result;
        String cacheNameToUse = Storesthal.COMMON_CACHE_NAME;

        if (doCache) {
            if (cacheName != null) {
                cacheNameToUse = cacheName;
            }
            result = cacheManager.getObjectFromCache(uri, Integer.class, cacheNameToUse);
            if (result != null) {
                return result;
            }
        }

        RestTemplate restTemplate = new RestTemplate();

        try {
            httpCalls += 1;
            result = restTemplate.getForObject(uri, Integer.class);
        } catch (RestClientException e) {
            throw new StoresthalException("Unable to extract scalar of type Integer from url \""+url+"\"!", e);
        }

        if (doCache) {
            cacheManager.putObjectInCache(uri, result, cacheNameToUse);
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
