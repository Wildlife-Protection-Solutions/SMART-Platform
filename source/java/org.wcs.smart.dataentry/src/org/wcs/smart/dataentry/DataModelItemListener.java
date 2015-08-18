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
package org.wcs.smart.dataentry;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.IDataModelItemListener;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;

/**
 * DataModel listener for removing data model items from configured models.
 * 
 * <p>Listeners for category deletes, category attribute deletes,
 * attribute tree node deletes, attribute list item deletes.  It automatically
 * removes categories, category/attributes and all tree node and list item
 * configurations.  In addition removes list items and tree nodes from
 * the default value options for attribute nodes.</p>
 * 
 * @author Emily
 *
 */
@SuppressWarnings("unchecked")
public class DataModelItemListener implements IDataModelItemListener {

	/**
	 * Called when item is removed from the data model.
	 */
	@Override
	public void deleteItem(Session currentSession, Object itemToDelete)
			throws Exception {
		
		if (itemToDelete instanceof Category){
			categoryDelete(currentSession, (Category)itemToDelete);
		}else if (itemToDelete instanceof CategoryAttribute){
			attributeDelete(currentSession, (CategoryAttribute)itemToDelete);
		}else if (itemToDelete instanceof AttributeListItem){
			listItemDelete(currentSession, (AttributeListItem)itemToDelete);
		}else if (itemToDelete instanceof AttributeTreeNode){
			treeNodeDelete(currentSession, (AttributeTreeNode)itemToDelete);
		}
		
		currentSession.flush();
		
	}
	
	/**
	 * remove any nodes that reference the category
	 * @param currentSession
	 * @param c
	 */
	private void categoryDelete(Session currentSession, Category c){
		List<CmNode> nodes = currentSession.createCriteria(CmNode.class).add(Restrictions.eq("category", c)).list(); //$NON-NLS-1$
		for (CmNode n : nodes){
			if (n.getParent() == null){
				currentSession.delete(n);	
			}else{
				n.getParent().getChildren().remove(n);
				n.setParent(null);
			}
			
		}
	}
	
	/**
	 * remove any attributes whose owner nodes
	 * represents the given category or any child of the given
	 * category.
	 * @param currentSession
	 * @param ca
	 */
	private void attributeDelete(Session currentSession, CategoryAttribute ca){
		deleteAttribute(currentSession, ca.getCategory(), ca.getAttribute());
		List<Category> kids = new ArrayList<Category>();
		kids.addAll(ca.getCategory().getChildren());
		while(kids.size() > 0){
			Category kid = kids.remove(0);
			deleteAttribute(currentSession, kid, ca.getAttribute());
			kids.addAll(kid.getChildren());
		}
	}
	
	private void deleteAttribute(Session currentSession, Category category, Attribute attribute){
		Query q = currentSession.createQuery(
				"FROM CmAttribute a WHERE a.attribute = :attribute and a.node.category = :category"); //$NON-NLS-1$
		q.setParameter("attribute", attribute); //$NON-NLS-1$
		q.setParameter("category", category); //$NON-NLS-1$
		
		List<CmAttribute> attributes = q.list();
		for (CmAttribute a : attributes){
			a.setAttribute(null);
			a.getNode().getCmAttributes().remove(a);
			
			//update the order of other attribute
			for (int i = 0; i < a.getNode().getCmAttributes().size(); i ++){
				a.getNode().getCmAttributes().get(i).setOrder(i);
			}
		}
	}
	
	/**
	 * remove list item configurations
	 * @param currentSession
	 * @param li
	 */
	private void listItemDelete(Session currentSession, AttributeListItem li){
		List<CmAttributeListItem> items = currentSession.createCriteria(CmAttributeListItem.class)
			.add(Restrictions.eq("listItem", li)).list(); //$NON-NLS-1$
		for (CmAttributeListItem item: items){
			currentSession.delete(item);
		}
		deleteAttributeOption(currentSession, li.getUuid());
	}

	/**
	 * remove tree node configurations
	 * @param currentSession
	 * @param node
	 */
	private void treeNodeDelete(Session currentSession, AttributeTreeNode node){
		List<CmAttributeTreeNode> nodes = currentSession.createCriteria(CmAttributeTreeNode.class)
			.add(Restrictions.eq("dmTreeNode", node)).list(); //$NON-NLS-1$
		for (CmAttributeTreeNode tnode : nodes){
			currentSession.delete(tnode);
		}
		deleteAttributeOption(currentSession, node.getUuid());
	}
	
	/**
	 * remove attribute option
	 * @param currentSession
	 * @param uuidValue
	 */
	private void deleteAttributeOption(Session currentSession, UUID uuidValue){
		Query q = currentSession.createQuery("From CmAttributeOption WHERE uuidValue = :uuid"); //$NON-NLS-1$
		q.setParameter("uuid", uuidValue); //$NON-NLS-1$
		List<CmAttributeOption> ops = q.list();
		for (CmAttributeOption o : ops){
			o.getCmAttribute().getCmAttributeOptions().remove(o.getOptionId());
			o.setCmAttribute(null);
		}
		
	}

	@Override
	public void addItem(Session currentSession, Object itemToAdd) {
		if (itemToAdd instanceof CategoryAttribute){
			//for each node the links to the category we need to add a new attribute node
			CategoryAttribute ca = (CategoryAttribute)itemToAdd;
			if (!ca.getIsActive()){
				return;
			}
			addAttributeToCategory(currentSession, ca.getCategory(), ca.getAttribute());
			List<Category> kids = new ArrayList<Category>();
			kids.addAll(ca.getCategory().getChildren());
			while(kids.size() > 0){
				Category c = kids.remove(0);
				addAttributeToCategory(currentSession, c, ca.getAttribute());
				kids.addAll(c.getChildren());
			}
		}
	}
	private void addAttributeToCategory(Session currentSession, Category category, Attribute attribute){
		List<CmNode> nodes = currentSession.createCriteria(CmNode.class).add(Restrictions.eq("category", category)).list(); //$NON-NLS-1$
		for (CmNode node : nodes){
			//ensure attribute doesn't already exist
			boolean add = true;
			for (CmAttribute cmAttribute : node.getCmAttributes()){
				if (cmAttribute.getAttribute().equals(attribute)){
					add = false; //already exists
				}
			}
			if (add){
				CmAttribute newAttribute = new CmAttribute();
				newAttribute.setAttribute(attribute);
				for (org.wcs.smart.ca.Label label : attribute.getNames()) { //we need a copy, not the same instance of set
					newAttribute.updateName(label.getLanguage(), label.getValue());
				}
				newAttribute.setNode(node);
				newAttribute.setOrder(node.getCmAttributes().size());
				newAttribute.setCmAttributeOptions(CmAttributeOptionFactory.buildDefaultOptions(newAttribute, attribute.getType()));
				node.getCmAttributes().add(newAttribute);
				
				currentSession.saveOrUpdate(newAttribute);
			}
		}
	}

	/**
	 * Here we only care if an attribute was enabled as it must be added.
	 * Disabled attributes are dealt with when save is called in
	 * the DataModelListener.
	 * 
	 */
	@Override
	public void itemEnabledStateChanged(Session currentSession, Object itemToAdd) {
		
		if (itemToAdd instanceof CategoryAttribute){
			CategoryAttribute ca = (CategoryAttribute)itemToAdd;
			if (ca.getIsActive()){
				//enabled; add to category
				addItem(currentSession, ca);
			}

		}

		
	}
}
