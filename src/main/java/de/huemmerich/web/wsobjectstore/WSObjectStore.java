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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
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

    private <T> T getObject(String url, Class<T> objectClass, Set<URI> linksVisited) {

        RestTemplate restTemplate = getRestTemplateWithHalMessageConverter();

        ResponseEntity<Resource<T>> response =
                restTemplate.exchange(url,
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
            e.printStackTrace();
        }

        for (Link l: response.getBody().getLinks()) {

            if ("self".equals(l.getRel())) {
                continue;
            }

            URI uri = null;

            try {
                uri = new URI(l.getHref());
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            if (linksVisited.contains(uri)) {
                continue;
            }

            try {
                String methodName = "set"+ucFirst(l.getRel());

                Method m = searchForSetter(objectClass, l.getRel());

                if (m!=null) {
                    Class type = m.getParameterTypes()[0];
                    Object subObject = getObject(l.getHref(), type, linksVisited);
                    m.invoke(result, subObject);
                }

            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
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

    public <T> T getObject(String url, Class<T> objectClass) {

        return getObject(url, objectClass, new HashSet<>());

    }

}
