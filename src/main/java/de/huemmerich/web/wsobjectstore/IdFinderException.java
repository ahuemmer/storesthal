package de.huemmerich.web.wsobjectstore;

public class IdFinderException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -573191666301572447L;

	public IdFinderException(Class<?> clazz, String message) {
		super("In class "+clazz.getName()+": "+message);
	}
	
}