package de.huemmerich.web.wsobjectstore;

public class WebServiceObjectStoreException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -2145554456004665428L;

	private String message;

	public WebServiceObjectStoreException(String message) {
		super();
		this.message = message;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

}
