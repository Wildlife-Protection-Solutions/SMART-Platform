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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
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
	public HashMap<Integer, List<QueryEditorInput>> getQueryProxies(Session session){
		
		HashMap<Integer, List<QueryEditorInput>> queries = new HashMap<Integer, List<QueryEditorInput>>();
	
		for (IQueryType type : QueryTypeManager.getInstance().getSupportedQueryTypes()){

			@SuppressWarnings("unchecked")
			List<org.wcs.smart.query.model.Query> objects = session.createCriteria(type.getHibernateClass())
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.or(Restrictions.eq("isShared", true),Restrictions.eq("owner", SmartDB.getCurrentEmployee()))) //$NON-NLS-1$ //$NON-NLS-2$
				.list();
			
			for (org.wcs.smart.query.model.Query q : objects){
				QueryEditorInput proxy = new QueryEditorInput(q.getUuid(),q.getName(),q.getId(),q.getIsShared(),q.getType());
				byte[] key = null;
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
					List<QueryEditorInput> proxies = queries.get(Arrays.hashCode(key));
					if (proxies == null) {
						proxies = new ArrayList<QueryEditorInput>();
						queries.put(Arrays.hashCode(key), proxies);
					}
					proxies.add(proxy);
				}
			}
		}
		return queries;
	}

}
