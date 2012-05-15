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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.Query.QueryType;
import org.wcs.smart.query.model.ListItem;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.util.SmartUtils;

/**
 * Query related database functions.
 * @author Emily
 * @since 1.0.0
 */
public class QueryHibernateManager {

	public static final byte[] CA_QUERY_KEY = new byte[]{1};
	public static final byte[] USER_QUERY_KEY = new byte[]{2};
	
	
	private static NumberFormat QUERY_ID_FORMATTER = new DecimalFormat("000000");
	
	
	/**
	 * @return <code>true</code> if the current user can edit
	 * conservation area queries; <code>false</code> otherwise
	 */
	public static boolean canModifyCaQueries(){
		return SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.ADMIN 
				|| SmartDB.getCurrentEmployee().getSmartUserLevel() == SmartUserLevel.MANAGER;
	}
	
	/**
	 * Generates a query id from the database 
	 * @param session
	 * @return the newly generated query id
	 */
	public static String generateQueryId(Session session){
		Query a = session.createQuery("select max(id) from WaypointQuery where conservationArea = :ca");
		a.setParameter("ca", SmartDB.getCurrentConservationArea());
		List<?> dataa = a.list();
		
		Query b = session.createQuery("select max(id) from SummaryQuery where conservationArea = :ca ");
		b.setParameter("ca", SmartDB.getCurrentConservationArea());
		List<?> datab = b.list();
		
		int valuea = 1;
		if( dataa != null && dataa.size() >= 1){
			valuea = Integer.parseInt( (String)dataa.get(0) );
		}
		int valueb = 1;
		if( datab != null && datab.size() >= 1){
			valueb = Integer.parseInt( (String)datab.get(0) );
		}
		int x = Math.max(valuea, valueb);
		return QUERY_ID_FORMATTER.format( ++x );
	}
	

