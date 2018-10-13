package de.huemmerich.web.wsobjectstore.testtypes;

import de.huemmerich.web.wsobjectstore.Id;
import de.huemmerich.web.wsobjectstore.WebServiceObject;


public class FaultyType3 extends WebServiceObject {

	@Id
	private int iHaveNoMatchingGetter;
	
	protected int getIHaveNoMatchingGetter() {
		return iHaveNoMatchingGetter;
	}

	@Override
	public String getUrl() {
		return null;
	}
	
}
