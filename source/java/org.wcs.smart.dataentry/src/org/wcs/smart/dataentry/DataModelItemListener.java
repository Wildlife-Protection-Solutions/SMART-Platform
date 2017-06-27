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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.transaction.spi.LocalStatus;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.IDataModelItemListener;
import org.wcs.smart.dataentry.dialog.AssociatedImageInterceptor;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

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
		
		//we cannot add the associatedimageinterceptor to the session as the session is provider by
		//another plugin so here we manually use it to ensure images are deleted when nodes are deleted
		//This is not ideal,
		AssociatedImageInterceptor interceptor = new AssociatedImageInterceptor();
		currentSession.getTransaction().registerSynchronization(new Synchronization() {
			@Override
			public void beforeCompletion() {
			}
			
			@Override
			public void afterCompletion(int status) {
				if (status == javax.transaction.Status.STATUS_COMMITTED){
					//we need any empty transaction that returns true for wasCommitted
					//for the itercceptor
					Transaction tx = new Transaction(){

						@Override
						public boolean isInitiator() { return false; }
						@Override
						public void begin() { }
						@Override
						public void commit() { }
						@Override
						public void rollback() { }
						@Override
						public LocalStatus getLocalStatus() { return null; }
						@Override
						public boolean isActive() { return false; }
						@Override
						public boolean isParticipating() { return false; }
						@Override
						public boolean wasCommitted() { return true; }
						@Override
						public boolean wasRolledBack() { return false; }
						@Override
						public void registerSynchronization( Synchronization synchronization) throws HibernateException { }
						@Override
						public void setTimeout(int seconds) { }
						@Override
						public int getTimeout() { return 0; }
						
					};
					interceptor.afterTransactionCompletion(tx);
				}
				
			}
		});
		if (itemToDelete instanceof Category){
			categoryDelete(currentSession, (Category)itemToDelete, interceptor);
		}else if (itemToDelete instanceof CategoryAttribute){
			attributeDelete(currentSession, (CategoryAttribute)itemToDelete, interceptor);
		}else if (itemToDelete instanceof AttributeListItem){
			listItemDelete(currentSession, (AttributeListItem)itemToDelete, interceptor);
		}else if (itemToDelete instanceof AttributeTreeNode){
			treeNodeDelete(currentSession, (AttributeTreeNode)itemToDelete, interceptor);
		}
		
		currentSession.flush();
		
	}
	
	/**
	 * remove any nodes that reference the category
	 * @param currentSession
	 * @param c
	 */
	private void categoryDelete(Session currentSession, Category c, AssociatedImageInterceptor interceptor ){
		List<CmNode> nodes = currentSession.createCriteria(CmNode.class).add(Restrictions.eq("category", c)).list(); //$NON-NLS-1$
		for (CmNode n : nodes){
			if (n.getParent() == null){
				currentSession.delete(n);	
				interceptor.onDelete(n, n.getUuid(), null, null, null);
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
	private void attributeDelete(Session currentSession, CategoryAttribute ca, AssociatedImageInterceptor interceptor){
		deleteAttribute(currentSession, ca.getCategory(), ca.getAttribute(), interceptor);
		List<Category> kids = new ArrayList<Category>();
		kids.addAll(ca.getCategory().getChildren());
		while(kids.size() > 0){
			Category kid = kids.remove(0);
			deleteAttribute(currentSession, kid, ca.getAttribute(), interceptor);
			kids.addAll(kid.getChildren());
		}
	}
	
	private void deleteAttribute(Session currentSession, Category category, Attribute attribute, AssociatedImageInterceptor interceptor){
		Query q = currentSession.createQuery(
				"FROM CmAttribute a WHERE a.attribute = :attribute and a.node.category = :category"); //$NON-NLS-1$
		q.setParameter("attribute", attribute); //$NON-NLS-1$
		q.setParameter("category", category); //$NON-NLS-1$
		
		List<CmAttribute> attributes = q.list();
		for (CmAttribute a : attributes){
			a.setAttribute(null);
			a.getNode().getCmAttributes().remove(a);
			
			interceptor.onDelete(a, a.getUuid(),null, null, null);
			//register delete on children (ensure images are removed)
			if (a.getList() != null){
				for (CmAttributeListItem l : a.getList()){
					interceptor.onDelete(l, l.getUuid(),null, null, null);
				}
			}
			if (a.getTree() != null){
				List<CmAttributeTreeNode> nodes = new ArrayList<>();
				nodes.addAll(a.getTree());
				while(!nodes.isEmpty()){
					CmAttributeTreeNode n = nodes.remove(0);
					interceptor.onDelete(n, n.getUuid(),null, null, null);
					nodes.addAll(n.getChildren());
				}
			}
			
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
	private void listItemDelete(Session currentSession, AttributeListItem li, AssociatedImageInterceptor interceptor){
		List<CmAttributeListItem> items = currentSession.createCriteria(CmAttributeListItem.class)
			.add(Restrictions.eq("listItem", li)).list(); //$NON-NLS-1$
		for (CmAttributeListItem item: items){
			currentSession.delete(item);
			interceptor.onDelete(item, item.getUuid(),null, null, null);
		}
		deleteAttributeOption(currentSession, li.getUuid());
	}

	/**
	 * remove tree node configurations
	 * @param currentSession
	 * @param node
	 */
	private void treeNodeDelete(Session currentSession, AttributeTreeNode node, AssociatedImageInterceptor interceptor){
		List<CmAttributeTreeNode> nodes = currentSession.createCriteria(CmAttributeTreeNode.class)
			.add(Restrictions.eq("dmTreeNode", node)).list(); //$NON-NLS-1$
		for (CmAttributeTreeNode tnode : nodes){
			currentSession.delete(tnode);
			interceptor.onDelete(tnode, tnode.getUuid(), null, null, null);
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
		if (itemToAdd instanceof CategoryAttribute) {
			addCategoryAttribute(currentSession, (CategoryAttribute)itemToAdd);
		} else if (itemToAdd instanceof AttributeListItem) {
			addAttributeListItem(currentSession, (AttributeListItem)itemToAdd);
		}
	}
	
	private void addCategoryAttribute(Session currentSession, CategoryAttribute ca) {
		//for each node the links to the category we need to add a new attribute node
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
	
	private void addAttributeToCategory(Session currentSession, Category category, Attribute attribute){
		List<CmNode> nodes = currentSession.createCriteria(CmNode.class).add(Restrictions.eq("category", category)).list(); //$NON-NLS-1$
		Set<ConfigurableModel> changedModels = new HashSet<>();
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
				
				ConfigurableModel cm = node.getModel();
				if (AttributeType.TREE.equals(attribute.getType()) || AttributeType.LIST.equals(attribute.getType())) {
					CmAttributeOption option = CmAttributeOptionFactory.createCustomCofigOption(newAttribute);
					option.setBooleanValue(true);
					newAttribute.getCmAttributeOptions().put(CmAttributeOption.ID_CUSTOM_CONFIG, option);
					if (!changedModels.contains(cm)) {
						//create default configurations if needed
						changedModels.add(cm);
						if (AttributeType.TREE.equals(attribute.getType())) {
							ensureDefaultTreeExists(currentSession, cm, attribute);
						} else if (AttributeType.LIST.equals(attribute.getType())) {
							ensureDefaultListExists(currentSession, cm, attribute);
						}
					}
					//create custom configurations
					if (AttributeType.TREE.equals(attribute.getType())) {
						List<CmAttributeTreeNode> customTree = CmCustomTreesUtil.buildCustomTree(cm, newAttribute, attribute);
						newAttribute.setTree(customTree);
					} else if (AttributeType.LIST.equals(attribute.getType())) {
						List<CmAttributeListItem> customList = CmCustomListsUtil.buildCustomList(cm, newAttribute, attribute);
						newAttribute.setList(customList);
					}
				}

				node.getCmAttributes().add(newAttribute);
				currentSession.saveOrUpdate(newAttribute);
			}
		}
	}

	private void ensureDefaultTreeExists(Session s, ConfigurableModel m, Attribute a) {
		Set<Attribute> existingTrees = CmDefaultTreesUtil.getPresentedTreeAttributes(m);
		if (!existingTrees.contains(a)) {
			List<CmAttributeTreeNode> defTree = CmDefaultTreesUtil.buildDefaultTree(m, a);
			m.getDefaultTrees().addAll(defTree);
			s.saveOrUpdate(m);
		}
	}

	private void ensureDefaultListExists(Session s, ConfigurableModel m, Attribute a) {
		Set<Attribute> existingLists = CmDefaultListsUtil.getPresentedListAttributes(m);
		if (!existingLists.contains(a)) {
			List<CmAttributeListItem> defList = CmDefaultListsUtil.buildDefaultList(m, a);
			m.getDefaultLists().addAll(defList);
			s.saveOrUpdate(m);
		}
	}
	
	private void addAttributeListItem(Session currentSession, AttributeListItem dmListItem) {
		List<CmAttribute> attrList = currentSession.createCriteria(CmAttribute.class).add(Restrictions.eq("attribute", dmListItem.getAttribute())).list(); //$NON-NLS-1$
		Set<CmAttribute> attributes = new HashSet<>(attrList); //this will remove duplicates
		Set<ConfigurableModel> processedCMs = new HashSet<>();
		for (CmAttribute a : attributes) {
			ConfigurableModel cm = a.getNode().getModel();
			if (!processedCMs.contains(cm)) {
				//need to add new CmAttributeListItem to default lists configuration
				processedCMs.add(cm);
				CmAttributeListItem li = createCmAttributeListItem(dmListItem, cm);
				li.setDmAttribute(dmListItem.getAttribute());
				currentSession.saveOrUpdate(li);
			}
			if (a.isUseCustomConfig()) {
				//need to add new CmAttributeListItem to custom list configuration
				CmAttributeListItem li = createCmAttributeListItem(dmListItem, cm);
				li.setAttribute(a);
				currentSession.saveOrUpdate(li);
			}
		}
	}
	
	/**
	 * Notice that returned item is not fully filled.
	 * DM or CM attribute must be additionally assigned to it
	 */
	private CmAttributeListItem createCmAttributeListItem(AttributeListItem dmListItem, ConfigurableModel model) {
		CmAttributeListItem cmListItem = new CmAttributeListItem();
		cmListItem.setConfigurableModel(model);
		cmListItem.setListItem(dmListItem);
		cmListItem.setIsActive(false); //by design
		cmListItem.setListOrder(dmListItem.getListOrder());
		return cmListItem;
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
