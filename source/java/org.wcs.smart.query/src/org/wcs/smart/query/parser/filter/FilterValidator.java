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
package org.wcs.smart.query.parser.filter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.SimpleListItem;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.internal.filter.AttributeFilter;
import org.wcs.smart.query.parser.internal.filter.CategoryFilter;
import org.wcs.smart.query.parser.internal.filter.IFilter;
import org.wcs.smart.query.parser.internal.filter.PatrolFilter;
import org.wcs.smart.query.xml.model.UuidItemType;
import org.wcs.smart.util.SmartUtils;

/**
 * Tools for validation query filters.
 * 
 * @author Emily
 *
 */
public class FilterValidator {

	private ArrayList<String> warnings = null;
	
	public FilterValidator(){
		warnings = new ArrayList<String>();
	}
	
	/**
	 * Warnings differ from Errors as warnings still allow the query
	 * to be imported.  Errors prevent the query from being imported.  Error
	 * are thrown from the validateFilterPart function.
	 * 
	 * @return any warning generated during the validation process
	 */
	public List<String> getWarnings(){
		return warnings;
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
	public void validateFilterPart(IFilter filter, String langCode, HashMap<String, UuidItemType> uuidLookup, Session session) throws Exception{
		if (filter instanceof AttributeFilter){
			AttributeFilter f = (AttributeFilter)filter;
			validateAttribute(f.getAttributeKey(), session);
			if (f.getAttributeType() == AttributeType.LIST){
				validateAttributeListItem((String)f.getValue(), f.getAttributeKey(), session);
			}else if (f.getAttributeType() == AttributeType.TREE){
				validateAttributeTreeNode((String)f.getValue(), f.getAttributeKey(), session);
			}
			
		}else if (filter instanceof CategoryFilter){
			String catId = ((CategoryFilter)filter).asString();
			String cathkey = catId.split(":")[1]; //$NON-NLS-1$
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
						throw new Exception(
								MessageFormat.format(
								Messages.FilterValidator_PatrolFilterError, new Object[]{ filter.asString()}));
					}
					if (SimpleListItem.class.isAssignableFrom(op.getSourceClass())){
						
						SimpleListItem it = findValue(langCode, item.getValue().get(0), op.getSourceClass().getSimpleName(), session, warnings);
						
						if (it == null){
							throw new Exception(MessageFormat.format(
									Messages.FilterValidator_PatrolFilter_ValueMatchingError,
									new Object[]{filter.asString(), op.getSourceClass().getSimpleName(), item.getValue().get(0)}));
							
						}else{
							warnings.add(MessageFormat.format(Messages.FilterValidator_PatrolFilter_UnqiueIdMatchingError,
									new Object[]{op.getGuiName(), item.getValue().get(0)}));
							
							//update uuid
							((PatrolFilter)filter).setValue(SmartUtils.encodeHex(it.getUuid()));
						}
					}else if (Employee.class.isAssignableFrom(op.getSourceClass())){
						//lookup employee
						Employee e = findEmployee(item.getId(), item.getValue().get(0), item.getValue().get(1), session, warnings);
						if (e != null){
							((PatrolFilter)filter).setValue(SmartUtils.encodeHex(e.getUuid()));
						}else{
							throw new Exception(
									MessageFormat.format(Messages.FilterValidator_PatrolFilter_EmployeeError,
											new Object[]{filter.asString(), 
											item.getValue().get(0) + " "+ item.getValue().get(1) + " [" + item.getId() + "] "}));	 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						}
					}else{
						throw new Exception(
								MessageFormat.format(
								Messages.FilterValidator_PatrolFilterErrorB, new Object[]{ filter.asString()}));
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
	 * <p>Warnings are created when the process can continue but
	 * incorrect values my be substituted.  Errors are thrown 
	 * if the process cannot continue (ie attribute key not found in database).
	 * </p>
	 * @param employeeId
	 * @param givenName
	 * @param familyName
	 * @param session
	 * @return null if employee not found, otherwise employee found
	 * 
	 */
	public static Employee findEmployee(String employeeId, String givenName, String familyName, Session session, List<String> warnings){
		Employee e = HibernateManager.findEmployeeByIdAndName(employeeId, givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add(MessageFormat.format(
					Messages.FilterValidator_Employee_DifferentUniqueId,
					new Object[]{e.getLabel()}));
			return e;
		}
		
		e = HibernateManager.findEmployeeById(employeeId, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add(MessageFormat.format(
					Messages.FilterValidator_EmployeeDifferentName,
					new Object[]{givenName + " " + familyName + "[" + employeeId + "]", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					e.getGivenName() + " " + e.getFamilyName()})); //$NON-NLS-1$
			return e;
		}
		
		e = HibernateManager.findEmployeeByName(givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add(MessageFormat.format(Messages.FilterValidator_EmployeeDifferentId,
					new Object[]{givenName + " " + familyName + "[" + employeeId + "]", e.getId()})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			return e;
		}
		
		return null;
	}
	
	
	/**
	 * Looks up the name of a simplelistitem.
	 * 
	 *  <p>Warnings are created when the process can continue but
	 * incorrect values my be substituted.  Errors are thrown 
	 * if the process cannot continue (ie attribute key not found in database).
	 * </p>
	 * 
	 * @param langCode language code
	 * @param value value to search for
	 * @param objectType type of list object
	 * @param session db connection
	 * @param warnings list to add warnings to
	 * 
	 * @return
	 */
	public static SimpleListItem findValue(String langCode, String value, String objectType, Session session, List<String> warnings){
		
		String sql = "SELECT c FROM Language a, Label b, " + objectType + " c WHERE b.id.language = a.uuid AND b.id.element.uuid = c.uuid and a.code = :cd and b.value = :value and c.conservationArea = :ca "; //$NON-NLS-1$ //$NON-NLS-2$
		
		org.hibernate.Query query = session.createQuery(sql);
		query.setParameter("cd", langCode); //$NON-NLS-1$
		query.setParameter("value", value); //$NON-NLS-1$
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			warnings.add(MessageFormat.format(Messages.FilterValidator_MultipleImportOptions, new Object[]{objectType, value}));
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
	public static void validateCategory(String hkey, Session session) throws Exception{
		String hql = " FROM Category Where conservationArea = :ca and hkey = :key"; //$NON-NLS-1$
		org.hibernate.Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		q.setParameter("key", hkey); //$NON-NLS-1$
		
		if (q.list().size() != 1){
			throw new Exception (MessageFormat.format(Messages.FilterValidator_CategoryNotFound, new Object[]{hkey}));
		}
	}
	/**
	 * Validates that an atttribute with the given key exists
	 * @param key
	 * @param session
	 * @throws Exception
	 */
	public static void validateAttribute(String key, Session session) throws Exception{
		String hql = " FROM Attribute Where conservationArea = :ca and keyId = :key"; //$NON-NLS-1$
		org.hibernate.Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		q.setParameter("key", key); //$NON-NLS-1$
		
		if (q.list().size() != 1){
			throw new Exception (MessageFormat.format(Messages.FilterValidator_AttributeNotFound, new Object[]{key}));
		}
	}
	
	/**
	 * Validates that a given attribute list item exists.  Throws an exception if not found.
	 * @param key the list item key
	 * @param attributeKey the attribute key (associated with the list item)
	 * @param session
	 * @throws Exception
	 */
	public static void validateAttributeListItem(String key, String attributeKey, Session session) throws Exception{
		
		Query q = session
				.createQuery(" From AttributeListItem ali join ali.attribute as a "+ //$NON-NLS-1$
						"where a.conservationArea = :ca and ali.keyId = :key and "+ //$NON-NLS-1$
						"a.keyId = :attributeKey"); //$NON-NLS-1$
		
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		q.setParameter("key", key); //$NON-NLS-1$
		q.setParameter("attributeKey", attributeKey); //$NON-NLS-1$
			
		if (q.list().size() != 1){
			throw new Exception (MessageFormat.format(Messages.FilterValidator_AttributeListItemNotFound, new Object[]{key}));
		}
	}
	

	/**
	 * Validates that a given attribute tree node item exists.  Throws an exception if not found.
	 * @param key the tree node key
	 * @param attributeKey the attribute key of the tree
	 * @param session
	 * @throws Exception
	 */
	public static void validateAttributeTreeNode(String key, String attributeKey, Session session) throws Exception{
		String hql = " FROM AttributeTreeNode ai join ai.attribute a " +  //$NON-NLS-1$
				"Where a.conservationArea = :ca and ai.hkey = :hkey and a.keyId = :attributeKey";  //$NON-NLS-1$
		org.hibernate.Query q = session.createQuery(hql);
		q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		q.setParameter("hkey", key); //$NON-NLS-1$
		q.setParameter("attributeKey", attributeKey); //$NON-NLS-1$
		
		if (q.list().size() != 1){
			throw new Exception (MessageFormat.format(Messages.FilterValidator_AttributeTreeNodeNotFound, new Object[]{key}));
		}
	}
	
}
