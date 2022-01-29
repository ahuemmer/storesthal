package com.github.ahuemmer.storesthal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration;
import com.github.ahuemmer.storesthal.configuration.StoreresthalConfigurationFactory;
import com.github.ahuemmer.storesthal.helpers.CacheManager;
import com.github.ahuemmer.storesthal.helpers.ReflectionHelper;
import com.github.ahuemmer.storesthal.helpers.PrimitiveValueRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.mediatype.hal.Jackson2HalModule;
import org.springframework.hateoas.server.mvc.TypeConstrainedMappingJackson2HttpMessageConverter;
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
import static org.springframework.hateoas.MediaTypes.HAL_JSON;

/**
 * The main class of the whole library, encapsulating the core functionality needed. Callers should mainly need just
 * the {@link #getObject(String, Class)} method which will take of everything else...
 */
public class Storesthal {

    /**
     * Depending on the state of {@link #initialized}, init the object store with the default configuration.
     */
    static {
        init(StoreresthalConfigurationFactory.DEFAULT_CONFIGURATION);
    }

    /**
     * The total number of HTTP calls made.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()} or {@link #printStatistics()}.
     */
    private static int httpCalls = 0;

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(Storesthal.class);

    /**
     * During a single {@link #getObject(String, Class)} call, transient object references are stored here. Such
     * transient references may occur, if e. g. a child object encountered (back)refers to the parent object just
     * being retrieved.
     */
    private static final Set<URI> transientObjects = new HashSet<>();

    /**
     * When handling transient objects (see description at {@link #transientObjects}, setter functions may be marked
     * down for being called later on, when the object to be set isn't in transient state any more, but "complete".
     * These setters are stored here.
     */
    private static final Map<URI, List<AbstractMap.SimpleEntry<Object, Method>>> invokeLater = new HashMap<>();

    /**
     * The configuration the object store runs with.
     */
    private static StoresthalConfiguration configuration;

    /**
     * The name of the "common" object cache, which is used, if no explicit object cache name has been configured
     * for a cache (see {@link Cacheable#cacheName()}).
     */
    public static final String COMMON_CACHE_NAME = "com.github.ahuemmer.wsobjectstore.cache.common";

    /**
     * Init the store with a new configuration. This should only be called initially, before using the store, as
     * all caches are cleared during initialization!
     *
     * @param configuration The configuration to use
     */
    public static void init(final StoresthalConfiguration configuration) {
        Storesthal.configuration = configuration;
        /**
         * The cache manager object
         */
        CacheManager cacheManager = CacheManager.getInstance(configuration);
    }

