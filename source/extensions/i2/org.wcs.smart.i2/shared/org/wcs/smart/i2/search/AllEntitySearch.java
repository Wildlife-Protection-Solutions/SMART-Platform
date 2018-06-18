package org.wcs.smart.i2.search;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import javax.naming.OperationNotSupportedException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelEntitySearch;

public class AllEntitySearch implements IIntelEntitySearch {

	private String filterString;
	private Set<String> queryColumns;
	
	public AllEntitySearch(String filterString) {
		this.filterString = filterString;
		this.queryColumns = null;
	}
	
	public AllEntitySearch(String filterString, Set<String> queryColumns) {
		this.filterString = filterString;
		this.queryColumns = queryColumns;
	}
	
	
	@Override
	public IntelSearchResult doSearch(Session session, Locale locale, IProgressMonitor monitor) throws Exception {
		throw new OperationNotSupportedException("");
	}

	public void setFilterString(String filterString){
		this.filterString = filterString;
	}
	
	public void setQueryColumns(Set<String> queryColumns) {
		this.queryColumns = queryColumns;
	}
	
	/**
	 * Set of attribute keyids for the visible columns or null if
	 * all columns are visible.  Empty string if no columns are visible.
	 * 
	 * @return
	 */
	public Set<String> getQueryColumns(){
		return this.queryColumns;
	}
	public String getFilterString(){
		return this.filterString;
	}
	
	@Override
	public String serialize() {
		StringBuilder sb = new StringBuilder();
		sb.append(IntelEntitySearch.Type.ALL.key);
		sb.append(IntelEntitySearch.SEPARATOR);
		sb.append(getFilterString());
		sb.append(IntelEntitySearch.SEPARATOR);
		if (queryColumns != null) {
			for (String x : queryColumns) {
				sb.append(x + ",");
			}
		}else {
			sb.append("<null>,");
		}
		
		return sb.toString();
	}

	public static AllEntitySearch parse(String searchString, Collection<ConservationArea> cas){
		String[] bits = searchString.split(IntelEntitySearch.SEPARATOR);
		if (!bits[0].equals(IntelEntitySearch.Type.ALL.key)) return null;
		
		if (bits.length != 3) return null;
		
		String filter = bits[1];
		String columns = bits[2];
		Set<String> cols = null;
		if (!columns.equals("<null>,")){
			cols = new HashSet<>();
			String[] attcols = columns.split(",");
			for(String x : attcols) {
				if (!x.trim().isEmpty()) cols.add(x);
			}
		}
		
		AllEntitySearch search = new AllEntitySearch(filter, cols);
		return search;
	}
	
}
