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
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.user.UserLevelManager;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

/**
 * Utility functions for supporting query module
 * when logged in to multiple conservation areas
 *  
 * @author Emily
 *
 */
public class MultiCaQueryHibernateManagerImpl extends
		AbstractQueryHibernateManager {
	
	/**
	 * @return <code>true</code> if the current user can edit
	 * conservation area queries; <code>false</code> otherwise
	 */
	@Override
	public boolean canModifyCaQueries(){
		for (Employee e : SmartDB.getConservationAreaConfiguration().getEmployees()){
			if (UserLevelManager.INSTANCE.supportsUser(e, UserLevelManager.ADMIN, UserLevelManager.MANAGER)) continue;
			
			//check if employee is in current ca configuration
			for (ConservationArea ca : SmartDB.getConservationAreaConfiguration().getConservationAreas()){
				if (ca.getUuid().equals(e.getConservationArea().getUuid())){
					if (!(UserLevelManager.INSTANCE.supportsUser(e, UserLevelManager.ADMIN, UserLevelManager.MANAGER))){
						return false;
					}	
				}
			}
		}
		return true;
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
		userRootFolder.setEmployee(SmartDB.getConservationAreaConfiguration().getCcaaUser());
		userRootFolder.setRootFolder(true);
		
		folders.add(userRootFolder);
		
		CriteriaBuilder cb = session.getCriteriaBuilder();
		if (includeCaFolder){
			CriteriaQuery<QueryFolder> c = cb.createQuery(QueryFolder.class);
			Root<QueryFolder> from = c.from(QueryFolder.class);
			c.where(cb.and(
					cb.equal(from.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
					cb.isNull(from.get("employee")), //$NON-NLS-1$
					cb.isNull(from.get("parentFolder")) //$NON-NLS-1$
					));
			
			List<QueryFolder> rootFolders = session.createQuery(c).getResultList();
			caRootFolder.setChildren(rootFolders);
		}
		
		CriteriaQuery<QueryFolder> c = cb.createQuery(QueryFolder.class);
		Root<QueryFolder> from = c.from(QueryFolder.class);
		c.where(cb.and(
				cb.equal(from.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
				cb.equal(from.get("employee"), SmartDB.getConservationAreaConfiguration().getCcaaUser()), //$NON-NLS-1$
				cb.isNull(from.get("parentFolder")) //$NON-NLS-1$
				));
		List<QueryFolder> userFolders = session.createQuery(c).getResultList();
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
	public HashMap<UUID, List<QueryEditorInput>> getQueryProxies(Session session){
		
		HashMap<UUID, List<QueryEditorInput>> queries = new HashMap<UUID, List<QueryEditorInput>>();
	
		for(IQueryType type: QueryTypeManager.INSTANCE.getSupportedQueryTypes()){

			if (!isMapped(type.getHibernateClass(), session)){
				//query is not mapped to hibernate class so skip it
				continue;
			}
			CriteriaBuilder cb = session.getCriteriaBuilder();
			CriteriaQuery<? extends Query> c = cb.createQuery(type.getHibernateClass());
			Root<? extends Query> from = c.from(type.getHibernateClass());
			c.where(cb.and(
					cb.equal(from.get("conservationArea"), SmartDB.getCurrentConservationArea()), //$NON-NLS-1$
					cb.or(
							cb.equal(from.get("isShared"), true), //$NON-NLS-1$
							cb.equal(from.get("owner"), SmartDB.getConservationAreaConfiguration().getCcaaUser()) //$NON-NLS-1$
							)));
			
			List<? extends Query> objects = session.createQuery(c).getResultList();
			for (org.wcs.smart.query.model.Query q : objects){
				if(!q.getTypeKey().equalsIgnoreCase(type.getKey())) continue;
				QueryEditorInput proxy = new QueryEditorInput(q.getUuid(),q.getName(),q.getId(),q.getIsShared(),
						QueryTypeManager.INSTANCE.findQueryType(q.getTypeKey()));
				UUID key = null;
				if (q.getFolder() == null) {
					if (q.getIsShared()) {
						// root conservation area queries
						key = CA_QUERY_KEY;
					} else {
						key = USER_QUERY_KEY;
					}
				} else {
					key = q.getFolder().getUuid();
				}
				if (key != null) {
					List<QueryEditorInput> proxies = queries.get(key);
					if (proxies == null) {
						proxies = new ArrayList<QueryEditorInput>();
						queries.put(key, proxies);
					}
					proxies.add(proxy);
				}
			}
		}
		return queries;
	}

}
