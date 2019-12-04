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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntitySearch;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelProfile;


/**
 * Basic search implementation 
 * 
 * @author Emily
 *
 */
public class BasicEntitySearch implements IIntelEntitySearch{

	private int maxResultCnt = MAX_RESULT_CNT;
	
	private static final String ENTITY_TYPE_SEP = ":"; //$NON-NLS-1$
	
	private String searchString = null;
	private List<String> entityTypes = null;
	
	private Collection<ConservationArea> cas;
	
	public static BasicEntitySearch parse(String queryString, ConservationArea ca){
		return parse(queryString, Collections.singleton(ca));
	}
	
	public static BasicEntitySearch parse(String queryString, Collection<ConservationArea> cas){
		String[] bits = queryString.split(IntelEntitySearch.SEPARATOR);
		if (queryString.endsWith(IntelEntitySearch.SEPARATOR)) {
			bits = Arrays.copyOf(bits, bits.length+1);
		}
		if (!bits[0].equals(IntelEntitySearch.Type.BASIC.key)) return null; //not a basic search
		
		
		//the old form included the search cnt in the second position
		//new form does not include this value
		String searchString = ""; //$NON-NLS-1$
		String[] types = null;
		if (bits.length == 4) {
			searchString = bits[2];
			types = bits[3] == null ? null : bits[3].split(ENTITY_TYPE_SEP);
		}else if (bits.length == 3) {
			searchString = bits[1];
			types = bits[2] == null ? null : bits[2].split(ENTITY_TYPE_SEP);
		}
		
		BasicEntitySearch search = new BasicEntitySearch(searchString, cas);
		if (types != null){
			search.entityTypes = new ArrayList<>();
			for (String t : types){
				search.entityTypes.add(t);
			}
		}
		return search;
	}
	
	public BasicEntitySearch(String searchString, ConservationArea ca, int maxResults){
		this(searchString, Collections.singletonList(ca), maxResults);
	}
	
	public BasicEntitySearch(String searchString, Collection<ConservationArea> cas, int maxResults){
		this(searchString, cas);
		this.maxResultCnt = maxResults;
	}
	
	public BasicEntitySearch(String searchString, Collection<ConservationArea> cas){
		this.searchString = searchString;
		this.cas = cas;
	}
	
	public BasicEntitySearch(String searchString, ConservationArea ca){
		this(searchString, Collections.singleton(ca));
	}
	
	public BasicEntitySearch(String searchString, List<IntelEntityType> entityTypeFilter, ConservationArea ca){
		this(searchString, entityTypeFilter, Collections.singleton(ca));
	}
	
	public BasicEntitySearch(String searchString, List<IntelEntityType> entityTypeFilter, Collection<ConservationArea> cas){
		this(searchString, cas);
		this.entityTypes = new ArrayList<String>();
		entityTypeFilter.forEach(e -> entityTypes.add(e.getKeyId()));
	}
	
	public String getSearchString(){
		return this.searchString;
	}
	
	public List<String> getEntityTypes(){
		return this.entityTypes;
	}
	
	/**
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility 
	 * to call done() on the given monitor
	 */
	public IntelSearchResult doSearch(Set<IntelProfile> profiles, Session session, Locale l, IProgressMonitor monitor){
		SubMonitor.convert(monitor, 2);
		Long now = System.nanoTime();

		if (profiles.isEmpty()) {
			return new IntelSearchResult(Collections.emptyList(),0);
		}
		if (searchString != null && searchString.length() > 0){
			//perform fuzzy search
			List<IntelSearchResultItem> sresults = SearchManager.INSTANCE.fuzzySearch(searchString,  entityTypes, cas, maxResultCnt, profiles, session);
			return new IntelSearchResult(sresults, System.nanoTime() - now);
		}

		
		if (searchString == null || searchString.isEmpty()){
			CriteriaBuilder cb = session.getCriteriaBuilder();
			
			CriteriaQuery<IntelEntity> c = cb.createQuery(IntelEntity.class);
			Root<IntelEntity> from = c.from(IntelEntity.class);
			c.select(from);
			ArrayList<Predicate> filters = new ArrayList<>();
			filters.add(from.get("profile").in(profiles)); //$NON-NLS-1$
			filters.add(from.get("conservationArea").in(this.cas)); //$NON-NLS-1$
			if (entityTypes != null && !entityTypes.isEmpty()){
				filters.add(from.join("entityType").get("keyId").in(entityTypes)); //$NON-NLS-1$ //$NON-NLS-2$
			}
			c.where(cb.and(filters.toArray(new Predicate[filters.size()])));
			Query<IntelEntity> q = session.createQuery(c);
			
			List<IntelSearchResultItem> results = new ArrayList<>();
			try(ScrollableResults r = q.scroll()){
				while(r.next()) {
					IntelEntity it = (IntelEntity) r.get()[0];
					if(results.size() < maxResultCnt) {
						IntelSearchResultItem result = new IntelSearchResultItem(it.getUuid(), 1.0);
						results.add(result);
					}
				}
			}
			return new IntelSearchResult(results, System.nanoTime() - now);
		}
		//should never get here
		return new IntelSearchResult(Collections.emptyList(), 0);
	}
		
	@Override
	public String serialize(){
		StringBuilder sb = new StringBuilder();
		sb.append(IntelEntitySearch.Type.BASIC.key);
		sb.append(IntelEntitySearch.SEPARATOR);
		if (searchString != null) sb.append(searchString);
		sb.append(IntelEntitySearch.SEPARATOR);
		if (entityTypes != null){
			entityTypes.forEach(a -> sb.append(a + ENTITY_TYPE_SEP)); 
		}
		return sb.toString();
	}
	
}
