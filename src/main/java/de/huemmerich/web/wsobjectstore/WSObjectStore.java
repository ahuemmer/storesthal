package de.huemmerich.web.wsobjectstore;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.reflections.Reflections;
import org.reflections.scanners.FieldAnnotationsScanner;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.type.filter.AnnotationTypeFilter;
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

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.reflect.TypeUtils.parameterize;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_UTF8;

public class WSObjectStore {

    /*public WSObjectStore() {
        this(null);
    }*/

    private static int httpCalls=0;
    private static final Map<String,Integer> cacheMisses = new HashMap<>();
    private static final Map<String,Integer> cacheHits = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(WSObjectStore.class);

    private static final String INTERMEDIATE_CACHE_NAME="com.github.ahuemmer.wsobjectstore.cache.intermediate";

    private static final Set<URI> transientObjects=new HashSet<>();

    private static final Map<URI, List<AbstractMap.SimpleEntry<Object,Method>>> invokeLater = new HashMap<>();

    private static final Map<String,LRUCache<URI,Object>> caches = new HashMap<>();

    private static WSObjectStoreConfiguration configuration;

    public static final String COMMON_CACHE_NAME="com.github.ahuemmer.wsobjectstore.cache.common";

    private static boolean initialized=false;

    private static void init() {
        if (!initialized) {
            init(new WSObjectStoreConfiguration());
        }
    }

    public static void init(final WSObjectStoreConfiguration configuration) {
        WSObjectStore.configuration = configuration;
        clearAllCaches(true);
        initialized=true;
    }

    public static WSObjectStoreConfiguration getConfiguration() {
        return configuration;
    }

