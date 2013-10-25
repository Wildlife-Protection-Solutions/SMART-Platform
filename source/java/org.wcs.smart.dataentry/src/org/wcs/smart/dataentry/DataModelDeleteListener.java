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

import java.util.List;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.IDataModelDeleteListener;
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
public class DataModelDeleteListener implements IDataModelDeleteListener {

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
	 * represents the given category
	 * @param currentSession
	 * @param ca
	 */
	private void attributeDelete(Session currentSession, CategoryAttribute ca){
		Query q = currentSession.createQuery(
				"FROM CmAttribute a WHERE a.attribute = :attribute and a.node.category = :category"); //$NON-NLS-1$
		q.setParameter("attribute", ca.getAttribute()); //$NON-NLS-1$
		q.setParameter("category", ca.getCategory()); //$NON-NLS-1$
		
		List<CmAttribute> attributes = q.list();
		for (CmAttribute a : attributes){
			currentSession.delete(a);
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
	private void deleteAttributeOption(Session currentSession, byte[] uuidValue){
		Query q = currentSession.createQuery("From CmAttributeOption WHERE uuidValue = :uuid"); //$NON-NLS-1$
		q.setParameter("uuid", uuidValue); //$NON-NLS-1$
		List<CmAttributeOption> ops = q.list();
		for (CmAttributeOption o : ops){
			o.getCmAttribute().getCmAttributeOptions().remove(o.getOptionId());
			o.setCmAttribute(null);
		}
		
	}
}
