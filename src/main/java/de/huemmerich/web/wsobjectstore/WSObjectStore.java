package de.huemmerich.web.wsobjectstore;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.GenericTypeResolver;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.MediaTypes;
import org.springframework.hateoas.Resource;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.hateoas.client.Traverson;
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

    private <T> void followLink(Link l, Set<URI> linksVisited, Class<T> objectClass, Map<String,Collection> collections, T intermediateResult) throws WSObjectStoreException {
        if ("self".equals(l.getRel())) {
            return;
        }

        URI uri = null;

        try {
            uri = new URI(l.getHref());
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not create URI from URL \""+l.getHref()+"\"to visited links collection!", e);
        }

        if (linksVisited.contains(uri)) {
            return;
        }

        try {
            String methodName = "set"+ucFirst(l.getRel());

            Method m = searchForSetter(objectClass, l.getRel());

            if (m!=null) {
                Class type = m.getParameterTypes()[0];

                if (Collection.class.isAssignableFrom(type)) {

                    Type[] x = m.getGenericParameterTypes();

                    ParameterizedType xy = (ParameterizedType) x[0];

                    Class realType = (Class) xy.getActualTypeArguments()[0];

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
                            } catch (NoSuchMethodException | InstantiationException e) {
                                e.printStackTrace();
                            }
                        }
                        collections.put(l.getRel(),coll);
                    }


                    Object subObject = getObject(l.getHref(), realType, linksVisited, collections);

                    coll.add(subObject);

                    m.invoke(intermediateResult,coll);

                }
                else {
                    Object subObject = getObject(l.getHref(), type, linksVisited, collections);
                    m.invoke(intermediateResult, subObject);
                }

            }

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        linksVisited.add(uri);
    }

    private <T> T getObject(String url, Class<T> objectClass, Set<URI> linksVisited, Map<String,Collection> collections) throws WSObjectStoreException {

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

        try {
            linksVisited.add(new URI(url));
        } catch (URISyntaxException e) {
            throw new WSObjectStoreException("Could not add url\""+url+"\"to visited links collection!", e);
        }

        for (Link l: response.getBody().getLinks()) {
            followLink(l, linksVisited, objectClass, collections, result);
        }

        return result;
    }

    private static final Method searchForSetter(Class objectClass, String rel) {
        String methodName = "set"+ucFirst(rel);
        for (Method m: objectClass.getMethods()) {
            if (m.getName().equals(methodName) && (m.getParameterCount() == 1)) {
                return m;
            }
        }
        return null;
    }

    public <T> T getObject(String url, Class<T> objectClass) throws WSObjectStoreException {

        return getObject(url, objectClass, new HashSet<>(), new HashMap<String,Collection>());

    }

}
