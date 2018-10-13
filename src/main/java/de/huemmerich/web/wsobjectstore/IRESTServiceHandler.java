package de.huemmerich.web.wsobjectstore;

import com.sun.jersey.api.client.ClientResponse;

public interface IRESTServiceHandler {
	
	enum Method {
	    GET,
	    POST,
	    PUT,
	    DELETE
	}
	
	final String DEFAULT_SENT_TYPE="application/json";
	final String DEFAULT_ACCEPTED_TYPE="application/json";
	
	public ClientResponse executeOperation(Method method, String url);
	
	public ClientResponse executeOperation(Method method, String url, String body);
	
	public ClientResponse executeOperation(Method method, String url, String body, String sentType, String acceptedType);
	
	public ClientResponse executeOperation(String method, String url, String body, String sentType, String acceptedType);
	
}
