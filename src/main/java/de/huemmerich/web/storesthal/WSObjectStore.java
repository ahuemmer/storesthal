package de.huemmerich.web.storesthal;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.huemmerich.web.storesthal.configuration.WSObjectStoreConfiguration;
import de.huemmerich.web.storesthal.configuration.WSObjectStoreConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.hal.Jackson2HalModule;
import org.springframework.hateoas.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import static org.apache.commons.lang3.reflect.TypeUtils.parameterize;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_UTF8;

/**
 * The main class of the whole library, encapsulating the core functionality needed. Callers should mainly need just
 * the {@link #getObject(String, Class)} method which will take of everything else...
 */
public class WSObjectStore {

    /**
     * The total number of HTTP calls made.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()} or {@link #printStatistics()}.
     */
    private static int httpCalls=0;

    /**
     * A map containing the number of cache misses by cache (name) for statistics creation.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()} or {@link #printStatistics()}.
     */
    private static final Map<String,Integer> cacheMisses = new HashMap<>();

    /**
     * A map containing the number of cache hits by cache (name) for statistics creation.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()} or {@link #printStatistics()}.
     */
    private static final Map<String,Integer> cacheHits = new HashMap<>();

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(WSObjectStore.class);

    /**
     * The name of the intermediate cache. This cache is only used while traversing the objects / relations found during
     * a single {@link #getObject(String, Class)} call.
     */
    private static final String INTERMEDIATE_CACHE_NAME="com.github.ahuemmer.wsobjectstore.cache.intermediate";

    /**
     * During a single {@link #getObject(String, Class)} call, transient object references are stored here. Such
     * transient references may occur, if e. g. a child object encountered (back)refers to the parent object just
     * being retrieved.
     */
    private static final Set<URI> transientObjects=new HashSet<>();

    /**
     * When handling transient objects (see description at {@link #transientObjects}, setter functions may be marked
     * down for being called later on, when the object to be set isn't in transient state any more, but "complete".
     * These setters are stored here.
     */
    private static final Map<URI, List<AbstractMap.SimpleEntry<Object,Method>>> invokeLater = new HashMap<>();

    /**
     * All configured object caches are stored in this map, the key is the cache name (see {@link LRUCache#getCacheName()}
     * and {@link Cacheable#cacheName()}).
     */
    private static final Map<String,LRUCache<URI,Object>> caches = new HashMap<>();

    /**
     * The configuration the object store runs with.
     */
    private static WSObjectStoreConfiguration configuration;

    /**
     * The name of the "common" object cache, which is used, if no explicit object cache name has been configured
     * for a cache (see {@link Cacheable#cacheName()}).
     */
    public static final String COMMON_CACHE_NAME="com.github.ahuemmer.wsobjectstore.cache.common";

    /**
     * Indicates whether the object store has already been initialized / configured.
     */
    private static boolean initialized=false;

    /**
     * Depending on the state of {@link #initialized}, init the object store with the default configuration.
     */
    private static void init() {
        if (!initialized) {
            init(WSObjectStoreConfigurationFactory.DEFAULT_CONFIGURATION);
        }
    }

    /**
     * Init the store with a new configuration. This should only be called initially, before using the store, as
     * all caches are cleared during initialization!
     * @param configuration The configuration to use
     */
    public static void init(final WSObjectStoreConfiguration configuration) {
        WSObjectStore.configuration = configuration;
        clearAllCaches(true);
        initialized=true;
    }

    /**
     * Get the configuration of the store.
     * Please note, that <i>changing</i> the configuration at runtime isn't possible (there are no public setters in
     * {@link WSObjectStoreConfiguration} as it might have unexpected side effects. The only way to change the
     * configuration is to use the {@link #init(WSObjectStoreConfiguration)} function (which should take place before
     * any other operations of the store).
     * @return The store configuration
     */
    public static WSObjectStoreConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Return a specialized message converter, supplying {@link org.springframework.hateoas.MediaTypes#HAL_JSON_UTF8} support.
     * @return HAL supporting message converter
     */
    private static HttpMessageConverter getHalMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
        halConverter.setSupportedMediaTypes(Collections.singletonList(HAL_JSON_UTF8));
        halConverter.setObjectMapper(objectMapper);
        return halConverter;
    }

