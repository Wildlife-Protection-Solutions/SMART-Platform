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
package org.wcs.smart.query.hibernate;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.util.SmartUtils;

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
		Query a = session.createQuery("select max(id) from ObservationQuery where conservationArea = :ca"); //$NON-NLS-1$
		a.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> dataa = a.list();
		
		Query b = session.createQuery("select max(id) from SummaryQuery where conservationArea = :ca "); //$NON-NLS-1$
		b.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> datab = b.list();
		
		Query c = session.createQuery("select max(id) from PatrolQuery where conservationArea = :ca "); //$NON-NLS-1$
		c.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> datac = c.list();
		
		Query d = session.createQuery("select max(id) from GriddedQuery where conservationArea = :ca "); //$NON-NLS-1$
		d.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		List<?> datad = d.list();

		int valuea = 1;
		if( dataa != null && dataa.size() >= 1 && dataa.get(0) != null){
			valuea = Integer.parseInt( (String)dataa.get(0) );
		}
		int valueb = 1;
		if( datab != null && datab.size() >= 1 && datab.get(0) != null){
			valueb = Integer.parseInt( (String)datab.get(0) );
		}
		int valuec = 1;
		if( datac != null && datac.size() >= 1 && datac.get(0) != null){
			valuec = Integer.parseInt( (String)datac.get(0) );
		}
		int valued = 1;
		if( datad != null && datad.size() >= 1 && datad.get(0) != null){
			valued = Integer.parseInt( (String)datad.get(0) );
		}
		int x = Math.max(Math.max(Math.max(valuea, valueb), valuec), valued);
		return QUERY_ID_FORMATTER.format( ++x );
	}
	

	/**
	 * 
	 * @param session
	 * @return all patrol ids for the current conservation area
	 */
	public List<String> getPatrolIds(Session session){
		String hql = "Select id FROM Patrol WHERE conservationArea = :ca"; //$NON-NLS-1$
		Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<String> data = q.list();
		return data;
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
	public abstract HashMap<Integer, List<QueryInput>> getQueryProxies(Session session);
		
	
	/**
	 * Get the patrol mandate object.
	 * 
	 * @param session
	 * @param value patrol mandate uuid as hex encoded string
	 * @return the patrol mandate or null if not found
	 * @throws Exception
	 */
	@Override
	public ListItem getPatrolMandate(Session session, String value) throws Exception{
		return getListItem(session, "PatrolMandate", value); //$NON-NLS-1$
	}
	
	/**
	 * Gets the station object
	 * @param session
	 * @param value station uuid as hex encoded string
	 * @return the station or null if not found
	 * @throws Exception
	 */
	@Override
	public ListItem getStation(Session session, String value) throws Exception{
		return getListItem(session, "Station", value); //$NON-NLS-1$
	}
	
	/**
	 * Gets team object
	 * @param session
	 * @param value team uuid as hex encoded string
	 * @return the team or null if not found
	 * @throws Exception
	 */
	@Override
	public ListItem getTeam(Session session, String value) throws Exception{
		return getListItem(session, "Team", value); //$NON-NLS-1$
	}
	
	/**
	 * Gets the transportation types listitem object 
	 * @param session
	 * @param value transportation type uuid as hex encoded string
	 * @return the transport type or null if not found
	 * @throws Exception
	 */
	public ListItem getTransportType(Session session, String value) throws Exception{
		return getListItem(session, "PatrolTransportType", value); //$NON-NLS-1$
	}
	
	/**
	 * Loads the list item for the given clazz and given uuid
	 *  
	 * @param session
	 * @param clazz object type
	 * @param value uuid an encoded hex string
	 * @return resulting list item
	 * @throws Exception
	 */
	private ListItem getListItem(Session session, String clazz, String value) throws Exception{
		Query q = session.createQuery("SELECT uuid, name FROM " + clazz + " WHERE uuid =:uuid"); //$NON-NLS-1$ //$NON-NLS-2$
		q.setParameter("uuid", SmartUtils.decodeHex(value)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if (results.size() == 1){
			return new ListItem( (byte[])((Object[])results.get(0))[0], (String)((Object[])results.get(0))[1]);
		}else{
			QueryPlugIn.log(MessageFormat.format(Messages.QueryHibernateManager_LoadError, new Object[]{clazz, value}), null);
			return null;
		}
	}
	
	/**
	 * Creates a list item for the given employee uuid
	 * @param session
	 * @param value employee uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getEmployee(Session session, String value) throws Exception{
		Query q = session.createQuery("SELECT uuid, givenName, familyName, id FROM Employee WHERE uuid =:uuid"); //$NON-NLS-1$
		
		q.setParameter("uuid", SmartUtils.decodeHex(value)); //$NON-NLS-1$
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if (results.size() == 1){
			Object[] d = results.get(0);
			return new ListItem( (byte[])d[0], (String) d[1] + " " + (String)d[2] + " [" + (String)d[3] + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}else{
			QueryPlugIn.log(MessageFormat.format(Messages.QueryHibernateManager_LoadEmployeeError, new Object[]{value}), null);
			return null;
		}
	}
	
	
	/**
	 * Saves the given query to the database.
	 * 
	 * @param query query to save
	 * @param generateDropItems if query should generate drop items.
	 * @return <code>true</code> if saved correctly
	 */
	public boolean saveQuery(org.wcs.smart.query.model.Query query,  boolean generateDropItems){

		boolean newQuery = query.getId() == null;
		
		//fire before save listeners in a separate transaction
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			//fire before save events; this may load the query
			if (!QueryEventManager.getInstance().fireBeforeSaveListeners(query, s)){
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
			if (generateDropItems){
				query.generateDropItems(s);
			}
			s.saveOrUpdate(query);
			query.updateName(SmartDB.getCurrentLanguage(), query.getName());
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
			QueryEventManager.getInstance().fireFolderChangedListeners(
					IQueryFolderListener.QUERY_ADDED, query);
		} else {
			QueryEventManager.getInstance().fireFolderChangedListeners(
					IQueryFolderListener.QUERY_SAVED, query);
		}
		return true;
	}
	
	/**
	 * Searches all the queries for the given
	 * query uuid.
	 * 
	 * @param session
	 * @param queryUuid
	 * @param queryType the type of query or null if query type not known
	 * @return
	 */
	public org.wcs.smart.query.model.Query findQuery(Session session, 
			byte[] queryUuid, QueryType queryType) {		
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
			String queryName, QueryType queryType) {
		
		
		List<org.wcs.smart.query.model.Query> queries = new ArrayList<org.wcs.smart.query.model.Query>();
		String hsql = "FROM " + queryType.getHibernateClass().getSimpleName() + " WHERE conservationArea = :ca and name=:name and (isShared = 'true' or (isShared = 'false' and owner = :employee))"; //$NON-NLS-1$ //$NON-NLS-2$
		Query query = session.createQuery(hsql);
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		query.setParameter("employee", SmartDB.getCurrentEmployee()); //$NON-NLS-1$
		query.setParameter("name", queryName); //$NON-NLS-1$
		queries.addAll(query.list());
				
		return queries;
	}
}
