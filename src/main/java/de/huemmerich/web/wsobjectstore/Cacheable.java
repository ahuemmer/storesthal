package de.huemmerich.web.wsobjectstore;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Cacheable {

    String cacheName() default WSObjectStore.COMMON_CACHE_NAME;

}
