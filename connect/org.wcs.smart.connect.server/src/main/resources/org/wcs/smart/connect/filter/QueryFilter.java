/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.filter;

import java.util.UUID;

/*
 * QueryFilter entity to allow users to select a list of queries restricted by CA, name, ID etc.
 * 
 */

public class QueryFilter {
	private String nameFilter;
	private int idFilter;
	private UUID typeFilter;
	private UUID caUuidFilter;
	private String sortBy;
	private String sortAscending;
	
	public QueryFilter(String nameFilter, int idFilter, UUID typeFilter, UUID caUuidFilter, String sortBy, String sortAscending){
		this.setNameFilter(nameFilter);
		this.setIdFilter(idFilter);
		this.setTypeFilter(typeFilter);
		this.setCaUuidFilter(caUuidFilter);
		this.setSortBy(sortBy);
		this.setSortAscending(sortAscending);
	}

	public String getNameFilter() {
		return nameFilter;
	}
	public void setNameFilter(String nameFilter) {
		this.nameFilter = nameFilter;
	}

	public int getIdFilter() {
		return idFilter;
	}
	public void setIdFilter(int idFilter) {
		this.idFilter = idFilter;
	}

	public UUID getTypeFilter() {
		return typeFilter;
	}
	public void setTypeFilter(UUID typeFilter) {
		this.typeFilter = typeFilter;
	}

	
	public UUID getCaUuidFilter() {
		return caUuidFilter;
	}
	public void setCaUuidFilter(UUID caUuidFilter) {
		this.caUuidFilter = caUuidFilter;
	}

	public String getSortBy() {
		return sortBy;
	}
	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public String getSortAscending() {
		return sortAscending;
	}
	public void setSortAscending(String sortAscending) {
		this.sortAscending = sortAscending;
	}
	
	
}
