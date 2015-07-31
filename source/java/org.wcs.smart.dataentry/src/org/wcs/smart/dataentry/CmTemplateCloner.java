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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Query;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Cloner for copying configurable models
 * 
 * @author Emily
 *
 */
public class CmTemplateCloner implements IConservationAreaTemplateCloner {

	private ConservationAreaClonerEngine engine;
	public CmTemplateCloner() {
	}

	@Override
	public void cloneTemplateData(ConservationAreaClonerEngine engine,
			IProgressMonitor monitor) throws Exception {
		this.engine = engine;
		
		List<ConfigurableModel> models = DataentryHibernateManager.getConfigurableModels(engine.getTemplateCa(), engine.getSession());
		monitor.beginTask(Messages.CmTemplateCloner_CopyProgress, models.size());
		for (ConfigurableModel m : models){
			monitor.subTask(MessageFormat.format(Messages.CmTemplateCloner_CopyConfigModelName, new Object[]{m.getName()}));
			cloneConfigurableModel(m, engine);
			monitor.worked(1);
		}
		monitor.done();
	}

	private void cloneConfigurableModel(ConfigurableModel cm, ConservationAreaClonerEngine engine) throws Exception{
		
		ConfigurableModel clone = new ConfigurableModel();
		clone.setConservationArea(engine.getNewCa());
		engine.copyLabels(cm, clone);
		engine.getSession().saveOrUpdate(clone);
		
		//clone nodes
		for(CmNode kid : cm.getNodes()){
			processNode(null, kid, clone);
		}
		cloneCmAttributeListItems(cm, clone);
		cloneCmAttributeTreeItems(cm, clone);
		
		engine.getSession().saveOrUpdate(clone);
		engine.getSession().flush();
		
		engine.addConservationItemMapping(cm, clone);
	}
	
	private void cloneCmAttributeListItems(ConfigurableModel sourceCm, ConfigurableModel clonedCm) throws Exception{
		@SuppressWarnings("unchecked")
		List<CmAttributeListItem> itemsToClone = engine.getSession().createCriteria(CmAttributeListItem.class).add(Restrictions.eq("configurableModel", sourceCm)).list(); //$NON-NLS-1$
		for (CmAttributeListItem listItem : itemsToClone){
			CmAttributeListItem clone = new CmAttributeListItem();
			clone.setConfigurableModel(clonedCm);
			engine.copyLabels(listItem, clone);
			clone.setIsActive(listItem.getIsActive());
			clone.setListOrder(listItem.getListOrder());
			if (listItem.getListItem() != null){
				clone.setListItem(findNewAttributeListItem(listItem.getListItem()));
			}else{
				clone.setListItem(null);
			}
			if (listItem.getDmAttribute() != null){
				clone.setDmAttribute(findNewAttribute(listItem.getDmAttribute()));
			}else{
				clone.setDmAttribute(null);
			}
			clone.setAttribute((CmAttribute)engine.getNewConservationItem(listItem.getAttribute()));
			engine.getSession().saveOrUpdate(clone);
		}
		engine.getSession().flush();
	}
	
