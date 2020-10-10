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
package org.wcs.smart.query.internal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.CcaaDataModel;
import org.wcs.smart.ca.datamodel.CcaaDataModelDesktop;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.util.SmartUtils;
/**
 * Data model manager for when users are performing cross
 * conservation analysis.
 * 
 * @author Emily
 */
public class MultiCaDataModelManagerImpl extends AbstractDataModelManager {

	private CcaaDataModel ccaaModel = null;
	
	public MultiCaDataModelManagerImpl() {
		super();
		ccaaModel = CcaaDataModelDesktop.getInstance();
	}
	
	/**
	 * Returns only items shared across all conservation areas.
	 * 
	 * <p>The objects returned are associated with the same conservation
	 * areas as the attribute passed in</p> 
	 * 
	 * 
	 * @param attribute
	 * @param session
	 * @return
	 */
	public List<AttributeListItem> getActiveAttributeListItems(Attribute attribute, Session session){
		return ccaaModel.getAttributeListItems(attribute, session);
	}
	
	/**
	 * Determines the attribute list items for the given attribute.
	 * 
	 * @param attribute
	 * @param session
	 * @param onlyActive
	 * @return
	 */
	@Override
	public List<AttributeListItem> getAttributeListItems(Attribute attribute, Session session, boolean onlyActive) {
		return getActiveAttributeListItems(attribute, session);	
	}
	
	/**
	 * Determines the attribute tree items for the given attribute (including inactive).
	 */
	@Override
	public List<AttributeTreeNode> getAllAttributeTreeNodes(Attribute attribute, Session session) {
		return getActiveAttributeTreeNodes(attribute, session);
	}	

	/**
	 * Returns only items shared across all conservation areas.
	 * 
	 * @param attribute
	 * @param session
	 * @return list of root attribute tree nodes
	 */
	public List<AttributeTreeNode> getActiveAttributeTreeNodes(Attribute attribute, Session session){
		return ccaaModel.getAttributeTreeNodes(attribute, session);
	}

	/**
	 * Returns the attribute with the given key from 
	 * the shared data model.
	 * 
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getAttribute(Session session, String attributeKey){
		for (Attribute a : ccaaModel.getAttributes()){
			if (a.getKeyId().equals(attributeKey)){
				return a;
			}
		}
		return null;
	}

	
	/**
	 * Returns the attribute associated with the main conservation area with the attribute of the given key.
	 * @param attributeKey
	 * @param session
	 * @return
	 */
	public Attribute getAttribute(Session session, Attribute attribute){
		if (attribute.getConservationArea() != null
				&& attribute.getConservationArea().equals(SmartDB.getConservationAreaConfiguration().getMainConservationArea())){
			return attribute;
		}
		return getAttribute(session, attribute.getKeyId());
	}
	
	
	/**
	 * 
	 * Gets all the attribute tree nodes at 
	 * a given level in the data tree.    
	 * 
	 * <p>returns only items shared across all conservation areas</p>
	 * 
	 * @param session
	 * @param uuid attribute uuid
	 * @param level tree node level
	 * @param active ignored
	 * @return
	 */
	public List<AttributeTreeNode> getAttributeTreeNodes(Session session, Attribute attribute, int level, boolean active){
		List<AttributeTreeNode> toVisit = new ArrayList<>(ccaaModel.getAttributeTreeNodes(attribute, session));
		int currentlevel = 0;
		
		if (currentlevel == level) return toVisit;
		
		List<AttributeTreeNode> next = new ArrayList<>();
		while(!toVisit.isEmpty()) {
			
			next = new ArrayList<>();
			while(!toVisit.isEmpty()) {
				AttributeTreeNode n = toVisit.remove(0);
				next.addAll(n.getChildren());
			}
			if (currentlevel + 1 == level) return next;
			
			toVisit = next;
			currentlevel++;
		}
		return Collections.emptyList();		
	}
	
	
	/**
	 * Loads the category for the given category key from the shared data model  
	 * @param session
	 * @param categoryKey
	 * @return category object or <code>null</code> if not loaded
	 */
	@Override
	public Category getCategory(Session session, String categoryKey){
		return findCategory(categoryKey, ccaaModel.getCategories());
		
	}
	
	/**
	 * 
	 * Gets all the categories at a given level that are shared across all conservation areas.
	 * The objects returned are associated with the main conservation area. 
	 * 
	 * @param session
	 * @param level
	 * @return
	 */
	@Override
	public List<Category> getCategories(Session session, int level){
		return ccaaModel.getCategories(session, level);
	}

	
	/**
	 * Loads an attribute list item for the given key and the
	 * main conservation area 
	 * 
	 * @param session
	 * @param attributeKey attribute key
	 * @return attributelistitem the list item key
	 * @return the attribute list item associated with the main conservation area or <code>null</code> if
	 * attribute list item is not shared across all conservation areas.
	 */
	@Override
	public AttributeListItem getAttributeListItem(Session session, String attributeKey, String attributeListItem){
		Attribute a = null;
		for (Attribute att : ccaaModel.getAttributes()) {
			if (att.getKeyId().equals(attributeKey)) {
				a = att;
				break;
			}
		}
		if (a == null) return null;
		
		for (AttributeListItem li : ccaaModel.getAttributeListItems(a, session)) {
			if (li.getKeyId().equals(attributeListItem)) return li;
		}
		return null;
	}
	
