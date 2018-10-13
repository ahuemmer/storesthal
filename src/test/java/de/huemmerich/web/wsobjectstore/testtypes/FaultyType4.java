package de.huemmerich.web.wsobjectstore.testtypes;

import de.huemmerich.web.wsobjectstore.Id;
import de.huemmerich.web.wsobjectstore.WebServiceObject;


public class FaultyType4 extends WebServiceObject {

	
	@Id
	protected int getId() {
		return 1;
	}

	@Override
	public String getUrl() {
		return null;
	}
	
}
