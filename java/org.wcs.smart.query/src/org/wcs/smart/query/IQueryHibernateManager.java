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
package org.wcs.smart.query;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.util.UuidUtils;

/**
 * Hibernate utility functions to support 
 * Query module.
 *  
 * @author Emily
 *
 */
public interface IQueryHibernateManager {
	/**
	 * 
	 */
	public static final String MY_QUERIES_NAME = Messages.QueryHibernateManager_MyQueryFolderName;
	/**
	 * 
	 */
	public static final String CONSERVATION_AREA_QUERIES_NAME = Messages.QueryHibernateManager_CaQueryFolderName;
	
	/**
	 * Conservation Area (shared queries) "Root Folder" UUID
	 */
	public static final UUID CA_QUERY_KEY = UuidUtils.stringToUuid("00000000000000000000000000000001"); //$NON-NLS-1$
	
	/**
	 * My queries "Root Folder" UUID
	 */
	public static final UUID USER_QUERY_KEY = UuidUtils.stringToUuid("00000000000000000000000000000002"); //$NON-NLS-1$
	
	
	public static final NumberFormat QUERY_ID_FORMATTER = new DecimalFormat("000000"); //$NON-NLS-1$
	
	
	/**
	 * @return <code>true</code> if the current user can edit
	 * conservation area queries; <code>false</code> otherwise
	 */
	public boolean canModifyCaQueries();
	
	/**
	 * Generates a query id from the database 
	 * @param session
	 * @return the newly generated query id
	 */
	public String generateQueryId(Session session);
	

	/**
	 * The list of query folders associated with the current conservation area.
	 * 
	 * @param session
	 * @param includeCaFolder if the conservation area root folder should be included in the results
	 * @return
	 */
	public List<QueryFolder> getQueryFolders(Session session, boolean includeCaFolder);
	
	/**
	 * A set of query proxies for all queries associated with the
	 * current conservation area  and the current user. 
	 * @param session
	 * @return a map of the uuid of a query folder 
	 * mapped to query input of the query
	 */
	public HashMap<UUID, List<QueryEditorInput>> getQueryProxies(Session session);

	/**
	 * Saves the given query to the database.
	 * 
	 * @param query query to save
	 * @param proxy if not null then drop items will be generated for the proxy
	 * @return <code>true</code> if saved correctly
	 */
	public boolean saveQuery(org.wcs.smart.query.model.Query query,
			QueryProxy proxy);
	
	
	/**
	 * Searches all the queries for the given
	 * query uuid.
	 * 
	 * @param session
	 * @param queryUuid
	 * @param queryType the type of query or null if query type not known
	 * @return the query or null if query not found
	 */
	public org.wcs.smart.query.model.Query findQuery(Session session, 
			UUID queryUuid, IQueryType queryType);
	
	
	
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
	public List<org.wcs.smart.query.model.Query> findQuery(Session session, 
			String queryName, IQueryType queryType);
	
}
