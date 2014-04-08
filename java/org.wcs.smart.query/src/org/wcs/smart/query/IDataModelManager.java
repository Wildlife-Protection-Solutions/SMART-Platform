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

import java.util.Collection;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;

/**
 * Interface for a query data model manager. The data model manager is
 * responsible for loading and managing the data model for the query
 * perspective.
 * 
 * @author Emily
 * 
 */
public interface IDataModelManager {

	/**
	 * Clears the current cached data model
	 */
	public void clearDataModel();

	/**
	 * In the case of single conservation area querying this will return the
	 * data model of the current conservation area. Otherwise this will return
	 * the merged data model of all the existing conservation areas.
	 * <p>
	 * This will block until the data model is loaded.  The data model
	 * is cached and only reloaded if clearDataModel() clears 
	 * the cached model
	 * </p>
	 * 
	 * @return the data model for querying
	 */
	public DataModel getDataModel();

	/**
	 * Determines the active attribute list items for the given attribute.
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	public List<AttributeListItem> getActiveAttributeListItems(
			Attribute attribute, Session session);

	/**
	 * Determines the all attribute list items for the given attribute.
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	public List<AttributeListItem> getAttributeListItems(Attribute attribute, Session session);
	
	/**
	 * Determines the active attribute tree items for the given attribute
	 * 
	 * @param attribute
	 * @param session
	 * @return list of root attribute tree nodes
	 */
	public List<AttributeTreeNode> getActiveAttributeTreeNodes(
			Attribute attribute, Session session);

	
	/**
	 * Returns the attribute with a given key for
	 * the current conservation area
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getAttribute(Session session, String attributeKey);

	/**
	 * Returns the attribute with a key provided by the attribute.
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getAttribute(Session session, Attribute attribute);

	/**
	 * Finds all attributes associated with the given category
	 * hkey or any of it's parents
	 * 
	 * @param categoryHkey
	 * @return collection of attributes
	 */
	public Collection<Attribute> getAttributes(Session session, String categoryHkey);
	/**
	 * 
	 * Gets all the attribute tree nodes at a given level in the data tree.
	 * 
	 * @param session
	 * @param level
	 * @param active
	 *            if only active tree nodes should be loaded
	 * @return
	 */
	public List<AttributeTreeNode> getAttributeTreeNodes(Session session,
			Attribute attribute, int level, boolean active);

	
	/**
	 * Loads the category for the given category key
	 * 
	 * @param session
	 * @param categoryKey
	 * @return category object or <code>null</code> if not loaded
	 */
	public Category getCategory(Session session, String categoryKey);

	/**
	 * 
	 * Gets all the categories at a given level in the data tree.
	 * 
	 * @param session
	 * @param level
	 * @return
	 */
	public List<Category> getCategories(Session session, int level);

	/**
	 * Loads an attribute list item for the given key 
	 * 
	 * @param session
	 * @param attributeKey attribute key
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	public AttributeListItem getAttributeListItem(Session session, String attributeKey, String attributeListItem);
	
	/**
	 * Loads an attribute tree not item item for the given hkey
	 * 
	 * @param session
	 * @param attributeHKey attribute tree node hkey
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	public AttributeTreeNode getAttributeTreeNode(Session session, String attributeKey, String attributeTreeHKey);
	
	
	/**
	 * Returns the full category label for a category with the
	 * given uuid.
	 * 
	 * @param session
	 * @param categoryUuid
	 * @return
	 */
	public String[] getFullCategoryLabel(Session session, byte[] categoryUuid);
	
	/**
	 * Returns the label for the attribute list item with the given uuid.
	 * 
	 * @param session
	 * @param attributeKey parent attribute
	 * @param keyuuid attribute list item uuid
	 * @return
	 */
	public String getAttributeListItemLabel(Session session, byte[] cauuid, byte[] keyuuid);
	
	/**
	 * Returns the label for the attribute tree node with the given uuid
	 * @param session
	 * @param attribute parent attribute
	 * @param keyuuid attribute tree node uuid
	 * @return
	 */
	public String getAttributeTreeNodeLabel(Session session, byte[] cauuid, byte[] keyuuid);
	
	/**
	 * Returns a modifiable list of all the active attributes
	 * in the given data model. An attribute is active if it has
	 * at least one active category association.
	 * 
	 * Note: the data model only has active categories loaded
	 * so getActiveCategories will work but getCategories will fail
	 * if hibernate session not active.
	 * @param dm
	 * @return
	 */
	public List<Attribute> getActiveAttributes(DataModel dm);
	
	
	/**
	 * Computes the depth of the active
	 * data model category tree.
	 * tree.
	 * 
	 * @return
	 */
	public int getActiveDepth();
}
