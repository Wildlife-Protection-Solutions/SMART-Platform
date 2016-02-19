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
package org.wcs.smart.dataentry.dialog;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeOption;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Factory for creating configurable data models.
 * 
 * @author Emily
 *
 */
public class ConfigurableModelFactory {

	/**
	 * Creates an empty configurable model with the given name.
	 * 
	 * @param name configurable model name
	 * @return
	 */
	public static ConfigurableModel createBlankModel(String name){
		ConfigurableModel cm = new ConfigurableModel();
		cm.setConservationArea(SmartDB.getCurrentConservationArea());
		cm.setName(name);
		cm.updateName(SmartDB.getCurrentLanguage(), cm.getName());
		return cm;
	}
	
	/**
	 * Creates a configurable model using another configurable model as a template.
	 * @return
	 */
	public static ConfigurableModel createConfigurableModelClone(ConfigurableModel cm, String name, IProgressMonitor monitor) {
		monitor.beginTask(Messages.ConfigurableModelFactory_TaskName, 1);
		
		monitor.subTask(Messages.ConfigurableModelFactory_BlankCmTaskName);
		ConfigurableModel clone = createBlankModel(name);
		
		int count = 0;
		Queue<CmNode> toProcess = new LinkedList<CmNode>();
		toProcess.addAll(cm.getNodes());
		while(!toProcess.isEmpty()) {
			CmNode node = toProcess.remove();
			count ++;
			toProcess.addAll(node.getChildren());
		}
		monitor.beginTask(Messages.ConfigurableModelFactory_TaskName, count + 2); //+2 is for default lists and trees
		
		//clone nodes
		for(CmNode kid : cm.getNodes()){
			processNode(null, kid, clone, monitor);
		}
		if (monitor.isCanceled()) return null;
		monitor.subTask(Messages.ConfigurableModelFactory_ProcessDefaultLists);
		monitor.worked(1);
		clone.setDefaultLists(cloneCmAttributeList(cm.getDefaultLists(), clone, null));
		if (monitor.isCanceled()) return null;
		monitor.subTask(Messages.ConfigurableModelFactory_ProcessDefaultTrees);
		monitor.worked(1);
		clone.setDefaultTrees(cloneCmAttributeTree(cm.getDefaultTrees(), null, clone, null));

		if (monitor.isCanceled()) return null;
		monitor.done();
		return clone;
	}
	
	/**
	 * Creates an configurable model from the conservation area
	 * data model.
	 * 
	 * @param session
	 * @param monitor
	 * @return
	 */
	public static ConfigurableModel createModelFromDataModel(Session session, IProgressMonitor monitor){
		return createModelFromDataModel(Messages.ConfigurableModelPropertyDialog_ConfigurableModelDeafultName,session,monitor);
	}
	
	/**
	 * Creates a configurable model from the conservation area data model with
	 * a given name.
	 * @param name
	 * @param session
	 * @param monitor
	 * @return
	 */
	public static ConfigurableModel createModelFromDataModel(String name, Session session, IProgressMonitor monitor){
		monitor.beginTask(Messages.ConfigurableModelFactory_TaskName, 1);
		
		monitor.subTask(Messages.ConfigurableModelFactory_BlankCmTaskName);
		ConfigurableModel init = createBlankModel(name);
		
		DataModel dm = HibernateManager.loadDataModel(SmartDB.getCurrentConservationArea(), session);
		
		int catCnt = 0;
		Queue<Category> toProcess = new LinkedList<Category>();
		toProcess.addAll(dm.getActiveCategories());
		while(!toProcess.isEmpty()) {
			Category c = toProcess.remove();
			catCnt ++;
			toProcess.addAll(c.getActiveChildren());
		}
		monitor.beginTask(Messages.ConfigurableModelFactory_TaskName, catCnt);
		
		for (Category c : dm.getActiveCategories()){
			processCategory(c, null, init,monitor);
			if (monitor.isCanceled()) return null;
		}
		
		if (monitor.isCanceled()) return null;
		return init;
	}
	
