package de.huemmerich.web.wsobjectstore;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class WebServiceObject {

	@JsonIgnore
	public abstract String getUrl();

	@JsonIgnore
	private boolean mapped=false;

	public boolean isMapped() {
		return mapped;
	}

	public void setMapped(boolean mapped) {
		this.mapped = mapped;
	}

	@JsonIgnore
	public String getJsonRepresentation() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.writeValueAsString(this);
	}

}