    /**
     * Get the configuration of the store.
     * Please note, that <i>changing</i> the configuration at runtime isn't possible (there are no public setters in
     * {@link StoresthalConfiguration} as it might have unexpected side effects. The only way to change the
     * configuration is to use the {@link #init(StoresthalConfiguration)} function (which should take place before
     * any other operations of the store).
     *
     * @return The store configuration
     */
    public static StoresthalConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * Return a specialized message converter, supplying {@link org.springframework.hateoas.MediaTypes#HAL_JSON} support.
     *
     * @param collection Whether to regard REST response as a collection, therefore using ArrayList as type
     * @return HAL supporting message converter
     */
    @SuppressWarnings("rawtypes")
    private static HttpMessageConverter getHalMessageConverter(boolean collection) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());
        MappingJackson2HttpMessageConverter halConverter;
        if (collection) {
            halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ArrayList.class);
        }
        else {
            halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(RepresentationModel.class);
        }
        halConverter.setSupportedMediaTypes(Collections.singletonList(HAL_JSON));
        halConverter.setObjectMapper(objectMapper);
        return halConverter;
    }

    /**
     * Return a HTTP entity accepting HAL+JSON answers only
     *
     * @return HTTP entity accepting HAL+JSON answers only
     */
    private static HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(HAL_JSON));
        return new HttpEntity<>(headers);
    }

    /**
     * Return a specialized {@link RestTemplate} able to demand and process HAL+JSON data.
     *
     * @param collection Whether to regard REST response as a collection
     * @return A specialized {@link RestTemplate} able to demand and process HAL+JSON data.
     */
    private static RestTemplate getRestTemplateWithHalMessageConverter(boolean collection) {
        RestTemplate restTemplate = new RestTemplate();

        List<HttpMessageConverter<?>> existingConverters = restTemplate.getMessageConverters();
        List<HttpMessageConverter<?>> newConverters = new ArrayList<>();
        newConverters.add(getHalMessageConverter(collection));
        newConverters.addAll(existingConverters);
        restTemplate.setMessageConverters(newConverters);

        return restTemplate;
    }

    /**
     * Handle a collection encountered during object traversal
     *
     * @param l                  The link containing the collection
     * @param m                  The setter method for the collection on the object being populated
     * @param collections        A map of known collections
     * @param linksVisited       A set of all links visited up to now
     * @param intermediateResult The intermediate result object up to now
     * @param depth              The depth in the object tree at the moment (for recursion handling)
     * @param <T>                The type of the object having the collection
     * @throws StoresthalException if something fails and the collection cannot be retrieved or handled
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> void handleCollection(String parentObject, Link l, Method m, Map<String, Collection> collections, int objectCounter, Set<URI> linksVisited, T intermediateResult, int depth) throws StoresthalException {
        Type[] genericParameterTypes = m.getGenericParameterTypes();
        ParameterizedType parameterizedType = (ParameterizedType) genericParameterTypes[0];
        Class realType = (Class) parameterizedType.getActualTypeArguments()[0];

        Class type = m.getParameterTypes()[0];

        String collectionKey= parentObject+":"+objectCounter+":"+l.getRel().value();
        Collection coll = collections.get(collectionKey);

        if (coll == null) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                if (List.class.isAssignableFrom(type)) {
                    coll = new Vector(); //Vector because of thread safety
                } else if (Set.class.isAssignableFrom(type)) {
                    coll = new HashSet();
                } else if (Queue.class.isAssignableFrom(type)) {
                    coll = new ConcurrentLinkedDeque();
                }
            } else {
                //TODO: Array...?
                try {
                    coll = (Collection) type.getConstructor().newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new StoresthalException("Could not instantiate collection of type \"" + type.getCanonicalName() + "\".", e);
                }
            }
            collections.put(collectionKey, coll);
        }

        URI uri;

        try {
            uri = new URI(l.getHref());
        } catch (URISyntaxException e) {
            throw new StoresthalException("Could not create URI from URL \"" + l.getHref() + "\"to visited links collection!", e);
        }

        if (transientObjects.contains(uri)) {
            Method addMethod;
            try {
                addMethod = Objects.requireNonNull(coll).getClass().getMethod("add", Object.class);
            } catch (NoSuchMethodException e) {
                throw new StoresthalException("Could not find \"add\" method for collection class " + Objects.requireNonNull(coll).getClass().getCanonicalName());
            }

            markForLaterInvocation(uri, coll, addMethod);
        } else {
            Object subObject = getObject(l.getHref(), realType, linksVisited, new HashMap<>(), depth + 1);
            Objects.requireNonNull(coll).add(subObject);
        }

        try {
            m.invoke(intermediateResult, coll);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new StoresthalException("Could not invoke method \"" + m.getName() + "(" + coll.getClass().getCanonicalName() + ")\" on instance of \"" + intermediateResult.getClass().getCanonicalName() + "\" class.", e);
        }
    }

    /**
     * Follow a link encountered when parsing an object
     *
     * @param l                  The link to follow
     * @param linksVisited       A set of links that have been visited already
     * @param objectClass        The expected target object class
     * @param collections        A map of collections already known
     * @param intermediateResult The intermediate result object up to now
     * @param depth              The current depth in the object tree (for reasons of recursion)
     * @param <U>                Type of the linked object
     * @throws StoresthalException If the link URL is invalid or an array collection is encountered
     *                             (array collections are not supported (yet?))
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <U> void followLink(String parentObject, Link l, Set<URI> linksVisited, Class<U> objectClass, Map<String, Collection> collections, int objectCounter, U intermediateResult, int depth) throws StoresthalException {

        logger.debug("Following link: "+l.toUri());

        URI uri;

        try {
            uri = new URI(l.getHref());
        } catch (URISyntaxException e) {
            throw new StoresthalException("Could not create URI from URL \"" + l.getHref() + "\"to visited links collection!", e);
        }

        Method m = ReflectionHelper.searchForSetter(objectClass, l.getRel().value());

        if (m != null) {

            Class type = m.getParameterTypes()[0];

            U subObject;

            if (transientObjects.contains(uri)) {
                if (Collection.class.isAssignableFrom(type)) {
                    handleCollection(parentObject, l, m, collections, objectCounter, linksVisited, intermediateResult, depth + 1);
                } else {
                    markForLaterInvocation(uri, intermediateResult, m);
                }
                return;
            }

            if (Collection.class.isAssignableFrom(type)) {
                handleCollection(parentObject, l, m, collections, objectCounter, linksVisited, intermediateResult, depth + 1);
                return;
            } else if (type.getComponentType() != null) {
                throw new StoresthalException("Array relations are not supported (yet?).");
            }

            subObject = (U) Storesthal.<U>getObject(l.getHref(), type, linksVisited, new HashMap<>(), depth + 1);

            invokeSetter(m, intermediateResult, subObject);

        }

        linksVisited.add(uri);
    }

    /**
     * Marks a method to be invoked "later", after the first full object traversal.
     * This is necessary as e. g. a child object may have a relation to its parent object, which is still being
     * traversed and therefore incomplete. It also avoids endless cycling within the object tree.
     * See also {@link #transientObjects}.
     *
     * @param uri    The URI for the object to be set later on
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
     *
     * @param m         The setter method
     * @param applyTo   The object on which the setter method is to be called
     * @param parameter The parameter object to be set
     * @throws StoresthalException on reflection based problems
     */
    private static void invokeSetter(Method m, Object applyTo, Object parameter) throws StoresthalException {
        try {
            m.invoke(applyTo, parameter);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new StoresthalException("Could not invoke method \"" + m.getName() + " of " + applyTo.getClass().getCanonicalName() + " with instance of \"" + parameter.getClass().getCanonicalName() + "\" class.", e);
        }
    }

    /**
     * Retrieve an Integer (just an Integer, no special object...) from the given URL.
     * <i>Note: </i> By default, caching is not enabled for this kind of retrieval. If caching is desired, use
     *               one of the overloaded functions.
     * @param url The URL to retrieve the integer from.
     * @return    The integer retrieved.
     * @throws StoresthalException If it was not possible to retrieve an Integer
     */
    public static Integer getInteger(String url) throws StoresthalException {
        return getInteger(url, false);
    }

    /**
     * Retrieve an Integer (just an Integer, no special object...) from the given URL.
     * @param url     The URL to retrieve the integer from.
     * @param doCache Whether the result should be cached. (Here, {@link #COMMON_CACHE_NAME} is used for the cache name,
     *                use the overloaded function to specify a different cache name if needed.)
     * @return The integer retrieved.
     * @throws StoresthalException If it was not possible to retrieve an Integer
     */
    public static Integer getInteger(String url, boolean doCache) throws StoresthalException {

        if (doCache) {
            return PrimitiveValueRetriever.getPrimitive(Integer.class, url, true, Storesthal.COMMON_CACHE_NAME);
        }

        return PrimitiveValueRetriever.getPrimitive(Integer.class, url, false, null);
    }

    /**
     * Retrieve an Integer (just an Integer, no special object...) from the given URL.
     * @param url       The URL to retrieve the integer from.
     * @param cacheName The name of the cache to used when retrieving the integer.
     * @return The integer retrieved.
     * @throws StoresthalException If it was not possible to retrieve an Integer
     */
    public static Integer getInteger(String url, String cacheName) throws StoresthalException {
        return PrimitiveValueRetriever.getPrimitive(Integer.class, url, true, cacheName);
    }

    /**
     * Retrieve a Double (just a Double, no special object...) from the given URL.
     * <i>Note: </i> By default, caching is not enabled for this kind of retrieval. If caching is desired, use
     *               one of the overloaded functions.
     * @param url The URL to retrieve the Double from.
     * @return    The Double retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Double
     */
    public static Double getDouble(String url) throws StoresthalException {
        return getDouble(url, false);
    }

    /**
     * Retrieve a Double (just a Double, no special object...) from the given URL.
     * @param url     The URL to retrieve the Double from.
     * @param doCache Whether the result should be cached. (Here, {@link #COMMON_CACHE_NAME} is used for the cache name,
     *                use the overloaded function to specify a different cache name if needed.)
     * @return The Double retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Double
     */
    public static Double getDouble(String url, boolean doCache) throws StoresthalException {

        if (doCache) {
            return PrimitiveValueRetriever.getPrimitive(Double.class, url, true, Storesthal.COMMON_CACHE_NAME);
        }

        return PrimitiveValueRetriever.getPrimitive(Double.class, url, false, null);
    }

    /**
     * Retrieve an Double (just a Double, no special object...) from the given URL.
     * @param url       The URL to retrieve the Double from.
     * @param cacheName The name of the cache to used when retrieving the Double.
     * @return The Double retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Double
     */
    public static Double getDouble(String url, String cacheName) throws StoresthalException {
        return PrimitiveValueRetriever.getPrimitive(Double.class, url, true, cacheName);
    }

    /**
     * Retrieve a Boolean (just a Boolean, no special object...) from the given URL.
     * <i>Note: </i> By default, caching is not enabled for this kind of retrieval. If caching is desired, use
     *               one of the overloaded functions.
     * @param url The URL to retrieve the Boolean from.
     * @return    The Boolean retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Boolean
     */
    public static Boolean getBoolean(String url) throws StoresthalException {
        return getBoolean(url, false);
    }

    /**
     * Retrieve a Boolean (just a Boolean, no special object...) from the given URL.
     * @param url     The URL to retrieve the Boolean from.
     * @param doCache Whether the result should be cached. (Here, {@link #COMMON_CACHE_NAME} is used for the cache name,
     *                use the overloaded function to specify a different cache name if needed.)
     * @return The Boolean retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Boolean
     */
    public static Boolean getBoolean(String url, boolean doCache) throws StoresthalException {

        if (doCache) {
            return PrimitiveValueRetriever.getPrimitive(Boolean.class, url, true, Storesthal.COMMON_CACHE_NAME);
        }

        return PrimitiveValueRetriever.getPrimitive(Boolean.class, url, false, null);
    }

    /**
     * Retrieve a Boolean (just a Boolean, no special object...) from the given URL.
     * @param url       The URL to retrieve the Boolean from.
     * @param cacheName The name of the cache to used when retrieving the Boolean.
     * @return The Boolean retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Boolean
     */
    public static Boolean getBoolean(String url, String cacheName) throws StoresthalException {
        return PrimitiveValueRetriever.getPrimitive(Boolean.class, url, true, cacheName);
    }

    /**
     * Retrieve a String (just a String, no other object...) from the given URL.
     * <i>Note: </i> By default, caching is not enabled for this kind of retrieval. If caching is desired, use
     *               one of the overloaded functions.
     * @param url The URL to retrieve the String from.
     * @return    The String retrieved.
     * @throws StoresthalException If it was not possible to retrieve a String
     */
    public static String getString(String url) throws StoresthalException {
        return getString(url, false);
    }

    /**
     * Retrieve a String (just a String, no other object...) from the given URL.
     * @param url     The URL to retrieve the String from.
     * @param doCache Whether the result should be cached. (Here, {@link #COMMON_CACHE_NAME} is used for the cache name,
     *                use the overloaded function to specify a different cache name if needed.)
     * @return The String retrieved.
     * @throws StoresthalException If it was not possible to retrieve a String
     */
    public static String getString(String url, boolean doCache) throws StoresthalException {

        if (doCache) {
            return PrimitiveValueRetriever.getPrimitive(String.class, url, true, Storesthal.COMMON_CACHE_NAME);
        }

        return PrimitiveValueRetriever.getPrimitive(String.class, url, false, null);
    }

    /**
     * Retrieve a String (just a String, no other object...) from the given URL.
     * @param url       The URL to retrieve the String from.
     * @param cacheName The name of the cache to used when retrieving the String.
     * @return The String retrieved.
     * @throws StoresthalException If it was not possible to retrieve a String
     */
    public static String getString(String url, String cacheName) throws StoresthalException {
        return PrimitiveValueRetriever.getPrimitive(String.class, url, true, cacheName);
    }

    /**
     * Retrieves a <i>collection</i> of objects (JSON-Array) from the given URL.
     * @param url         The URL to retrieve the collection from.
     * @param objectClass The class of the collection items to be returned.
     * @param <T>         The type of the collection item object (being consistent with the `objectClass`)
     * @return The collection requested.
     * @throws StoresthalException if no collection could be retrieved.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> ArrayList<T> getCollection(String url, Class<T> objectClass) throws StoresthalException {

        logger.info("Getting object collection of class \"" + objectClass.getCanonicalName() + "\" from URL \"" + url + "\".");

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new StoresthalException("Could not create URI from url\"" + url + "\"!", e);
        }

        ArrayList<T> resultFromCache = CacheManager.getObjectFromCache(uri, objectClass, null);

        if (resultFromCache != null) {
            return resultFromCache;
        }

        httpCalls += 1;

        logger.debug("Adding URI " + uri + " to transient objects...");
        transientObjects.add(uri);

        ResponseEntity response =
                getRestTemplateWithHalMessageConverter(true).exchange(url,
                        HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<ArrayList<EntityModel<T>>>() {
                            @Override
                            @NonNull
                            public Type getType() {
                                Type[] responseWrapperActualTypes = {objectClass};
                                return parameterize(ArrayList.class,
                                        parameterize(EntityModel.class, responseWrapperActualTypes));
                            }
                        });

        ArrayList<EntityModel<T>> result = (ArrayList<EntityModel<T>>) response.getBody();

        ArrayList<T> realResult = new ArrayList<>();

        Set<URI> linksVisited = new HashSet<>();
        @SuppressWarnings("rawtypes") Map<String, Collection> collections = new HashMap<>();

        linksVisited.add(uri);
        int objectCounter = 0;
        for (EntityModel<T> entry : Objects.requireNonNull(result)) {
            realResult.add(entry.getContent());
            for (Link l : entry.getLinks()) {
                if ("self".equals(l.getRel().value())) {
                    logger.debug("Self-Link for object: "+l.toUri());
                    if (!(l.getRel().value().isBlank())) {
                        CacheManager.putObjectInCache(l.toUri(), entry.getContent(), null);
                    }
                }
                else {
                    followLink(url, l, linksVisited, objectClass, collections, objectCounter, entry.getContent(), 0);
                }
            }
            objectCounter++;
        }
        CacheManager.putObjectInCache(uri, realResult, null);

        for (URI invokeUri : invokeLater.keySet()) {
            List<AbstractMap.SimpleEntry<Object, Method>> invocationList = invokeLater.get(invokeUri);
            for (AbstractMap.SimpleEntry<Object, Method> objectAndMethod : invocationList) {
                Object cachedObject = CacheManager.getObjectFromCache(invokeUri, objectClass, null);
                invokeSetter(objectAndMethod.getValue(), objectAndMethod.getKey(), cachedObject);
            }
        }

        transientObjects.clear();
        CacheManager.clearCache(StoresthalConfiguration.INTERMEDIATE_CACHE_NAME, true);
        invokeLater.clear();

        logger.debug("Removing URI " + uri + " from transient objects...");
        transientObjects.remove(uri);

        return realResult;
    }

    /**
     * Internal representation of {@link #getObject(String, Class)}, used for recursion.
     *
     * @param url          The URL representing the object.
     * @param objectClass  The destination class of the object.
     * @param linksVisited A set of the links (URLs) visited so far.
     * @param collections  A map of the collections already known.
     * @param depth        The current recursion depth.
     * @param <T>          The expected type of the returned object.
     * @return The object queried
     * @throws StoresthalException if the URL is invalid
     */
    private static <T> T getObject(String url, Class<T> objectClass, Set<URI> linksVisited, @SuppressWarnings("rawtypes") Map<String, Collection> collections, int depth) throws StoresthalException {

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new StoresthalException("Could not create URI from url\"" + url + "\"!", e);
        }

        T resultFromCache = CacheManager.getObjectFromCache(uri, objectClass, null);

        if (resultFromCache != null) {
            return resultFromCache;
        }

        httpCalls += 1;

        logger.debug("Adding URI " + uri + " to transient objects...");
        transientObjects.add(uri);

        //^^ otherwise, when using the diamond operator a java compiler error (!) will arise!
        ResponseEntity<EntityModel<T>> response =
                getRestTemplateWithHalMessageConverter(false).exchange(url,
                        HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<>() {
                            @Override
                            @NonNull
                            public Type getType() {
                                Type type = super.getType();
                                if (type instanceof ParameterizedType) {
                                    Type[] responseWrapperActualTypes = {objectClass};
                                    return parameterize(EntityModel.class,
                                            responseWrapperActualTypes);
                                }
                                return type;
                            }
                        });
        T result = Objects.requireNonNull(response.getBody()).getContent();


        linksVisited.add(uri);
        for (Link l : response.getBody().getLinks()) {

            if ("self".equals(l.getRel().value())) {
                logger.debug("Self-Link for object: "+l.toUri());
                if (!(l.getRel().value().isBlank())) {
                    CacheManager.putObjectInCache(l.toUri(), result, null);
                }
            }
            else {
                followLink(url, l, linksVisited, objectClass, collections, 0, result, depth);
            }
        }
        CacheManager.putObjectInCache(uri, result, null);


        /*
         * During object retrieval, it might happen, that links to "parent" objects are not followed / populated,
         * as the parent object itself is just being examined and populated. This function corrects this afterwards,
         * when the parent object is fully available and in cache.
         */

        if (depth == 0) {

            for (URI invokeUri : invokeLater.keySet()) {
                List<AbstractMap.SimpleEntry<Object, Method>> invocationList = invokeLater.get(invokeUri);
                for (AbstractMap.SimpleEntry<Object, Method> objectAndMethod : invocationList) {
                    Object cachedObject = CacheManager.getObjectFromCache(invokeUri, objectClass, null);
                    invokeSetter(objectAndMethod.getValue(), objectAndMethod.getKey(), cachedObject);
                }
            }

            transientObjects.clear();
            CacheManager.clearCache(StoresthalConfiguration.INTERMEDIATE_CACHE_NAME, true);
            invokeLater.clear();
        }

        logger.debug("Removing URI " + uri + " from transient objects...");
        transientObjects.remove(uri);

        return result;
    }

    /**
     * Retrieve an object from an URL. Calling GET on the URL is expected to return UTF-8-encoded JSON. If the JSON
     * content / object contains links, these are expected to conform to the
     * <a href="http://stateless.co/hal_specification.html">HAL specifications</a>.
     * <p>
     * The JSON content will be retrieved and any collections encountered will be followed, resulting in a "complete"
     * object structure (including possible collections as well). Warnings and/or errors will be logged, if something
     * goes wrong (e. g. unparseable JSON / no setter for a relation was found / unable to retrieve relation / ...).
     * <p>
     * If not disabled (see {@link StoreresthalConfigurationFactory#setDisableCaching(boolean)}), caching is used to
     * avoid calling the same URL multiple times. This will also lead to one object (with the same URL) being referenced
     * multiple times will only have <i>one</i> representation in memory, so all references will point to the same
     * (not just an equal) object.
     * <p>
     * The exact behavior can be adjusted by {@link StoresthalConfiguration} (see also {@link StoreresthalConfigurationFactory}
     * and {@link #init(StoresthalConfiguration)}).
     *
     * @param url         The URL to retrieve the object from. Must be well-formed and absolute!
     * @param objectClass The class of the object to be returned.
     * @param <T>         The type of the object (being consistent with the `objectClass`)
     * @return The object structure retrieved from the URL.
     * @throws StoresthalException if something goes wrong
     */
    public static <T> T getObject(String url, Class<T> objectClass) throws StoresthalException {

        if (Collection.class.isAssignableFrom(objectClass)) {
            logger.warn("You seem to be trying to retrieve a collection of objects using Storesthal.getObject on the first level. This will likely fail.\n"+
                    "Please consider using Storesthal.getCollection in that case.\n"+
                    "(Handling collections *within* the objects retrieved, therefore on any other but the first level, will work anyway.)");
        }

        logger.info("Getting object of class \"" + objectClass.getCanonicalName() + "\" from URL \"" + url + "\".");
        return getObject(url, objectClass, new HashSet<>(), new HashMap<>(), 0);
    }

    /**
     * For debugging purposes only: Print out some statistics to `stdout`.
     */
    public static void printStatistics() {
        System.out.println("Storesthal statistics:");
        System.out.println("-------------------------");
        System.out.println("- HTTP Calls: " + httpCalls);
        System.out.println("- Cache hits:");
        CacheManager.getCacheHits().keySet().forEach(key -> System.out.println("   - " + key + ": " + CacheManager.getCacheHits().get(key)));
        System.out.println("- Cache misses:");
        CacheManager.getCacheMisses().keySet().forEach(key -> System.out.println("   - " + key + ": " + CacheManager.getCacheMisses().get(key)));
    }

    /**
     * Reset all statistics about HTTP calls, cache hits and cache misses.
     */
    public static void resetStatistics() {
        httpCalls = 0;
        PrimitiveValueRetriever.resetStatistics();
        CacheManager.resetStatistics();
    }

    /**
     * Clear all caches, but do not clear the cache statistics.
     */
    public static void clearAllCaches() {
        CacheManager.clearAllCaches(false);
    }

    /**
     * Clear all caches and possibly their related statistics as well.
     *
     * @param clearStatisticsAsWell Whether to clear all cache hit and miss statistics as well (resetting
     *                              all of them to zero).
     */
    public static void clearAllCaches(boolean clearStatisticsAsWell) {
        CacheManager.clearAllCaches(clearStatisticsAsWell);
        if (clearStatisticsAsWell) {
            httpCalls = 0;
        }
    }

    /**
     * Return some information on cache hits and misses
     * ATTN: Only "direct" hits and misses are counted. E. g., if an object is retrieved from cache the sub-object
     * of which is also cached, the sub-object cache hit will not be counted! (Nevertheless the sub-object is correctly
     * retrieved from cache.)
     *
     * @return The cache statistics map
     */
    public static Map<String, Object> getStatistics() {
        Map<String, Object> result = new HashMap<>();
        result.put("httpCalls", httpCalls + PrimitiveValueRetriever.getHttpCalls());
        result.putAll(CacheManager.getStatistics());
        return result;
    }

    /**
     * Get the number of objects stored in a specific cache.
     *
     * @param cacheName The name of the cache (see {@link Cacheable#cacheName()}).
     * @return The number of objects in the cache. Note, that a zero return value can mean that the cache either is
     * empty or doesn't exist (yet).
     */
    public static int getCachedObjectCount(String cacheName) {
        return CacheManager.getCachedObjectCount(cacheName);
    }

    /**
     * Clear a specific cache using its name (see {@link Cacheable#cacheName()}). Every object stored in the cache
     * will be removed and a new HTTP call will be needed to retrieve the again (which happens automatically once
     * a matching call to {@link Storesthal#getObject(String, Class)} occurs).
     *
     * @param cacheName             The cache to clear.
     * @param clearStatisticsAsWell Whether to clear the cache hit and miss statistics of the cache as well (resetting
     *                              both of them to zero).
     */
    public static void clearCache(String cacheName, boolean clearStatisticsAsWell) {
        CacheManager.clearCache(cacheName, clearStatisticsAsWell);
    }

}