	private void cloneCmAttributeTreeItems(ConfigurableModel sourceCm, ConfigurableModel clonedCm) throws Exception{
		@SuppressWarnings("unchecked")
		List<CmAttributeTreeNode> itemsToClone = engine.getSession().createCriteria(CmAttributeTreeNode.class).add(Restrictions.eq("configurableModel", sourceCm)).list(); //$NON-NLS-1$
		
		HashMap<CmAttributeTreeNode, CmAttributeTreeNode> templateToClone = new HashMap<CmAttributeTreeNode, CmAttributeTreeNode>();
		HashMap<CmAttributeTreeNode, CmAttributeTreeNode> cloneTotemplate = new HashMap<CmAttributeTreeNode, CmAttributeTreeNode>();
		
		for (CmAttributeTreeNode treeItem : itemsToClone){
			CmAttributeTreeNode clone = new CmAttributeTreeNode();
			clone.setConfigurableModel(clonedCm);
			engine.copyLabels(treeItem, clone);
			clone.setIsActive(treeItem.getIsActive());
			clone.setNodeOrder(treeItem.getNodeOrder());
			if (treeItem.getDmTreeNode() != null){
				clone.setDmTreeNode(findNewAttributeTreeNode(treeItem.getDmTreeNode()));
			}else{
				clone.setDmTreeNode(null);
			}
			if (treeItem.getDmAttribute() != null){
				clone.setDmAttribute(findNewAttribute(treeItem.getDmAttribute()));
			}else{
				clone.setDmAttribute(null);
			}
			clone.setAttribute((CmAttribute)engine.getNewConservationItem(treeItem.getAttribute()));
			clone.setChildren(new ArrayList<CmAttributeTreeNode>());
			engine.getSession().saveOrUpdate(clone);
			
			//at this point we don't necessarily have the
			//parent cloned; so we need to set up
			//the parent after we clone all items
			templateToClone.put(treeItem, clone);
			cloneTotemplate.put(clone, treeItem);
		}
		
		//configure parents
		for(CmAttributeTreeNode treenode : cloneTotemplate.keySet()){
			CmAttributeTreeNode templateNode = cloneTotemplate.get(treenode);
			if (templateNode != null && templateNode.getParent() != null){
				CmAttributeTreeNode cparent = templateToClone.get(templateNode.getParent());
				if (cparent != null){
					treenode.setParent(cparent);
					cparent.getChildren().add(treenode);
				}
			}
		}
		
		engine.getSession().flush();
	}

	private void processNode(CmNode clonedParent, CmNode toCopy, ConfigurableModel clonedModel) throws Exception{
		
		CmNode clonedNode = new CmNode();
		
		clonedNode.setModel(clonedModel);
		clonedNode.setNodeOrder(toCopy.getNodeOrder());
		clonedNode.setPhotoAllowed(toCopy.isPhotoAllowed());
		clonedNode.setPhotoRequired(toCopy.isPhotoRequired());
		
		if (toCopy.getCategory() != null){
			clonedNode.setCategory(findNewCategory(toCopy.getCategory()));
		}else{
			clonedNode.setCategory(null);
		}
		engine.copyLabels(toCopy, clonedNode);
		for (CmAttribute att : toCopy.getCmAttributes()){
			CmAttribute clonedAtt = cloneAttribute(att);
			clonedAtt.setNode(clonedNode);
			clonedNode.getCmAttributes().add(clonedAtt);
			
			engine.getSession().flush();
			engine.addConservationItemMapping(att, clonedAtt);
		}
		
		
		//add to parent
		if (clonedParent != null){
			clonedNode.setParent(clonedParent);
			clonedParent.getChildren().add(clonedNode);
		}else{
			clonedModel.getNodes().add(clonedNode);
		}
		
		//process kids
		for (CmNode kid : toCopy.getChildren()){
			processNode(clonedNode,kid,clonedModel);
		}
		
	}
	
	private CmAttribute cloneAttribute(CmAttribute attributeToClone) throws Exception{
		CmAttribute clone = new CmAttribute();
		
		clone.setAttribute(findNewAttribute(attributeToClone.getAttribute()));
		
		//clone options
		for (CmAttributeOption op : attributeToClone.getCmAttributeOptions().values()){
			CmAttributeOption clonedOp = cloneOption(op);
			clonedOp.setCmAttribute(clone);
			clone.getCmAttributeOptions().put(clonedOp.getOptionId(), clonedOp);
		}
		engine.copyLabels(attributeToClone, clone);
		clone.setOrder(attributeToClone.getOrder());
		
		return clone;
	}
	
