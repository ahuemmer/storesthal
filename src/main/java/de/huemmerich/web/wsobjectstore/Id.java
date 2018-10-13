package de.huemmerich.web.wsobjectstore;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;

@Target({ FIELD, METHOD })
@Retention(value=RetentionPolicy.RUNTIME)
public @interface Id {

		int order() default 0;

}
