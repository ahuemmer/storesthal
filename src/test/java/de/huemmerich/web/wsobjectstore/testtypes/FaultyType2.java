package de.huemmerich.web.wsobjectstore.testtypes;

import de.huemmerich.web.wsobjectstore.Id;
import de.huemmerich.web.wsobjectstore.WebServiceObject;


public class FaultyType2 extends WebServiceObject {

	@Id
	private int iHaveNoMatchingGetter;
	
	public int getSomething() {
		return iHaveNoMatchingGetter;
	}

	@Override
	public String getUrl() {
		return null;
	}
	
}