    /**
     * Return a HTTP entity accepting HAL+JSON answers only
     * @return HTTP entity accepting HAL+JSON answers only
     */
    private static HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(HAL_JSON_UTF8));
        return new HttpEntity<>(headers);
    }

    /**
     * Return a specialized {@link RestTemplate} able to demand and process HAL+JSON data.
     * @return A specialized {@link RestTemplate} able to demand and process HAL+JSON data.
     */
    private static RestTemplate getRestTemplateWithHalMessageConverter() {
        RestTemplate restTemplate = new RestTemplate();

        List<HttpMessageConverter<?>> existingConverters = restTemplate.getMessageConverters();
        List<HttpMessageConverter<?>> newConverters = new ArrayList<>();
        newConverters.add(getHalMessageConverter());
        newConverters.addAll(existingConverters);
        restTemplate.setMessageConverters(newConverters);

        return restTemplate;
    }

    /**
     * Handle a collection encountered during object traversal
     * @param l The link containing the collection
     * @param m The setter method for the collection on the object being populated
     * @param collections A map of known collections
     * @param linksVisited A set of all links visited up to now
     * @param intermediateResult The intermediate result object up to now
     * @param depth The depth in the object tree at the moment (for recursion handling)
     * @param <T> The type of the object having the collection
     * @throws WSObjectStoreException if something fails and the collection cannot be retrieved or handled
     */
    @SuppressWarnings("unchecked")
    private static <T> void handleCollection(Link l, Method m, Map<String,Collection> collections, Set<URI> linksVisited, T intermediateResult, int depth) throws WSObjectStoreException {
        Type[] genericParameterTypes = m.getGenericParameterTypes();
        ParameterizedType parameterizedType = (ParameterizedType) genericParameterTypes[0];
        Class realType = (Class) parameterizedType.getActualTypeArguments()[0];

        Class type = m.getParameterTypes()[0];

        Collection coll = collections.get(l.getRel());

        if (coll==null) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                if (List.class.isAssignableFrom(type)) {
                    coll = new Vector(); //Vector because of thread safety
                }
                else if (Set.class.isAssignableFrom(type)) {
                    coll = new HashSet();
                }
                else if (Queue.class.isAssignableFrom(type)) {
                    coll = new ConcurrentLinkedDeque();
                }
            }
            else {
                //TODO: Array...?
                try {
                    coll = (Collection) type.getConstructor().newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new WSObjectStoreException("Could not instantiate collection of type \""+type.getCanonicalName()+"\".", e);
                }
            }
            collections.put(l.getRel(),coll);
        }

        URI uri;

        try {
            uri = new URI(l.getHref());
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not create URI from URL \""+l.getHref()+"\"to visited links collection!", e);
        }

        if (transientObjects.contains(uri)) {
            Method addMethod;
            try {
                addMethod = Objects.requireNonNull(coll).getClass().getMethod("add", Object.class);
            } catch (NoSuchMethodException e) {
                throw new WSObjectStoreException("Could not find \"add\" method for collection class "+ Objects.requireNonNull(coll).getClass().getCanonicalName());
            }

            markForLaterInvocation(uri, coll, addMethod);
        }
        else {
            Object subObject = getObject(l.getHref(), realType, linksVisited, new HashMap<>(), depth + 1);
            Objects.requireNonNull(coll).add(subObject);
        }

        try {
            m.invoke(intermediateResult,coll);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new WSObjectStoreException("Could not invoke method \""+m.getName()+"("+coll.getClass().getCanonicalName()+")\" on instance of \""+intermediateResult.getClass().getCanonicalName()+"\" class.", e);
        }
    }

    /**
     * Try to retrieve an object from the associated cache (or the common cache, if the {@link Cacheable} annotation does
     * not state an explicit cache name).
     * @param uri The object's URI
     * @param objectClass The class of the object
     * @return The cached object instance or NULL, if the cache didn't contain an object for the given URI.
     */
    @SuppressWarnings("unchecked")
    private static<T> T getObjectFromCache(URI uri, Class objectClass) {

        logger.debug("Trying to get object with URI "+uri+" from cache...");

        LRUCache<URI,Object> cache = getCache(objectClass);

        if (configuration.isCachingDisabled()&&!(cache.getCacheName().equals(INTERMEDIATE_CACHE_NAME))) {
            logger.debug("Caching is disabled!");
            return null;
        }

        T result = (T)cache.get(uri);

        if (result!=null) {
            cacheHits.putIfAbsent(cache.getCacheName(), 0);
            cacheHits.put(cache.getCacheName(), cacheHits.get(cache.getCacheName())+1);
            logger.debug("Cache hit for URI "+uri+" in cache \""+cache.getCacheName()+"\"!");
        }
        else {
            cacheMisses.putIfAbsent(cache.getCacheName(), 0);
            cacheMisses.put(cache.getCacheName(), cacheMisses.get(cache.getCacheName())+1);
            logger.debug("Cache miss for URI "+uri+" in cache \""+cache.getCacheName()+"\"!");
        }
        return result;
    }

    /**
     * Find the cache an object belongs into and put it there.
     * @param uri The uri of the object
     * @param object The object to be cached
     */
    @SuppressWarnings("unchecked")
    private static void putObjectInCache(URI uri, Object object) {

        LRUCache cache = getCache(object.getClass());

        if (configuration.isCachingDisabled()&&!(cache.getCacheName().equals(INTERMEDIATE_CACHE_NAME))) {
            return;
        }

        logger.debug("Putting one object of class \""+object+"\" into cache named \""+cache.getCacheName()+"\" for URI "+uri.toString());

        cache.put(uri, object);

        logger.debug("\""+cache.getCacheName()+"\" cache size is now: "+cache.size());

    }

    /**
     * Get the cache for a specific object class.
     * @param cls The object class
     * @return The {@link LRUCache} for this object class. If there was no such cache yet, it will be created.
     */
    private static LRUCache<URI, Object> getCache(Class cls) {
        Cacheable annotation = (Cacheable) cls.getDeclaredAnnotation(Cacheable.class);

        String cacheName = (annotation!=null)?annotation.cacheName():INTERMEDIATE_CACHE_NAME;

        logger.debug("Cache for object class \""+cls.getCanonicalName()+"\" is named \""+cacheName+"\".");

        int cacheSize = (annotation!=null)?annotation.cacheSize():configuration.getDefaultCacheSize();

        caches.putIfAbsent(cacheName, new LRUCache<>(cacheName, cacheSize));

        return caches.get(cacheName);
    }

    /**
     * Follow a link encountered when parsing an object
     * @param l The link to follow
     * @param linksVisited A set of links that have been visited already
     * @param objectClass The expected target object class
     * @param collections A map of collections already known
     * @param intermediateResult The intermediate result object up to now
     * @param depth The current depth in the object tree (for reasons of recursion)
     * @param <U> Type of the linked object
     * @throws WSObjectStoreException If the link URL is invalid or an array collection is encountered
     *         (array collections are not supported (yet?))
     */
    @SuppressWarnings("unchecked")
    private static <U> void followLink(Link l, Set<URI> linksVisited, Class<U> objectClass, Map<String,Collection> collections, U intermediateResult, int depth) throws WSObjectStoreException {
        if ("self".equals(l.getRel())) {
            return;
        }

        URI uri;

        try {
            uri = new URI(l.getHref());
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not create URI from URL \""+l.getHref()+"\"to visited links collection!", e);
        }

        Method m = ReflectionHelper.searchForSetter(objectClass, l.getRel());

        if (m!=null) {

            Class type = m.getParameterTypes()[0];

            U subObject;

            if (transientObjects.contains(uri)) {
                if (Collection.class.isAssignableFrom(type)) {
                    handleCollection(l, m, collections, linksVisited, intermediateResult, depth + 1);
                }
                else {
                    markForLaterInvocation(uri, intermediateResult, m);
                }
                return;
            }

            if (Collection.class.isAssignableFrom(type)) {
                handleCollection(l, m, collections, linksVisited, intermediateResult, depth + 1);
                return;
            }
            else if (type.getComponentType() != null) {
                throw new WSObjectStoreException("Array relations are not supported (yet?).");
            }

            subObject = (U) WSObjectStore.<U>getObject(l.getHref(), type, linksVisited, new HashMap<>(), depth + 1);

            invokeSetter(m, intermediateResult, subObject);

        }

        linksVisited.add(uri);
    }

    /**
     * Marks a method to be invoked "later", after the first full object traversal.
     * This is necessary as e. g. a child object may have a relation to its parent object, which is still being
     * traversed and therefore incomplete. It also avoids endless cycling within the object tree.
     * See also {@link #transientObjects}.
     * @param uri The URI for the object to be set later on
     * @param object The object on which the method is to be called
     * @param method The method (usually a setter) to be called on the given object. It will be given the object
     *               retrieved via the `link` parameter as one and only parameter.
     */
    private static void markForLaterInvocation(URI uri, Object object, Method method) {
        if (!invokeLater.containsKey(uri)) {
            invokeLater.put(uri, new LinkedList<>());
        }
        invokeLater.get(uri).add(new AbstractMap.SimpleEntry<>(object, method));
    }

    /**
     * Invokes a method (setter) on a given object, supplying exactly one parameter (the object to bet set)
     * @param m The setter method
     * @param applyTo The object on which the setter method is to be called
     * @param parameter The parameter object to be set
     * @throws WSObjectStoreException on reflection based problems
     */
    private static void invokeSetter(Method m, Object applyTo, Object parameter) throws WSObjectStoreException {
        try {
            m.invoke(applyTo, parameter);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new WSObjectStoreException("Could not invoke method \"" + m.getName() + "(" + applyTo.getClass().getCanonicalName() + ")\" on instance of \"" + parameter.getClass().getCanonicalName() + "\" class.", e);
        }
    }

    /**
     * Internal representation of {@link #getObject(String, Class)}, used for recursion.
     * @param url The URL representing the object.
     * @param objectClass The destination class of the object.
     * @param linksVisited A set of the links (URLs) visited so far.
     * @param collections A map of the collections already known.
     * @param depth The current recursion depth.
     * @param <T> The expected type of the returned object.
     * @return The object queried
     * @throws WSObjectStoreException if the URL is invalid
     */
    private static <T> T getObject(String url, Class<T> objectClass, Set<URI> linksVisited, Map<String,Collection> collections, int depth) throws WSObjectStoreException {

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not create URI from url\""+url+"\"!", e);
        }

        T resultFromCache = getObjectFromCache(uri, objectClass);

        if (resultFromCache!=null) {
            return resultFromCache;
        }

        httpCalls+=1;

        logger.debug("Adding URI "+uri.toString()+" to transient objects...");
        transientObjects.add(uri);

        @SuppressWarnings("Convert2Diamond")
        //^^ otherwise, when using the diamond operator a java compiler error (!) will arise!
        ResponseEntity<Resource<T>> response =
                getRestTemplateWithHalMessageConverter().exchange(url,
                        HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<Resource<T>>() {
                            @Override
                            @NonNull
                            public Type getType() {
                                Type type = super.getType();
                                if (type instanceof ParameterizedType) {
                                    Type[] responseWrapperActualTypes = {objectClass};
                                    return parameterize(Resource.class,
                                            responseWrapperActualTypes);
                                }
                                return type;
                            }
                        });

        T result = response.getBody().getContent();

        linksVisited.add(uri);
        for (Link l: response.getBody().getLinks()) {
            followLink(l, linksVisited, objectClass, collections, result, depth);
        }
        putObjectInCache(uri, result);


        /*
         * During object retrieval, it might happen, that links to "parent" objects are not followed / populated,
         * as the parent object itself is just being examined and populated. This function corrects this afterwards,
         * when the parent object is fully available and in cache.
         */

        if (depth==0) {

            for(URI invokeUri: invokeLater.keySet()) {
                List<AbstractMap.SimpleEntry<Object, Method>> invocationList = invokeLater.get(invokeUri);
                for (AbstractMap.SimpleEntry<Object, Method> objectAndMethod: invocationList) {
                    Object cachedObject = getObjectFromCache(invokeUri, objectClass);
                    invokeSetter(objectAndMethod.getValue(), objectAndMethod.getKey(), cachedObject);
                }
            }

            transientObjects.clear();
            clearCache(INTERMEDIATE_CACHE_NAME, true);
            invokeLater.clear();
        }

        logger.debug("Removing URI "+uri.toString()+" from transient objects...");
        transientObjects.remove(uri);

        return result;
    }


    /**
     * Retrieve an object from an URL. Calling GET on the URL is expected to return UTF-8-encoded JSON. If the JSON
     * content / object contains links, these are expected to conform to the
     * <a href="http://stateless.co/hal_specification.html">HAL specifications</a>.
     *
     * The JSON content will be retrieved and any collections encountered will be followed, resulting in a "complete"
     * object structure (including possible collections as well). Warnings and/or errors will be logged, if something
     * goes wrong (e. g. unparseable JSON / no setter for a relation was found / unable to retrieve relation / ...).
     *
     * If not disabled (see {@link WSObjectStoreConfigurationFactory#setDisableCaching(boolean)}), caching is used to
     * avoid calling the same URL multiple times. This will also lead to one object (with the same URL) being referenced
     * multiple times will only have <i>one</i> representation in memory, so all references will point to the same
     * (not just an equal) object.
     *
     * The exact behavior can be adjusted by {@link WSObjectStoreConfiguration} (see also {@link WSObjectStoreConfigurationFactory}
     * and {@link #init(WSObjectStoreConfiguration)}).
     *
     * @param url The URL to retrieve the object from. Must be well-formed and absolute!
     * @param objectClass The class of the object to be returned.
     * @param <T> The type of the object (being consistent with the `objectClass`)
     * @return The object structure retrieved from the URL.
     * @throws WSObjectStoreException if something goes wrong
     */
    public static <T> T getObject(String url, Class<T> objectClass) throws WSObjectStoreException {
        init();
        logger.info("Getting object of class \""+objectClass.getCanonicalName()+"\" from URL \""+url+"\".");
        return getObject(url, objectClass, new HashSet<>(), new HashMap<>(), 0);
    }

    /**
     * Return some information on cache hits and misses
     * ATTN: Only "direct" hits and misses are counted. E. g., if an object is retrieved from cache the sub-object
     * of which is also cached, the sub-object cache hit will not be counted! (Nevertheless the sub-object is correctly
     * retrieved from cache.)
     * @return The cache statistics map
     */
    public static Map<String,Object> getStatistics() {
        return Map.of("httpCalls",httpCalls, "cacheHits", cacheHits, "cacheMisses", cacheMisses);
    }

    /**
     * Reset all statistics about HTTP calls, cache hits and cache misses.
     */
    public static void resetStatistics() {
        httpCalls = 0;
        cacheHits.clear();
        cacheMisses.clear();
    }

    /**
     * Clear a specific cache using its name (see {@link Cacheable#cacheName()}). Every object stored in the cache
     * will be removed and a new HTTP call will be needed to retrieve the again (which happens automatically once
     * a matching call to {@link #getObject(String, Class)} occurs).
     * @param cacheName The cache to clear.
     * @param clearStatisticsAsWell Whether to clear the cache hit and miss statistics of the cache as well (resetting
     *                              both of them to zero).
     */
    public static void clearCache(String cacheName, boolean clearStatisticsAsWell) {
        if (caches.containsKey(cacheName)) {
            caches.get(cacheName).clear();
        }
        if (clearStatisticsAsWell) {
            cacheHits.put(cacheName,0);
            cacheMisses.put(cacheName,0);
        }
    }

    /**
     * Get the number of objects stored in a specific cache.
     * @param cacheName The name of the cache (see {@link Cacheable#cacheName()}).
     * @return The number of objects in the cache. Note, that a zero return value can mean that the cache either is
     * empty or doesn't exist (yet).
     */
    public static int getCachedObjectCount(String cacheName) {
        if (caches.get(cacheName)==null) {
            return 0;
        }
        return caches.get(cacheName).size();
    }

    /**
     * Clear all caches, but do not clear the cache statistics.
     */
    public static void clearAllCaches() {
        clearAllCaches(false);
    }

    /**
     * Clear all caches and possibly their related statistics as well.
     * @param clearStatisticsAsWell Whether to clear all cache hit and miss statistics as well (resetting
     *                              all of them to zero).
     */
    public static void clearAllCaches(boolean clearStatisticsAsWell) {
        for (String key: caches.keySet()) {
            clearCache(key, clearStatisticsAsWell);
        }
        if (clearStatisticsAsWell) {
            httpCalls=0;
        }
    }

    /**
     * For debugging purposes only: Print out some statistics to `stdout`.
     */
    public static void printStatistics() {
        System.out.println("WSObjectStore statistics:");
        System.out.println("-------------------------");
        System.out.println("- HTTP Calls: "+httpCalls);
        System.out.println("- Cache hits:");
        cacheHits.keySet().forEach(key -> System.out.println("   - "+key+": "+cacheHits.get(key)));
        System.out.println("- Cache misses:");
        cacheMisses.keySet().forEach(key -> System.out.println("   - "+key+": "+cacheHits.get(key)));

    }

}
