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
import org.springframework.web.client.RestTemplate;

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

    private Map<String, HALObjectMetadata> halObjectClasses = new HashMap<>();
    private Set<HALObjectMetadata> halObjectClassesWithoutUrl = new HashSet<>();

    public WSObjectStore() {
        this(null);
    }

    private static int httpCalls=0;
    private static Map<String,Integer> cacheMisses = new HashMap<>();
    private static Map<String,Integer> cacheHits = new HashMap<>();

    private static Logger logger = LoggerFactory.getLogger(WSObjectStore.class);

    private static final String INTERMEDIATE_CACHE_NAME="intermediateCache";

    private static Set<URI> transientObjects=new HashSet<>();

    private static Map<URI, List<AbstractMap.SimpleEntry<Object,Method>>> invokeLater = new HashMap<>();

    public WSObjectStore(String basePackage) {

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

    }

    private static Map<URI,Object> intermediateCache = new HashMap<>();

    public final Set<HALObjectMetadata> getHalObjectClasses() {
        Stream<HALObjectMetadata> result = Stream.concat(halObjectClasses.values().stream(), halObjectClassesWithoutUrl.stream());
        return result.collect(Collectors.toSet());
    }

    private HttpMessageConverter getHalMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jackson2HalModule());
        MappingJackson2HttpMessageConverter halConverter = new TypeConstrainedMappingJackson2HttpMessageConverter(ResourceSupport.class);
        halConverter.setSupportedMediaTypes(Arrays.asList(HAL_JSON_UTF8));
        halConverter.setObjectMapper(objectMapper);
        return halConverter;
    }

    private HttpEntity<String> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(HAL_JSON_UTF8));
        HttpEntity<String> entity = new HttpEntity<>(headers);
        return entity;
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

    private static final String ucFirst(String input) {
        if (input==null) {
            return null;
        }
        return input.substring(0,1).toUpperCase()+input.substring(1);
    }

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
            Method addMethod = null;
            try {
                addMethod = coll.getClass().getMethod("add", Object.class);
            } catch (NoSuchMethodException e) {
                throw new WSObjectStoreException("Could not find \"add\" method for collection class "+coll.getClass().getCanonicalName());
            }

            markForLaterInvocation(uri, coll, addMethod);
        }
        else {
            Object subObject = getObject(l.getHref(), realType, linksVisited, new HashMap<String, Collection>(), depth + 1);
            coll.add(subObject);
        }

        try {
            m.invoke(intermediateResult,coll);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new WSObjectStoreException("Could not invoke method \""+m.getName()+"("+coll.getClass().getCanonicalName()+")\" on instance of \""+intermediateResult.getClass().getCanonicalName()+"\" class.", e);
        }
    }

    private static Object getObjectFromCache(URI uri) {
        logger.debug("Trying to get object with URI "+uri+" from cache \""+INTERMEDIATE_CACHE_NAME+"\"...");
        Object result = intermediateCache.get(uri);
        if (result!=null) {
            if (cacheHits.get(INTERMEDIATE_CACHE_NAME)==null) {
                cacheHits.put(INTERMEDIATE_CACHE_NAME,0);
            }
            cacheHits.put(INTERMEDIATE_CACHE_NAME, cacheHits.get(INTERMEDIATE_CACHE_NAME)+1);
            logger.debug("Cache hit for URI "+uri+" in cache \""+INTERMEDIATE_CACHE_NAME+"\"!");
        }
        else {
            if (cacheMisses.get(INTERMEDIATE_CACHE_NAME)==null) {
                cacheMisses.put(INTERMEDIATE_CACHE_NAME,0);
            }
            cacheMisses.put(INTERMEDIATE_CACHE_NAME, cacheMisses.get(INTERMEDIATE_CACHE_NAME)+1);
            logger.debug("Cache miss for URI "+uri+" in cache \""+INTERMEDIATE_CACHE_NAME+"\"!");
        }
        return result;
    }

    private static void putObjectInCache(URI uri, Object object, String cacheName) {

        if (INTERMEDIATE_CACHE_NAME.equals(cacheName)) {
            intermediateCache.put(uri, object);
            logger.debug("Put one object into "+INTERMEDIATE_CACHE_NAME+" for URI \""+uri.toString()+"\".");
            logger.debug(INTERMEDIATE_CACHE_NAME+" size is now "+intermediateCache.size()+".");
        }
        else {
            logger.error("Caches other than \""+INTERMEDIATE_CACHE_NAME+"\" are not supported yet!");
        }

    }

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

            Object subObject = null;

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
                subObject = getObjectFromCache(uri);
            }
            else {
                subObject = getObject(l.getHref(), type, linksVisited, new HashMap<String, Collection>(), depth + 1);
            }

            invokeSetter(m, intermediateResult, subObject);

        }

        linksVisited.add(uri);
    }

    private void markForLaterInvocation(URI uri, Object object, Method method) {
        if (!invokeLater.containsKey(uri)) {
            invokeLater.put(uri, new LinkedList<AbstractMap.SimpleEntry<Object,Method>>());
        }
        invokeLater.get(uri).add(new AbstractMap.SimpleEntry<Object,Method>(object,method));
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

        ResponseEntity<Resource<T>> response =
                getRestTemplateWithHalMessageConverter().exchange(url,
                        HttpMethod.GET, getHttpEntity(), new ParameterizedTypeReference<Resource<T>>() {
                            @Override
                            public Type getType() {
                                Type type = super.getType();
                                if (type instanceof ParameterizedType) {
                                    Type[] responseWrapperActualTypes = {objectClass};
                                    ParameterizedType responseWrapperType = parameterize(Resource.class,
                                            responseWrapperActualTypes);
                                    return responseWrapperType;
                                }
                                return type;
                            }
                        });

        T result = response.getBody().getContent();

        linksVisited.add(uri);
        for (Link l: response.getBody().getLinks()) {
            followLink(l, linksVisited, objectClass, collections, result, depth);
        }
        putObjectInCache(uri, result, INTERMEDIATE_CACHE_NAME);


        /**
         * During object retrieval, it might happen, that links to "parent" objects are not followed / populated,
         * as the parent object itself is just being examined and populated. This function corrects this afterwards,
         * when the parent object is fully available and in cache.
         */

        if (depth==0) {

            for(URI invokeUri: invokeLater.keySet()) {
                List<AbstractMap.SimpleEntry<Object, Method>> invocationList = invokeLater.get(invokeUri);
                for (AbstractMap.SimpleEntry<Object, Method> objectAndMethod: invocationList) {
                    Object cachedObject = getObjectFromCache(uri);
                    invokeSetter(objectAndMethod.getValue(), objectAndMethod.getKey(), cachedObject);
                }
            }

            transientObjects.clear();
            intermediateCache.clear();
            invokeLater.clear();
        }

        logger.debug("Removing URI "+uri.toString()+" from transient objects...");
        transientObjects.remove(uri);

        return result;
    }

    private static final Method searchForSetter(Class objectClass, String rel) {
        String methodName = "set"+ucFirst(rel);
        for (Method m: objectClass.getMethods()) {
            if (m.getName().equals(methodName) && (m.getParameterCount() == 1)) {
                return m;
            }
        }
        logger.warn("No setter found for relation \""+rel+"\" in class \""+objectClass.getCanonicalName()+"\"!");
        return null;
    }

    private static final Method searchForGetter(Class objectClass, String rel) {
        String methodName = "get"+ucFirst(rel);
        for (Method m: objectClass.getMethods()) {
            if (m.getName().equals(methodName) && (m.getParameterCount() == 0)) {
                return m;
            }
        }
        return null;
    }

    public <T> T getObject(String url, Class<T> objectClass) throws WSObjectStoreException {

        logger.info("Getting object of class \""+objectClass.getCanonicalName()+"\" from URL \""+url+"\".");

        //transientObjects.clear();

        return getObject(url, objectClass, new HashSet<>(), new HashMap<String,Collection>(), 0);

    }

    public static Map<String,Object> getStatistics() {
        return Map.of("httpCalls",httpCalls, "cacheHits", cacheHits, "cacheMisses", cacheMisses);
    }

    public static void resetStatistics() {
        httpCalls = 0;
        cacheHits.clear();
        cacheMisses.clear();
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
