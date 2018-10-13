package de.huemmerich.web.wsobjectstore.testtypes;

import de.huemmerich.web.wsobjectstore.Id;
import de.huemmerich.web.wsobjectstore.WebServiceObject;

public class FaultyType5 extends WebServiceObject {

	
	@Id(order=1)
	public int getId() {
		return 1;
	}
	
	@Id(order=-99)
	public int getId2() {
		return 1;
	}

	@Override
	public String getUrl() {
		return null;
	}
	
}
