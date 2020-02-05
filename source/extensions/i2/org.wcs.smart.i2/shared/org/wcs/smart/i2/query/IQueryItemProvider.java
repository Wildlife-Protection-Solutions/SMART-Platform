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
import java.util.Set;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelAttributeListItem;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelEntityTypeAttribute;
import org.wcs.smart.i2.model.IntelProfile;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.model.IntelRecordSourceAttribute;
/**
 * Query item provider for providing items for query module 
 * 
 * @author Emily
 *
 */
public interface IQueryItemProvider {

	/**
	 * Get all records sources in the system
	 * @param session
	 * @return
	 */
	public List<IntelRecordSource> getRecordSources(Set<UUID> profiles, Session session);
	
	/**
	 * Get all record attribute associated with the record source key
	 * @param session
	 * @return
	 */
	public List<IntelRecordSourceAttribute> getRecordSourceAttributes(IntelRecordSource recordSource, Session session);
	
	
	/**
	 * Get the record source given entity source key
	 * @param recordsourceKey
	 * @param session
	 * @return
	 */
	public IntelRecordSource getRecordSource(String recordsourceKey, Session session);
	
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
	public List<IntelEntityType> getEntityTypes(Set<UUID> profiles, Session session);
	
	/**
	 * Gets all profiles that match a key in the given
	 * key list
	 * 
	 * @param profileKeys
	 * @param session
	 * @return
	 */
	public Collection<IntelProfile> getProfiles(Set<String> profileKeys, Session session);
		
	/**
	 * Get all entity types
	 * @param session
	 * @return
	 */
	public List<IntelAttribute> getAttributes(Session session);
	
	/**
	 * Get all entity attribute associated with the entity type
	 * @param session
	 * @return
	 */
	public List<IntelEntityTypeAttribute> getEntityTypeAttributes(IntelEntityType entityType, Session session);
	
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
	 * Get all conservation areas supported by item provider
	 * @return
	 */
	public Collection<ConservationArea> getConservationAreas();
	
	/**
	 * The conservation area the system is logged into 
	 */
	public ConservationArea getQueryConservationArea();
	
	/**
	 * Get all entities of the given type
	 * @param entityTypeKey
	 * @param session
	 * @return
	 */
	public List<IntelEntity> getEntities(Set<UUID> profiles, String entityTypeKey, Session session);
	
	/**
	 * Get all data model attributes
	 * @param session
	 * @return
	 */
	public List<Attribute> getDmAttributes(Session session);
	
	/**
	 * The the data model root categories
	 * @param session
	 * @return
	 */
	public List<Category> getRootCategories(Session session);

	/**
	 * Get the children categories for a given parent category
	 * @param category
	 * @param session
	 * @return
	 */
	public List<Category> getChildren(Category category, Session session);
	
	/**
	 * Find a category based on the hkey
	 * @param categoryHkey
	 * @param session
	 * @return
	 */
	public Category getCategory(String categoryHkey, Session session);
	
	/**
	 * Find data model attribute based on the attribute key
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getDmAttribute(String attributeKey, Session session);
	
	/**
	 * Find all data model attribute list items for a given attribute
	 * @param attribute
	 * @param session
	 * @return
	 */
	public List<AttributeListItem> getDmAttributeListItem(Attribute attribute, Session session);
	
	/**
	 * Find root data model attribute tree nodes for a given attribute
	 * @param attribute
	 * @param session
	 * @return
	 */
	public List<AttributeTreeNode> getDmAttributeTreeNodes(Attribute attribute, Session session);
	
	/**
	 * The maximum category depth for the datamodel
	 * @param session
	 * @return
	 */
	public int getMaxDmCategoryDepth(Session session);
	
}
