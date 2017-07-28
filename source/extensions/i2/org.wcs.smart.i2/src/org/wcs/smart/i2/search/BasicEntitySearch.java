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
import java.util.Collections;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;


/**
 * Basic search implementation 
 * 
 * @author Emily
 *
 */
public class BasicEntitySearch implements IIntelEntitySearch{

	private int maxResultCnt = MAX_RESULT_CNT;
	
	private String searchString = null;
	private List<String> entityTypes = null;
	
	public static BasicEntitySearch parse(String queryString){
		String[] bits = queryString.split(SEPARATOR);
		if (!bits[0].equals(Type.BASIC.key)) return null; //not a basic search
		
		int maxResultsCnt = Integer.parseInt(bits[1]);
		String searchString = ""; //$NON-NLS-1$
		if (bits.length >= 3){
			searchString = bits[2];
		}
		String[] types = null;
		if (bits.length >= 3){
			types = bits[3].split(":"); //$NON-NLS-1$
		}
		
		BasicEntitySearch search = new BasicEntitySearch(searchString, maxResultsCnt);
		if (types != null){
			search.entityTypes = new ArrayList<>();
			for (String t : types){
				search.entityTypes.add(t);
			}
		}
		return search;
	}
	

	
	public BasicEntitySearch(String searchString){
		this.searchString = searchString;
	}
	
	public BasicEntitySearch(String searchString, int maxResults){
		this(searchString);
		this.maxResultCnt = maxResults;
	}
	
	public BasicEntitySearch(String searchString, List<IntelEntityType> entityTypeFilter){
		this(searchString);
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
	public IntelSearchResult doSearch(Session session, IProgressMonitor monitor){
		SubMonitor progress = SubMonitor.convert(monitor, Messages.BasicEntitySearch_taskName, maxResultCnt);
		Long now = System.nanoTime();
		
		if (searchString != null && searchString.length() > 0){
			//perform fuzzy search
			
			List<IntelSearchResultItem> sresults = SearchManager.INSTANCE.fuzzySearch(searchString,  entityTypes, session);
			int actualCnt = Math.min(sresults.size(), maxResultCnt);
			for (int i = 0; i < actualCnt; i ++){
				IntelEntity it = (IntelEntity) session.get(IntelEntity.class, sresults.get(i).getEntityUuid());
				lazyLoadEntity(it, session);
				sresults.get(i).setEntity(it);
				progress.worked(1);
				progress.checkCanceled();
			}
			return new IntelSearchResult(sresults.size(), sresults.subList(0, actualCnt), System.nanoTime() - now);
		}

		if (searchString == null || searchString.isEmpty()){
			
			
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<IntelEntity> c = cb.createQuery(IntelEntity.class);
			CriteriaQuery<Long> c2 = cb.createQuery(Long.class);
			Root<IntelEntity> from = c.from(IntelEntity.class);
			c2.from(IntelEntity.class);
			List<Predicate> filters = new ArrayList<>();
			filters.add(cb.equal(from.get("conservationArea"), SmartDB.getCurrentConservationArea())); //$NON-NLS-1$
			
			if (entityTypes != null && !entityTypes.isEmpty()){
				Root<IntelEntityType> typefrom = c.from(IntelEntityType.class);
				filters.add(typefrom.get("keyId").in(entityTypes)); //$NON-NLS-1$
				
			}
			c.where(cb.and(filters.toArray(new Predicate[filters.size()])));
			c2.where(cb.and(filters.toArray(new Predicate[filters.size()])));
			
			c2.select(cb.count(from));
			Long maxCnt = session.createQuery(c2).uniqueResult();
			
			Query<IntelEntity> q = session.createQuery(c).setMaxResults(maxResultCnt);
		
			List<IntelEntity> items = q.getResultList();
			List<IntelSearchResultItem> results = new ArrayList<IntelSearchResultItem>();
			for (IntelEntity it : items){
				lazyLoadEntity(it, session);
				IntelSearchResultItem result = new IntelSearchResultItem(it.getUuid(),"", 1.0); //$NON-NLS-1$
				result.setEntity(it);
				results.add(result);
				progress.worked(1);
				progress.checkCanceled();
			}
			return new IntelSearchResult(maxCnt, results, System.nanoTime() - now);
		}
		//should never get here
		return new IntelSearchResult(0, Collections.emptyList(), 0);
	}
	

	
	@Override
	public String serialize(){
		StringBuilder sb = new StringBuilder();
		sb.append(Type.BASIC.key);
		sb.append(SEPARATOR);
		sb.append(maxResultCnt);
		sb.append(SEPARATOR);
		if (searchString != null) sb.append(searchString);
		sb.append(SEPARATOR);
		if (entityTypes != null){
			entityTypes.forEach(a -> sb.append(a + ":")); //$NON-NLS-1$
		}
		return sb.toString();
	}
	
	
	public BasicEntitySearch deserialize(String search){
		String[] bits = search.split(":"); //$NON-NLS-1$
		if (bits.length < 2) return null;
		if (!bits[0].equals("basic")) return null; //$NON-NLS-1$
		
		
		int maxCnt = Integer.parseInt(bits[1]);
		String searchString = bits[2];
		
		BasicEntitySearch basicSearch = new BasicEntitySearch(searchString, maxCnt);
		if (bits.length >= 3){
			basicSearch.entityTypes = new ArrayList<>();
		}
		for (int i = 3; i < bits.length; i ++){
			String entityKey = bits[i];
			basicSearch.entityTypes.add(entityKey);
		}
		
		return basicSearch;
	}
}
