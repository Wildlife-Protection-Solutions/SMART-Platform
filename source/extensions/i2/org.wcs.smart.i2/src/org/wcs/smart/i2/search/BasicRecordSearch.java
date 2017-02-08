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
//			narrativePattern = Pattern.compile(".*(" + narrativeSearch.toLowerCase() + ").*",Pattern.DOTALL);
			narrativePattern = Pattern.compile(narrativeSearch.toLowerCase());
		}
		Pattern titlePattern = null;
		if (titleSearch != null) titlePattern = Pattern.compile(titleSearch);
		
		for (Object[] i : items){
			UUID uuid = (UUID)i[0];
			String title = (String) i[1];
			UUID src = (UUID)i[2];
			String narrative = (String)i[4];
			
			StringBuilder localMatch = new StringBuilder();;
			
			if (narrativePattern != null){
				Matcher m = narrativePattern.matcher(narrative.toLowerCase());
				while(m.find()){
					int index = m.start();
					int start = index - 150;
					if (start < 0) start = 0;
					index = m.end();
					int end = index + 150;
					if (end > narrative.length()) end = narrative.length();
					
					localMatch.append("..." + narrative.substring(start, end) + "...");
					localMatch.append("\n\n");
				}
			}
			
			resultItems.add(new IntelRecordSearchResultItem(uuid, src, title, (IntelRecord.Status)i[3], localMatch.toString()));
		}
		
		IntelRecordResult results = new IntelRecordResult(cnt, resultItems, System.nanoTime() - startTime);
		return results;
	}
}
