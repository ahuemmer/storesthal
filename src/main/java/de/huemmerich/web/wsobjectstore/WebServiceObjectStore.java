package de.huemmerich.web.wsobjectstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.UniformInterfaceException;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

public class WebServiceObjectStore<T extends WebServiceObject> extends Observable {

	protected Class<T> clazz;

	protected boolean hasSimpleContents;

	protected Map<Integer, T> contents;

	protected Vector<Member> ids;

	private final static Object[] emptyArray = new Object[0];

	private static Map<Class<? extends WebServiceObject>,WebServiceObjectStore<? extends WebServiceObject>> stores = new HashMap<Class<? extends WebServiceObject>,WebServiceObjectStore<? extends WebServiceObject>>();

	protected WebServiceObjectStore(){}

	protected WebServiceObjectHandler<T> handler;

	protected WebServiceObjectStore(Class<T> clazz) {
		this(clazz,new WebServiceObjectHandler<T>(clazz));
	}

	protected WebServiceObjectStore(Class<T> clazz, WebServiceObjectHandler<T> handler) {
		this.clazz = clazz;
		if (handler!=null) {
            this.handler = handler;
        }
        else {
            this.handler = new WebServiceObjectHandler<T>(clazz);
        }

		try {
			ids = IdFinder.findIds(clazz);
		} catch (IdFinderException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		hasSimpleContents = (ids.size()==1);
		contents = new HashMap<Integer ,T>();
	}

	protected void contentChanged() {
	      setChanged();
	      notifyObservers();
	}

	public static <T extends WebServiceObject> WebServiceObjectStore<T> getInstance(Class<T> clazz) {
		return getInstance(clazz, null);
	}

    public static <T extends WebServiceObject> WebServiceObjectStore<T> getInstance(Class<T> clazz, WebServiceObjectHandler<T> handler) {

        if (!stores.containsKey(clazz)) {
			WebServiceObjectStore<T> result;
            if (handler==null) {
                result = new WebServiceObjectStore<T>(clazz);
            }
            else {
                result = new WebServiceObjectStore<T>(clazz, handler);
            }
            stores.put(clazz, result);
        }

        return (WebServiceObjectStore<T>)stores.get(clazz);
    }

	public void loadAll() throws WebServiceObjectStoreException, WebServiceException {
		loadAll(false);
	}

	public void loadAll(boolean renew) throws WebServiceObjectStoreException {

		if (renew) {
			this.contents.clear();
		}

		try {
            T[] values = handler.loadAll();
			for(T t: values) {
				t.setMapped(true);
				this.add(t, false);
			}
		}
		catch (UniformInterfaceException | ClientHandlerException | JsonProcessingException
				| WebServiceException e) {
			WebServiceObjectStoreException se = new WebServiceObjectStoreException(e.getMessage());
			se.initCause(e);
			throw se;
		}

		contentChanged();

	}

	public void add(T object) throws WebServiceObjectStoreException {
		try {
			add(object, true);
		} catch (UniformInterfaceException | ClientHandlerException | JsonProcessingException | WebServiceException e) {
			WebServiceObjectStoreException se = new WebServiceObjectStoreException(e.getMessage());
			se.initCause(e);
			throw se;
		}
	}

	public void add(T object, boolean writeThrough) throws UniformInterfaceException, ClientHandlerException, JsonProcessingException, WebServiceException {

		if (writeThrough) {
			object = handler.persist(object);
		}
		this.contents.put(new HashCodeBuilder().append(getIds(object)).toHashCode(), object);
		//TODO: Jetzt ID in Objekt setzen!
		contentChanged();
	}

	public void remove(T object) throws ClientHandlerException, UniformInterfaceException, JsonProcessingException, WebServiceException {
		remove(object,true);
	}

	public void remove(T object, boolean writeThrough) throws ClientHandlerException, UniformInterfaceException, JsonProcessingException, WebServiceException {
		this.contents.remove(new HashCodeBuilder().append(getIds(object)).toHashCode(), object);
		if (writeThrough) {
			handler.delete(object);
		}
		contentChanged();
	}

	public T get(Object key) throws WebServiceObjectStoreException {
		return this.contents.get(new HashCodeBuilder().append(key).toHashCode());
	}

	public Collection<T> getAll() throws WebServiceObjectStoreException {
		return this.contents.values();
	}

	public T get(Object[] keys)  throws WebServiceObjectStoreException {
		return this.contents.get(new HashCodeBuilder().append(keys).toHashCode());
	}

	public int countItems() {
		if (this.contents==null) {
			return 0;
		}
		return this.contents.size();
	}

	public Object[] getIds(T object) {
		Object keys[] = new Object[ids.size()];

		int i=0;
		try {
			for(Member member: ids) {
				if (member instanceof Field) {
					((Field)member).setAccessible(true);

					keys[i]=((Field)member).get(object);

				}
				else if (member instanceof Method) {
					((Method)member).setAccessible(true);
					keys[i]=((Method)member).invoke(object, emptyArray);
				}
				i++;
			}
		} catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return keys;
	}

	public T getObjectByString(String string) {
		for(T object: this.contents.values()) {
			if (object.toString().equals(string)) {
				return object;
			}
		}
		return null;
	}


}
