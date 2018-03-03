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
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;

/**
 * Basic record search
 * 
 * @author Emily
 *
 */
public class BasicRecordSearch implements IRecordSearch{

	private IntelRecordSource source;
	private String narrativeSearch;
	private String titleSearch;
	
	public BasicRecordSearch(IntelRecordSource source, String narrativeSearch, String titleSearch){
		this.source = source;
		this.narrativeSearch = narrativeSearch;
		this.titleSearch = titleSearch;
	}
	
	@Override
	public IntelRecordResult doSearch(Session session, IProgressMonitor monitor) {
		Long startTime = System.nanoTime();
		String hql = " FROM IntelRecord "; //$NON-NLS-1$
		
		boolean where = false;
		if (source != null){
			if (!where){
				where = true;
				hql += " WHERE "; //$NON-NLS-1$
			}
			hql += " recordSource = :source "; //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			if (where){
				hql += " AND "; //$NON-NLS-1$
			}else{
				where = true;
				hql += " WHERE "; //$NON-NLS-1$
			}
			hql += " lower(description) like :narrative "; //$NON-NLS-1$
		}
		if (titleSearch != null){
			if (where){
				hql += " AND "; //$NON-NLS-1$
			}else{
				where = true;
				hql += " WHERE "; //$NON-NLS-1$
			}
			hql += " lower(title) like :title "; //$NON-NLS-1$
		}
		
		Query<?> query = session.createQuery("SELECT count(*) " + hql); //$NON-NLS-1$
		if (source != null){
			query.setParameter("source", source); //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			query.setParameter("narrative", "%" + narrativeSearch.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (titleSearch != null){
			query.setParameter("title", "%" + titleSearch.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		long cnt = (long) query.uniqueResult();
		
		query = session.createQuery("SELECT uuid, title, recordSource.uuid, status, description " + hql); //$NON-NLS-1$
		query.setMaxResults(IRecordSearch.MAX_RESULT_CNT);
		if (source != null){
			query.setParameter("source", source); //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			query.setParameter("narrative", "%" + narrativeSearch.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (titleSearch != null){
			query.setParameter("title", "%" + titleSearch.toLowerCase() + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		List<?> items = query.list();
		List<IntelRecordSearchResultItem> resultItems = new ArrayList<>();
				
		for (Object it : items){
			Object[] i = (Object[])it;
			UUID uuid = (UUID)i[0];
			String title = (String) i[1];
			UUID src = (UUID)i[2];
			resultItems.add(new IntelRecordSearchResultItem(uuid, src == null ? null : session.get(IntelRecordSource.class, src), title, (IntelRecord.Status)i[3]));
		}
		
		IntelRecordResult results = new IntelRecordResult(cnt, resultItems, System.nanoTime() - startTime);
		return results;
	}
}
