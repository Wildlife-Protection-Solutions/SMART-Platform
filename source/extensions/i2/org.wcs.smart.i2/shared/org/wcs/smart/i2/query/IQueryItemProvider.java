/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query;

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntityType;
/**
 * Query item provider for providing items for query module 
 * 
 * @author Emily
 *
 */
public interface IQueryItemProvider {

	/**
	 * Get the entity type with the given entity type key
	 * @param entityTypeKey
	 * @param session
	 * @return
	 */
	public IntelEntityType getEntityType(String entityTypeKey, Session session);
	
	/**
	 * Get all employees
	 * @param session
	 * @return
	 */
	public List<Employee> getEmployees(Session session);
	
	/**
	 * Get all entity types
	 * @param session
	 * @return
	 */
	public List<IntelEntityType> getEntityTypes(Session session);
	
	/**
	 * Get all areas of the given types
	 * @param type
	 * @param session
	 * @return
	 */
	public List<Area> getAreas(Area.AreaType type, Session session);
	
	/**
	 * Get all list items associated with the attribute represented by the attribute key
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public List<IntelAttributeListItem> getAttributeListItems(String attributeKey, Session session);
	
	/**
	 * Find the attribute associated with the attribute key
	 * 
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public IntelAttribute getAttribute(String attributeKey, Session session);
	
	/**
	 * Find the datamode attribute associated with the attribute key
	 * 
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getDataModelAttribute(String attributeKey, Session session);
	
	/**
	 * Find the datamodel attribute list associated with the attribute key and list item key
	 * 
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public AttributeListItem getDataModelAttributeListItem(String attributeKey, String listItemKey, Session session);
	
	/**
	 * Find the datamodel attribute list associated with the attribute key and tree hkey item key
	 * 
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public AttributeTreeNode getDataModelAttributeTreeNode(String attributeKey, String hkey, Session session);
	
	/**
	 * Get all conservation areas supported by item provider
	 * @return
	 */
	public Collection<ConservationArea> getConservationAreas();
	
	/**
	 * The conservation area the system is logged into 
	 */
	public ConservationArea getQueryConservationArea();
}
