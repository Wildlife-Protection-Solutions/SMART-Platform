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

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;

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
	
	public static final byte[] CA_QUERY_KEY = new byte[]{1};
	
	public static final byte[] USER_QUERY_KEY = new byte[]{2};
	
	
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
	 * 
	 * @param session
	 * @return all patrol ids for the current conservation area
	 */
	public List<String> getPatrolIds(Session session);
		
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
	 * @return a map of the hashcode of the uuid array of a query folder mapped to query input of the query
	 */
	public HashMap<Integer, List<QueryInput>> getQueryProxies(Session session);
	
	/**
	 * Get the patrol mandate object.
	 * 
	 * @param session
	 * @param value patrol mandate uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getPatrolMandate(Session session, String value)throws Exception;
	
	/**
	 * Gets the station object
	 * @param session
	 * @param value station uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getStation(Session session, String value)throws Exception;
	
	/**
	 * Gets team object
	 * @param session
	 * @param value team uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getTeam(Session session, String value)throws Exception;
	
	/**
	 * Gets the transportation types listitem object 
	 * @param session
	 * @param value transportation type uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getTransportType(Session session, String value)throws Exception;
	
		
	/**
	 * Creates a list item for the given employee uuid
	 * @param session
	 * @param value employee uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public ListItem getEmployee(Session session, String value) throws Exception;
	
	
	
	
	
	/**
	 * Saves the given query to the database.
	 * 
	 * @param query query to save
	 * @param generateDropItems if query should generate drop items.
	 * @return <code>true</code> if saved correctly
	 */
	public boolean saveQuery(org.wcs.smart.query.model.Query query, 
			boolean generateDropItems);
	
	
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
			byte[] queryUuid, QueryType queryType);
	
	
	
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
			String queryName, QueryType queryType);
	
	/**
	 * 
	 * @return a list of supported query types 
	 */
	public QueryType[] getSupportedQueryTypes();

	
	/**
	 * 
	 * @return  list of all active teams as list items
	 */
	public List<ListItem> getActiveTeams(Session session);
	/**
	 * 
	 * @return  list of all active patrol mandates
	 */
	public List<ListItem> getActiveMandates(Session session);
	/**
	 * 
	 * @return  list of all active transport types
	 */
	public List<ListItem> getActiveTransportTypes(Session session);
}
