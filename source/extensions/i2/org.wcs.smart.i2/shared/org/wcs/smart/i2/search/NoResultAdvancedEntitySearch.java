/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.util.UuidUtils;

/**
 * An extension of the advanced entity search results that
 * returns no results and instead stores the results
 * in a database table.  Users are responsible for cleaning
 * up the table after it is used.
 * 
 * @author Emily
 *
 */
public class NoResultAdvancedEntitySearch extends AdvancedEntitySearch {
		
	
	public static NoResultAdvancedEntitySearch parse(String searchString, ConservationArea ca){
		return parse(searchString, Collections.singleton(ca));
	}
	
	public static NoResultAdvancedEntitySearch parse(String searchString, Collection<ConservationArea> cas){
		String[] bits = searchString.split(IntelEntitySearch.SEPARATOR);
		if (!bits[0].equals(IntelEntitySearch.Type.ADVANCED.key)) return null;
		
		//old form included max search result
		//new form does not include this
		String ss = ""; //$NON-NLS-1$
		if (bits.length == 3) {
			ss = bits[2];
		}else if (bits.length == 2) {
			ss = bits[1];
		}
		
		NoResultAdvancedEntitySearch search = new NoResultAdvancedEntitySearch(cas);
		search.searchString = ss;
		return search;
	}
	
	
	public NoResultAdvancedEntitySearch(Collection<ConservationArea> cas){
		super(cas);
	}
	
	public NoResultAdvancedEntitySearch(ConservationArea ca){
		this(Collections.singleton(ca));
	}
	
	/**
	 * The results table with a single
	 * entity_uuid field that contains all
	 * entities that match the search
	 * 
	 * @return
	 */
	public String getResultsTable() {
		return this.resultsTable;
	}
	@Override
	public IntelSearchResult doSearch(Set<IntelProfile> profiles, Session session, Locale locale, IProgressMonitor monitor) throws Exception {
		
		String eresultsTable = "query_temp_i2search_" + tableCnter.getAndIncrement(); //$NON-NLS-1$
		
		StringBuilder sb = new StringBuilder();
		sb.append("CREATE TABLE "); //$NON-NLS-1$
		sb.append(eresultsTable);
		sb.append("(entity_uuid char(16) for bit data)"); //$NON-NLS-1$
		
		session.createNativeQuery(sb.toString()).executeUpdate();
		
		if (profiles.isEmpty()) return null;
		
		if (searchString == null || searchString.trim().isEmpty()){

			sb = new StringBuilder();
			sb.append(" INSERT INTO "); //$NON-NLS-1$
			sb.append(eresultsTable);
			sb.append(" SELECT uuid FROM smart.i_entity WHERE profile_uuid in (:profiles) and ca_uuid in (:cas)"); //$NON-NLS-1$
			
			List<byte[]> pb = profiles.stream().map(e->UuidUtils.uuidToByte(e.getUuid())).collect(Collectors.toList());
			List<byte[]> cab = cas.stream().map(e->UuidUtils.uuidToByte(e.getUuid())).collect(Collectors.toList());
			
			session.createNativeQuery(sb.toString())
				.setParameterList("profiles", pb) //$NON-NLS-1$
				.setParameterList("cas", cab) //$NON-NLS-1$
				.executeUpdate();
			
			super.resultsTable = eresultsTable;
			
		}else{
			String where = populateResultsTable(profiles, session, locale);
			
			sb = new StringBuilder();
			sb.append(" INSERT INTO "); //$NON-NLS-1$
			sb.append(eresultsTable);
			sb.append(" SELECT entity_uuid FROM "); //$NON-NLS-1$
			sb.append(super.resultsTable);
			sb.append(" WHERE "); //$NON-NLS-1$
			sb.append(where);
			
			session.createNativeQuery(sb.toString()).executeUpdate();

			dropResultsTable(session);
			
			super.resultsTable = eresultsTable;
		}
		return null;
	}
}
