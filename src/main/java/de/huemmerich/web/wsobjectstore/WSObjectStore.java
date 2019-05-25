package de.huemmerich.web.wsobjectstore;

import com.fasterxml.jackson.databind.ObjectMapper;
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

    private final Map<String, HALObjectMetadata> halObjectClasses = new HashMap<>();
    private final Set<HALObjectMetadata> halObjectClassesWithoutUrl = new HashSet<>();

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

    private static final Map<String,Map<URI,Object>> caches = new HashMap<>();

    private static WSObjectStoreConfiguration configuration;

    public static final String COMMON_CACHE_NAME="com.github.ahuemmer.wsobjectstore.cache.common";

    /*public WSObjectStore(String basePackage) {

        if (basePackage==null) {
            basePackage="";
        }

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(HALObject.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            try {
                Class objectClass = Class.forName(bd.getBeanClassName());
                String url = ((HALObject) objectClass.getAnnotation(HALObject.class)).url();
                if ("".equals(url)) {
                    halObjectClassesWithoutUrl.add(new HALObjectMetadata(objectClass));
                }
                else {
                    halObjectClasses.put(url,new HALObjectMetadata(objectClass,url));
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }*/

    public static void setConfiguration(WSObjectStoreConfiguration configuration) {
        WSObjectStore.configuration = configuration;
    }


    /*public final Set<HALObjectMetadata> getHalObjectClasses() {
        Stream<HALObjectMetadata> result = Stream.concat(halObjectClasses.values().stream(), halObjectClassesWithoutUrl.stream());
        return result.collect(Collectors.toSet());
    }*/

    private HttpMessageConverter getHalMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
        halConverter.setSupportedMediaTypes(Collections.singletonList(HAL_JSON_UTF8));
        halConverter.setObjectMapper(objectMapper);
        return halConverter;
    }

    private HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(HAL_JSON_UTF8));
        return new HttpEntity<>(headers);
    }

    private RestTemplate getRestTemplateWithHalMessageConverter() {
        RestTemplate restTemplate = new RestTemplate();

        List<HttpMessageConverter<?>> existingConverters = restTemplate.getMessageConverters();
        List<HttpMessageConverter<?>> newConverters = new ArrayList<>();
        newConverters.add(getHalMessageConverter());
        newConverters.addAll(existingConverters);
        restTemplate.setMessageConverters(newConverters);

        return restTemplate;
    }

    private static String ucFirst(String input) {
        if (input==null) {
            return null;
        }
        return input.substring(0,1).toUpperCase()+input.substring(1);
    }

    @SuppressWarnings("unchecked")
    private<T> void handleCollection(Link l, Method m, Map<String,Collection> collections, Set<URI> linksVisited, T intermediateResult, int depth) throws WSObjectStoreException {
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

        String cacheName = getObjectCacheName(objectClass);
        if (cacheName == null) {
            cacheName = INTERMEDIATE_CACHE_NAME;
        }

        logger.debug("Trying to get object with URI "+uri+" from cache \""+cacheName+"\"...");

        Map<URI,Object> cache = caches.get(cacheName);
        Object result = null;
        if (cache!=null) {
            result = cache.get(uri);
        }

        if (result!=null) {
            cacheHits.putIfAbsent(cacheName, 0);
            cacheHits.put(cacheName, cacheHits.get(cacheName)+1);
            logger.debug("Cache hit for URI "+uri+" in cache \""+cacheName+"\"!");
        }
        else {
            cacheMisses.putIfAbsent(cacheName, 0);
            cacheMisses.put(cacheName, cacheMisses.get(cacheName)+1);
            logger.debug("Cache miss for URI "+uri+" in cache \""+cacheName+"\"!");
        }
        return result;
    }

    private static void putObjectInCache(URI uri, Object object) {
        String cacheName = getObjectCacheName(object.getClass());
        if (cacheName==null) {
            cacheName = INTERMEDIATE_CACHE_NAME;
        }

        caches.putIfAbsent(cacheName, new HashMap<URI, Object>());

        logger.debug("Putting one object into cache named \""+cacheName+"\".");

        caches.get(cacheName).put(uri, object);

        logger.debug("\""+cacheName+" size is now "+caches.get(cacheName).size());
    }

    private static String getObjectCacheName(Class cls) {
        Cacheable annotation = (Cacheable) cls.getDeclaredAnnotation(Cacheable.class);

        if (annotation!=null) {
            return annotation.cacheName();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> void followLink(Link l, Set<URI> linksVisited, Class<T> objectClass, Map<String,Collection> collections, T intermediateResult, int depth) throws WSObjectStoreException {
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
            else if (linksVisited.contains(uri)) {
                subObject = getObjectFromCache(uri, objectClass);
            }
            else {
                subObject = getObject(l.getHref(), type, linksVisited, new HashMap<>(), depth + 1);
            }

            invokeSetter(m, intermediateResult, subObject);

        }

        linksVisited.add(uri);
    }

    private void markForLaterInvocation(URI uri, Object object, Method method) {
        if (!invokeLater.containsKey(uri)) {
            invokeLater.put(uri, new LinkedList<>());
        }
        invokeLater.get(uri).add(new AbstractMap.SimpleEntry<>(object, method));
    }

    private void invokeSetter(Method m, Object applyTo, Object parameter) throws WSObjectStoreException {
        try {
            m.invoke(applyTo, parameter);
        } catch (IllegalAccessException | InvocationTargetException | IllegalArgumentException e) {
            throw new WSObjectStoreException("Could not invoke method \"" + m.getName() + "(" + applyTo.getClass().getCanonicalName() + ")\" on instance of \"" + parameter.getClass().getCanonicalName() + "\" class.", e);
        }
    }

    private <T> T getObject(String url, Class<T> objectClass, Set<URI> linksVisited, Map<String,Collection> collections, int depth) throws WSObjectStoreException {

        httpCalls+=1;

        URI uri;

        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not create URI from url\""+url+"\"!", e);
        }

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
                    Object cachedObject = getObjectFromCache(uri, objectClass);
                    invokeSetter(objectAndMethod.getValue(), objectAndMethod.getKey(), cachedObject);
                }
            }

            transientObjects.clear();
            clearCache(INTERMEDIATE_CACHE_NAME);
            invokeLater.clear();
        }

        logger.debug("Removing URI "+uri.toString()+" from transient objects...");
        transientObjects.remove(uri);

        return result;
    }

    private static Method searchForSetter(Class objectClass, String rel) {
        String methodName = "set"+ucFirst(rel);
        for (Method m: objectClass.getMethods()) {
            if (m.getName().equals(methodName) && (m.getParameterCount() == 1)) {
                return m;
            }
        }
        logger.warn("No setter found for relation \""+rel+"\" in class \""+objectClass.getCanonicalName()+"\"!");
        return null;
    }

    public <T> T getObject(String url, Class<T> objectClass) throws WSObjectStoreException {

        logger.info("Getting object of class \""+objectClass.getCanonicalName()+"\" from URL \""+url+"\".");

        //transientObjects.clear();

        return getObject(url, objectClass, new HashSet<>(), new HashMap<>(), 0);

    }

    public static Map<String,Object> getStatistics() {
        return Map.of("httpCalls",httpCalls, "cacheHits", cacheHits, "cacheMisses", cacheMisses);
    }

    public static void resetStatistics() {
        httpCalls = 0;
        cacheHits.clear();
        cacheMisses.clear();
    }

    public static void clearCache(String cacheName) {
        caches.get(cacheName).clear();
    }


    public static void clearAllCaches() {
        for (String key: caches.keySet()) {
            clearCache(key);
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
