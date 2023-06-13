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

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Interceptor;
import org.hibernate.query.Query;
import org.wcs.smart.ca.ConservationAreaClonerEngine;
import org.wcs.smart.ca.IConservationAreaTemplateCloner;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.dataentry.dialog.AssociatedImageInterceptor;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeConfig;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.QueryFactory;

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
	public Collection<? extends Interceptor> getInterceptors() {
		return Arrays.asList(new AssociatedImageInterceptor());
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
		clone.setDisplayMode(cm.getDisplayMode());
		clone.setInstantGps(cm.isInstantGps());
		clone.setPhotoFirst(cm.isPhotoFirst());
		
		//look for iconset in new ca
		if (clone.getIconSet() != null) {
			IconSet caIconSet = QueryFactory.buildQuery(engine.getSession(), IconSet.class, 
					new Object[] {"conservationArea", engine.getNewCa()}, //$NON-NLS-1$
					new Object[] {"keyId", clone.getIconSet().getKeyId()}).uniqueResult(); //$NON-NLS-1$
			clone.setIconSet(caIconSet);
		}
		
		engine.copyLabels(cm, clone);
		engine.getSession().persist(clone);
		
		cloneCmAttributeConfigs(cm, clone);
		//clone nodes
		for(CmNode kid : cm.getNodes()){
			processNode(null, kid, clone);
		}

		
		engine.getSession().flush();
		
		engine.addConservationItemMapping(cm, clone);
	}
	
	private void cloneCmAttributeConfigs(ConfigurableModel sourceCm, ConfigurableModel clonedCm) throws Exception {
		List<CmAttributeConfig> configsToClone = QueryFactory.buildQuery(engine.getSession(), CmAttributeConfig.class, "model", sourceCm).list();  //$NON-NLS-1$
		for (CmAttributeConfig cfg : configsToClone) {
			CmAttributeConfig clone = new CmAttributeConfig();
			engine.copyLabels(cfg, clone);
			clone.setModel(clonedCm);
			clone.setDefault(cfg.isDefault());
			clone.setDisplayMode(cfg.getDisplayMode());
			clone.setAttribute(findNewAttribute(cfg.getAttribute()));

			engine.getSession().persist(clone);
			engine.addConservationItemMapping(cfg, clone);
			engine.getSession().flush();

			cloneCmAttributeListItems(cfg, clone);
			cloneCmAttributeTreeItems(cfg, clone);
		}
	}

	private void cloneCmAttributeListItems(CmAttributeConfig sourceCfg, CmAttributeConfig clonedCfg) throws Exception {
		List<CmAttributeListItem> itemsToClone = QueryFactory.buildQuery(engine.getSession(), CmAttributeListItem.class, "config", sourceCfg).list();  //$NON-NLS-1$
		for (CmAttributeListItem listItem : itemsToClone){
			CmAttributeListItem clone = new CmAttributeListItem();
			clone.setConfig(clonedCfg);
			engine.copyLabels(listItem, clone);
			clone.setIsActive(listItem.getIsActive());
			clone.setListOrder(listItem.getListOrder());
			if (listItem.getListItem() != null){
				clone.setListItem(findNewAttributeListItem(listItem.getListItem()));
			}else{
				clone.setListItem(null);
			}
			Path imgFile = listItem.getImageFile();
			if (imgFile != null && Files.exists(imgFile)) {
				clone.setImageFile(listItem.getImageFile());
			}
			engine.getSession().persist(clone);
			engine.addConservationItemMapping(listItem, clone);
		}
		engine.getSession().flush();
	}
	
	private void cloneCmAttributeTreeItems(CmAttributeConfig sourceCfg, CmAttributeConfig clonedCfg) throws Exception {
		List<CmAttributeTreeNode> itemsToClone = QueryFactory.buildQuery(engine.getSession(), CmAttributeTreeNode.class, "config", sourceCfg).list();  //$NON-NLS-1$

		HashMap<CmAttributeTreeNode, CmAttributeTreeNode> templateToClone = new HashMap<CmAttributeTreeNode, CmAttributeTreeNode>();
		HashMap<CmAttributeTreeNode, CmAttributeTreeNode> cloneTotemplate = new HashMap<CmAttributeTreeNode, CmAttributeTreeNode>();
		
		for (CmAttributeTreeNode treeItem : itemsToClone){
			CmAttributeTreeNode clone = new CmAttributeTreeNode();
			engine.copyLabels(treeItem, clone);
			clone.setIsActive(treeItem.getIsActive());
			clone.setNodeOrder(treeItem.getNodeOrder());
			if (treeItem.getDmTreeNode() != null){
				clone.setDmTreeNode(findNewAttributeTreeNode(treeItem.getDmTreeNode()));
			}else{
				clone.setDmTreeNode(null);
			}
			clone.setConfig(clonedCfg);
			clone.setDisplayMode(treeItem.getDisplayMode());
			Path imgFile = treeItem.getImageFile();
			if (imgFile != null && Files.exists(imgFile)) {
				clone.setImageFile(treeItem.getImageFile());
			}
			clone.setChildren(new ArrayList<CmAttributeTreeNode>());
			engine.getSession().persist(clone);
			engine.addConservationItemMapping(treeItem, clone);
			
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
		clonedNode.setCollectMultipleObservations(toCopy.isCollectMultipleObservations());
		clonedNode.setUseSingleGpsPoint(toCopy.isUseSingleGpsPoint());
		clonedNode.setDisplayMode(toCopy.getDisplayMode());
		
		Path imgFile = toCopy.getImageFile();
		if (imgFile != null && Files.exists(imgFile)) {
			clonedNode.setImageFile(toCopy.getImageFile());
		}
		
		if (toCopy.getCategory() != null){
			clonedNode.setCategory(findNewCategory(toCopy.getCategory()));
		}else{
			clonedNode.setCategory(null);
		}
		if (toCopy.getSignatureUuids() != null) {
			Set<SignatureType> stypes = new HashSet<>();
			for (UUID cuuid : toCopy.getSignatureUuids()) {
				SignatureType temp = new SignatureType();
				temp.setUuid(cuuid);
				SignatureType newItem = engine.getNewConservationItem(temp);
				if (newItem != null) stypes.add(newItem);
			}
			clonedNode.setSignatures(stypes);
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
		engine.getSession().flush();
		engine.addConservationItemMapping(toCopy, clonedNode);

		//process kids
		for (CmNode kid : toCopy.getChildren()){
			processNode(clonedNode,kid,clonedModel);
		}
		
	}
	
	private CmAttribute cloneAttribute(CmAttribute attributeToClone) throws Exception{
		CmAttribute clone = new CmAttribute();
		
		clone.setAttribute(findNewAttribute(attributeToClone.getAttribute()));
		if (attributeToClone.getConfig() != null) {
			clone.setConfig((CmAttributeConfig)engine.getNewConservationItem(attributeToClone.getConfig()));
		}
		
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
		List<Category> categories = QueryFactory.buildQuery(engine.getSession(), Category.class,
				new Object[] {"conservationArea", engine.getNewCa()}, //$NON-NLS-1$
				new Object[] {"hkey", oldCategory.getHkey()}).list(); //$NON-NLS-1$
		if (categories.size() == 1){
			return (Category) categories.get(0);
		}
		throw new Exception(MessageFormat.format(Messages.CmTemplateCloner_CategoryNotFoundErrpr,new Object[]{oldCategory.getHkey()}));
	}
	
	private Attribute findNewAttribute(Attribute oldAttribute) throws Exception{
		List<Attribute> attributes = QueryFactory.buildQuery(engine.getSession(), Attribute.class,
				new Object[] {"conservationArea", engine.getNewCa()}, //$NON-NLS-1$
				new Object[] {"keyId", oldAttribute.getKeyId()}).list(); //$NON-NLS-1$
		if (attributes.size() == 1){
			return (Attribute) attributes.get(0);
		}
		throw new Exception(MessageFormat.format(Messages.CmTemplateCloner_AttributeNotFoundError,new Object[]{oldAttribute.getKeyId()}));
	}
	
	private AttributeListItem findNewAttributeListItem(AttributeListItem oldListItem) throws Exception{
		Query<AttributeListItem> q = engine.getSession().createQuery("From AttributeListItem a WHERE a.attribute.conservationArea = :ca and a.keyId = :key and a.attribute.keyId = :attributeKey", AttributeListItem.class); //$NON-NLS-1$
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
		Query<AttributeTreeNode> q = engine.getSession().createQuery("From AttributeTreeNode a WHERE a.attribute.conservationArea = :ca and a.hkey = :key and a.attribute.keyId = :attributeKey", AttributeTreeNode.class); //$NON-NLS-1$
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
