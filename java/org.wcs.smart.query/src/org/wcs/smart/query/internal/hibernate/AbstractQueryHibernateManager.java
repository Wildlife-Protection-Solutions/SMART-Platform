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
package org.wcs.smart.query.internal.hibernate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Hibernate utility functions to support 
 * Query module.
 *  
 * @author Emily
 *
 */
public abstract class AbstractQueryHibernateManager implements IQueryHibernateManager{
	
	/**
	 * @return <code>true</code> if the current user can edit
	 * conservation area queries; <code>false</code> otherwise
	 */
	public abstract boolean canModifyCaQueries();
	
	

	/**
	 * Generates a query id from the database 
	 * @param session
	 * @return the newly generated query id
	 */
	public String generateQueryId(Session session){
		
		IQueryType[] types = QueryTypeManager.getInstance().getSupportedQueryTypes();
		
		int id = 0;
		for (IQueryType type : types){
			Query a = session.createQuery("select max(id) from " + type.getHibernateClass().getSimpleName() + " where conservationArea = :ca"); //$NON-NLS-1$ //$NON-NLS-2$
			a.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			List<?> dataa = a.list();
			if( dataa != null && dataa.size() >= 1 && dataa.get(0) != null){
				int temp = Integer.parseInt( (String)dataa.get(0) );
				if (temp > id){
					id = temp;
				}
			}
		}
	
		return QUERY_ID_FORMATTER.format( ++id );
	}
	
		
	/**
	 * The list of query folders associated with the current conservation area.
	 * 
	 * @param session
	 * @param includeCaFolder if the conservation area root folder should be included in the results
	 * @return
	 */
	public abstract List<QueryFolder> getQueryFolders(Session session, boolean includeCaFolder);
	
	/**
	 * A set of query proxies for all queries associated with the
	 * current conservation area  and the current user. 
	 * @param session
	 * @return a map of the hashcode of the uuid array of a query folder mapped to query input of the query
	 */
	public abstract HashMap<Integer, List<QueryEditorInput>> getQueryProxies(Session session);
		
	
//	/**
//	 * Loads the list item for the given clazz and given uuid
//	 *  
//	 * @param session
//	 * @param clazz object type
//	 * @param value uuid an encoded hex string
//	 * @return resulting list item
//	 * @throws Exception
//	 */
//	private ListItem getListItem(Session session, String clazz, String value) throws Exception{
//		Query q = session.createQuery("SELECT uuid, name FROM " + clazz + " WHERE uuid =:uuid"); //$NON-NLS-1$ //$NON-NLS-2$
//		q.setParameter("uuid", SmartUtils.decodeHex(value)); //$NON-NLS-1$
//		@SuppressWarnings("unchecked")
//		List<Object[]> results = q.list();
//		if (results.size() == 1){
//			return new ListItem( (byte[])((Object[])results.get(0))[0], (String)((Object[])results.get(0))[1]);
//		}else{
//			QueryPlugIn.log(MessageFormat.format(Messages.QueryHibernateManager_LoadError, new Object[]{clazz, value}), null);
//			return null;
//		}
//	}
	
	
	
	/**
	 * Saves the given query to the database.
	 * 
	 * @param query query to save
	 * @param proxy - if proxy is not null then drop item will
	 * be generated
	 * @return <code>true</code> if saved correctly
	 */
	public boolean saveQuery(org.wcs.smart.query.model.Query query,
			QueryProxy proxy){

		boolean newQuery = query.getId() == null;
		
		//fire before save listeners in a separate transaction
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			//fire before save events; this may load the query
			if (!QueryEventManager.getInstance().fireBeforeSave(query, s)){
				return false;
			}
		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.QueryHibernateManager_CouldNotSaveQueryError + ex.getLocalizedMessage(), ex);
			return false;
		}finally{
			s.getTransaction().rollback();
		}
		s.close();
		
		s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			if (newQuery){
				query.setId(generateQueryId(s));
				//page1.setQuery();
			}
			if (proxy != null){
				query.getType().getDropItemFactory().generateDropItems(proxy, s);
			}
			s.saveOrUpdate(query);
			query.updateName(SmartDB.getCurrentLanguage(), query.getName());
			if (SmartDB.getCurrentConservationArea().getDefaultLanguage() != null){
				if (query.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage()) == null){
					//if label for default language is null then update the default as well
					query.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), query.getName());
				}
			}
			s.getTransaction().commit();

		}catch (Exception ex){
			QueryPlugIn.displayLog(Messages.QueryHibernateManager_CouldNotSaveQueryError + ex.getLocalizedMessage(), ex);
			s.getTransaction().rollback();
			if (newQuery){
				query.setUuid(null);
				query.setId(null);
			}
			return false;
		}finally{
			s.close();
		}

		if (newQuery) {
			QueryEventManager.getInstance().fireQueryAdded(query);
		} 
		QueryEventManager.getInstance().fireQuerySaved(query);
		return true;
	}
	
	/**
	 * Searches all the queries for the given
	 * query uuid.
	 * 
	 * @param session
	 * @param queryUuid
	 * @param queryType the type of query
	 * @return the query or null if query not found
	 */
	public org.wcs.smart.query.model.Query findQuery(Session session, 
			byte[] queryUuid, IQueryType queryType) {		
		return (org.wcs.smart.query.model.Query) session.get(queryType.getHibernateClass(), queryUuid);

	}
	
	
	
	/**
	 * Searches the queries for the current conservation area
	 * that the current user has access to for a query
	 * with the name provided.
	 * 
	 * @param session
	 * @param queryUuid
	 * @param queryType the type of query or null if query type not known
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public List<org.wcs.smart.query.model.Query> findQuery(Session session, 
			String queryName, IQueryType queryType) {
		
		
		List<org.wcs.smart.query.model.Query> queries = new ArrayList<org.wcs.smart.query.model.Query>();
		String hsql = "SELECT q FROM " + queryType.getHibernateClass().getSimpleName() + " q, Label l WHERE l.id.element = q.uuid and q.conservationArea = :ca " +  //$NON-NLS-1$//$NON-NLS-2$
				"and l.value = :name and (q.isShared = 'true' or (q.isShared = 'false' and q.owner = :employee))"; //$NON-NLS-1$ 
		Query query = session.createQuery(hsql); 
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		query.setParameter("employee", SmartDB.getCurrentEmployee()); //$NON-NLS-1$
		query.setParameter("name", queryName); //$NON-NLS-1$
		List<org.wcs.smart.query.model.Query> list = query.list();
		for (org.wcs.smart.query.model.Query q : list){
			if (!queries.contains(q)){
				queries.add(q);
			}
		}
				
		return queries;
	}
}
