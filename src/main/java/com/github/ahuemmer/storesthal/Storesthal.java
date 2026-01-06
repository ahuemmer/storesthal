package com.github.ahuemmer.storesthal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.ahuemmer.storesthal.configuration.StoreresthalConfigurationFactory;
import com.github.ahuemmer.storesthal.configuration.StoresthalConfiguration;
import com.github.ahuemmer.storesthal.helpers.CacheManager;
import com.github.ahuemmer.storesthal.helpers.EmbeddedCollectionHelper;
import com.github.ahuemmer.storesthal.helpers.PrimitiveValueRetriever;
import com.github.ahuemmer.storesthal.helpers.ReflectionHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
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
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.reflect.TypeUtils.parameterize;
import static org.springframework.hateoas.MediaTypes.HAL_JSON;

/**
 * The main class of the whole library, encapsulating the core functionality needed. Callers should mainly need just
 * the {@link #getObjectWithoutLinks(String, Class)} method which will take of everything else...
 */
public class Storesthal {

    /**
     * The name of the "common" object cache, which is used, if no explicit object cache name has been configured
     * for a cache (see {@link Cacheable#cacheName()}).
     */
    public static final String COMMON_CACHE_NAME = "com.github.ahuemmer.wsobjectstore.cache.common";

    /**
     * When searching for the "real" type of an object to populate, which was given as an
     * {@link org.springframework.hateoas.EntityModel}, this regex is applied.
     */
    public static final String ENTITY_MODEL_CLASS_REGEX = ".+<org\\.springframework\\.hateoas\\.EntityModel<(.+)>.*>.*";

    /**
     * The {@link java.util.regex.Pattern} of the Regex {@link #ENTITY_MODEL_CLASS_REGEX}.
     */
    public static final Pattern ENTITY_MODEL_CLASS_PATTERN = Pattern.compile(ENTITY_MODEL_CLASS_REGEX);

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(Storesthal.class);

    /**
     * During a single {@link #getObjectWithoutLinks(String, Class)} call, transient object references are stored here. Such
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
     * The total number of HTTP calls made.
     * Can be re-zeroed by {@link #resetStatistics()} or {@link #clearAllCaches(boolean)} and retrieved by
     * {@link #getStatistics()} or {@link #printStatistics()}.
     */
    private static int httpCalls = 0;

    /**
     * The configuration the object store runs with.
     */
    private static StoresthalConfiguration configuration;

