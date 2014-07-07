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
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.dataentry.internal.CmAttributeOptionFactory;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
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
	 * Creates an empty configurable model with the default name.
	 * 
	 * @return
	 */
	public static ConfigurableModel createBlankModel(){
		return createBlankModel(Messages.ConfigurableModelPropertyDialog_ConfigurableModelDeafultName);
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
		List<Category> toProcess = new ArrayList<Category>();
		toProcess.addAll(dm.getActiveCategories());
		while(toProcess.size() > 0){
			Category c = toProcess.remove(0);
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
}
