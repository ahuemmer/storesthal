package de.huemmerich.web.wsobjectstore.impl;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import de.huemmerich.web.wsobjectstore.IRESTServiceHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RESTServiceHandler implements IRESTServiceHandler {

	protected Client client = Client.create();

	protected static IRESTServiceHandler instance = null;

	private static final Logger logger = LogManager.getFormatterLogger(RESTServiceHandler.class.getSimpleName());

	@Override
	public ClientResponse executeOperation(String method, String url, String body, String sentType, String acceptedType) {
		WebResource webResource = client.resource(url);

		logger.debug("Calling method %s on URL %s with sentType %s. Accepted answert type is %s.",method, url, sentType, acceptedType);
		logger.debug("Request body:");
		logger.debug(body);

		if (acceptedType==null) {
			acceptedType = DEFAULT_ACCEPTED_TYPE;
		}
		if (sentType==null) {
			sentType = DEFAULT_SENT_TYPE;
		}

		webResource.accept(acceptedType).type(sentType);

		if (body==null) {
			return webResource.method(method, ClientResponse.class);
		}
		return webResource.method(method, ClientResponse.class, body);
	}


	@Override
	public ClientResponse executeOperation(Method method, String url, String body, String sentType, String acceptedType) {

		logger.debug("Calling method %s on URL %s with sentType %s. Accepted answert type is %s.",method, url, sentType, acceptedType);
		logger.debug("Request body:");
		logger.debug(body);

		WebResource webResource = client.resource(url);

		if (acceptedType==null) {
			acceptedType = DEFAULT_ACCEPTED_TYPE;
		}
		if (sentType==null) {
			sentType = DEFAULT_SENT_TYPE;
		}

		webResource.accept(acceptedType).type(sentType);

		ClientResponse response=null;

		switch (method) {
			case GET:
				response = webResource.get(ClientResponse.class);
				break;
			case DELETE:
				if (body==null) {
					response = webResource.accept(acceptedType).type(sentType).delete(ClientResponse.class);
				}
				else {
					response = webResource.accept(acceptedType).type(sentType).delete(ClientResponse.class, body);
				}
				break;
			case POST:
				if (body==null) {
					response = webResource.accept(acceptedType).type(sentType).post(ClientResponse.class);
				}
				else {
					response =  webResource.accept(acceptedType).type(sentType).post(ClientResponse.class, body);
				}
				break;
			case PUT:
				if (body==null) {
					response = webResource.accept(acceptedType).type(sentType).put(ClientResponse.class);
				}
				else {
					response = webResource.accept(acceptedType).type(sentType).put(ClientResponse.class, body);
				}
				break;
			default:
				throw new IllegalArgumentException("Operation "+method.toString()+" is not a standard operation. Please use the custom method execution method.");

		}

		logger.debug("Response status: %s",response.getStatusInfo().toString());

		return response;

	}

	@Override
	public ClientResponse executeOperation(Method method, String url) {
		return executeOperation(method,url,null,null,null);
	}

	@Override
	public ClientResponse executeOperation(Method method, String url, String body) {
		return executeOperation(method,url,body,null,null);
	}

	public static IRESTServiceHandler getInstance() {
		if (instance==null) {
			instance = new RESTServiceHandler();
		}
		return instance;
	}

}