	private static void processCategory(Category c, CmNode parent, ConfigurableModel model, IProgressMonitor monitor){
		monitor.subTask(MessageFormat.format(Messages.ConfigurableModelFactory_ProgressMessage, c.getName()));
		monitor.worked(1);
		c.getFullCategoryName();
		if (monitor.isCanceled()) return;
		
		if (c.getActiveChildren().size() > 0){
			//there are kids so this needs to be converted to a group
			CmNode node = new CmNode();
			node.setModel(model);
			node.setParent(parent);
			node.setName(c.getName());
			for (Label l : c.getNames()){
				node.updateName(l.getLanguage(), l.getValue());
			}
			if (parent == null){
				node.setNodeOrder(model.getNodes().size()+1);
				model.getNodes().add(node);
			}else{
				node.setNodeOrder(parent.getChildren().size()+1);
				parent.getChildren().add(node);
			}
			
			for (Category kid : c.getActiveChildren()){
				processCategory(kid, node, model, monitor);
			}
		}else{
			//this are leafs and can be added as such to model
			CmNode node = new CmNode();
			node.setModel(model);
			node.setParent(parent);
			node.setCategory(c);
			node.setName(c.getName());
			for (Label l : c.getNames()){
				node.updateName(l.getLanguage(), l.getValue());
			}
			
			List<Attribute> attrList = new ArrayList<Attribute>();
			c.getAllAttribute(attrList, true);
			for (Attribute a : attrList) {
				CmAttribute cma = new CmAttribute();
				cma.setNode(node);
				cma.setAttribute(a);
				cma.setName(a.getName());
				for (Label label : a.getNames()) { //we need a copy, not the same instance of set
					cma.updateName(label.getLanguage(), label.getValue());
				}
				cma.setOrder(node.getCmAttributes().size());
				cma.setCmAttributeOptions(CmAttributeOptionFactory.buildDefaultOptions(cma, a.getType()));
				node.getCmAttributes().add(cma);
				addAttributeDefaultValues(model, a);
				
				loadAttributeInfo(a);
			}
			if (parent == null){
				node.setNodeOrder(model.getNodes().size()+1);
				model.getNodes().add(node);
			}else{
				node.setNodeOrder(parent.getChildren().size()+1);
				parent.getChildren().add(node);
			}
		}
	}

	private static void addAttributeDefaultValues(ConfigurableModel model, Attribute a) {
		switch(a.getType()) {
		case LIST:
			model.addDefaultListItems(a);
			break;
		case TREE:
			model.addDefaultTreeModes(a);
			break;
		default:
			//nothing to add
			break;
		}
	}


	//this is adopted copy from ConservationAreaClonerEngine
	private static void copyLabels(NamedItem copyFrom, NamedItem copyTo){
		for (Label l : copyFrom.getNames()){
			Label clone = new Label();
			clone.setLanguage(l.getLanguage());
			clone.setValue(l.getValue());
			clone.setElement(copyTo);
			copyTo.getNames().add(clone);
		}
	}

	private static void loadCategoryInfo(Category c){
		while(c != null){
			c.getNames().size();
			c = c.getParent();
		}
	}
	
	//this is adopted copy from CmTemplateCloner
	private static void processNode(CmNode clonedParent, CmNode toCopy, ConfigurableModel clonedModel, IProgressMonitor monitor) {
		monitor.subTask(MessageFormat.format(Messages.ConfigurableModelFactory_ProcessCmNode, toCopy.getName()));
		monitor.worked(1);
		
		if (monitor.isCanceled()) return;
		
		CmNode clonedNode = new CmNode();
		
		clonedNode.setModel(clonedModel);
		clonedNode.setNodeOrder(toCopy.getNodeOrder());
		clonedNode.setPhotoAllowed(toCopy.isPhotoAllowed());
		clonedNode.setPhotoRequired(toCopy.isPhotoRequired());
		clonedNode.setCollectMultipleObservations(toCopy.isCollectMultipleObservations());
		clonedNode.setUseSingleGpsPoint(toCopy.isUseSingleGpsPoint());
		
		if (toCopy.getCategory() != null){
			clonedNode.setCategory(toCopy.getCategory());	
			loadCategoryInfo(toCopy.getCategory());
		}else{
			clonedNode.setCategory(null);
		}
		copyLabels(toCopy, clonedNode);
		for (CmAttribute att : toCopy.getCmAttributes()){
			CmAttribute clonedAtt = cloneAttribute(att, clonedModel);
			clonedAtt.setNode(clonedNode);
			clonedNode.getCmAttributes().add(clonedAtt);
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
			processNode(clonedNode,kid,clonedModel, monitor);
		}
	}

