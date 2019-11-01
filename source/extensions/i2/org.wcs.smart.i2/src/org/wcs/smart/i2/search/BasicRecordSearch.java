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
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.model.IntelProfile;
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
		String hql = " FROM IntelRecord WHERE profile in (:profiles) "; //$NON-NLS-1$
		if (source != null){
			hql += " AND recordSource = :source "; //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			hql += " AND lower(description) like lower(:narrative) "; //$NON-NLS-1$
		}
		if (titleSearch != null){
			hql += " AND lower(title) like lower(:title) "; //$NON-NLS-1$
		}
		
		Query<?> query = session.createQuery("SELECT count(*) " + hql); //$NON-NLS-1$
		query.setParameter("profiles", ProfilesManager.INSTANCE.getActiveProfiles());
		if (source != null){
			query.setParameter("source", source); //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			query.setParameter("narrative", "%" + narrativeSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (titleSearch != null){
			query.setParameter("title", "%" + titleSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		
		long cnt = (long) query.uniqueResult();
		
		query = session.createQuery("SELECT uuid, title, recordSource.uuid, status, description, profile.uuid " + hql); //$NON-NLS-1$
		query.setMaxResults(IRecordSearch.MAX_RESULT_CNT);
		query.setParameter("profiles", ProfilesManager.INSTANCE.getActiveProfiles());
		if (source != null){
			query.setParameter("source", source); //$NON-NLS-1$
		}
		if (narrativeSearch != null){
			query.setParameter("narrative", "%" + narrativeSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (titleSearch != null){
			query.setParameter("title", "%" + titleSearch + "%"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		List<?> items = query.list();
		List<IntelRecordSearchResultItem> resultItems = new ArrayList<>();
				
		for (Object it : items){
			Object[] i = (Object[])it;
			UUID uuid = (UUID)i[0];
			String title = (String) i[1];
			UUID src = (UUID)i[2];
			IntelRecord.Status status = (IntelRecord.Status)i[3];
			IntelProfile profile = session.get(IntelProfile.class, (UUID)i[5]);
			resultItems.add(new IntelRecordSearchResultItem(uuid, profile, src == null ? null : session.get(IntelRecordSource.class, src), title, status));
		}
		
		IntelRecordResult results = new IntelRecordResult(cnt, resultItems, System.nanoTime() - startTime);
		return results;
	}
}
