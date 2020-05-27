package org.wcs.smart.connect.query;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.ca.ConservationArea;

public class QueryFolderProxy {
	private final String name;
	private List<QueryFolderProxy> subFolders = new ArrayList<QueryFolderProxy>();
	private List<QueryProxy> queries = new ArrayList<QueryProxy>();

	public QueryFolderProxy(final String name) {
		this.name = name;
	}
	
	public void addSubFolder(QueryFolderProxy folder) {
		subFolders.add(folder);
	}
	
	public void addQuery(QueryProxy query) {
		queries.add(query);
	}

	public String getName() {
		return name;
	}

	public List<QueryFolderProxy> getSubFolders() {
		return subFolders;
	}

	public List<QueryProxy> getQueries() {
		return queries;
	}
	
}
