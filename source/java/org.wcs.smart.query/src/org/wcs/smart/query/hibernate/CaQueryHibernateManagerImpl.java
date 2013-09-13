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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.Team;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;
/**
 * Utility functions for supporting query module
 * when logged in as a single conservation area
 *  
 * @author Emily
 *
 */
public class CaQueryHibernateManagerImpl extends AbstractQueryHibernateManager {

	/**
	 * @return <code>true</code> if the current user can edit
	 * conservation area queries; <code>false</code> otherwise
	 */
	@Override
	public boolean canModifyCaQueries(){
		return SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN 
				|| SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER;
	}

	/**
	 * The list of query folders associated with the current conservation area.
	 * 
	 * @param session
	 * @param includeCaFolder if the conservation area root folder should be included in the results
	 * @return
	 */
	@Override
	public List<QueryFolder> getQueryFolders(Session session, boolean includeCaFolder){
		List<QueryFolder> folders = new ArrayList<QueryFolder>();
		
		QueryFolder caRootFolder = new QueryFolder();
		if(includeCaFolder){
			
			caRootFolder.setName(CONSERVATION_AREA_QUERIES_NAME);
			caRootFolder.setUuid(CA_QUERY_KEY);
			caRootFolder.setConservationArea(SmartDB.getCurrentConservationArea());
			caRootFolder.setRootFolder(true);
			folders.add(caRootFolder);
		}
		
		QueryFolder userRootFolder = new QueryFolder();
		userRootFolder.setName(MY_QUERIES_NAME);
		userRootFolder.setUuid(USER_QUERY_KEY);
		userRootFolder.setConservationArea(SmartDB.getCurrentConservationArea());
		userRootFolder.setEmployee(SmartDB.getCurrentEmployee());
		userRootFolder.setRootFolder(true);
		
		
		folders.add(userRootFolder);
		
		
		if (includeCaFolder){
			@SuppressWarnings("unchecked")
			List<QueryFolder> rootFolders = session.createCriteria(QueryFolder.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
					.add(Restrictions.isNull("employee")) //$NON-NLS-1$
					.add(Restrictions.isNull("parentFolder")).list(); //$NON-NLS-1$
						caRootFolder.setChildren(rootFolders);
		}
			@SuppressWarnings("unchecked")
		List<QueryFolder> userFolders = session.createCriteria(QueryFolder.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("employee", SmartDB.getCurrentEmployee())) //$NON-NLS-1$
				.add(Restrictions.isNull("parentFolder")).list(); //$NON-NLS-1$
			
		userRootFolder.setChildren(userFolders);

		
		return folders;
	}
	
	/**
	 * A set of query proxies for all queries associated with the
	 * current conservation area  and the current user. 
	 * @param session
	 * @return a map of the hashcode of the uuid array of a query folder mapped to query input of the query
	 */
	@Override
	public HashMap<Integer, List<QueryInput>> getQueryProxies(Session session){
		
		HashMap<Integer, List<QueryInput>> queries = new HashMap<Integer, List<QueryInput>>();
	
		for (int i = 0; i < QueryType.values().length; i++) {

			Query hquery = session
					.createQuery("SELECT a.uuid, a.name, a.folder.uuid, a.isShared, a.id " //$NON-NLS-1$
							+ "FROM " //$NON-NLS-1$
							+ QueryType.values()[i].getObjectName()
							+ " a " //$NON-NLS-1$
							+ "WHERE a.conservationArea = :ca " //$NON-NLS-1$
							+ "and (a.isShared ='true' or a.owner= :user)"); //$NON-NLS-1$
			hquery.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
			hquery.setParameter("user", SmartDB.getCurrentEmployee()); //$NON-NLS-1$

			List<?> results = hquery.list();
			for (Iterator<?> iterator = results.iterator(); iterator.hasNext();) {
				Object[] object = (Object[]) iterator.next();
				QueryInput proxy = new QueryInput((byte[]) object[0],
						(String) object[1], (String) object[4],
						(Boolean) object[3], QueryType.values()[i]);
				byte[] key = null;
				if (object[2] == null) {
					if (((Boolean) object[3]).booleanValue()) {
						// root conservation area queries
						key = CA_QUERY_KEY;
					} else {
						key = USER_QUERY_KEY;
					}
				} else {
					key = (byte[]) object[2];
				}
				if (key != null) {
					List<QueryInput> proxies = queries.get(Arrays
							.hashCode(key));
					if (proxies == null) {
						proxies = new ArrayList<QueryInput>();
						queries.put(Arrays.hashCode(key), proxies);
					}
					proxies.add(proxy);
				}
			}
		}
		return queries;
	}

	/**
	 * All query types are supported for single
	 * ca analysis
	 * @see org.wcs.smart.query.hibernate.IQueryHibernateManager#getSupportedQueryTypes()
	 */
	@Override
	public QueryType[] getSupportedQueryTypes() {
		return new QueryType[]{
				QueryType.OBSERVATION,
				QueryType.WAYPOINT,
				QueryType.PATROL,
				QueryType.SUMMARY,
				QueryType.GRIDDED
		};
	}

	@Override
	public List<ListItem> getActiveTeams(Session session) {
		List<Team> teams = PatrolHibernateManager.getActiveTeams(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (Team t : teams){
			items.add(new ListItem(t.getUuid(), t.getName(), t.getKeyId()));
		}
		return items;
	}

	@Override
	public List<ListItem> getActiveMandates(Session session) {
		List<PatrolMandate> teams = PatrolHibernateManager.getActiveMandates(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (PatrolMandate t : teams){
			items.add(new ListItem(t.getUuid(), t.getName(), t.getKeyId()));
		}
		return items;
	}
	@Override
	public List<ListItem> getActiveTransportTypes(Session session) {
		List<PatrolTransportType> teams = PatrolHibernateManager.getActivePatrolTransporationTypes(SmartDB.getCurrentConservationArea(), session);
		ArrayList<ListItem> items = new ArrayList<ListItem>();
		for (PatrolTransportType t : teams){
			items.add(new ListItem(t.getUuid(), t.getName(), t.getKeyId()));
		}
		return items;
	}
}
