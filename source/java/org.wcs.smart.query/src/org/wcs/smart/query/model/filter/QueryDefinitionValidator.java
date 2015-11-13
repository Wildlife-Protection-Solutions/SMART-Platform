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
package org.wcs.smart.query.model.filter;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.NamedKeyItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.summary.AreaGroupBy;
import org.wcs.smart.query.model.summary.AttributeGroupBy;
import org.wcs.smart.query.model.summary.AttributeValueItem;
import org.wcs.smart.query.model.summary.CategoryGroupBy;
import org.wcs.smart.query.model.summary.CategoryValueItem;
import org.wcs.smart.query.model.summary.IGroupBy;
import org.wcs.smart.query.model.summary.IValueItem;

/**
 * Basic tool for validating query definitions.  This class should be
 * extended where custom filters, values and group are validated
 * 
 * 
 * @author Emily
 *
 */
public class QueryDefinitionValidator {

	protected Session session;

	/**
	 * @param langCode the language value of the query 
	 * @param uuidLookup a uuid lookup map that looks up uuid values
	 * @param session database session
	 * 
	 */
	public QueryDefinitionValidator(Session session ){
		this.session = session;
	}

	
	/**
	 * Validates a filter item against the database.
	 * <p>Warnings differ from Errors as warnings still allow the query
	 * to be imported.  Errors prevent the query from being imported.  Error
	 * are thrown from the validateFilterPart function.</p>
	 * 
	 * @param filter the filter to validate
	 * @return list of warnings generated during validate call.
	 * 
	 * @throws Exception if filter cannot be validated
	 */
	public List<String> validate(IFilter filter) throws Exception{
		FilterValidatorVisitor vv = new FilterValidatorVisitor();
		filter.accept(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return vv.warnings;
	}
	
	/**
	 * Validates a value item.
	 * 
	 * @param item
	 * @return 
	 * @throws Exception
	 */
	public List<String> validate(IValueItem item) throws Exception{
		ValueVisitor vv = new ValueVisitor();
		item.accept(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return new ArrayList<String>();
	}
	

	/**
	 * Validates a group by item.
	 * @param item
	 * @return
	 * @throws Exception
	 */
	public List<String> validate(IGroupBy item) throws Exception{
		GroupByValidatorVisitor vv = new GroupByValidatorVisitor();
		item.visit(vv);
		if (vv.ex != null){
			throw vv.ex;
		}
		return new ArrayList<String>();
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
	public Employee findEmployee(String employeeId, String givenName, String familyName, List<String> warnings){
		Employee e = HibernateManager.findEmployeeByIdAndName(employeeId, givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add(MessageFormat.format(
					Messages.FilterValidator_Employee_DifferentUniqueId,
					new Object[]{e.getFullLabel()}));
			return e;
		}
		
		e = HibernateManager.findEmployeeById(employeeId, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add(MessageFormat.format(
					Messages.FilterValidator_EmployeeDifferentName,
					new Object[]{Employee.formatName(givenName, familyName, employeeId),
					e.getShortLabel()})); 
			return e;
		}
		
		e = HibernateManager.findEmployeeByName(givenName, familyName, SmartDB.getCurrentConservationArea(), session);
		if (e != null){
			warnings.add(MessageFormat.format(Messages.FilterValidator_EmployeeDifferentId,
					new Object[]{Employee.formatName(givenName, familyName, employeeId), e.getId()}));
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
	public NamedItem findValue(String langCode, String value, String objectType, List<String> warnings){
		
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
			return (NamedItem)results.get(0);
		}else{
			return (NamedItem)results.get(0);
		}
	}
	
	/**
	 * Looks up a named by item by the keyid
	 * @param key the key to find
	 * @param objectType the type of object to search.  Assumes this object type has a conservationArea property
	 * 
	 * @return the matching item or null if nothing found
	 */
	public NamedKeyItem findKeyValue(String key, String objectType){
		
		String sql = "SELECT c FROM " + objectType + " c WHERE keyId = :keyId and c.conservationArea = :ca "; //$NON-NLS-1$ //$NON-NLS-2$
		
		org.hibernate.Query query = session.createQuery(sql);
		query.setParameter("keyId", key); //$NON-NLS-1$
		query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
		
		List<?> results = query.list();
		if (results.size() == 0){
			return null;
		}else if (results.size() > 1){
			return null;
		}else{
			return (NamedKeyItem)results.get(0);
		}
	}
	
	
	/**
	 * Validates that a category with the given hkey exists
	 * @param hkey
	 * @param session
	 * @throws Exception
	 */
	public void validateCategory(String hkey) throws Exception{
		Category c = QueryDataModelManager.getInstance().getCategory(session, hkey);
		if (c == null){
			throw new Exception (MessageFormat.format(Messages.FilterValidator_CategoryNotFound, new Object[]{hkey}));
		}
	}
	
	
	/**
	 * Validates the area filter ensuring the area with the given key and type exists.
	 * 
	 * @param type the area type
	 * @param areaKey the area key
	 * @param session
	 * @throws Exception
	 */
	public void validateArea(AreaType type, String areaKey) throws Exception{
		Area a = HibernateManager.findArea(type, areaKey, session);
		if (a == null){
			throw new Exception (MessageFormat.format(Messages.FilterValidator_InvalidAreaFilter, new Object[]{type.getGuiName(), areaKey}));
		}
	}
	
	/**
	 * Validates that an atttribute with the given key exists
	 * @param key
	 * @param session
	 * @throws Exception
	 */
	public void validateAttribute(String key) throws Exception{
		Attribute a = QueryDataModelManager.getInstance().getAttribute(session, key);
		if (a == null){
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
	public void validateAttributeListItem(String key, String attributeKey) throws Exception{
		if (key.equals(AttributeFilter.ANY_OPTION.getKey())){
			return;
		}
		Object x = QueryDataModelManager.getInstance().getAttributeListItem(session, attributeKey, key);
		if (x == null){
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
	public void validateAttributeTreeNode(String key, String attributeKey) throws Exception{
		Object x = QueryDataModelManager.getInstance().getAttributeTreeNode(session, attributeKey, key);
		if (x == null){
			throw new Exception (MessageFormat.format(Messages.FilterValidator_AttributeTreeNodeNotFound, new Object[]{key}));
		}
	}
	
	
	class FilterValidatorVisitor implements IFilterVisitor{

		private Exception ex;
		private ArrayList<String> warnings = new ArrayList<String>();
		
		@Override
		public void visit(IFilter filter) {
			if (ex != null)
				return;
			try {
				if (filter instanceof AttributeFilter) {
					AttributeFilter f = (AttributeFilter) filter;
					validateAttribute(f.getAttributeKey());
					if (f.getAttributeType() == AttributeType.LIST) {
						validateAttributeListItem((String) f.getValue(),f.getAttributeKey());
					} else if (f.getAttributeType() == AttributeType.TREE) {
						validateAttributeTreeNode((String) f.getValue(),f.getAttributeKey());
					}

				} else if (filter instanceof CategoryFilter) {
					String catId = ((CategoryFilter) filter).asString();
					String cathkey = catId.split(":")[1]; //$NON-NLS-1$
					validateCategory(cathkey);
				}
			} catch (Exception ex) {
				this.ex = ex;
			}
		}
	}
	
	class GroupByValidatorVisitor implements IGroupByVisitor{

		private Exception ex;
		
		@Override
		public void visit(IGroupBy filter) {
			if (ex != null)
				return;
			try {
				if (filter instanceof AreaGroupBy) {
					for (String key : ((AreaGroupBy) filter).getAreaFilterKeys()){
						validateArea(((AreaGroupBy) filter).getAreaType(), key);
					}
				} else if (filter instanceof AttributeGroupBy) {
					AttributeGroupBy gg = (AttributeGroupBy)filter;
					if (gg.getCategoryHkey() != null){
						validateCategory(gg.getCategoryHkey());
					}
					if (gg.getAttributeType()==AttributeType.TREE){
						if (gg.getFilterKeys() != null){
							for (String key : gg.getFilterKeys()){
								validateAttributeTreeNode(key, gg.getAttributeKey());
							}
						}
					}else if (gg.getAttributeType() == AttributeType.LIST){
						if (gg.getFilterKeys() != null){
							for (String key : gg.getFilterKeys()){
								validateAttributeListItem(key, gg.getAttributeKey());
							}
						}
					}
				} else if (filter instanceof CategoryGroupBy) {
					for(String key : ((CategoryGroupBy) filter).getFilterKeys()){
						validateCategory(key);
					}
				}
			} catch (Exception ex) {
				this.ex = ex;
			}
		}
	}
	
	class ValueVisitor implements IValueVisitor{
		private Exception ex;
		
		@Override
		public void visit(IValueItem item) {
			if (ex != null) return ;
			try{
				if (item instanceof AttributeValueItem){
					AttributeValueItem it = (AttributeValueItem)item;
					validateAttribute(it.getAttributeKey());
					if (it.getCategoryKey() != null){
						validateCategory(it.getCategoryKey());
					}
					if (it.getAggregation() == null){
						throw new Exception(MessageFormat.format(Messages.QueryDefinitionValidator_AggregationNotSupported, new Object[]{ it.getAggregationKey() }));
					}
					
				}else if (item instanceof CategoryValueItem){
					CategoryValueItem i = (CategoryValueItem)item;
					if (i.getCategoryHKey() == null){
						//this is okay; we assume all categories
					}else{
						validateCategory(((CategoryValueItem) item).getCategoryHKey());
					}
				}
			}catch (Exception ex){
				this.ex = ex;
			}
		}
	};
}
