package de.huemmerich.web.storesthal;

import de.huemmerich.web.storesthal.configuration.WSObjectStoreConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Static helper class bundling some reflection related methods.
 */
public class ReflectionHelper {

    /**
     * The logger.
     */
    private static final Logger logger = LoggerFactory.getLogger(ReflectionHelper.class);

    /**
     * Get all methods of a class having a specific annotation
     * @param objectClass The class to search for the annotation
     * @return All methods of the object class having the annotation specified (if any)
     */
    private static Set<Method> getMethodsAnnotatedWith(Class objectClass) {

        Set<Method> result = new HashSet<>();

        for (Method m: objectClass.getMethods()) {
            for (Annotation a: m.getAnnotationsByType((Class<? extends Annotation>) HALRelation.class)) {
                result.add(m);
            }
        }

        return result;

    }

    /**
     * Get all fields of a class having a specific annotation
     * @param objectClass The class to search for the annotation
     * @return All fields of the object class having the annotation specified (if any)
     */
    private static Set<Field> getFieldsAnnotatedWith(Class objectClass) {

        Set<Field> result = new HashSet<>();

        for (Field f: objectClass.getDeclaredFields()) {
            for (Annotation a: f.getAnnotationsByType((Class<? extends Annotation>) HALRelation.class)) {
                result.add(f);
            }
        }

        return result;

    }

    /**
     * Look up a setter method of a specific class
     * @param objectClass The class to search for the setter
     * @param methodName The setter method name to be searched for
     * @return The first matching method encountered (if any) or null.
     * TODO: Consider searching for method parameter as well for reasons of type safety!
     */
    private static Method searchForSetterByMethodName(Class objectClass, String methodName) {
        for (Method m: objectClass.getMethods()) {
            if (m.getName().equals(methodName) && (m.getParameterCount() == 1)) {
                return m;
            }
        }
        return null;
    }

    /**
     * Return the input string with the first character being converted to lower case
     * @param input The input string
     * @return The input string with lower case first character
     */
    private static String lcFirst(String input) {
        if (input==null) {
            return null;
        }
        return input.substring(0,1).toLowerCase()+input.substring(1);
    }

    /**
     * Return the input string with the first character being converted to upper case
     * @param input The input string
     * @return The input string with upper case first character
     */
    private static String ucFirst(String input) {
        if (input==null) {
            return null;
        }
        return input.substring(0,1).toUpperCase()+input.substring(1);
    }

    /**
     * Search for the setter method for a relation of a specific object class.
     * The search is carried out like this:
     * <ul>
     *     <li>If {@link WSObjectStoreConfiguration#isAnnotationless()} is `true`, the setter will only be searched by
     *         the method name (`set${relationName}`) is expected, where ${relationName} is the name of the relation
     *         als retrieved from the JSON content, with the first letter capitalized.
     *     </li>
     *     <li>
     *         If {@link WSObjectStoreConfiguration#isAnnotationless()} is `false` (which is the default!):
     *         <ul>
     *             <li>
     *                 The class will be searched for <i>methods</i> annotated with {@link HALRelation} and a value matching
     *                 the relation name OR methods annotated with {@link HALRelation} and no value but a method name
     *                 matching the relation name. In the latter case, the first three characters of the method name
     *                 are truncated and the following character is converted to lower case. (E. g. `setParent`
     *                 becomes `parent` which is matched against the relation name.)
     *             </li>
     *             <li>
     *                 The class will be searched for <i>fields</i> annotated with {@link HALRelation} and a value matching
     *                 the relation name OR fields annotated with {@link HALRelation} and no value but a field name
     *                 matching the relation name. If such a field is found, a setter matching the field name (with
     *                 `set` prefix, upper case for the first character) is expected and - if present - returned.
     *                 E. g.: If the field's name is `category`, the setter method is expected to be called `setCategory`.
     *             </li>
     *         </ul>
     *     </li>
     * </ul>
     * These rules are evaluated in the order they are described here.
     * @param objectClass The class of the object where the setter is to be searched for.
     * @param rel The name of the relation
     * @return A matching setter method or `null` if none was found.
     */
    static Method searchForSetter(Class objectClass, String rel) {

        ReflectionHelper.logger.debug("Searching setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\"");

        if (WSObjectStore.getConfiguration().isAnnotationless()) {
            String methodName = "set"+ ReflectionHelper.ucFirst(rel);
            Method m = searchForSetterByMethodName(objectClass, methodName);
            if (m==null) {
                ReflectionHelper.logger.warn("No setter found for relation \""+rel+"\" in class \""+objectClass.getCanonicalName()+"\"!");
            }
            else {
                ReflectionHelper.logger.debug("Setter for relation \"" + rel + "\" for object class \"" + objectClass.getCanonicalName() + "\" found: " + m.getName()+" (annotationless mode!)");
            }
            return m;
        }
        else {
            Set<Method> methods = getMethodsAnnotatedWith(objectClass);
            for (Method m: methods) {
                if (m.getAnnotation(HALRelation.class).value().equals(rel)) {
                    if (m.getParameterCount() == 1) {
                        ReflectionHelper.logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                    ReflectionHelper.logger.warn("Method \""+m.getName()+"\" is annotated with \""+HALRelation.class.getName()+"\" and would be a suitable setter candidate for relation \""+rel+"\", but has the wrong number of parameters!");
                }
                else if (m.getAnnotation(HALRelation.class).value().equals("")) {
                    if (ReflectionHelper.lcFirst(m.getName().substring(3)).equals(rel)) {
                        ReflectionHelper.logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                }
            }
            //Nothing found up to now, let's go on and search the fields...
            Set<Field> fields = getFieldsAnnotatedWith(objectClass);
            for (Field f: fields) {
                if (f.getAnnotation(HALRelation.class).value().equals(rel)) {
                    String methodName = "set"+ ReflectionHelper.ucFirst(f.getName());
                    Method m = searchForSetterByMethodName(objectClass, methodName);
                    if (m!=null) {
                        ReflectionHelper.logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                }
                else if (f.getAnnotation(HALRelation.class).value().equals("")) {
                    String methodName = "set"+ ReflectionHelper.ucFirst(rel);
                    Method m = searchForSetterByMethodName(objectClass, methodName);
                    if (m!=null) {
                        ReflectionHelper.logger.debug("Setter for relation \""+rel+"\" for object class \""+objectClass.getCanonicalName()+"\" found: "+m.getName());
                        return m;
                    }
                }
            }
            ReflectionHelper.logger.warn("No setter found for relation \""+rel+"\" in class \""+objectClass.getCanonicalName()+"\"!");
            return null;
        }
    }
}