	/**
	 * 
	 * @param session
	 * @return all patrol ids for the current conservation area
	 */
	public static List<String> getPatrolIds(Session session){
		String hql = "Select id FROM Patrol WHERE conservationArea = :ca";
		Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
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
	public static List<QueryFolder> getQueryFolders(Session session, boolean includeCaFolder){
		List<QueryFolder> folders = new ArrayList<QueryFolder>();
		
		QueryFolder caRootFolder = new QueryFolder();
		if(includeCaFolder){
			
			caRootFolder.setName("Conservation Area Queries");
			caRootFolder.setUuid(CA_QUERY_KEY);
			caRootFolder.setConservationArea(SmartDB.getCurrentConservationArea());
			caRootFolder.setRootFolder(true);
			folders.add(caRootFolder);
		}
		
		QueryFolder userRootFolder = new QueryFolder();
		userRootFolder.setName("My Queries");
		userRootFolder.setUuid(USER_QUERY_KEY);
		userRootFolder.setConservationArea(SmartDB.getCurrentConservationArea());
		userRootFolder.setEmployee(SmartDB.getCurrentEmployee());
		userRootFolder.setRootFolder(true);
		
		
		folders.add(userRootFolder);
		
		try{
			if (includeCaFolder){
				@SuppressWarnings("unchecked")
				List<QueryFolder> rootFolders = session.createCriteria(QueryFolder.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
						.add(Restrictions.isNull("employee"))
						.add(Restrictions.isNull("parentFolder")).list();
		
				caRootFolder.setChildren(rootFolders);
			}

			@SuppressWarnings("unchecked")
			List<QueryFolder> userFolders = session.createCriteria(QueryFolder.class)
					.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
					.add(Restrictions.eq("employee", SmartDB.getCurrentEmployee()))
					.add(Restrictions.isNull("parentFolder")).list();
				
			userRootFolder.setChildren(userFolders);

		}catch (Exception ex){
			QueryPlugIn.log("Error loading query list." + ex.getMessage(), ex);
			session.close();
		}
		
		return folders;
	}
	
	/**
	 * A set of query proxies for all queries associated with the
	 * current conservation area  and the current user. 
	 * @param session
	 * @return a map of the hashcode of the uuid array of a query folder mapped to query input of the query
	 */
	public static HashMap<Integer, List<QueryInput>> getQueryProxies(Session session){
		
		HashMap<Integer, List<QueryInput>> queries = new HashMap<Integer, List<QueryInput>>();
		
		try{
			for (int i = 0; i < QueryType.values().length; i++) {
			
				Query hquery = session
						.createQuery("SELECT a.uuid, a.name, a.folder.uuid, a.isShared, a.id "
								+ "FROM "
								+ QueryType.values()[i].getObjectName()
								+ " a "
								+ "WHERE a.conservationArea = :ca "
								+ "and (a.isShared ='true' or a.owner= :user)");
				hquery.setParameter("ca", SmartDB.getCurrentConservationArea());
				hquery.setParameter("user", SmartDB.getCurrentEmployee());

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
			
			
		}catch (Exception ex){
			QueryPlugIn.displayLog("Could not load queries for query list." + ex.getMessage(),ex);
			session.close();
		}
		return queries;
	}
	
	/**
	 * Loads an attribute from the given attribute key and the
	 * current conservation area 
	 * 
	 * @param session
	 * @param attributeKey attribute key
	 * @return attribute loaded from the database or <code>null</code> if attribute not found
	 */
	public static Attribute getAttribute(Session session, String attributeKey){
		Query q = session.createQuery("From Attribute where conservationArea = :ca and keyid = :key");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("key", attributeKey);
		@SuppressWarnings("unchecked")
		List<Attribute> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
		
	}
	
	
	/**
	 * Loads an attribute list item for the given key and the
	 * current conservation area 
	 * 
	 * @param session
	 * @param attributeKey attribute key
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	public static AttributeListItem getAttributeListItem(Session session, String attributeKey){
		Query q = session.createQuery(" SELECT ali From AttributeListItem ali join ali.attribute as a where a.conservationArea = :ca and ali.keyId = :key");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("key", attributeKey);
		@SuppressWarnings("unchecked")
		List<AttributeListItem> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}	
	}
	
	/**
	 * Loads an attribute tree not item item for the given hkey and the
	 * current conservation area 
	 * 
	 * @param session
	 * @param attributeHKey attribute tree node hkey
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	public static AttributeTreeNode getAttributeTreeNode(Session session, String attributeHKey){
		Query q = session.createQuery(" SELECT ali From AttributeTreeNode ali join ali.attribute as a where a.conservationArea = :ca and ali.hkey = :key");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("key", attributeHKey);
		@SuppressWarnings("unchecked")
		List<AttributeTreeNode> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
		
	}
	
	
	/**
	 * Get the patrol mandate object.
	 * 
	 * @param session
	 * @param value patrol mandate uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public static ListItem getPatrolMandate(Session session, String value)throws Exception{
		return getListItem(session, "PatrolMandate", value);
	}
	
	/**
	 * Gets the station object
	 * @param session
	 * @param value station uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public static ListItem getStation(Session session, String value)throws Exception{
		return getListItem(session, "Station", value);
	}
	
	/**
	 * Gets team object
	 * @param session
	 * @param value team uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public static ListItem getTeam(Session session, String value)throws Exception{
		return getListItem(session, "Team", value);
	}
	
	/**
	 * Gets the transportation types listitem object 
	 * @param session
	 * @param value transportation type uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public static ListItem getTransportType(Session session, String value)throws Exception{
		return getListItem(session, "PatrolTransportType", value);
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
	private static ListItem getListItem(Session session, String clazz, String value)throws Exception{
		Query q = session.createQuery("SELECT uuid, name FROM " + clazz + " WHERE uuid =:uuid");
		q.setParameter("uuid", SmartUtils.decodeHex(value));
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if (results.size() == 1){
			return new ListItem( (byte[])((Object[])results.get(0))[0], (String)((Object[])results.get(0))[1]);
		}else{
			QueryPlugIn.log("Error loading " + clazz + " value '" + value + "' for query.", null);
			return new ListItem("");
		}
	}
	
	/**
	 * Creates a list item for the given employee uuid
	 * @param session
	 * @param value employee uuid as hex encoded string
	 * @return
	 * @throws Exception
	 */
	public static ListItem getEmployee(Session session, String value) throws Exception{
		Query q = session.createQuery("SELECT uuid, givenName, familyName, id FROM Employee WHERE uuid =:uuid");
		
		q.setParameter("uuid", SmartUtils.decodeHex(value));
		@SuppressWarnings("unchecked")
		List<Object[]> results = q.list();
		if (results.size() == 1){
			Object[] d = results.get(0);
			return new ListItem( (byte[])d[0], (String) d[1] + " " + (String)d[2] + " [" + (String)d[3] + "]");
		}else{
			QueryPlugIn.log("Error loading employee value '" + value + "' for query.", null);
			return new ListItem("");
		}
	}
	
	
	/**
	 * Loads the category for the given category key 
	 * @param session
	 * @param categoryKey
	 * @return cateogyr object or <code>null</code> if not loaded
	 */
	public static Category getCategory(Session session, String categoryKey){
		Query q = session.createQuery("From Category where conservationArea = :ca and hkey = :key");
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("key", categoryKey);
		@SuppressWarnings("unchecked")
		List<Category> results = q.list();
		if (results.size() != 1 ){
			return null;
		}else{
			return results.get(0);
		}
		
	}
}
