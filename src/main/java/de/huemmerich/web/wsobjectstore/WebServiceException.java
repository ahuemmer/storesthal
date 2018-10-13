package de.huemmerich.web.wsobjectstore;

import javax.ws.rs.core.Response.StatusType;

public class WebServiceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6346614463370286284L;
	
	private StatusType statusInfo;
	private String result;
	private String message;
	private String url;

	public WebServiceException(String message) {
		this.message = message;
	}
	
	public WebServiceException(StatusType statusInfo, String result, String url, String message) {
		super();
		this.statusInfo = statusInfo;
		this.result = result;
		this.message = message;
		this.url = url;
	}

	public StatusType getStatusInfo() {
		return statusInfo;
	}

	public String getResult() {
		return result;
	}

	public String getMessage() {
		return message;
	}

	public String getUrl() {
		return url;
	}
	
	
			
}