    /**
     * Depending on the state of {@link #initialized}, init the object store with the default configuration.
     */
    static {
        init(StoreresthalConfigurationFactory.DEFAULT_CONFIGURATION);
    }

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
        CacheManager.getInstance(configuration);
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
        } else {
            halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(RepresentationModel.class);
        }
        halConverter.setSupportedMediaTypes(Collections.singletonList(HAL_JSON));
        halConverter.setObjectMapper(objectMapper);

        // Possibly useful for Debugging:
        // objectMapper.enable(INCLUDE_SOURCE_IN_LOCATION);
        return halConverter;
    }

    /**
     * Return an HTTP entity accepting HAL+JSON answers only
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

    private static Class getRealType(boolean maintainLinks, ParameterizedType parameterizedType, Method m) throws StoresthalException {
        if (!maintainLinks) {
            return (Class) parameterizedType.getActualTypeArguments()[0];
        }

        String realTypeName = null;
        Class realType = null;

        if ((parameterizedType.getActualTypeArguments()[0]).getTypeName().startsWith("org.springframework.hateoas.EntityModel<")) {
            try {

                // We don't need to check if Collection is assignable from m.getParameterTypes()[0], as this takes place
                // in followLink already.

                logger.debug("Searching for real object type of class {}", m.getParameterTypes()[0].getCanonicalName());

                final Matcher matcher = ENTITY_MODEL_CLASS_PATTERN.matcher(m.getGenericParameterTypes()[0].getTypeName());

                if (matcher.matches()) {
                    realTypeName = matcher.group(1);
                    logger.debug("Found real object type name: {}", realTypeName);
                    realType = Class.forName(realTypeName);
                } else {
                    throw new StoresthalException("Could not extract real object type from type of class" + m.getParameterTypes()[0].getCanonicalName());
                }

            } catch (ClassNotFoundException e) {
                throw new StoresthalException("Class " + realTypeName + ", which seems to be the real type object type of " + m.getParameterTypes()[0].getCanonicalName() + ", could not be found.", e);
            }
        }

        if (realType == null) {
            realType = (Class) parameterizedType.getActualTypeArguments()[0];
        }

        return realType;
    }

    /**
     * Handles the retrieval of a collection encountered during object structure traversal.
     *
     * @param parentObject                   The parent object of the collection
     * @param l                              The link leading to the object collection
     * @param m                              The setter method for assigning the collection to its parent object
     * @param collections                    A map of "already-known" collections in order to avoid multiple retrievals
     * @param objectCounter                  Part of the key of the collections map
     * @param intermediateResultWithoutLinks The result so far, if "without links" mode is used.
     * @param intermediateResultWithLinks    The result so far, if "with links" mode is used
     * @param depth                          The depth in the object tree, used to determine when final operations can be applied
     * @param <T>                            The type of the collection's objects
     * @throws StoresthalException mainly if some of the reflective / cast operations fail.
     */
    private static <T> void handleCollection(String parentObject, Link l, Method m, Map<String, Collection> collections, int objectCounter, T intermediateResultWithoutLinks, EntityModel<T> intermediateResultWithLinks, int depth) throws StoresthalException {
        Type[] genericParameterTypes = m.getGenericParameterTypes();
        ParameterizedType parameterizedType = (ParameterizedType) genericParameterTypes[0];
        boolean maintainLinks = intermediateResultWithLinks != null;

        Class realType = getRealType(maintainLinks, parameterizedType, m);

        Class type = m.getParameterTypes()[0];

        String collectionKey = parentObject + ":" + objectCounter + ":" + l.getRel().value();
        Collection coll = collections.get(collectionKey);

        if (coll == null) {
            if (type.isInterface() || Modifier.isAbstract(type.getModifiers())) {
                if (List.class.isAssignableFrom(type)) {
                    coll = new LinkedList();
                } else if (Set.class.isAssignableFrom(type)) {
                    coll = new HashSet();
                } else if (Queue.class.isAssignableFrom(type)) {
                    coll = new ConcurrentLinkedDeque();
                }
            } else {
                //TODO: Array...?
                try {
                    coll = (Collection) type.getConstructor().newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException |
                         InvocationTargetException e) {
                    throw new StoresthalException("Could not instantiate collection of type \"" + type.getCanonicalName() + "\".", e);
                }
            }
            collections.put(collectionKey, coll);
        }

        URI uri = getUriFromLink(l);

        if (transientObjects.contains(uri)) {
            Method addMethod;
            try {
                addMethod = Objects.requireNonNull(coll).getClass().getMethod("add", Object.class);
            } catch (NoSuchMethodException e) {
                throw new StoresthalException("Could not find \"add\" method for collection class " + Objects.requireNonNull(coll).getClass().getCanonicalName());
            }

            markForLaterInvocation(uri, coll, addMethod);
        } else {
            Object subObject;
            if (maintainLinks) {
                subObject = getObject(l.getHref(), realType, new HashMap<>(), depth + 1);
            } else {
                subObject = getObjectWithoutLinks(l.getHref(), realType, new HashMap<>(), depth + 1);
            }
            Objects.requireNonNull(coll).add(subObject);
        }

        try {
            if (maintainLinks) {
                m.invoke(intermediateResultWithLinks.getContent(), coll);
            } else {
                m.invoke(intermediateResultWithoutLinks, coll);
            }
        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            if (maintainLinks) {
                throw new StoresthalException("Could not invoke method \"" + m.getName() + "(" + coll.getClass().getCanonicalName() + ")\" on instance of \"" + intermediateResultWithLinks.getClass().getCanonicalName() + "\" class.", e);
            } else {
                throw new StoresthalException("Could not invoke method \"" + m.getName() + "(" + coll.getClass().getCanonicalName() + ")\" on instance of \"" + intermediateResultWithoutLinks.getClass().getCanonicalName() + "\" class.", e);
            }
        }

    }

    /**
     * Generic method to follow a link, not dependent on linkless mode ("...withoutLinks" methods) being true or false.
     *
     * @param objectClass                    The class of the object to be retrieved from the link
     * @param parentObject                   The parent/"owner" object of the link
     * @param l                              The link itself
     * @param collections                    All collections encountered so far in order to avoid multiple retrievals
     * @param objectCounter                  Part of the key of the collections map
     * @param intermediateResult             The result so far, if linkless mode is not active
     * @param intermediateResultWithoutLinks The result so far, if linkless mode is active ("...withoutLinks" methods)
     * @param depth                          The depth in the object tree, used to determine when final operations can be applied
     * @param <U>                            The type of the collection's objects
     * @throws StoresthalException If an unsupported type of collection was used
     */
    private static <U> void followLink(Class<U> objectClass, String parentObject, Link l, Map<String, Collection> collections, int objectCounter, EntityModel<U> intermediateResult, U intermediateResultWithoutLinks, int depth) throws StoresthalException {

        URI uri = getUriFromLink(l);

        Method m = ReflectionHelper.searchForSetter(objectClass, l.getRel().value());

        if (m == null) {
            return;
        }

        boolean maintainLinks = intermediateResult != null;

        Class type = m.getParameterTypes()[0];

        if (transientObjects.contains(uri)) {
            if (Collection.class.isAssignableFrom(type)) {
                if (!maintainLinks) {
                    handleCollection(parentObject, l, m, collections, objectCounter, intermediateResultWithoutLinks, null, depth + 1);
                    //handleCollectionWithoutLinks(parentObject, l, m, collections, objectCounter, intermediateResultWithoutLinks, depth + 1);
                } else {
                    //handleCollection(parentObject, l, m, collections, objectCounter, intermediateResult, depth + 1);
                    handleCollection(parentObject, l, m, collections, objectCounter, null, intermediateResult, depth + 1);
                }
            } else {
                markForLaterInvocation(uri, intermediateResult != null ? intermediateResult.getContent() : intermediateResultWithoutLinks, m);
            }
            return;
        }

        if (Collection.class.isAssignableFrom(type)) {
            if (!maintainLinks) {
                //handleCollectionWithoutLinks(parentObject, l, m, collections, objectCounter, intermediateResultWithoutLinks, depth + 1);
                handleCollection(parentObject, l, m, collections, objectCounter, intermediateResultWithoutLinks, null, depth + 1);
            } else {
                //handleCollection(parentObject, l, m, collections, objectCounter, intermediateResult, depth + 1);
                handleCollection(parentObject, l, m, collections, objectCounter, null, intermediateResult, depth + 1);
            }
            return;
        } else if (type.getComponentType() != null) {
            throw new StoresthalException("Array relations are not supported (yet?).");
        }

        if (maintainLinks) {
            EntityModel<U> subObject;
            subObject = (EntityModel<U>) Storesthal.<U>getObject(l.getHref(), type, new HashMap<>(), depth + 1);

            if (m.getParameterTypes()[0].isAssignableFrom(EntityModel.class)) {
                invokeSetter(m, intermediateResult.getContent(), subObject);
            } else {
                invokeSetter(m, intermediateResult.getContent(), subObject.getContent());
            }
        } else {
            U subObject;
            subObject = (U) Storesthal.<U>getObjectWithoutLinks(l.getHref(), type, new HashMap<>(), depth + 1);
            invokeSetter(m, intermediateResultWithoutLinks, subObject);
        }

    }

    /**
     * Marks a method to be invoked "later", after the first full object traversal.
     * This is necessary as e.g. a child object may have a relation to its parent object, which is still being
     * traversed and therefore incomplete. It also avoids endless cycling within the object tree.
     * See also {@link #transientObjects}.
     *
     * @param uri    The URI for the object to be set later on
     * @param object The object on which the method is to be called
     * @param method The method (usually a setter) to be called on the given object. It will be given the object
     *               retrieved via the `link` parameter as one and only parameter.
     */
    private static void markForLaterInvocation(URI uri, Object object, Method method) {
        invokeLater.computeIfAbsent(uri, k -> new LinkedList<>());
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
     * one of the overloaded functions.
     *
     * @param url The URL to retrieve the integer from.
     * @return The integer retrieved.
     * @throws StoresthalException If it was not possible to retrieve an Integer
     */
    public static Integer getInteger(String url) throws StoresthalException {
        return getInteger(url, false);
    }

    /**
     * Retrieve an Integer (just an Integer, no special object...) from the given URL.
     *
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
     *
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
     * one of the overloaded functions.
     *
     * @param url The URL to retrieve the Double from.
     * @return The Double retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Double
     */
    public static Double getDouble(String url) throws StoresthalException {
        return getDouble(url, false);
    }

    /**
     * Retrieve a Double (just a Double, no special object...) from the given URL.
     *
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
     *
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
     * one of the overloaded functions.
     *
     * @param url The URL to retrieve the Boolean from.
     * @return The Boolean retrieved.
     * @throws StoresthalException If it was not possible to retrieve a Boolean
     */
    public static Boolean getBoolean(String url) throws StoresthalException {
        return getBoolean(url, false);
    }

    /**
     * Retrieve a Boolean (just a Boolean, no special object...) from the given URL.
     *
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
     *
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
     * one of the overloaded functions.
     *
     * @param url The URL to retrieve the String from.
     * @return The String retrieved.
     * @throws StoresthalException If it was not possible to retrieve a String
     */
    public static String getString(String url) throws StoresthalException {
        return getString(url, false);
    }

    /**
     * Retrieve a String (just a String, no other object...) from the given URL.
     *
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
     *
     * @param url       The URL to retrieve the String from.
     * @param cacheName The name of the cache to used when retrieving the String.
     * @return The String retrieved.
     * @throws StoresthalException If it was not possible to retrieve a String
     */
    public static String getString(String url, String cacheName) throws StoresthalException {
        return PrimitiveValueRetriever.getPrimitive(String.class, url, true, cacheName);
    }

    /**
     * Returns a collection retrieved from a given url, not expecting an embedded collection.
     *
     * @param url         The URL to retrieve the collection from
     * @param objectClass The class ob the collection's objects (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @param <T>         The type of ollection's objects (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @return The collection retrieved from the URL as {@link java.util.ArrayList} of {@link org.springframework.hateoas.EntityModel} objects
     * @throws StoresthalException If the URL was invalid
     */
    public static <T> ArrayList<EntityModel<T>> getCollection(String url, Class<T> objectClass) throws StoresthalException {
        return getCollection(url, objectClass, null);
    }

    private static <T> void handleEntryLinks(List<EntityModel<T>> result, Class<T> objectClass, String url, boolean maintainLinks) {


    }

    /**
     * Returns a collection retrieved from a given url, not expecting an embedded collection.
     *
     * @param url                    The URL to retrieve the collection from
     * @param objectClass            The class ob the collection's objects (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @param embeddedCollectionName The name of the object starting an embedded collection or NULL, if an embedded collection is not used
     * @param <T>                    The type of ollection's objects (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @return The collection retrieved from the URL as {@link java.util.ArrayList} of {@link org.springframework.hateoas.EntityModel} objects
     * @throws StoresthalException If the URL was invalid
     */
    public static <T> ArrayList<EntityModel<T>> getCollection(String url, Class<T> objectClass, Optional<String> embeddedCollectionName) throws StoresthalException {

        logger.info("Getting object collection of class \"{}\" from URL \"{}\", maintaining the object links.", objectClass.getCanonicalName(), url);

        URI uri = getUriFromUrl(url);

        ParameterizedTypeReference<ArrayList<EntityModel<T>>> type = new ParameterizedTypeReference<>() {
            @Override
            @NonNull
            public Type getType() {
                Type[] responseWrapperActualTypes = {objectClass};
                if (embeddedCollectionName != null) { // This is intended - NULL would mean "collection is not embedded" here.
                    return parameterize(EmbeddedCollectionHelper.class, responseWrapperActualTypes);
                } else {
                    return parameterize(ArrayList.class,
                            parameterize(EntityModel.class, responseWrapperActualTypes));
                }
            }
        };

        ArrayList<EntityModel<T>> resultFromCache = CacheManager.getObjectFromCache(uri, type.getClass(), null);

        if (resultFromCache != null) {
            return resultFromCache;
        }

        httpCalls += 1;

        logger.debug("Adding URI {} to transient objects...", uri);
        transientObjects.add(uri);

        List<EntityModel<T>> result = getCollectionResponse(url, objectClass, embeddedCollectionName);

        ArrayList<EntityModel<T>> realResult = new ArrayList<>();

        @SuppressWarnings("rawtypes") Map<String, Collection> collections = new HashMap<>();

        int objectCounter = 0;
        for (EntityModel<T> entry : Objects.requireNonNull(result)) {
            realResult.add(entry);
            for (Link l : entry.getLinks()) {
                if ("self".equals(l.getRel().value())) {
                    logger.debug("Self-Link for object: {}", l.toUri());
                    if (!(l.getRel().value().isBlank())) {
                        CacheManager.putObjectInCache(l.toUri(), entry, null);
                    }
                } else {
                    followLink(objectClass, url, l, collections, objectCounter, entry, null, 0);
                }
            }
            objectCounter++;
        }
        CacheManager.putObjectInCache(uri, realResult, null);

        finishGetCollection(objectClass, uri);

        return realResult;
    }

    /**
     * Retrieves a <i>collection</i> of objects (JSON-Array) from the given URL.
     * Using this method, it is assumed, that the collection is not delivered within an `_embedded` object. If it is, please use the
     * method {@link  #getCollectionWithoutLinks(String, Class, java.util.Optional)} method.
     *
     * @param url         The URL to retrieve the collection from.
     * @param objectClass The class of the collection items to be returned.
     * @param <T>         The type of the collection item object (being consistent with the `objectClass`)
     * @return The collection requested.
     * @throws StoresthalException if no collection could be retrieved.
     */
    public static <T> ArrayList<T> getCollectionWithoutLinks(String url, Class<T> objectClass) throws StoresthalException {
        return getCollectionWithoutLinks(url, objectClass, null);
    }

    private static <T> List<EntityModel<T>> getCollectionResponse(String url, Class<T> objectClass, Optional<String> embeddedCollectionName) throws StoresthalException {
        ResponseEntity response =
                getRestTemplateWithHalMessageConverter(true).exchange(url,
                        HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<ArrayList<EntityModel<T>>>() {
                            @Override
                            @NonNull
                            public Type getType() {
                                Type[] responseWrapperActualTypes = {objectClass};
                                if (embeddedCollectionName != null) { // This is intended - NULL would mean "collection is not embedded" here.
                                    return parameterize(EmbeddedCollectionHelper.class, responseWrapperActualTypes);
                                } else {
                                    return parameterize(ArrayList.class,
                                            parameterize(EntityModel.class, responseWrapperActualTypes));
                                }
                            }
                        });

        List<EntityModel<T>> result;

        if (embeddedCollectionName != null) { // This is intended - NULL would mean "collection is not embedded" here.
            result = EmbeddedCollectionHelper.getObjects(response, objectClass, embeddedCollectionName);
        } else {
            result = (List<EntityModel<T>>) response.getBody();
        }

        return result;
    }

    private static <T> void finishGetCollection(Class<T> objectClass, URI uri) throws StoresthalException {
        for (Map.Entry<URI, List<AbstractMap.SimpleEntry<Object, Method>>> entry : invokeLater.entrySet()) {
            URI invokeUri = entry.getKey();
            List<AbstractMap.SimpleEntry<Object, Method>> invocationList = invokeLater.get(invokeUri);
            for (AbstractMap.SimpleEntry<Object, Method> objectAndMethod : invocationList) {
                Object cachedObject = CacheManager.getObjectFromCache(invokeUri, objectClass, null);
                invokeSetter(objectAndMethod.getValue(), objectAndMethod.getKey(), cachedObject);
            }
        }

        transientObjects.clear();
        CacheManager.clearCache(StoresthalConfiguration.INTERMEDIATE_CACHE_NAME, true);
        invokeLater.clear();

        logger.debug("Removing URI \"{}\" from transient objects...", uri);
        transientObjects.remove(uri);
    }

    /**
     * Retrieves a <i>collection</i> of objects (JSON-Array) from the given URL.
     *
     * @param url                    The URL to retrieve the collection from.
     * @param objectClass            The class of the collection items to be returned.
     * @param <T>                    The type of the collection item object (being consistent with the `objectClass`)
     * @param embeddedCollectionName If you supply a String here, (the Optional is <i>not</i> empty), the `_embedded` object will be
     *                               searched for a field with this String, containing the collection to return.
     *                               If the Optional <i>is</i> empty, the first array field encountered within the `_embedded`
     *                               object will be used.
     *                               An empty List will be returned in either case, if no matching field was found and a warning
     *                               will be logged accordingly.
     *                               (See README.md for details.)
     * @return The collection requested.
     * @throws StoresthalException if no collection could be retrieved.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> ArrayList<T> getCollectionWithoutLinks(String url, Class<T> objectClass, Optional<String> embeddedCollectionName) throws StoresthalException {

        logger.info("Getting object collection of class \"{}\" from URL \"{}\".", objectClass.getCanonicalName(), url);

        URI uri = getUriFromUrl(url);

        ArrayList<T> resultFromCache = CacheManager.getObjectFromCache(uri, objectClass, null);

        if (resultFromCache != null) {
            return resultFromCache;
        }

        httpCalls += 1;

        logger.debug("Adding URI {} to transient objects...", uri);
        transientObjects.add(uri);

        List<EntityModel<T>> result = getCollectionResponse(url, objectClass, embeddedCollectionName);

        ArrayList<T> realResult = new ArrayList<>();

        @SuppressWarnings("rawtypes") Map<String, Collection> collections = new HashMap<>();

        int objectCounter = 0;
        for (EntityModel<T> entry : Objects.requireNonNull(result)) {
            realResult.add(entry.getContent());
            for (Link l : entry.getLinks()) {
                if ("self".equals(l.getRel().value())) {
                    logger.debug("Self-Link for object: {}", l.toUri());
                    if (!(l.getRel().value().isBlank())) {
                        CacheManager.putObjectInCache(l.toUri(), entry.getContent(), null);
                    }
                } else {
                    followLink(objectClass, url, l, collections, objectCounter, null, entry.getContent(), 0);
                }
            }
            objectCounter++;
        }
        CacheManager.putObjectInCache(uri, realResult, null);

        finishGetCollection(objectClass, uri);

        return realResult;
    }

    /**
     * Return an object from a given URL, wrapped as {@link org.springframework.hateoas.EntityModel}
     *
     * @param url         The URL to retrieve the object from
     * @param objectClass The class of the object to be retrieved (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @param collections The collections encountered so far, in order to avoid multiple retrievals of the same one.
     * @param depth       The depth in the object tree, used to determine when final operations can be applied
     * @param <T>         The type of the object to be retrieved (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @return The collection found at the given URL
     * @throws StoresthalException If the URL was invalid or no object could be retrieved from it
     */
    private static <T> EntityModel<T> getObject(String url, Class<T> objectClass, @SuppressWarnings("rawtypes") Map<String, Collection> collections, int depth) throws StoresthalException {

        URI uri = getUriFromUrl(url);

        EntityModel<T> resultFromCache = CacheManager.getObjectFromCache(uri, objectClass, null);

        if (resultFromCache != null) {
            return resultFromCache;
        }

        httpCalls += 1;

        logger.debug("Adding URI \"{}\" to transient objects...", uri);
        transientObjects.add(uri);
        ResponseEntity<EntityModel<T>> response =
                null;

        try {
            //^^ otherwise, when using the diamond operator a java compiler error (!) will arise!
            response = getRestTemplateWithHalMessageConverter(false).exchange(url,
                    HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<>() {
                        @Override
                        @NonNull
                        public Type getType() {
                            Type type = super.getType();
                            if (type instanceof ParameterizedType) {
                                Type[] responseWrapperActualTypes = {objectClass};
                                type = parameterize(EntityModel.class,
                                        responseWrapperActualTypes);
                            }
                            return type;
                        }
                    });
        } catch (RestClientException e) {
            throw new StoresthalException("Exception trying to get object from " + url, e);
        }
        EntityModel<T> result = Objects.requireNonNull(response.getBody());


        for (Link l : response.getBody().getLinks()) {

            if ("self".equals(l.getRel().value())) {
                logger.debug("Self-Link for object: {}", l.toUri());
                if (!(l.getRel().value().isBlank())) {
                    CacheManager.putObjectInCache(l.toUri(), result, null);
                }
            } else {
                followLink(objectClass, url, l, collections, 0, result, null, depth);
            }
        }
        CacheManager.putObjectInCache(uri, result, null);


        /*
         * During object retrieval, it might happen, that links to "parent" objects are not followed / populated,
         * as the parent object itself is just being examined and populated. This function corrects this afterward,
         * when the parent object is fully available and in cache.
         */

        if (depth == 0) {

            for (Map.Entry<URI, List<AbstractMap.SimpleEntry<Object, Method>>> entry : invokeLater.entrySet()) {
                URI invokeUri = entry.getKey();
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

        logger.debug("Removing URI \"{}\" from transient objects...", uri);
        transientObjects.remove(uri);

        return result;
    }

    /**
     * Internal representation of {@link #getObjectWithoutLinks(String, Class)}, used for recursion.
     *
     * @param url         The URL representing the object.
     * @param objectClass The destination class of the object.
     * @param collections A map of the collections already known.
     * @param depth       The current recursion depth.
     * @param <T>         The expected type of the returned object.
     * @return The object queried
     * @throws StoresthalException if the URL is invalid
     */
    private static <T> T getObjectWithoutLinks(String url, Class<T> objectClass, @SuppressWarnings("rawtypes") Map<String, Collection> collections, int depth) throws StoresthalException {

        URI uri = getUriFromUrl(url);

        T resultFromCache = CacheManager.getObjectFromCache(uri, objectClass, null);

        if (resultFromCache != null) {
            return resultFromCache;
        }

        httpCalls += 1;

        logger.debug("Adding URI \"{}\" to transient objects...", uri);
        transientObjects.add(uri);
        ResponseEntity<EntityModel<T>> response =
                null;

        try {
            //^^ otherwise, when using the diamond operator a java compiler error (!) will arise!
            response = getRestTemplateWithHalMessageConverter(false).exchange(url,
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
        } catch (RestClientException e) {
            throw new StoresthalException("Exception trying to get object from " + url, e);
        }
        T result = Objects.requireNonNull(response.getBody()).getContent();


        for (Link l : response.getBody().getLinks()) {

            if ("self".equals(l.getRel().value())) {
                logger.debug("Self-Link for object: {}", l.toUri());
                if (!(l.getRel().value().isBlank())) {
                    CacheManager.putObjectInCache(l.toUri(), result, null);
                }
            } else {
                followLink(objectClass, url, l, collections, 0, null, result, depth);
            }
        }
        CacheManager.putObjectInCache(uri, result, null);


        /*
         * During object retrieval, it might happen, that links to "parent" objects are not followed / populated,
         * as the parent object itself is just being examined and populated. This function corrects this afterwards,
         * when the parent object is fully available and in cache.
         */

        if (depth == 0) {

            for (Map.Entry<URI, List<AbstractMap.SimpleEntry<Object, Method>>> entry : invokeLater.entrySet()) {
                URI invokeUri = entry.getKey();
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

        logger.debug("Removing URI \"{}\" from transient objects...", uri);
        transientObjects.remove(uri);

        return result;
    }

    /**
     * Retrieve an object from a URL. Calling GET on the URL is expected to return UTF-8-encoded JSON. If the JSON
     * content / object contains links, these are expected to conform to the
     * <a href="http://stateless.co/hal_specification.html">HAL specifications</a>.
     * <p>
     * The JSON content will be retrieved and any collections encountered will be followed, resulting in a "complete"
     * object structure (including possible collections as well). Warnings and/or errors will be logged, if something
     * goes wrong (e.g. unparseable JSON / no setter for a relation was found / unable to retrieve relation / ...).
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
    public static <T> T getObjectWithoutLinks(String url, Class<T> objectClass) throws StoresthalException {

        if (Collection.class.isAssignableFrom(objectClass)) {
            logger.warn("""
                    You seem to be trying to retrieve a collection of objects using Storesthal.getObjectWithoutLinks on the first level. This will likely fail.
                    Please consider using Storesthal.getCollectionWithoutLinks in that case.
                    (Handling collections *within* the objects retrieved, therefore on any other but the first level, will work anyway.)""");
        }

        logger.info("Getting object of class \"{}\" from URL \"{}\".", objectClass.getCanonicalName(), url);
        return getObjectWithoutLinks(url, objectClass, new HashMap<>(), 0);
    }

    /**
     * Retrieve an object from a given URL, wrapped in {@link org.springframework.hateoas.EntityModel}
     *
     * @param url         The URL to retrieve the object from
     * @param objectClass The class of the object to retrieve (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @param <T>         The type of the object to retrieve (not wrapped in {@link org.springframework.hateoas.EntityModel})
     * @return The object found at the URL, wrapped in {@link org.springframework.hateoas.EntityModel}
     * @throws StoresthalException If the URL was invalid or no object could be retrieved from it
     */
    public static <T> EntityModel<T> getObject(String url, Class<T> objectClass) throws StoresthalException {

        if (Collection.class.isAssignableFrom(objectClass)) {
            logger.warn("""
                    You seem to be trying to retrieve a collection of objects using Storesthal.getObject on the first level. This will likely fail.
                    Please consider using Storesthal.getCollection in that case.
                    (Handling collections *within* the objects retrieved, therefore on any other but the first level, will work anyway.)""");
        }

        logger.info("Getting object of class \"EntityModel<{}>\" from URL \"{}\".", objectClass.getCanonicalName(), url);
        return getObject(url, objectClass, new HashMap<>(), 0);
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
     * a matching call to {@link Storesthal#getObjectWithoutLinks(String, Class)} occurs).
     *
     * @param cacheName             The cache to clear.
     * @param clearStatisticsAsWell Whether to clear the cache hit and miss statistics of the cache as well (resetting
     *                              both of them to zero).
     */
    public static void clearCache(String cacheName, boolean clearStatisticsAsWell) {
        CacheManager.clearCache(cacheName, clearStatisticsAsWell);
    }

    /**
     * Return the URI from a {@link org.springframework.hateoas.Link} object
     *
     * @param l The link containing the URI as href
     * @return The URI of the link
     * @throws StoresthalException If no URI could be created from the link's href
     */
    private static URI getUriFromLink(Link l) throws StoresthalException {
        return getUriFromUrl(l.getHref());
    }

    /**
     * Creates a URI from a URL (String) and returns it
     *
     * @param url The URL
     * @return The URI created from the URL
     * @throws StoresthalException if it was not possible to create a URI from the URL
     */
    private static URI getUriFromUrl(String url) throws StoresthalException {
        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new StoresthalException("Could not create URI from URL \"" + url + "\"", e);
        }

        return uri;
    }

}