	/**
	 * Loads an attribute tree node for the given key.
	 * Returns the attributetreenode associated with the main conservation area if 
	 * found otherwise will return null
	 * 
	 * @param session
	 * @param attributeHKey attribute tree node hkey
	 * @return attributelistitem loaded from the database or <code>null</code> if attribute not found
	 */
	@Override
	public AttributeTreeNode getAttributeTreeNode(Session session, String attributeKey, String attributeTreeHKey){
		
		Attribute a = null;
		for (Attribute att : ccaaModel.getAttributes()) {
			if (att.getKeyId().equals(attributeKey)) {
				a = att;
				break;
			}
		}
		if (a == null) return null;
		
		List<AttributeTreeNode> toVisit = new ArrayList<>(ccaaModel.getAttributeTreeNodes(a, session));
		while(!toVisit.isEmpty()) {
			AttributeTreeNode n = toVisit.remove(0);
			if (n.getHkey().equals(attributeTreeHKey)) return n;
			toVisit.addAll(n.getChildren());
		}
		return null;
	}
	
	/**
	 * Returns the full category label for a category with the
	 * given uuid.  This attempts to find the label
	 * in the same language as the current system language.
	 * If not it uses the default language of the conservation area.
	 * 
	 * @param session
	 * @param categoryUuid
	 * @return
	 */
	@Override
	public String[] getFullCategoryLabel(Session session, UUID categoryUuid){
		//if (true) return new String[]{"abc"};
		
		Category category = (Category) session.load(Category.class, categoryUuid);
		Category existingCategory = findCategory(category.getHkey(), ccaaModel.getCategories());
		
		//find the shared category in the datamodel
		//and use those labels; if not found use
		//the labels of the category
		ArrayList<String> values = new ArrayList<String>();
		if (existingCategory != null){
			values.add(existingCategory.getName());
			Category parent = existingCategory.getParent();
			while(parent != null){
				values.add(parent.getName());
				parent = parent.getParent();
			}	
		}else{
			Language l = SmartUtils.findLanguageMatch(category.getConservationArea().getLanguages());
			if (l == null){
				//default language of conservation area
				l = category.getConservationArea().getDefaultLanguage();
			}
			values.add(category.findName(l));
			Category parent = category.getParent();
			while(parent != null){
				values.add(parent.findName(l));
				parent = parent.getParent();
			}
			
		}

		Collections.reverse(values);
		return values.toArray(new String[values.size()]);
	}
	
	
	/**
	 * Finds all attributes associated with the given category
	 * hkey or parent key.
	 * 
	 * @param categoryHkey
	 * @return
	 */
	@Override
	public Collection<Attribute> getAttributes(Session session, String categoryHkey){
		List<Attribute> attributes = new ArrayList<Attribute>();
		Category c = getCategory(session, categoryHkey);
		while (c != null){
			for(CategoryAttribute a : c.getAttributes()){
				attributes.add(a.getAttribute());
			}
			c = c.getParent();
		}
		return attributes;
	}
	
	private Category findCategory(String hkey, List<Category> categories){
		if (categories == null){
			return null;
		}
		for (Category kid :categories){
			if (kid.getHkey().equals(hkey)){
				return kid;
			}
		}
		for (Category kid :categories){
			Category find = findCategory(hkey, kid.getChildren());
			if (find != null){
				return find;
			}
		}
		return null;
	}
	
	/**
	 * This searches the merged data model for the given attribute and list
	 * item.  If found it uses the label provided by the data model otherwise
	 * it uses the label provided by the conservation area associated with the
	 * attribute that matches the code of the current active language
	 * or the default language.
	 * 
	 * @return the label to use for the given 
	 * attribute list item
	 * 
	 */
	@Override
	public String getAttributeListItemLabel(Session session, UUID cauuid, UUID keyuuid ){
		return SmartLabelProvider.getDescription(keyuuid, cauuid, session);
	}

	
	/**
	 * This searches the merged data model for the given attribute and tree
	 * node.  If found it uses the label provided by the data model otherwise
	 * it uses the label provided by the conservation area associated with the
	 * attribute that matches the code of the current active language
	 * or the default language.
	 * 
	 * @return the label to use for the given attribute tree node
	 */
	@Override
	public String getAttributeTreeNodeLabel(Session session, UUID cauuid, UUID keyuuid){
		return SmartLabelProvider.getDescription(keyuuid, cauuid, session);
	}
	
	
	@Override
	public int getActiveDepth(){
		return ccaaModel.getCategoryDepth();
	}

	@Override
	public void clearDataModel() {
		ccaaModel.clearDataModel();
		
	}

	@Override
	public DataModel getDataModel() {
		return (DataModel) ccaaModel.getDataModel();
	}

	
}
