package com.toheda.server.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
@JsonAutoDetect(fieldVisibility = Visibility.ANY)
public class ProcessModel {
	
	private final String id;
	
	private final String name;
	
	public ProcessModel(final String id, final String name) {
		this.id = id;
		this.name = name;
	}
}