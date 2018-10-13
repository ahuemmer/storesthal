package de.huemmerich.web.wsobjectstore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class IdFinder {

	private static Map<Class<? extends WebServiceObject>,Vector<Member>> knownClasses = new HashMap<Class<? extends WebServiceObject>,Vector<Member>>();
	private static SortedMap<Integer,List<Member>> findings = new TreeMap<Integer, List<Member>>();

	private static final Logger logger = LogManager.getFormatterLogger(IdFinder.class.getSimpleName());

	public static Vector<Member> findIds(Class<? extends WebServiceObject> clazz) throws IdFinderException {

		if (knownClasses.containsKey(clazz)) {
			return knownClasses.get(clazz);
		}

		findings.clear();

		searchFields(clazz);
		searchMethods(clazz);

		if (findings.size()==0) {
			searchWithoutAnnotation(clazz);
		}

		if (findings.size()==0) {
			throw new IdFinderException(clazz, "Neither an @"+Id.class.getSimpleName()+" annotation nor a field named \"id\" nor a method named\"getId\" was found!");
		}

		if (findings.keySet().contains(0) && findings.keySet().size()>1) {
			throw new IdFinderException(clazz, "Annotations with and without order attribute were found. Please use no order attributes at all or supply every @"+Id.class.getSimpleName()+" annotation with one.");
		}

		Vector<Member> result = new Vector<Member>();
		for (List<Member> memberList: findings.values()) {
			result.addAll(memberList);
		}

		knownClasses.put(clazz, result);
		return result;
	}

	private static void searchWithoutAnnotation(Class<? extends WebServiceObject> clazz) throws IdFinderException {

		logger.warn("No %s annotation found for class %! Searching for a matching field or method...", Id.class.getSimpleName(), clazz.getSimpleName());

		Field idField = null;
		try {
			idField = clazz.getDeclaredField("id");
			if (checkField(idField,0)) {
				return;
			}
		} catch (NoSuchFieldException | SecurityException e) {
			//OK, field isn't there...
		}

		Method idGetter = null;
		try {
			idGetter = clazz.getDeclaredMethod("getId");
			checkMethod(idGetter,0);
		} catch (NoSuchMethodException | SecurityException e) {
			//OK, method isn't there...
		}
		return;
	}

	private static void searchFields(Class<? extends WebServiceObject> clazz) throws IdFinderException {
		for (Field field: clazz.getDeclaredFields()) {
			if (field.isAnnotationPresent(Id.class)) {

				logger.debug("Annotation @%s found at field \"%s\" of class \"%s\".",Id.class.getSimpleName(),field.getName(),clazz.getSimpleName());

				Annotation annotation = field.getAnnotation(Id.class);
				Id idInfo = (Id) annotation;
				int order = idInfo.order();
				checkField(field,order);

			}
		}
	}

	private static boolean checkField(Field field, int order) throws IdFinderException {

		if (!Modifier.isPublic(field.getModifiers())) {
			String getterName = "get"+ucFirst(field.getName());
			Method getter = null;
			boolean getterFound=true;
			try {
				getter = field.getDeclaringClass().getMethod(getterName);
				if (!Modifier.isPublic(getter.getModifiers())) {
					getterFound=false;
				}
			} catch (NoSuchMethodException | SecurityException e) {
				getterFound=false;
			}

			if (!getterFound) {
				throw new IdFinderException(field.getDeclaringClass(), "Field "+field.getName()+" is annotated with @"+Id.class.getSimpleName()+", but it is not accessible and there is no matching getter called "+getterName+" (or this getter is not accessible as well).");
			}

			addFinding(getter, order);

		}
		else {
			addFinding(field, order);
		}

		return true;
	}

	private static void searchMethods(Class<? extends WebServiceObject> clazz) throws IdFinderException {

		for (Method method: clazz.getDeclaredMethods()) {
			if (method.isAnnotationPresent(Id.class)) {
				logger.debug("Annotation @%s found at method \"%s\" of class \"%s\".",Id.class.getSimpleName(),method.getName(),clazz.getSimpleName());

				Annotation annotation = method.getAnnotation(Id.class);
				Id idInfo = (Id) annotation;
				int order = idInfo.order();

				checkMethod(method,order);

			}
		}

	}

	private static void checkMethod(Method method, int order) throws IdFinderException {
		if (!Modifier.isPublic(method.getModifiers())) {
			throw new IdFinderException(method.getDeclaringClass(), "Method "+method.getName()+" is annotated with @"+Id.class.getSimpleName()+", but it is not accessible!");
		}

		addFinding(method, order);
	}

	private final static String ucFirst(String input) {
		return input.substring(0,1).toUpperCase() + input.substring(1);
	}

	private static void addFinding(Member member, int order) throws IdFinderException {

		if (order<0) {
			throw new IdFinderException(member.getDeclaringClass(),"Order attribute of @"+Id.class.getName()+" annotation must be 0 (default) or greater!");
		}

		if (findings.get(order)==null) {
			findings.put(order, new LinkedList<Member>());
		}

		findings.get(order).add(member);

	}
}
