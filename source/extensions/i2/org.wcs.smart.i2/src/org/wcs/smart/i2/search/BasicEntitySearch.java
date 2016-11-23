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

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;

public class BasicEntitySearch implements IIntelEntitySearch{

	
	private int maxResultCnt = MAX_RESULT_CNT;
	private String searchString = null;
	private List<String> entityTypes = null;
	
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
	
	@SuppressWarnings("unchecked")
	public IntelSearchResult doSearch(Session session, IProgressMonitor monitor){
		
		Long now = System.nanoTime();
		
		if (searchString != null && searchString.length() > 0){
			//perform fuzzy search
			monitor.beginTask("searching...", maxResultCnt);
			List<IntelSearchResultItem> sresults = SearchManager.INSTANCE.fuzzySearch(searchString,  entityTypes, session);
			int actualCnt = Math.min(sresults.size(), maxResultCnt);
			for (int i = 0; i < actualCnt; i ++){
				IntelEntity it = (IntelEntity) session.get(IntelEntity.class, sresults.get(i).getEntityUuid());
				lazyLoadEntity(it, session);
				sresults.get(i).setEntity(it);
				monitor.worked(1);
			}
			monitor.done();;
			return new IntelSearchResult(sresults.size(), sresults.subList(0, actualCnt), System.nanoTime() - now);
		}

		if (searchString == null || searchString.isEmpty()){
			//search all entities
			monitor.beginTask("searching...", maxResultCnt);
			Criteria c = session.createCriteria(IntelEntity.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()));
			Criteria c1 = session.createCriteria(IntelEntity.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()));
			if (entityTypes != null && !entityTypes.isEmpty()){
				c.createAlias("entityType", "et");
				c.add(Restrictions.in("et.keyId", entityTypes));
				c1.createAlias("entityType", "et");
				c1.add(Restrictions.in("et.keyId", entityTypes));
			}
			
			
			Long maxCnt = (Long) c.setProjection(Projections.rowCount()).list().get(0);
			
			c1.setMaxResults(maxResultCnt);
			
			List<IntelEntity> items = c1.list();
			List<IntelSearchResultItem> results = new ArrayList<IntelSearchResultItem>();
			for (IntelEntity it : items){
				lazyLoadEntity(it, session);
				IntelSearchResultItem result = new IntelSearchResultItem(it.getUuid(),"", 1.0);
				result.setEntity(it);
				results.add(result);
				monitor.worked(1);
			}
			monitor.done();
			return new IntelSearchResult(maxCnt, results, System.nanoTime() - now);
		}
		//should never get here
		monitor.done();
		return new IntelSearchResult(0, Collections.emptyList(), 0);
	}
	
	private void lazyLoadEntity(IntelEntity it, Session session){
		it.getIdAttributeAsText();
		it.getEntityType();
		it.getEntityType().getIcon();
		if (it.getPrimaryAttachment() != null){
			try {
				it.getPrimaryAttachment().getCopyFromLocation();
				it.getPrimaryAttachment().computeFileLocation(session);
			} catch (Exception e) {
				Intelligence2PlugIn.log("Unable to compute attachment location", e);
			}
		}
	}
	
	@Override
	public String serialize(){
		StringBuilder sb = new StringBuilder();
		sb.append("basic");
		sb.append(":");
		sb.append(maxResultCnt);
		sb.append(":");
		sb.append(searchString);
		sb.append(":");
		if (entityTypes != null){
			entityTypes.forEach(a -> sb.append(a + ":"));
		}
		return sb.toString();
	}
	
	
	public BasicEntitySearch deserialize(String search){
		String[] bits = search.split(":");
		if (bits.length < 2) return null;
		if (!bits[0].equals("basic")) return null;
		
		
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
