package org.wcs.smart.query.model;

import java.util.ArrayList;
import java.util.List;

public class QueryTypeGroup {

	private String name;
	private String id;
	private String htmlDescription;
	private List<IQueryType> qtypes;
	
	public QueryTypeGroup(String id, String name, String htmlDescription) {
		super();
		this.name = name;
		this.id = id;
		this.htmlDescription = htmlDescription;
		qtypes = new ArrayList<IQueryType>();
	}

	public void addQueryType(IQueryType type){
		qtypes.add(type);
	}
	
	public List<IQueryType> getTypes(){
		return this.qtypes;
	}
	
	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public String getHtmlDescription() {
		return htmlDescription;
	}
	
}
