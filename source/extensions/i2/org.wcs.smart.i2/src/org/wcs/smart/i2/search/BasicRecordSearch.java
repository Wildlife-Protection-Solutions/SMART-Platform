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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.Session;
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
	
	@SuppressWarnings("unchecked")
	@Override
	public IntelRecordResult doSearch(Session session, IProgressMonitor monitor) {
		Long startTime = System.nanoTime();
		String hql = " FROM IntelRecord ";
		
		boolean where = false;
		if (source != null){
			if (!where){
				where = true;
				hql += " WHERE ";
			}
			hql += " recordSource = :source ";
		}
		if (narrativeSearch != null){
			if (where){
				hql += " AND ";
			}else{
				where = true;
				hql += " WHERE ";
			}
			hql += " lower(description) like :narrative ";
		}
		if (titleSearch != null){
			if (where){
				hql += " AND ";
			}else{
				where = true;
				hql += " WHERE ";
			}
			hql += " lower(title) like :title ";
		}
		
		Query query = session.createQuery("SELECT count(*) " + hql);
		if (source != null){
			query.setParameter("source", source);
		}
		if (narrativeSearch != null){
			query.setParameter("narrative", "%" + narrativeSearch.toLowerCase() + "%");
		}
		if (titleSearch != null){
			query.setParameter("title", "%" + titleSearch.toLowerCase() + "%");
		}
		
		long cnt = (long) query.uniqueResult();
		
		query = session.createQuery("SELECT uuid, title, recordSource.uuid, status, description " + hql);
		query.setMaxResults(IRecordSearch.MAX_RESULT_CNT);
		if (source != null){
			query.setParameter("source", source);
		}
		if (narrativeSearch != null){
			query.setParameter("narrative", "%" + narrativeSearch.toLowerCase() + "%");
		}
		if (titleSearch != null){
			query.setParameter("title", "%" + titleSearch.toLowerCase() + "%");
		}
		List<Object[]> items = query.list();
		List<IntelRecordSearchResultItem> resultItems = new ArrayList<>();
		
		Pattern narrativePattern = null;
		if (narrativeSearch != null){
			narrativePattern = Pattern.compile(narrativeSearch.toLowerCase());
		}
		
		for (Object[] i : items){
			UUID uuid = (UUID)i[0];
			String title = (String) i[1];
			UUID src = (UUID)i[2];
			String narrative = (String)i[4];
			
			StringBuilder localMatch = new StringBuilder();
			
			List<int[]> matchRanges = new ArrayList<>();
			if (narrativePattern != null){
				Matcher m = narrativePattern.matcher(narrative.toLowerCase());
				while(m.find()){
					int sIndex = m.start();
					int eIndex = m.end();
					int offset = 150;
					
					int start = sIndex - offset; 
					if (start < 0) start = 0;
					int rangeStart = sIndex - start;
					
					int end = eIndex + offset;
					if (end > narrative.length()) end = narrative.length();
					
					
					rangeStart = rangeStart + 3 + localMatch.length();
					int rangeEnd = rangeStart + (eIndex - sIndex);
					
					matchRanges.add(new int[]{rangeStart, rangeEnd});
					localMatch.append("..." + narrative.substring(start, end) + "...");
					localMatch.append("\n\n");
				}
			}
			
			resultItems.add(new IntelRecordSearchResultItem(uuid, src, title, (IntelRecord.Status)i[3], localMatch.length() == 0 ? null : localMatch.toString(), matchRanges.toArray(new int[matchRanges.size()][2])));
		}
		
		IntelRecordResult results = new IntelRecordResult(cnt, resultItems, System.nanoTime() - startTime);
		return results;
	}
}