	private static CmAttribute cloneAttribute(CmAttribute attributeToClone, ConfigurableModel cm) {
		CmAttribute clone = new CmAttribute();
		
		clone.setAttribute(attributeToClone.getAttribute());
		attributeToClone.getAttribute().getNames().size();
		
		//clone options
		for (CmAttributeOption op : attributeToClone.getCmAttributeOptions().values()){
			CmAttributeOption clonedOp = cloneOption(op);
			clonedOp.setCmAttribute(clone);
			clone.getCmAttributeOptions().put(clonedOp.getOptionId(), clonedOp);
		}
		copyLabels(attributeToClone, clone);
		clone.setOrder(attributeToClone.getOrder());
		clone.setList(cloneCmAttributeList(attributeToClone.getList(), cm, clone));
		clone.setTree(cloneCmAttributeTree(attributeToClone.getTree(), null, cm, clone));
		
		loadAttributeInfo(attributeToClone.getAttribute());
		return clone;
	}
	
	private static void loadAttributeInfo(Attribute attribute){
		attribute.getNames().size();
		
		if (attribute.getActiveListItems() != null){
			for (AttributeListItem i : attribute.getActiveListItems()){
				i.getNames().size();
			}
		}
		loadTreeNodes(attribute.getActiveTreeNodes());
	}
	private static void loadTreeNodes(List<AttributeTreeNode> nodes){
		if (nodes == null) return;
		for (AttributeTreeNode n : nodes){
			n.getNames().size();
			loadTreeNodes(n.getActiveChildren());
		}
	}
	private static CmAttributeOption cloneOption(CmAttributeOption op) {
		CmAttributeOption cloned = new CmAttributeOption();
		cloned.setBooleanValue(op.getBooleanValue());
		cloned.setDoubleValue(op.getDoubleValue());
		cloned.setOptionId(op.getOptionId());
		cloned.setStringValue(op.getStringValue());
		cloned.setUuidValue(op.getUuidValue());
		return cloned;
	}
	
	private static List<CmAttributeListItem> cloneCmAttributeList(List<CmAttributeListItem> srcList, ConfigurableModel cm, CmAttribute cmAttribute) {
		List<CmAttributeListItem> clonedList = new ArrayList<CmAttributeListItem>();
		for (CmAttributeListItem listItem : srcList) {
			CmAttributeListItem clone = new CmAttributeListItem();
			clone.setConfigurableModel(cm);
			copyLabels(listItem, clone);
			clone.setIsActive(listItem.getIsActive());
			clone.setListOrder(listItem.getListOrder());
			clone.setListItem(listItem.getListItem());
			clone.setDmAttribute(listItem.getDmAttribute());
			clone.setAttribute(cmAttribute);
			clonedList.add(clone);
		}
		return clonedList;
	}

	private static List<CmAttributeTreeNode> cloneCmAttributeTree(List<CmAttributeTreeNode> srcTree, CmAttributeTreeNode parent, ConfigurableModel cm, CmAttribute cmAttribute) {
		List<CmAttributeTreeNode> clonedTree = new ArrayList<CmAttributeTreeNode>();
		for (CmAttributeTreeNode treeItem : srcTree) {
			CmAttributeTreeNode clone = new CmAttributeTreeNode();
			clone.setConfigurableModel(cm);
			copyLabels(treeItem, clone);
			clone.setIsActive(treeItem.getIsActive());
			clone.setNodeOrder(treeItem.getNodeOrder());
			clone.setDmTreeNode(treeItem.getDmTreeNode());
			clone.setDmAttribute(treeItem.getDmAttribute());
			clone.setAttribute(cmAttribute);
			clone.setParent(parent);
			clone.setChildren(cloneCmAttributeTree(treeItem.getChildren(), clone, cm, cmAttribute));
			clonedTree.add(clone);
		}
		return clonedTree;
	}
	
}