	private CmAttributeOption cloneOption(CmAttributeOption op) throws Exception{
		CmAttributeOption cloned = new CmAttributeOption();
		cloned.setBooleanValue(op.getBooleanValue());
		cloned.setDoubleValue(op.getDoubleValue());
		cloned.setOptionId(op.getOptionId());
		cloned.setStringValue(op.getStringValue());
		if (op.getUuidValue() != null){
			cloned.setUuidValue(findNewUuidItem(op.getUuidValue()));
			
		}

		return cloned;
	}
	
	
	private Category findNewCategory(Category oldCategory) throws Exception{
		List<?> categories = engine.getSession().createCriteria(Category.class).add(Restrictions.eq("conservationArea", engine.getNewCa())).add(Restrictions.eq("hkey", oldCategory.getHkey())).list();  //$NON-NLS-1$//$NON-NLS-2$
		if (categories.size() == 1){
			return (Category) categories.get(0);
		}
		throw new Exception(MessageFormat.format(Messages.CmTemplateCloner_CategoryNotFoundErrpr,new Object[]{oldCategory.getHkey()}));
	}
	
	private Attribute findNewAttribute(Attribute oldAttribute) throws Exception{
		List<?> attributes = engine.getSession().createCriteria(Attribute.class).add(Restrictions.eq("conservationArea", engine.getNewCa())).add(Restrictions.eq("keyId", oldAttribute.getKeyId())).list(); //$NON-NLS-1$ //$NON-NLS-2$
		if (attributes.size() == 1){
			return (Attribute) attributes.get(0);
		}
		throw new Exception(MessageFormat.format(Messages.CmTemplateCloner_AttributeNotFoundError,new Object[]{oldAttribute.getKeyId()}));
	}
	
	private AttributeListItem findNewAttributeListItem(AttributeListItem oldListItem) throws Exception{
		Query q = engine.getSession().createQuery("From AttributeListItem a WHERE a.attribute.conservationArea = :ca and a.keyId = :key and a.attribute.keyId = :attributeKey"); //$NON-NLS-1$
		q.setParameter("ca", engine.getNewCa()); //$NON-NLS-1$
		q.setParameter("key", oldListItem.getKeyId()); //$NON-NLS-1$
		q.setParameter("attributeKey", oldListItem.getAttribute().getKeyId()); //$NON-NLS-1$
		List<?> items = q.list();
		if (items.size() == 1){
			return (AttributeListItem) items.get(0);
		}
		//this may be an entity list item that is not cloned
		return null;
		
		
	}
	
	private AttributeTreeNode findNewAttributeTreeNode(AttributeTreeNode oldTreeNode) throws Exception{
		Query q = engine.getSession().createQuery("From AttributeTreeNode a WHERE a.attribute.conservationArea = :ca and a.hkey = :key and a.attribute.keyId = :attributeKey"); //$NON-NLS-1$
		q.setParameter("ca", engine.getNewCa()); //$NON-NLS-1$
		q.setParameter("key", oldTreeNode.getHkey()); //$NON-NLS-1$
		q.setParameter("attributeKey", oldTreeNode.getAttribute().getKeyId()); //$NON-NLS-1$
		List<?> items = q.list();
		if (items.size() == 1){
			return (AttributeTreeNode) items.get(0);
		}
		throw new Exception(MessageFormat.format(Messages.CmTemplateCloner_TreeNodeNotFoundError,new Object[]{oldTreeNode.getHkey()}));
	}
	
	private UUID findNewUuidItem(UUID oldUuidItem) throws Exception{
		//search list nodes
		AttributeListItem li = (AttributeListItem) engine.getSession().get(AttributeListItem.class, oldUuidItem);
		if (li != null){
			AttributeListItem f = findNewAttributeListItem(li);
			if (f == null){
				return null;
			}
			return findNewAttributeListItem(li).getUuid();
		}
		
		//search tree nodes
		AttributeTreeNode treeNode = (AttributeTreeNode) engine.getSession().get(AttributeTreeNode.class, oldUuidItem);
		if (treeNode != null){
			return findNewAttributeTreeNode(treeNode).getUuid();
		}
		
		//search all other uuid fields in the engine
		UuidItem i = engine.getNewConservationItem(oldUuidItem);
		if (i != null){
			return i.getUuid();
		}
		return null;
		
	}
}
