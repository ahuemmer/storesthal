package de.huemmerich.web.wsobjectstore.testtypes;

import de.huemmerich.web.wsobjectstore.Id;
import de.huemmerich.web.wsobjectstore.WebServiceObject;

public class SimpleType4 extends WebServiceObject {

	
	private int id;
	
	@Id
	private String name;

	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
	public String getUrl() {
		return null;
	}
	@Override
	public String toString() {
		return "SimpleType [id=" + id + ", name=" + name + "]";
	}
	
	
	
}
