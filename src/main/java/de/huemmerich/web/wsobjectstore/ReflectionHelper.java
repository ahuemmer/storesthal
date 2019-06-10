package de.huemmerich.web.wsobjectstore;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class ReflectionHelper {

    public static Set<Method> getMethodsAnnotatedWith(Class objectClass, Class<? extends Annotation> annotationClass) {

        Set<Method> result = new HashSet<>();

        for (Method m: objectClass.getMethods()) {
            for (Annotation a: m.getAnnotationsByType(annotationClass)) {
                result.add(m);
            }
        }

        return result;

    }

    public static Set<Field> getFieldsAnnotatedWith(Class objectClass, Class<? extends Annotation> annotationClass) {

        Set<Field> result = new HashSet<>();

        for (Field f: objectClass.getDeclaredFields()) {
            for (Annotation a: f.getAnnotationsByType(annotationClass)) {
                result.add(f);
            }
        }

        return result;

    }

    public static Method searchForSetterByMethodName(Class objectClass, String methodName) {
        for (Method m: objectClass.getMethods()) {
            if (m.getName().equals(methodName) && (m.getParameterCount() == 1)) {
                return m;
            }
        }
        return null;
    }

}
