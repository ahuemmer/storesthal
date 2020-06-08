package com.github.ahuemmer.storesthal;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Every field or setter method annotation with this annotation will be treated as an object retrieved via a HAL
 * relation (resp. a corresponding setter).
 * The name (JSON key) of the relation can be given using {@link #value()}, which is advised. If no value is set,
 * the JSON key is expected to be named equally to the field / setter (with the prefix "set" stripped off).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface HALRelation {

    /**
     * The name (JSON key) of the relation If no value is set, the JSON key is expected to be named equally to the field
     * / setter (with the prefix "set" stripped off) annotated with {@link HALRelation}.
     * @return relation name (JSON key)
     */
    String value() default "";
}
