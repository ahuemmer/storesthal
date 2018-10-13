package de.huemmerich.web.wsobjectstore.testtypes;

import de.huemmerich.web.wsobjectstore.Id;
import de.huemmerich.web.wsobjectstore.WebServiceObject;


public class FaultyType1 extends WebServiceObject {

	@Id(order=1)
	private int id1;
	
	@Id
	public String getId2() {
		return "";
	}
	
	@Id(order=3)
	public boolean id3;

	public int getId1() {
		return id1;
	}

	@Override
	public String getUrl() {
		return null;
	}
	
}
