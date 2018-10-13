package de.huemmerich.web.wsobjectstore.testtypes;

import de.huemmerich.web.wsobjectstore.WebServiceObject;

public class IdLessType extends WebServiceObject {

	private String name;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getUrl() {
		return null;
	}
	
}
