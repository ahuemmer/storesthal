package de.huemmerich.web.wsobjectstore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import de.huemmerich.web.wsobjectstore.IRESTServiceHandler.Method;
import de.huemmerich.web.wsobjectstore.impl.RESTServiceHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.ws.rs.core.Response.Status;
import java.io.IOException;

public class WebServiceObjectHandler<T extends WebServiceObject> {

	protected T instance;
	protected Class<T> clazz;

	protected Client client = Client.create();

	protected IRESTServiceHandler serviceHandler = RESTServiceHandler.getInstance();
	private static final Logger logger = LogManager.getFormatterLogger(WebServiceObjectHandler.class.getSimpleName());

	private ObjectMapper mapper = new ObjectMapper();

	public WebServiceObjectHandler(Class<T> clazz) {
		this.clazz = clazz;
		try {
			this.instance = clazz.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public T[] loadAll() throws WebServiceException {
		return get(instance.getUrl());
	}

	public T[] get(int id) throws WebServiceException {
		return get(instance.getUrl()+id);
	}

	@SuppressWarnings("unchecked")
	public T[] get(String url) throws WebServiceException {
		ClientResponse response = serviceHandler.executeOperation(Method.GET, url);
		String result = response.getEntity(String.class);
		logger.debug("Response Body: %s",result);

		if (response.getStatus() != Status.OK.getStatusCode()) {
			throw new WebServiceException(response.getStatusInfo(), result, url, "Could not get "+clazz.getSimpleName()+"(s) from web service! Response: "+result);
		}

		ObjectMapper mapper = new ObjectMapper();

		T[] values=null;

		Class<?> arrayType=null;
		try {
			arrayType = Class.forName("[L" + clazz.getName() + ";");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			values = (T[]) mapper.readValue(result, arrayType);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return values;
	}

	public T persist(T object) throws UniformInterfaceException, ClientHandlerException, JsonProcessingException, WebServiceException {
		return persist(object, null);
	}

	public T persist(T object, String url) throws UniformInterfaceException, ClientHandlerException, JsonProcessingException, WebServiceException {

		if (object.isMapped()) {
		    if (url==null) {
                return put(object);
            }
			return put(object, url);
		}
		T result;
        if (url==null) {
            result = post(object);
        }
        else {
            result = post(object, url);
        }
		result.setMapped(true);
		return result;
	}

	public T post(T object) throws UniformInterfaceException, ClientHandlerException, JsonProcessingException, WebServiceException {
		return post(object, instance.getUrl());
	}

	public T post(T object, String url) throws UniformInterfaceException, ClientHandlerException, JsonProcessingException, WebServiceException {

		ClientResponse response = serviceHandler.executeOperation(Method.POST, url, object.getJsonRepresentation(), "application/json", "application/json");
		String responseString = response.getEntity(String.class);
		logger.debug("Response Body: %s",responseString);
		if (response.getStatus() != Status.CREATED.getStatusCode()) {
			throw new WebServiceException(response.getStatusInfo(), responseString, url, "Could not post "+clazz.getSimpleName()+" to web service! Response: "+responseString);
		}

		try {
			T value = (T) mapper.readValue(responseString, clazz);
			return value;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;

	}

	public T put(T object) throws WebServiceException, JsonProcessingException {
		String addonURL = "";
		Object[] ids = WebServiceObjectStore.getInstance(clazz).getIds(object);
		for(Object id: ids) {
			addonURL+=id.toString();
		}
		return put(object, instance.getUrl()+addonURL);
	}

	public T put(T object, String url) throws WebServiceException, JsonProcessingException {

		ClientResponse response = serviceHandler.executeOperation(Method.PUT, url, object.getJsonRepresentation(), "application/json", "application/json");

		if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {
			String responseString = response.getEntity(String.class);
			logger.debug("Response Body: %s",responseString);
			throw new WebServiceException(response.getStatusInfo(), responseString, url, "Could not put "+clazz.getSimpleName()+" to web service! Response: "+responseString);
		}

		return object;
	}

	public boolean delete(T object) throws ClientHandlerException, UniformInterfaceException, WebServiceException {
		Object[] ids = WebServiceObjectStore.getInstance(clazz).getIds(object);

		String addonURL = "";

		for(Object id: ids) {
			addonURL+=id.toString()+"/";
		}

		addonURL=addonURL.substring(0, addonURL.length()-1);
		return delete(object, instance.getUrl()+addonURL);
	}

	public boolean delete(T object, String url) throws ClientHandlerException, UniformInterfaceException, WebServiceException {

		if (!object.isMapped()) {
			return false;
		}

		ClientResponse response = serviceHandler.executeOperation(Method.DELETE, url, null, "application/json", "application/json");

		if (response.getStatus() != Status.NO_CONTENT.getStatusCode()) {

			String responseString = response.getEntity(String.class);
			logger.debug("Response Body: %s",responseString);
			throw new WebServiceException(response.getStatusInfo(), responseString, url, "Could not delete "+clazz.getSimpleName()+" from web service!");
		}

		return true;
	}

	public T get(Object[] ids) {

		String addonURL = "";

		for(Object id: ids) {
			addonURL+=id.toString()+"/";
		}

		ClientResponse response = serviceHandler.executeOperation(Method.GET, instance.getUrl()+addonURL);

		// a successful response returns 200

		if (response.getStatus() != 200) {
			new RuntimeException("HTTP Error: " + response.getStatus()).printStackTrace();
			return null;
		}

		String result = response.getEntity(String.class);
		logger.debug("Response Body: %s",result);


		//mapper.enableDefaultTyping();

		T value = null;

		try {
			value = (T) mapper.readValue(result, clazz);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return value;

	}

	public void setServiceHandler(IRESTServiceHandler serviceHandler) {
		this.serviceHandler = serviceHandler;
	}



}