    private static HttpMessageConverter getHalMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
        halConverter.setSupportedMediaTypes(Collections.singletonList(HAL_JSON_UTF8));
        halConverter.setObjectMapper(objectMapper);
        return halConverter;
    }

    private static HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(HAL_JSON_UTF8));
        return new HttpEntity<>(headers);
    }

    private static RestTemplate getRestTemplateWithHalMessageConverter() {
        RestTemplate restTemplate = new RestTemplate();

        List<HttpMessageConverter<?>> existingConverters = restTemplate.getMessageConverters();
        List<HttpMessageConverter<?>> newConverters = new ArrayList<>();
        newConverters.add(getHalMessageConverter());
        newConverters.addAll(existingConverters);
        restTemplate.setMessageConverters(newConverters);

        return restTemplate;
    }

    private static String lcFirst(String input) {
        if (input==null) {
            return null;
        }
        return input.substring(0,1).toLowerCase()+input.substring(1);
    }

    private static String ucFirst(String input) {
        if (input==null) {
            return null;
        }
        return input.substring(0,1).toUpperCase()+input.substring(1);
    }

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

    private static Object getObjectFromCache(URI uri, Class objectClass) {

        logger.debug("Trying to get object with URI "+uri+" from cache...");

        LRUCache<URI,Object> cache = getCache(objectClass);

        if (configuration.isCachingDisabled()&&!(cache.getCacheName().equals(INTERMEDIATE_CACHE_NAME))) {
            logger.debug("Caching is disabled!");
            return null;
        }

        Object result = null;
        if (cache!=null) {
            result = cache.get(uri);
        }

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

    private static void putObjectInCache(URI uri, Object object) {

        LRUCache cache = getCache(object.getClass());

        if (configuration.isCachingDisabled()&&!(cache.getCacheName().equals(INTERMEDIATE_CACHE_NAME))) {
            return;
        }

        logger.debug("Putting one object of class \""+object+"\" into cache named \""+cache.getCacheName()+"\" for URI "+uri.toString());

        cache.put(uri, object);

        logger.debug("\""+cache.getCacheName()+"\" cache size is now: "+cache.size());

    }

    private static LRUCache<URI, Object> getCache(Class cls) {
        Cacheable annotation = (Cacheable) cls.getDeclaredAnnotation(Cacheable.class);

        String cacheName = (annotation!=null)?annotation.cacheName():INTERMEDIATE_CACHE_NAME;

        logger.debug("Cache for object class \""+cls.getCanonicalName()+"\" is named \""+cacheName+"\".");

        int cacheSize = (annotation!=null)?annotation.cacheSize():configuration.getDefaultCacheSize();

        caches.putIfAbsent(cacheName, new LRUCache<URI, Object>(cacheName, cacheSize));

        return caches.get(cacheName);
    }

    @SuppressWarnings("unchecked")
    private static <T> void followLink(Link l, Set<URI> linksVisited, Class<T> objectClass, Map<String,Collection> collections, T intermediateResult, int depth) throws WSObjectStoreException {
        if ("self".equals(l.getRel())) {
            return;
        }

        URI uri;

        try {
            uri = new URI(l.getHref());
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not create URI from URL \""+l.getHref()+"\"to visited links collection!", e);
        }

        Method m = searchForSetter(objectClass, l.getRel());

        if (m!=null) {

            Class type = m.getParameterTypes()[0];

            Object subObject;

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

            subObject = getObject(l.getHref(), type, linksVisited, new HashMap<>(), depth + 1);

            invokeSetter(m, intermediateResult, subObject);

        }

        linksVisited.add(uri);
    }

    private static void markForLaterInvocation(URI uri, Object object, Method method) {
        if (!invokeLater.containsKey(uri)) {
            invokeLater.put(uri, new LinkedList<>());
        }
        invokeLater.get(uri).add(new AbstractMap.SimpleEntry<>(object, method));
    }

    private static void invokeSetter(Method m, Object applyTo, Object parameter) throws WSObjectStoreException {
        try {
            m.invoke(applyTo, parameter);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new WSObjectStoreException("Could not invoke method \"" + m.getName() + "(" + applyTo.getClass().getCanonicalName() + ")\" on instance of \"" + parameter.getClass().getCanonicalName() + "\" class.", e);
        }
    }

    private static <T> T getObject(String url, Class<T> objectClass, Set<URI> linksVisited, Map<String,Collection> collections, int depth) throws WSObjectStoreException {

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not create URI from url\""+url+"\"!", e);
        }

        Object resultFromCache = getObjectFromCache(uri, objectClass);

        if (resultFromCache!=null) {
            return (T) resultFromCache;
        }

        httpCalls+=1;

        logger.debug("Adding URI "+uri.toString()+" to transient objects...");
        transientObjects.add(uri);

        //noinspection Convert2Diamond
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

    private static Method searchForSetter(Class objectClass, String rel) {

        logger.debug("Searching setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\"");

        if (configuration.isAnnotationless()) {
            String methodName = "set"+ucFirst(rel);
            Method m = ReflectionHelper.searchForSetterByMethodName(objectClass, methodName);
            if (m==null) {
                logger.warn("No setter found for relation \""+rel+"\" in class \""+objectClass.getCanonicalName()+"\"!");
            }
            else {
                logger.debug("Setter for relation \"" + rel + "\" for object class \"" + objectClass.getCanonicalName() + "\" found: " + m.getName()+" (annotationless mode!)");
            }
            return m;
        }
        else {
            Set<Method> methods = ReflectionHelper.getMethodsAnnotatedWith(objectClass, HALRelation.class);
            for (Method m: methods) {
                if (m.getAnnotation(HALRelation.class).value().equals(rel)) {
                    if (m.getParameterCount() == 1) {
                        logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                    logger.warn("Method \""+m.getName()+"\" is annotated with \""+HALRelation.class.getName()+"\" and would be a suitable setter candidate for relation \""+rel+"\", but has the wrong number of parameters!");
                }
                else if (m.getAnnotation(HALRelation.class).value().equals("")) {
                    if (lcFirst(m.getName().substring(3)).equals(rel)) {
                        logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                }
            }
            //Nothing found up to now, let's go on and search the fields...
            Set<Field> fields = ReflectionHelper.getFieldsAnnotatedWith(objectClass, HALRelation.class);
            for (Field f: fields) {
                if (f.getAnnotation(HALRelation.class).value().equals(rel)) {
                    String methodName = "set"+ucFirst(f.getName());
                    Method m = ReflectionHelper.searchForSetterByMethodName(objectClass, methodName);
                    if (m!=null) {
                        logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                }
                else if (f.getAnnotation(HALRelation.class).value().equals("")) {
                    String methodName = "set"+ucFirst(rel);
                    Method m = ReflectionHelper.searchForSetterByMethodName(objectClass, methodName);
                    if (m!=null) {
                        logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                }
            }
            logger.warn("No setter found for relation \""+rel+"\" in class \""+objectClass.getCanonicalName()+"\"!");
            return null;
        }
    }



    public static <T> T getObject(String url, Class<T> objectClass) throws WSObjectStoreException {
        init();
        logger.info("Getting object of class \""+objectClass.getCanonicalName()+"\" from URL \""+url+"\".");
        return getObject(url, objectClass, new HashSet<>(), new HashMap<>(), 0);

    }

    /**
     * Return some information on cache hits and misses
     * ATTN: Only "direct" hits and misses are counted. E. g., if an object is retrieved from cache the subobject
     * of which is also cached, the subobject cache hit will not be counted! (Nevertheless the subobject is correctly
     * retrieved from cache.)
     * @return
     */
    public static Map<String,Object> getStatistics() {
        return Map.of("httpCalls",httpCalls, "cacheHits", cacheHits, "cacheMisses", cacheMisses);
    }

    public static void resetStatistics() {
        httpCalls = 0;
        cacheHits.clear();
        cacheMisses.clear();
    }

    public static void clearCache(String cacheName, boolean clearStatisticsAsWell) {
        if (caches.containsKey(cacheName)) {
            caches.get(cacheName).clear();
        }
        if (clearStatisticsAsWell) {
            cacheHits.put(cacheName,0);
            cacheMisses.put(cacheName,0);
        }
    }

    public static int getCachedObjectCount(String cacheName) {
        if (caches.get(cacheName)==null) {
            return 0;
        }
        return caches.get(cacheName).size();
    }

    public static void clearAllCaches() {
        clearAllCaches(false);
    }

    public static void clearAllCaches(boolean clearStatisticsAsWell) {
        for (String key: caches.keySet()) {
            clearCache(key, clearStatisticsAsWell);
        }
        if (clearStatisticsAsWell) {
            httpCalls=0;
        }
    }

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
