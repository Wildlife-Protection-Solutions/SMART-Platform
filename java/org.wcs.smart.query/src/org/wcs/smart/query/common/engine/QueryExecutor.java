package org.wcs.smart.query.common.engine;

import java.text.MessageFormat;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;

public enum QueryExecutor {
	INSTANCE;
	
	public IQueryResult executeQuery(Query query, Session session, IProgressMonitor monitor ) throws Exception{
		IQueryType type = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
		IQueryEngine engine = findQueryEnginge(type);
		if (engine == null){
			throw new Exception(MessageFormat.format(Messages.QueryExecutor_QueryEngineNotFound, type.getGuiName()));
		}
		Session lSession = session;
		if (lSession == null){
			lSession = HibernateManager.openSession();
			lSession.beginTransaction();
		}
		try{
			HashMap<String, Object> parameters = new HashMap<String, Object>();
			parameters.put(Session.class.getName(), lSession);
			parameters.put(IProgressMonitor.class.getName(), monitor);
		
			IQueryResult results = engine.executeQuery(query, parameters);
			query.setCachedResults(results);
			return results;
		}finally{
			if (session == null && lSession.isOpen()){
				lSession.getTransaction().commit();
				lSession.close();
			}
		}
	}
	
	private IQueryEngine findQueryEnginge(IQueryType qType) throws Exception{
		return QueryTypeManager.INSTANCE.getQueryEngine(qType);
	}
}
