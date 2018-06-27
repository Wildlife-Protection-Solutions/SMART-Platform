/*
 * Copyright (C) 2016 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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

/**
 * All entity search
 * 
 * @author Emily
 *
 */
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
		throw new OperationNotSupportedException(""); //$NON-NLS-1$
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
				sb.append(x + ","); //$NON-NLS-1$
			}
		}else {
			sb.append("<null>,"); //$NON-NLS-1$
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
		if (!columns.equals("<null>,")){ //$NON-NLS-1$
			cols = new HashSet<>();
			String[] attcols = columns.split(","); //$NON-NLS-1$
			for(String x : attcols) {
				if (!x.trim().isEmpty()) cols.add(x);
			}
		}
		
		AllEntitySearch search = new AllEntitySearch(filter, cols);
		return search;
	}
	
}
