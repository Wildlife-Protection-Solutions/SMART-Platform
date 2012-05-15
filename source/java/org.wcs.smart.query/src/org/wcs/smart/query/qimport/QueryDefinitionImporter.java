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
package org.wcs.smart.query.qimport;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.model.waypoint.WaypointQuery;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.internal.filter.AttributeFilter;
import org.wcs.smart.query.parser.internal.filter.CategoryFilter;
import org.wcs.smart.query.parser.internal.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.xml.QueryXmlManager;
import org.wcs.smart.query.xml.model.Query;
import org.wcs.smart.query.xml.model.QueryType;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.SmartUtils;

/**
 * Importer for importing query definition files.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryDefinitionImporter {

	/*
	 * list of warnings generated during import process
	 */
	private ArrayList<String> warnings = new ArrayList<String>();
	
	
	/**
	 * Imports the given definition file.
	 * 
	 * <p>
	 * The returned query does not have the folder or shared value
	 * set.  This must be set by the calling function based
	 * on where the query is to be saved.
	 * </p> 
	 * 
	 * @param file the query definition xml file to import
	 * @return the imported query
	 * @throws Exception if the file cannot be converted to a query.
	 * 
	 */
	public WaypointQuery importQuery(File file) throws Exception{
		warnings.clear();
		
		InputStream fin = new BufferedInputStream(new FileInputStream(file));
		Query q = QueryXmlManager.readDataModel(fin);
		fin.close();
		
		QueryType qt = q.getQuery();
		
		String langCode = qt.getLanguage();
		
		WaypointQuery wq = new WaypointQuery();
		wq.setName(qt.getName());
		
		HashMap<String, UuidItemType> uuidLookup = new HashMap<String, UuidItemType>();
		for (UuidItemType type : qt.getUuiditem()){
			uuidLookup.put(type.getUuid(), type);
		}
		
		String strQueryFilter = "";
		if (qt.getDefinition() != null && qt.getDefinition().length() > 0){
			InputStream is = new ByteArrayInputStream(qt.getDefinition().getBytes());
			Parser parser = new Parser(is);
			IFilter queryFilter = parser.QueryFilter();
			is.close();
			
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				validateFilterPart(queryFilter, langCode, uuidLookup, session);
			}finally{
				session.getTransaction().rollback();
				session.close();
			}
			strQueryFilter = queryFilter.asString();
		}
		
		wq.setQueryFilter(strQueryFilter);
		wq.setConservationArea(SmartDB.getCurrentConservationArea());
		wq.setOwner(SmartDB.getCurrentEmployee());
		
		ConservationAreaFilter caFilter = new ConservationAreaFilter();
		caFilter.addConservationArea(SmartDB.getCurrentConservationArea());
		wq.setConservationAreaFilter(caFilter);
		
		
		return wq;
	}
	
	/**
	 * @return warnings generated during import process
	 */
	public ArrayList<String> getWarnings(){
		return this.warnings;
	}
	
	
	/**
	 * Validates a filter item against the database.
	 * 
	 * @param filter the filter to validate
	 * @param langCode the language value of the query 
	 * @param uuidLookup a uuid lookup map that looks up uuid values
	 * @param session database session
	 * 
	 * @throws Exception if filter cannot be validated
	 */
	private void validateFilterPart(IFilter filter, String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception{
		if (filter instanceof AttributeFilter){
			AttributeFilter f = (AttributeFilter)filter;
			validateAttribute(f.getAttributeKey(), session);
			if (f.getAttributeType() == AttributeType.LIST){
				validateAttributeListItem((String)f.getValue(), session);
			}else if (f.getAttributeType() == AttributeType.TREE){
				validateAttributeTreeNode((String)f.getValue(), session);
			}
			
		}else if (filter instanceof CategoryFilter){
			String catId = ((CategoryFilter)filter).asString();
			String cathkey = catId.split(":")[1];
			validateCategory(cathkey, session);
		}else if (filter instanceof PatrolFilter){
			PatrolQueryOption op = ((PatrolFilter) filter).getPatrolOption();
			if (op.getType() == PatrolQueryOptions.PatrolQueryOptionType.UUID){
				byte[] uuid = SmartUtils.decodeHex( ((PatrolFilter)filter).getValue() );
				if (op.getObject(session, uuid)  != null){
					//object exists in db
					return;
				}else{
					UuidItemType item = uuidLookup.get(  ((PatrolFilter)filter).getValue()  );
					if (item == null){
						throw new Exception("Could not resolve patrol filter : " + filter.asString());
					}
					if (SimpleListItem.class.isAssignableFrom(op.getSourceClass())){
						
						SimpleListItem it = findValue(langCode, item.getValue().get(0), op.getSourceClass().getSimpleName(), session);
						
						if (it == null){
							throw new Exception("Could not resolve patrol filter : " + filter.asString() + ".  Could not find a value for " + op.getSourceClass().getSimpleName() + " that matches '" + item.getValue().get(0) + "'");
						}else{
							warnings.add("The unique identifier for " + op.getGuiName() + " filter does not match any idnetifiers in the database.  However the name '" + item.getValue().get(0) + "' was matched and will be used in the query instead." );
							//update uuid
							((PatrolFilter)filter).setValue(SmartUtils.encodeHex(it.getUuid()));
						}
					}else if (Employee.class.isAssignableFrom(op.getSourceClass())){
						//lookup employee
						Employee e = findEmployee(item.getId(), item.getValue().get(0), item.getValue().get(1), session);
						if (e != null){
							((PatrolFilter)filter).setValue(SmartUtils.encodeHex(e.getUuid()));
						}else{
							throw new Exception("Could not resolve patrol filter : " + filter.asString() + ".  Could not find a matching employee with value '" + item.getValue().get(0) + " " + item.getValue().get(1) + " [" + item.getId() + "] ");	
						}
					}else{
						throw new Exception("Could not resolve patrol filter : " + filter.asString());
					}
				}
				
			}
		}
		
		//validate children
		if (filter.getChildren() != null){
			for (IFilter kid : filter.getChildren()){	
				validateFilterPart(kid, langCode, uuidLookup, session);
			}
		}
	}
	
	/**
	 * Attempts to find an employee based on the values.
	 * <p>
	 * First attempts based on the id & name, then the id only, then the name only.
	 * </p>
	 * @param employeeId
	 * @param givenName
	 * @param familyName
	 * @param session
	 * @return null if employee not found, otherwise employee found
	 * 
	 */
	private Employee findEmployee(String employeeId, String givenName, String familyName, Session session){
		Employee e = HibernateManager.findEmployeeByIdAndName(employeeId, givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add("The unique identifier for Employee " + givenName + " " + familyName + "[" + employeeId + "] did not match the database unique identifier. However an employee with the same name and identifier was found and will be used in the query.");
			return e;
		}
		
		e = HibernateManager.findEmployeeById(employeeId, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add("The unique identifier for Employee " + givenName + " " + familyName + "[" + employeeId + "] did not match the database unique identifier. However an employee with the same identifier but a different name '" + e.getGivenName() + " " + e.getFamilyName() + "' was found and will be used in the query.");
			return e;
		}
		
		e = HibernateManager.findEmployeeByName(givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add("The unique identifier for Employee " + givenName + " " + familyName + "[" + employeeId + "] did not match the database unique identifier. However employee with the same name but a different identifier '" + e.getId() + "' was found and will be used in the query.");
			return e;
		}
		
		return null;
	}
	
	
	/**
	 * Looks up the name of a simplelistitem.
	 * 
	 * @param langCode
	 * @param value
	 * @param objectType
	 * @param session
	 * @return
	 */
	private SimpleListItem findValue(String langCode, String value, String objectType, Session session){
		
		String sql = "SELECT c FROM Language a, Label b, " + objectType + " c WHERE b.id.language = a.uuid AND b.id.element.uuid = c.uuid and a.code = :cd and b.value = :value and c.conservationArea = :ca ";
		
		org.hibernate.Query query = session.createQuery(sql);
		query.setParameter("cd", langCode);
		query.setParameter("value", value);
		query.setParameter("ca", SmartDB.getCurrentConservationArea());
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			warnings.add("Multiple options found for " + objectType + " for value '" + value + "'. The first value found will be used.");
			return (SimpleListItem)results.get(0);
		}else{
			return (SimpleListItem)results.get(0);
		}
	}
	
	
	/**
	 * Validates that a category with the given hkey exists
	 * @param hkey
	 * @param session
	 * @throws Exception
	 */
	private void validateCategory(String hkey, Session session) throws Exception{
		String hql = " FROM Category Where conservationArea = :ca and hkey = :key";
		org.hibernate.Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("key", hkey);
		
		if (q.list().size() != 1){
			throw new Exception ("Could not find Category with key '" + hkey + "' in datamodel. ");
		}
	}
	/**
	 * Validates that an atttribute with the given key exists
	 * @param key
	 * @param session
	 * @throws Exception
	 */
	private void validateAttribute(String key, Session session) throws Exception{
		String hql = " FROM Attribute Where conservationArea = :ca and keyId = :key";
		org.hibernate.Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("key", key);
		
		if (q.list().size() != 1){
			throw new Exception ("Could not find Attribute with key '" + key + "' in datamodel. ");
		}
	}
	
	/**
	 * Validates that a given attribute list item exists
	 * @param key
	 * @param session
	 * @throws Exception
	 */
	private void validateAttributeListItem(String key, Session session) throws Exception{
		
		String hql = " FROM AttributeListItem ai join ai.attribute a Where a.conservationArea = :ca and ai.keyId = :key";
		org.hibernate.Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("key", key);
		
		if (q.list().size() != 1){
			throw new Exception ("Could not find Attribute List Item with key '" + key + "' in datamodel. ");
		}
	}
	

	/**
	 * Validates that a given attribute tree node item exists
	 * @param key
	 * @param session
	 * @throws Exception
	 */
	private void validateAttributeTreeNode(String key, Session session) throws Exception{
		String hql = " FROM AttributeTreeNode ai join ai.attribute a Where a.conservationArea = :ca and ai.hkey = :hkey";
		org.hibernate.Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea());
		q.setParameter("hkey", key);
		
		if (q.list().size() != 1){
			throw new Exception ("Could not find Attribute Tree Node with key '" + key + "' in datamodel. ");
		}
	}
}
